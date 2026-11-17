package org.jibe77.hermanas.security;

import org.jibe77.hermanas.data.entity.Resident;
import org.jibe77.hermanas.data.repository.ResidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single source of truth for credential strength (AUTH-VULN-04).
 *
 * <p>Before this class existed, each write path invented its own rule — or none at all:
 * {@code RegistrationService} asked for 6 characters, {@code UserRestController.create}
 * accepted any non-empty string for an {@code ROLE_ADMIN} account, and
 * {@code UserRestController.applyUpdate} had no length floor whatsoever. That is how the
 * primary administrator ended up protected by a six-character dictionary word that was
 * also the name of one of the operator's own hens.</p>
 *
 * <h3>The rules</h3>
 * <ol>
 *   <li><b>Minimum length</b> — {@value #DEFAULT_MIN_LENGTH} characters by default,
 *       overridable with {@code hermanas.security.password.min-length}. Length is the
 *       dominant factor against offline cracking, so a long passphrase is preferred over
 *       "8 characters including a symbol".</li>
 *   <li><b>Maximum 72 bytes</b> — BCrypt silently truncates its input at 72 bytes, so a
 *       longer secret is weaker than it looks and two different long passphrases sharing a
 *       72-byte prefix would authenticate identically. We reject instead of misleading.</li>
 *   <li><b>Breached-password denylist</b> — a small offline list shipped in
 *       {@code /security/common-passwords.txt}, optionally extended from disk with
 *       {@code hermanas.security.password.denylist-file} (point it at a Pwned Passwords
 *       export to get the full corpus). A denylist is the only defence that catches
 *       "correct length, zero entropy".</li>
 *   <li><b>No value from the application's own data</b> — the login, the e-mail local part
 *       and every resident name are rejected, because the target publishes the residents
 *       roster and the account journal, which is exactly the corpus a spray attack is built
 *       from.</li>
 *   <li><b>At least two character classes</b> — a deliberately light rule (letters-only is
 *       rejected, "correct horse battery staple" passes). We do not force punctuation:
 *       composition rules buy little against a wordlist and cost a lot in reuse.</li>
 * </ol>
 *
 * <h3>Not covered here</h3>
 * <p>Password <em>reuse history</em> is not implemented — it needs a per-user hash history
 * table and a migration. See the commit that introduced this class for the residual list.</p>
 */
@Component
public class PasswordPolicy {

    /** Error code carried by {@link WeakPasswordException}, stable for i18n on the SPA. */
    public static final String CODE_TOO_SHORT = "PASSWORD_TOO_SHORT";
    public static final String CODE_TOO_LONG = "PASSWORD_TOO_LONG";
    public static final String CODE_BREACHED = "PASSWORD_TOO_COMMON";
    public static final String CODE_SELF_REFERENCE = "PASSWORD_FROM_OWN_DATA";
    public static final String CODE_TOO_PLAIN = "PASSWORD_TOO_SIMPLE";
    public static final String CODE_MISSING = "PASSWORD_MISSING";

    /** BCrypt ignores everything past this many bytes, so longer is a lie. */
    static final int MAX_BCRYPT_BYTES = 72;

    static final int DEFAULT_MIN_LENGTH = 12;

    private static final Logger logger = LoggerFactory.getLogger(PasswordPolicy.class);
    private static final String BUNDLED_DENYLIST = "/security/common-passwords.txt";

    /** Words that belong to the product itself and must never be a credential. */
    private static final Set<String> PRODUCT_WORDS = Set.of("hermanas", "poulailler", "coop", "chicken");

    private final ResidentRepository residentRepository;

    @Value("${hermanas.security.password.min-length:" + DEFAULT_MIN_LENGTH + "}")
    private int minLength = DEFAULT_MIN_LENGTH;

    /** Optional operator-supplied denylist (one password per line), merged with the bundled one. */
    @Value("${hermanas.security.password.denylist-file:}")
    private String externalDenylistFile = "";

    private Set<String> denylist = Collections.emptySet();

    @Autowired(required = false)
    public PasswordPolicy(ResidentRepository residentRepository) {
        this.residentRepository = residentRepository;
    }

    @PostConstruct
    void loadDenylist() {
        Set<String> words = new HashSet<>();
        try (InputStream in = PasswordPolicy.class.getResourceAsStream(BUNDLED_DENYLIST)) {
            if (in == null) {
                logger.warn("Bundled password denylist {} is missing — only the external list applies.",
                        BUNDLED_DENYLIST);
            } else {
                readWords(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)), words);
            }
        } catch (IOException e) {
            logger.warn("Could not read the bundled password denylist.", e);
        }
        if (externalDenylistFile != null && !externalDenylistFile.isBlank()) {
            Path path = Paths.get(externalDenylistFile.trim());
            if (Files.isReadable(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    int before = words.size();
                    readWords(reader, words);
                    logger.info("Loaded {} extra breached passwords from {}.", words.size() - before, path);
                } catch (IOException e) {
                    logger.warn("Could not read the password denylist at {}.", path, e);
                }
            } else {
                logger.warn("hermanas.security.password.denylist-file points at {} which is not readable.", path);
            }
        }
        words.addAll(PRODUCT_WORDS);
        this.denylist = Collections.unmodifiableSet(words);
        logger.info("Password policy active: min length {}, denylist of {} entries, 72-byte BCrypt cap.",
                minLength, denylist.size());
    }

    private static void readWords(BufferedReader reader, Set<String> sink) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String word = line.trim().toLowerCase(Locale.ROOT);
            if (word.isEmpty() || word.startsWith("#")) {
                continue;
            }
            sink.add(word);
        }
    }

    /**
     * Validates a candidate password against every rule above.
     *
     * @param password      the raw, un-encoded candidate
     * @param contextValues identity strings the password must not be built from — typically
     *                      the login and the e-mail local part. May be {@code null}.
     * @throws WeakPasswordException with a stable {@link WeakPasswordException#getCode()}
     */
    public void validate(String password, List<String> contextValues) {
        if (password == null || password.isEmpty()) {
            throw new WeakPasswordException(CODE_MISSING, "A password is required.");
        }
        if (password.length() < minLength) {
            throw new WeakPasswordException(CODE_TOO_SHORT,
                    "The password must be at least " + minLength + " characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new WeakPasswordException(CODE_TOO_LONG,
                    "The password must not exceed " + MAX_BCRYPT_BYTES
                            + " bytes; BCrypt silently ignores anything longer.");
        }
        String normalized = normalize(password);
        if (denylist.contains(normalized)) {
            throw new WeakPasswordException(CODE_BREACHED,
                    "That password is on the list of known breached passwords.");
        }
        for (String forbidden : forbiddenContextValues(contextValues)) {
            if (!forbidden.isEmpty() && normalized.contains(forbidden)) {
                throw new WeakPasswordException(CODE_SELF_REFERENCE,
                        "The password must not contain the account login, the e-mail address "
                                + "or the name of a resident.");
            }
        }
        if (characterClasses(password) < 2) {
            throw new WeakPasswordException(CODE_TOO_PLAIN,
                    "The password must mix at least two of: lower-case letters, upper-case "
                            + "letters, digits, punctuation.");
        }
    }

    /**
     * Convenience overload for callers with no identity context to pass.
     */
    public void validate(String password) {
        validate(password, null);
    }

    /**
     * The strings a password must not contain: the caller-supplied identity values plus
     * every resident name currently in the database. Resident names are the reason this
     * check exists — the roster is published on the public dashboard, so it is the first
     * thing an attacker builds a wordlist from.
     */
    private Set<String> forbiddenContextValues(List<String> contextValues) {
        Set<String> forbidden = new HashSet<>();
        if (contextValues != null) {
            for (String value : contextValues) {
                if (value == null) {
                    continue;
                }
                String normalized = normalize(value);
                if (normalized.length() >= 3) {
                    forbidden.add(normalized);
                }
                // "first.last@domain" must also reject "first" and "last" on their own.
                int at = value.indexOf('@');
                if (at > 0) {
                    for (String part : value.substring(0, at).split("[._+-]")) {
                        String partNorm = normalize(part);
                        if (partNorm.length() >= 3) {
                            forbidden.add(partNorm);
                        }
                    }
                }
            }
        }
        if (residentRepository != null) {
            try {
                List<Resident> residents = residentRepository.findAll();
                for (Resident resident : residents) {
                    String name = normalize(resident.getName());
                    if (name.length() >= 3) {
                        forbidden.add(name);
                    }
                }
            } catch (RuntimeException e) {
                // Never let a lookup failure turn into a 500 on the registration form:
                // the other rules still apply.
                logger.warn("Could not load resident names for the password check; "
                        + "that rule was skipped for this request.", e);
            }
        }
        return forbidden;
    }

    /** Lower-case, no whitespace, no separators — so "B a t m a n" cannot slip through. */
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\s._\\-+]", "");
    }

    private static int characterClasses(String password) {
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean other = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                other = true;
            }
        }
        int classes = 0;
        if (lower) classes++;
        if (upper) classes++;
        if (digit) classes++;
        if (other) classes++;
        return classes;
    }

    /** Thrown by {@link #validate}; carries a stable code so the SPA can translate it. */
    public static class WeakPasswordException extends RuntimeException {
        private final String code;

        public WeakPasswordException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
