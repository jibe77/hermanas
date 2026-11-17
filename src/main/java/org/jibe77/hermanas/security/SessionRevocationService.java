package org.jibe77.hermanas.security;

import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;

/**
 * Revokes everything the application has already handed out for an account
 * (AUTH-VULN-01).
 *
 * <p>Until now nothing the victim could do had any effect on an attacker who had already
 * stolen the session: a password change re-hashed the row and stopped there. Three things
 * had to change together, and this service is where they happen:</p>
 * <ol>
 *   <li><b>HTTP sessions</b> — the {@code SPRING_SESSION} rows for that principal are
 *       deleted. {@code spring-session-jdbc} loads the session from the database on every
 *       request, so the very next request from the stolen cookie is a brand-new anonymous
 *       session. Deleting the child rows first keeps the FK happy on MariaDB.</li>
 *   <li><b>Remember-me tokens</b> — {@code persistent_logins} rows for the login are
 *       removed, so the 31-day cookie cannot re-authenticate either.</li>
 *   <li><b>The session epoch</b> — {@code hermanas_user.session_epoch} is bumped, which is
 *       what {@link SessionEpochFilter} checks on every subsequent request. It is the
 *       backstop for the case where a session was created through a path this service was
 *       not told about.</li>
 * </ol>
 */
@Service
public class SessionRevocationService {

    private static final Logger logger = LoggerFactory.getLogger(SessionRevocationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final PersistentTokenRepository persistentTokenRepository;
    private final HermanasUserRepository userRepository;
    private final org.jibe77.hermanas.service.event.EventService eventService;

    public SessionRevocationService(JdbcTemplate jdbcTemplate,
                                    PersistentTokenRepository persistentTokenRepository,
                                    HermanasUserRepository userRepository,
                                    org.jibe77.hermanas.service.event.EventService eventService) {
        this.jdbcTemplate = jdbcTemplate;
        this.persistentTokenRepository = persistentTokenRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    /**
     * Bumps the account's session epoch and drops its live sessions and remember-me tokens.
     * Called on password change, role change and account deletion.
     *
     * @param login the account whose previously-issued credentials must stop working
     * @param reason short text journaled next to the change that triggered it
     */
    public void revokeAllFor(String login, String reason) {
        if (login == null || login.isBlank()) {
            return;
        }
        bumpEpoch(login);
        int sessions = deleteSessions(login);
        removeRememberMeTokens(login);
        logger.info("Revoked {} live session(s) and the remember-me tokens of '{}' ({}).",
                sessions, login, reason);
        if (eventService != null) {
            eventService.record(EventType.USER_UPDATED,
                    "sessions_revoked login=" + login + " reason=" + reason);
        }
    }

    /** {@code session_epoch + 1} for one login; a no-op when the row is already gone. */
    private void bumpEpoch(String login) {
        try {
            userRepository.findByLogin(login).ifPresent(user -> {
                user.setSessionEpoch(user.getSessionEpoch() + 1);
                userRepository.save(user);
            });
        } catch (DataAccessException e) {
            logger.warn("Could not bump the session epoch of '{}'.", login, e);
        }
    }

    /**
     * Deletes the spring-session rows whose {@code PRINCIPAL_NAME} is this login.
     *
     * @return how many sessions were dropped
     */
    private int deleteSessions(String login) {
        try {
            // Attributes first: SPRING_SESSION_ATTRIBUTES has a foreign key onto
            // SPRING_SESSION(PRIMARY_ID) and MariaDB will refuse the parent delete otherwise.
            jdbcTemplate.update(
                    "DELETE FROM SPRING_SESSION_ATTRIBUTES WHERE SESSION_PRIMARY_ID IN "
                            + "(SELECT PRIMARY_ID FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?)",
                    login);
            return jdbcTemplate.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", login);
        } catch (DataAccessException e) {
            // Expected on a slice that has no spring-session schema (H2 unit tests).
            logger.warn("Could not evict the HTTP sessions of '{}': {}", login, e.getMessage());
            return 0;
        }
    }

    private void removeRememberMeTokens(String login) {
        try {
            persistentTokenRepository.removeUserTokens(login);
        } catch (DataAccessException e) {
            logger.warn("Could not remove the remember-me tokens of '{}': {}", login, e.getMessage());
        }
    }

    /**
     * Convenience for the deletion path, where the row is already gone by the time we get
     * here and only the epoch bump can be skipped.
     */
    public void revokeAllFor(HermanasUser user, String reason) {
        if (user == null) {
            return;
        }
        revokeAllFor(user.getLogin(), reason);
    }
}
