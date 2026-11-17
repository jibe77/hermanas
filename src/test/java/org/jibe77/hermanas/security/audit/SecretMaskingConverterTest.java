package org.jibe77.hermanas.security.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CRYPTO-006 : ce que l'appendeur d'audit écrit sur le disque ne doit pas contenir le
 * secret qui y est passé.
 */
class SecretMaskingConverterTest {

    @Test
    void masksKeyEqualsValuePairs() {
        assertThat(SecretMaskingConverter.mask("set mail.password = hunter2 (by admin)"))
                .isEqualTo("set mail.password = *** (by admin)");
        assertThat(SecretMaskingConverter.mask(
                "Access denied for user 'hermanas'@'pru', password: S3cr3t"))
                .isEqualTo("Access denied for user 'hermanas'@'pru', password: ***");
    }

    @Test
    void masksTheKnownKeyNamesCaseInsensitively() {
        assertThat(SecretMaskingConverter.mask("API_KEY=AZERTY123")).isEqualTo("API_KEY=***");
        assertThat(SecretMaskingConverter.mask("remember-me token : abc.def"))
                .isEqualTo("remember-me token : ***");
        assertThat(SecretMaskingConverter.mask("smtp appid=0123456789"))
                .isEqualTo("smtp appid=***");
    }

    /** Une ligne d'audit courante ne doit pas être abîmée. */
    @Test
    void leavesOrdinaryLinesAlone() {
        String line = "2026-11-17 door.open by marguerite from 192.168.1.10 status=OK";
        assertThat(SecretMaskingConverter.mask(line)).isEqualTo(line);
        assertThat(SecretMaskingConverter.mask(null)).isNull();
        assertThat(SecretMaskingConverter.mask("")).isEmpty();
    }

    /** Ce qui ressemble à un secret mais n'en est pas un est caviardé sans risque : mieux vaut trop que pas assez. */
    @Test
    void masksWhateverLooksLikeACredential() {
        assertThat(SecretMaskingConverter.mask("Password: hunter2"))
                .isEqualTo("Password: ***");
    }
}
