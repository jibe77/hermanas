package org.jibe77.hermanas.security.ratelimit;

import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.service.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Account-side defence against password guessing (AUTH-VULN-03).
 *
 * <p>The only friction the target offered was {@link LoginRateLimitFilter}: one in-memory
 * bucket keyed on the source IP. That leaves a named, privilege-tagged account — the
 * journal hands out the names and roles — with zero account-side cost, and the counters
 * die with the JVM. The public journal showed 959 {@code STARTUP} events, several per day
 * recently, so the IP budget refilled on the operator's own restarts. That is how the
 * administrator credential fell on attempt #336 while the account itself was never locked,
 * never challenged, never slowed.</p>
 *
 * <p>This service puts a failed-attempt counter and a lockout deadline on the account row,
 * so they survive a restart and follow the account rather than the source address. The
 * backoff is progressive: the first {@code threshold} failures cost nothing, then the lock
 * doubles each time up to a ceiling. A successful login clears both.</p>
 *
 * <h3>What is deliberately not disclosed</h3>
 * <p>A locked account answers with the same generic 401 as a wrong password. Telling an
 * unauthenticated caller "this account exists and is currently locked" would rebuild the
 * enumeration oracle AUTH-VULN-02 just closed. Administrators see the lockout as a
 * {@code LOGIN_LOCKED_OUT} event on the admin-only auth journal instead.</p>
 *
 * <h3>Not implemented</h3>
 * <p>No CAPTCHA behind the first ten failures — that needs a shared secret between the SPA
 * and the backend and a rendering decision on a headless API. The progressive backoff is
 * what an attacker actually meets before any challenge would have appeared.</p>
 */
@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    /** Failures tolerated before the first lockout kicks in. */
    @Value("${hermanas.security.login-lockout.failure-threshold:5}")
    private int failureThreshold = 5;

    /** Duration of the first lockout; it doubles on each subsequent trip. */
    @Value("${hermanas.security.login-lockout.initial-lockout:1m}")
    private Duration initialLockout = Duration.ofMinutes(1);

    /** Ceiling for the progressive backoff. */
    @Value("${hermanas.security.login-lockout.max-lockout:1h}")
    private Duration maxLockout = Duration.ofHours(1);

    private final HermanasUserRepository userRepository;
    private final EventService eventService;

    public LoginAttemptService(HermanasUserRepository userRepository,
                               org.jibe77.hermanas.service.event.EventService eventService) {
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    /**
     * True when {@code login} exists and is currently inside a lockout window. Unknown
     * logins are never locked — the caller must not be able to tell the two apart.
     */
    public boolean isLocked(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        return userRepository.findByLogin(login)
                .map(user -> user.getLockedUntil() != null
                        && user.getLockedUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * Counts one failed attempt against {@code login} and applies the backoff when the
     * threshold is crossed. Silently does nothing for an unknown login: the generic 401 must
     * stay generic, and there is no row to write to.
     */
    public void recordFailure(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        userRepository.findByLogin(login).ifPresent(user -> {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= failureThreshold) {
                int trips = 1 + ((attempts - failureThreshold) / failureThreshold);
                Duration lock = backoffFor(trips);
                user.setLockedUntil(LocalDateTime.now().plus(lock));
                logger.warn("Account '{}' locked until {} after {} consecutive failed logins.",
                        user.getLogin(), user.getLockedUntil(), attempts);
                if (eventService != null) {
                    eventService.recordAuto(EventType.LOGIN_LOCKED_OUT,
                            "login=" + user.getLogin() + " attempts=" + attempts
                                    + " locked_until=" + user.getLockedUntil());
                }
            }
            userRepository.save(user);
        });
    }

    /** Clears the counter and any lock after a successful authentication. */
    public void recordSuccess(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        userRepository.findByLogin(login)
                .filter(user -> user.getFailedAttempts() != 0 || user.getLockedUntil() != null)
                .ifPresent(user -> {
                    user.setFailedAttempts(0);
                    user.setLockedUntil(null);
                    userRepository.save(user);
                });
    }

    /** How long a given account is locked right now, or null when it is not locked. */
    public Duration remainingLockout(String login) {
        return userRepository.findByLogin(login)
                .map(HermanasUser::getLockedUntil)
                .filter(until -> until.isAfter(LocalDateTime.now()))
                .map(until -> Duration.between(LocalDateTime.now(), until))
                .orElse(null);
    }

    /** {@code initial × 2^(trips-1)}, capped at {@code max}. */
    private Duration backoffFor(int trips) {
        long millis = initialLockout.toMillis();
        for (int i = 1; i < trips && millis < maxLockout.toMillis(); i++) {
            millis = Math.min(millis * 2, maxLockout.toMillis());
        }
        return Duration.ofMillis(Math.min(millis, maxLockout.toMillis()));
    }
}
