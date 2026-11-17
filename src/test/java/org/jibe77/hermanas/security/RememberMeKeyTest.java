package org.jibe77.hermanas.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CRYPTO-001 : la constante connue du dépôt ne doit jamais finir dans les services
 * remember-me, et une clé explicitement configurée ne doit pas être remplacée.
 */
class RememberMeKeyTest {

    @Test
    void configuredValueIsUsedVerbatim() {
        assertThat(new RememberMeKey("  ma-cle-d-exploitant  ").value()).isEqualTo("ma-cle-d-exploitant");
    }

    @Test
    void blankOrNullConfiguredNeverReachesSpring() {
        for (String configured : Arrays.asList("", "   ", null, RememberMeKey.KNOWN_DEFAULT)) {
            String value = new RememberMeKey(configured).value();
            assertThat(value)
                    .as("configurée à %s", configured)
                    .isNotBlank()
                    .isNotEqualTo(RememberMeKey.KNOWN_DEFAULT);
        }
    }

    /** Deux démarrages ne doivent pas partager la même clé générée. */
    @Test
    void generatedKeysDifferAndAreLongEnough() {
        String first = new RememberMeKey("").value();
        String second = new RememberMeKey("").value();

        assertThat(first).isNotEqualTo(second);
        // 32 octets en base64 URL-safe sans bourrage : au moins 43 caractères.
        assertThat(first).hasSizeGreaterThanOrEqualTo(43);
    }
}
