package org.jibe77.hermanas.security;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private final MessageSource messageSource;

    public RegistrationService(HermanasUserRepository repository,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService,
                               MessageSource messageSource) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.messageSource = messageSource;
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

        String language = normalizeLanguage(body.getLanguage());
        HermanasUser user = new HermanasUser(
                login,
                passwordEncoder.encode(password),
                email,
                SecurityConfig.ROLE_NOT_VALIDATED_YET,
                false,
                language);
        repository.save(user);
        logger.info("New registration: '{}' (pending validation, lang={}).", login, language);

        sendConfirmationToUser(user);
        sendNotificationToAdmins(user);

        return user;
    }

    /**
     * Normalises the requested language to one of the supported codes ("fr", "en", "ro").
     * Falls back to {@code "fr"} when the value is missing or unrecognised — keeps the
     * field forgiving on the API while ensuring we never persist garbage.
     */
    private static String normalizeLanguage(String raw) {
        if (raw == null) {
            return "fr";
        }
        String code = raw.trim().toLowerCase();
        if (code.startsWith("en")) {
            return "en";
        }
        if (code.startsWith("ro")) {
            return "ro";
        }
        return "fr";
    }

    private void sendConfirmationToUser(HermanasUser user) {
        Locale locale = Locale.forLanguageTag(user.getLanguage());
        String subject = messageSource.getMessage("register.user.subject", null, locale);
        String body = messageSource.getMessage(
                "register.user.body",
                new Object[]{escapeHtml(user.getLogin())},
                locale);
        try {
            emailService.sendMailTo(Collections.singletonList(user.getEmail()), subject, body);
        } catch (Exception e) {
            // never let a failed mail block account creation — the admin will be alerted via DB.
            logger.warn("Failed to send confirmation mail to '{}'.", user.getEmail(), e);
        }
    }

    private void sendNotificationToAdmins(HermanasUser pendingUser) {
        // Group admins by their preferred language so we render the template once per
        // language and not once per recipient. Admins without an email are filtered
        // out — the registration is still persisted, the admin will discover it via UI.
        List<HermanasUser> admins = repository.findByRole(SecurityConfig.ROLE_ADMIN).stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().trim().isEmpty())
                .collect(Collectors.toList());
        if (admins.isEmpty()) {
            logger.warn("No administrator email available — pending account '{}' will need manual "
                    + "discovery by an admin.", pendingUser.getLogin());
            return;
        }
        // Bucket each admin into one of the three supported languages — everything
        // unrecognised lands in the French bucket (historical default).
        java.util.Map<String, java.util.List<String>> byLang = new java.util.LinkedHashMap<>();
        byLang.put("fr", new java.util.ArrayList<>());
        byLang.put("en", new java.util.ArrayList<>());
        byLang.put("ro", new java.util.ArrayList<>());
        for (HermanasUser admin : admins) {
            String lang = admin.getLanguage();
            if (!byLang.containsKey(lang)) {
                lang = "fr";
            }
            byLang.get(lang).add(admin.getEmail());
        }
        byLang.forEach((lang, recipients) ->
                sendPendingNotification(recipients, pendingUser, Locale.forLanguageTag(lang)));
    }

    private void sendPendingNotification(List<String> recipients, HermanasUser pendingUser, Locale locale) {
        if (recipients.isEmpty()) {
            return;
        }
        String subject = messageSource.getMessage(
                "register.admin.subject",
                new Object[]{pendingUser.getLogin()},
                locale);
        String body = messageSource.getMessage(
                "register.admin.body",
                new Object[]{escapeHtml(pendingUser.getLogin()), escapeHtml(pendingUser.getEmail())},
                locale);
        try {
            emailService.sendMailTo(recipients, subject, body);
        } catch (Exception e) {
            logger.warn("Failed to notify admins about pending account '{}' (locale={}).",
                    pendingUser.getLogin(), locale, e);
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
