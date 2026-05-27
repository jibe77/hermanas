package org.jibe77.hermanas.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

/**
 * Pre-authentication check that refuses to authenticate users whose role is
 * {@link SecurityConfig#ROLE_NOT_VALIDATED_YET}. Spring Security's
 * {@code DaoAuthenticationProvider} runs this checker just after the password match — throwing
 * here turns into a 401 with the standard {@link DisabledException} message, which the frontend
 * picks up and translates into "Compte en attente de validation".
 */
@Component
public class PendingValidationUserDetailsChecker implements UserDetailsChecker {

    /** Code surfaced in the JSON error body so the SPA can show a tailored message. */
    public static final String PENDING_VALIDATION_MESSAGE = "ACCOUNT_PENDING_VALIDATION";

    @Override
    public void check(UserDetails user) {
        boolean pending = user.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + SecurityConfig.ROLE_NOT_VALIDATED_YET).equals(a.getAuthority()));
        if (pending) {
            throw new DisabledException(PENDING_VALIDATION_MESSAGE);
        }
    }
}
