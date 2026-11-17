package org.jibe77.hermanas.security;

import org.jibe77.hermanas.security.ratelimit.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

/**
 * Pre-authentication checks run by {@code DaoAuthenticationProvider} right after the
 * password match and before the authentication is accepted.
 *
 * <h3>Pending validation</h3>
 * <p>Refuses to authenticate users whose role is
 * {@link SecurityConfig#ROLE_NOT_VALIDATED_YET}. Without this Spring would happily create a
 * session for the user even though every authorization rule then denies them.</p>
 *
 * <h3>Account lockout (AUTH-VULN-03)</h3>
 * <p>Refuses a login while the account is inside its progressive lockout window. The
 * rejection is a plain {@link BadCredentialsException} — deliberately <em>not</em> a
 * distinguishable one: the answer an unauthenticated caller gets must stay the same for
 * "no such user", "wrong password", "pending validation" and "locked", otherwise this
 * filter rebuilds the enumeration oracle AUTH-VULN-02 just closed. The operator sees the
 * lockout as a {@code LOGIN_LOCKED_OUT} row on the admin-only auth journal.</p>
 */
@Component
public class PendingValidationUserDetailsChecker implements UserDetailsChecker {

    /** Code surfaced in the JSON error body so the SPA can show a tailored message. */
    public static final String PENDING_VALIDATION_MESSAGE = "ACCOUNT_PENDING_VALIDATION";

    private static final Logger logger = LoggerFactory.getLogger(PendingValidationUserDetailsChecker.class);

    private final LoginAttemptService loginAttemptService;

    public PendingValidationUserDetailsChecker(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void check(UserDetails user) {
        // Lockout first: a locked account must not leak that its password was even checked.
        if (loginAttemptService.isLocked(user.getUsername())) {
            logger.warn("Refusing a login for the locked account '{}'.", user.getUsername());
            throw new BadCredentialsException("Invalid username or password.");
        }
        boolean pending = user.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + SecurityConfig.ROLE_NOT_VALIDATED_YET).equals(a.getAuthority()));
        if (pending) {
            throw new DisabledException(PENDING_VALIDATION_MESSAGE);
        }
    }
}
