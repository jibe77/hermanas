package org.jibe77.hermanas.service.config;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The single registry of configuration keys whose value is a secret, and the one way to
 * render such a value for a log line (CRYPTO-006, CRYPTO-007).
 *
 * <p>Two separate defects grew out of the same missing abstraction. First, the redaction
 * that existed was a substring heuristic — {@code contains("password")}, {@code
 * endsWith(".key")} — applied <em>only</em> to the copy pushed into the event journal. The
 * root logger and the {@code CONFIG_AUDIT} file received the raw value for every key,
 * including {@code spring.mail.password}, {@code weather.info.key} and
 * {@code ai.inference.api-key}, directly contradicting the code comment claiming the audit
 * file got the redacted form too. Second, the heuristic missed hyphen-delimited names
 * entirely, so {@code ai.inference.api-key} and {@code hermanas.push.vapid.private-key}
 * fell outside it — a latent leak waiting for someone to add an HTTP setter for either
 * (CRYPTO-007).</p>
 *
 * <h3>Two layers, deliberately</h3>
 * <ul>
 *   <li><b>An explicit registry</b> of every key known to hold a secret. Exact names, so a
 *       key renamed to something innocent cannot silently escape the mask.</li>
 *   <li><b>The old substring heuristic, kept as a fallback.</b> Deny-by-default over every
 *       key would make the configuration audit unreadable, so the heuristic is what catches
 *       a <em>future</em> {@code foo.bar.api-key} before anyone adds it to the registry.</li>
 * </ul>
 *
 * <p>Static on purpose: Logback instantiates its filter outside the Spring context, and a
 * log path must never depend on a bean being wired.</p>
 */
public final class SecretConfigKeys {

    /** Placeholder written instead of the value. Length is kept: it distinguishes "set"
     * from "empty" and catches a paste-the-wrong-key mistake without revealing the key. */
    public static final String MASK_FORMAT = "<set, len=%d>";

    private static final Set<String> SECRET_KEYS = new LinkedHashSet<>();

    static {
        // SMTP
        SECRET_KEYS.add("spring.mail.password");
        SECRET_KEYS.add("spring.mail.username");
        SECRET_KEYS.add("email.notification.from");
        // Provider credentials
        SECRET_KEYS.add("weather.info.key");
        SECRET_KEYS.add("ai.inference.api-key");
        // Web Push identity
        SECRET_KEYS.add("hermanas.push.vapid.private-key");
        // Application secrets
        SECRET_KEYS.add("hermanas.security.remember-me-key");
        SECRET_KEYS.add("hermanas.security.siri-token");
        // Physical location of the coop — not a credential, but private (the weather
        // request leaks it anyway, which is CRYPTO-014).
        SECRET_KEYS.add("suntime.latitude");
        SECRET_KEYS.add("suntime.longitude");
    }

    private SecretConfigKeys() {
    }

    /** Read-only view of the registry, for tests and diagnostics. */
    public static Set<String> registry() {
        return Collections.unmodifiableSet(SECRET_KEYS);
    }

    /** Registers an extra secret key at runtime — used by tests and by future modules. */
    public static void register(String key) {
        if (key != null && !key.isBlank()) {
            SECRET_KEYS.add(key.trim().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * True when {@code key} holds something that must never be written to a log, an audit
     * file or the public event journal.
     */
    public static boolean isSecret(String key) {
        if (key == null) {
            return false;
        }
        String k = key.trim().toLowerCase(Locale.ROOT);
        if (SECRET_KEYS.contains(k)) {
            return true;
        }
        // Fallback heuristic, kept so an unregistered future key still gets masked.
        return k.contains("password") || k.contains("passwd") || k.contains("secret")
                || k.contains("token") || k.contains("credential")
                || k.endsWith(".key") || k.contains(".key.")
                || k.endsWith("-key") || k.contains("-key-")
                || k.endsWith("-secret") || k.contains("-secret-")
                || k.endsWith("-token") || k.contains("-token-")
                || k.endsWith("-password") || k.contains("-password-")
                || k.contains("apikey") || k.contains("appkey")
                || k.equals("suntime.latitude") || k.equals("suntime.longitude");
    }

    /**
     * Renders a configuration value for a log line: a masked placeholder for secret keys,
     * the value untouched otherwise.
     */
    public static String render(String key, Object value) {
        if (!isSecret(key)) {
            return String.valueOf(value);
        }
        if (value == null) {
            return "<unset>";
        }
        int length = value.toString().getBytes(StandardCharsets.UTF_8).length;
        return length == 0 ? "<empty>" : String.format(MASK_FORMAT, length);
    }
}
