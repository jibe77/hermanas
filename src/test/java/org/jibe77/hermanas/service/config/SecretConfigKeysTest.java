package org.jibe77.hermanas.service.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The secret-key registry and its masking (CRYPTO-006, CRYPTO-007).
 *
 * <p>The heuristic that used to do this job matched {@code "password"} / {@code ".key"}
 * and missed every hyphen-delimited name, so {@code ai.inference.api-key} and
 * {@code hermanas.push.vapid.private-key} were outside it — and neither line was masked in
 * the first place, only in the journal copy.</p>
 */
class SecretConfigKeysTest {

    @Test
    void registryCoversEveryShippedSecret() {
        for (String key : new String[]{
                "spring.mail.password",
                "weather.info.key",
                "ai.inference.api-key",
                "hermanas.push.vapid.private-key",
                "hermanas.security.remember-me-key",
                "hermanas.security.siri-token"}) {
            assertTrue(SecretConfigKeys.isSecret(key), key + " must be registered as a secret");
        }
    }

    @Test
    void hyphenDelimitedNamesAreNotMissedAnymore() {
        // The exact gap CRYPTO-007 described.
        assertTrue(SecretConfigKeys.isSecret("some.future.api-key"));
        assertTrue(SecretConfigKeys.isSecret("some.future.private-key"));
        assertTrue(SecretConfigKeys.isSecret("some.future.client-secret"));
        assertTrue(SecretConfigKeys.isSecret("some.future.access-token"));
    }

    @Test
    void harmlessKeysStayReadableInTheAuditLog() {
        assertFalse(SecretConfigKeys.isSecret("door.opening.duration"));
        assertFalse(SecretConfigKeys.isSecret("light.security.timer.delay.eco"));
        assertFalse(SecretConfigKeys.isSecret("weather.info.enabled"));
        assertFalse(SecretConfigKeys.isSecret("camera.rotation"));
    }

    @Test
    void renderMasksSecretsButNotOrdinaryValues() {
        assertEquals("10000", SecretConfigKeys.render("door.opening.duration", 10000));
        assertEquals("<set, len=14>", SecretConfigKeys.render("spring.mail.password", "hunter2hunter2"));
        assertEquals("<unset>", SecretConfigKeys.render("weather.info.key", null));
        assertEquals("<empty>", SecretConfigKeys.render("weather.info.key", ""));
    }

    @Test
    void maskNeverLeaksAPrefixOfTheValue() {
        String secret = "ghp_abcdefghijklmnopqrstuvwxyz0123456789";
        String masked = SecretConfigKeys.render("ai.inference.api-key", secret);

        assertFalse(masked.contains("ghp"), masked);
        assertTrue(masked.startsWith("<set"), masked);
    }
}
