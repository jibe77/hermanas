package org.jibe77.hermanas;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void messageSource() {
        MessageSource messageSource = new AppConfig().messageSource();
        // Pin the locale explicitly — Locale.getDefault() depends on the developer machine
        // (FR on most of ours) and would resolve to messages_fr.properties since the French
        // bundle was introduced.
        // Title now carries a {0} host placeholder (see ApplicationStatusListener.restartArgs).
        // Pass a fixed host so the assertion stays deterministic across machines.
        Object[] args = {"poulailler", "2026-01-01 00:00:00", "2026-01-01 00:00:00", "0 min"};
        assertEquals("Hermanas : unexpected restart on poulailler",
                messageSource.getMessage(
                        "restarted.incorrectly.title", args, Locale.ENGLISH));
        assertEquals("Hermanas : redémarrage inattendu sur poulailler",
                messageSource.getMessage(
                        "restarted.incorrectly.title", args, Locale.FRENCH));
    }
}