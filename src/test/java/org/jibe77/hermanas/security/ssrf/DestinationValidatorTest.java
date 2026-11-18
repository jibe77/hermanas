package org.jibe77.hermanas.security.ssrf;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Règles du douan de destination, sans dépendance réseau : les hôtes testés sont soit des
 * adresses littérales, soit des hôtes explicitement approuvés (qui court-circuitent la
 * résolution DNS).
 */
class DestinationValidatorTest {

    private static final String WEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather?lat={latitude}&lon={longitude}&appid={key}";

    private DestinationValidator validator(boolean allowInsecureHttp) {
        DestinationValidator validator = new DestinationValidator();
        ReflectionTestUtils.setField(validator, "allowedHostsRaw", "api.openweathermap.org,trusted.lan");
        ReflectionTestUtils.setField(validator, "allowedPortsRaw", "25,80,443,8080,587");
        ReflectionTestUtils.setField(validator, "allowPrivateDestinations", false);
        ReflectionTestUtils.setField(validator, "allowInsecureHttp", allowInsecureHttp);
        validator.init();
        return validator;
    }

    private DestinationValidator validator() {
        // Cleartext globalement autorisé : c'est le réglage par défaut, et celui qui rend
        // les règles ci-dessous intéressantes.
        return validator(true);
    }

    @Test
    void httpsDestinationOfATrustedHostIsAccepted() {
        assertThatCode(() -> validator().validateHttpUrl(WEATHER_URL, DestinationValidator.Sink.WEATHER))
                .doesNotThrowAnyException();
    }

    /**
     * Régression : les accolades d'un gabarit d'URI-Template ne sont pas des caractères
     * légaux pour {@code java.net.URI}. Sans neutralisation, la seule URL météo par défaut
     * de l'application — {@code ...appid={key}} — était refusée comme mal formée, et la
     * météo tombait silencieusement sur ses valeurs par défaut.
     */
    @Test
    void uriTemplatePlaceholdersSurviveValidation() {
        DestinationValidator validator = validator();

        assertThatCode(() -> validator.validateHttpUrl(
                "https://api.openweathermap.org/data/2.5/weather?lat={latitude}&lon={longitude}",
                DestinationValidator.Sink.WEATHER)).doesNotThrowAnyException();

        // L'accolade ne doit pas pour autant devenir une porte dérobée : un gabarit pointant
        // vers la metadata service reste refusé.
        assertThatThrownBy(() -> validator.validateHttpUrl(
                "https://169.254.169.254/{path}", DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);
    }

    /**
     * CRYPTO-014 : la clé du fournisseur est interpolée dans l'URL, et un endpoint de push
     * porte la clé du navigateur. Même avec {@code allow-insecure-http-destinations = true},
     * le temps clair reste refusé pour la météo, son diagnostic et le push.
     */
    @Test
    void cleartextIsRefusedForTheSinksThatSendASecret() {
        DestinationValidator validator = validator();

        assertThatThrownBy(() -> validator.validateHttpUrl(
                "http://api.openweathermap.org/data/2.5/weather?lat=1&lon=2&appid={key}",
                DestinationValidator.Sink.WEATHER))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class)
                .hasMessageContaining("https");

        assertThatThrownBy(() -> validator.validateHttpUrl("http://api.openweathermap.org/x",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);

        // Un endpoint de push transporte la clé publique P-256 du navigateur et son
        // secret d'auth ; tous les services de push existants répondent en 443.
        assertThatThrownBy(() -> validator.validateHttpUrl("http://push.example.org/wpush/v1/abc",
                DestinationValidator.Sink.WEB_PUSH))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class)
                .hasMessageContaining("https");
    }

    /** Le dérogatoire reste valable pour les autres destinations (le serveur d'inférence du LAN). */
    @Test
    void cleartextIsStillAcceptedForTheOtherSinksWhenEnabled() {
        assertThatCode(() -> validator().validateHttpUrl("http://trusted.lan:8080/v1/chat",
                DestinationValidator.Sink.AI_INFERENCE)).doesNotThrowAnyException();

        assertThatThrownBy(() -> validator(false).validateHttpUrl("http://trusted.lan:8080/v1/chat",
                DestinationValidator.Sink.AI_INFERENCE))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);
    }

    @Test
    void schemesOtherThanHttpAreRefused() {
        DestinationValidator validator = validator();

        assertThatThrownBy(() -> validator.validateHttpUrl("file:///etc/passwd",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);
        assertThatThrownBy(() -> validator.validateHttpUrl("gopher://trusted.lan:70/",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);
        assertThatThrownBy(() -> validator.validateHttpUrl("trusted.lan/no-scheme",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);
    }

    /** En https, pour que ce soit bien la règle d'adresse qui parle, pas celle du transport clair. */
    @Test
    void loopbackAndMetadataAddressesAreRefused() {
        DestinationValidator validator = validator();

        assertThatThrownBy(() -> validator.validateHttpUrl("https://127.0.0.1:8080/",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class)
                .hasMessageContaining("loopback");

        assertThatThrownBy(() -> validator.validateHttpUrl("https://169.254.169.254/latest/meta-data/",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class)
                .hasMessageContaining("link-local");

        assertThatThrownBy(() -> validator.validateHttpUrl("https://10.20.30.40:8080/",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class)
                .hasMessageContaining("private");
    }

    @Test
    void portsOffTheAllowlistAreRefused() {
        assertThatThrownBy(() -> validator().validateHttpUrl("https://8.8.8.8:9000/",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class)
                .hasMessageContaining("port");
    }

    /**
     * Le gabarit {@code {key}} ne doit jamais être développé vers un hôte que l'exploitant
     * n'a pas explicitement approuvé : c'est ainsi que la clé partait chez un tiers.
     */
    @Test
    void keyPlaceholderIsRefusedForUntrustedHosts() {
        assertThatThrownBy(() -> validator().validateHttpUrl("https://8.8.8.8/q?k={key}",
                DestinationValidator.Sink.DIAGNOSTIC))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class)
                .hasMessageContaining("{key}");
    }

    @Test
    void smtpDestinationFollowsTheSameRules() {
        DestinationValidator validator = validator();

        // Adresses littérales : ce test ne doit rien au DNS.
        assertThatCode(() -> validator.validateSmtpDestination("8.8.8.8", 587))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateSmtpDestination("127.0.0.1", 25))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);
        assertThatThrownBy(() -> validator.validateSmtpDestination("8.8.8.8", 6667))
                .isInstanceOf(DestinationValidator.InvalidDestinationException.class);
    }
}
