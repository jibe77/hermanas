package org.jibe77.hermanas.security;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Encapsulates the self-service registration flow. Performs validation, creates the account with
 * {@link SecurityConfig#ROLE_NOT_VALIDATED_YET} (no permissions) and triggers two notification
 * mails — one to the registrant for confirmation, one to every administrator so they can promote
 * the new account from the admin screen.
 */
@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern LOGIN_RE = Pattern.compile("^[A-Za-z0-9._-]{2,64}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final HermanasUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RegistrationService(HermanasUserRepository repository,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Validates the payload then persists a new pending account. Errors are reported via
     * {@link RegistrationException} with a stable code suitable for i18n on the frontend.
     */
    public HermanasUser register(RegisterRequest body) {
        if (body == null) {
            throw new RegistrationException("INVALID_PAYLOAD", "Empty payload");
        }
        String login = trimOrNull(body.getLogin());
        String password = body.getPassword();
        String email = trimOrNull(body.getEmail());

        if (login == null || !LOGIN_RE.matcher(login).matches()) {
            throw new RegistrationException("INVALID_LOGIN",
                    "Login must be 2-64 characters from [A-Za-z0-9._-]");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new RegistrationException("INVALID_PASSWORD",
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (email == null || !EMAIL_RE.matcher(email).matches()) {
            throw new RegistrationException("INVALID_EMAIL", "A valid email is required");
        }
        if (repository.existsByLogin(login)) {
            throw new RegistrationException("LOGIN_TAKEN", "This login is already taken");
        }

        HermanasUser user = new HermanasUser(
                login,
                passwordEncoder.encode(password),
                email,
                SecurityConfig.ROLE_NOT_VALIDATED_YET,
                false);
        repository.save(user);
        logger.info("New registration: '{}' (pending validation).", login);

        sendConfirmationToUser(user);
        sendNotificationToAdmins(user);

        return user;
    }

    private void sendConfirmationToUser(HermanasUser user) {
        String subject = "Hermanas — confirmation d'inscription";
        String body = "<p>Bonjour " + escapeHtml(user.getLogin()) + ",</p>"
                + "<p>Votre compte a bien été créé sur Hermanas. Il est actuellement <strong>en attente "
                + "de validation</strong> par un administrateur.</p>"
                + "<p>Vous recevrez une confirmation par email dès que votre compte sera activé. "
                + "En attendant, vous ne pouvez pas encore vous connecter.</p>"
                + "<p>— L'équipe Hermanas</p>";
        try {
            emailService.sendMailTo(Collections.singletonList(user.getEmail()), subject, body);
        } catch (Exception e) {
            // never let a failed mail block account creation — the admin will be alerted via DB.
            logger.warn("Failed to send confirmation mail to '{}'.", user.getEmail(), e);
        }
    }

    private void sendNotificationToAdmins(HermanasUser pendingUser) {
        List<String> adminEmails = repository.findByRole(SecurityConfig.ROLE_ADMIN).stream()
                .map(HermanasUser::getEmail)
                .filter(e -> e != null && !e.trim().isEmpty())
                .collect(Collectors.toList());
        if (adminEmails.isEmpty()) {
            logger.warn("No administrator email available — pending account '{}' will need manual "
                    + "discovery by an admin.", pendingUser.getLogin());
            return;
        }
        String subject = "Hermanas — nouvelle inscription en attente : " + pendingUser.getLogin();
        String body = "<p>Une nouvelle inscription est en attente de validation :</p>"
                + "<ul>"
                + "<li><strong>Login :</strong> " + escapeHtml(pendingUser.getLogin()) + "</li>"
                + "<li><strong>Email :</strong> " + escapeHtml(pendingUser.getEmail()) + "</li>"
                + "</ul>"
                + "<p>Connectez-vous à l'écran <em>Users</em> pour promouvoir ce compte en "
                + "<code>USER</code> ou <code>ADMIN</code>, ou supprimez-le s'il est illégitime.</p>";
        try {
            emailService.sendMailTo(adminEmails, subject, body);
        } catch (Exception e) {
            logger.warn("Failed to notify admins about pending account '{}'.", pendingUser.getLogin(), e);
        }
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Carries a stable error {@link #getCode()} for i18n + a human-readable message. */
    public static class RegistrationException extends RuntimeException {
        private final String code;

        public RegistrationException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
