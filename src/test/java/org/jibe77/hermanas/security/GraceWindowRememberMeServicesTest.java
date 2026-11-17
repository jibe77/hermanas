package org.jibe77.hermanas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Régression : le SPA émet plusieurs requêtes en parallèle au démarrage, toutes porteuses du
 * même cookie remember-me. La rotation de token faisait passer les requêtes concurrentes pour
 * un vol de cookie ; la réponse en erreur supprimait le cookie fraîchement renouvelé et
 * l'utilisateur devait se reconnecter.
 */
class GraceWindowRememberMeServicesTest {

    private static final String SERIES = "serie-1";
    private static final String USERNAME = "rmuser";

    private PersistentTokenRepository tokenRepository;
    private GraceWindowRememberMeServices services;

    @BeforeEach
    void setUp() {
        tokenRepository = new InMemoryTokenRepositoryImpl();
        UserDetailsService uds = username -> (UserDetails) User.withUsername(username)
                .password("ignored")
                .roles(SecurityConfig.ROLE_USER)
                .build();
        services = new GraceWindowRememberMeServices("test-key", uds, tokenRepository);
    }

    private void storeToken(String token) {
        tokenRepository.createNewToken(
                new PersistentRememberMeToken(USERNAME, SERIES, token, new Date()));
    }

    private UserDetails autoLogin(String token) {
        return services.processAutoLoginCookie(new String[] {SERIES, token},
                new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    @Test
    void firstUseAuthenticatesAndRotatesTheToken() {
        storeToken("token-initial");

        assertThat(autoLogin("token-initial").getUsername()).isEqualTo(USERNAME);
        assertThat(tokenRepository.getTokenForSeries(SERIES).getTokenValue())
                .as("le token doit avoir tourné")
                .isNotEqualTo("token-initial");
    }

    /** Le cœur du correctif : la deuxième requête concurrente ne doit pas échouer. */
    @Test
    void concurrentRequestWithSupersededTokenStillAuthenticates() {
        storeToken("token-initial");
        autoLogin("token-initial");

        assertThat(autoLogin("token-initial").getUsername())
                .as("une requête concurrente présentant le token précédent doit être acceptée")
                .isEqualTo(USERNAME);
    }

    /** Une rafale complète, comme au démarrage du PWA. */
    @Test
    void burstOfConcurrentRequestsAllAuthenticate() {
        storeToken("token-initial");

        for (int i = 0; i < 5; i++) {
            assertThat(autoLogin("token-initial")).isNotNull();
        }
    }

    /**
     * La tolérance ne doit pas réécrire le cookie : c'est précisément l'écrasement du cookie
     * valide (posé par la requête gagnante) qui cassait le remember-me.
     */
    @Test
    void concurrentRequestDoesNotRewriteTheCookie() {
        storeToken("token-initial");
        autoLogin("token-initial");
        String rotated = tokenRepository.getTokenForSeries(SERIES).getTokenValue();

        MockHttpServletResponse response = new MockHttpServletResponse();
        services.processAutoLoginCookie(new String[] {SERIES, "token-initial"},
                new MockHttpServletRequest(), response);

        assertThat(response.getCookies())
                .as("aucun cookie ne doit être écrit par la requête en fenêtre de grâce")
                .isEmpty();
        assertThat(tokenRepository.getTokenForSeries(SERIES).getTokenValue())
                .as("le token ne doit pas tourner une seconde fois")
                .isEqualTo(rotated);
    }

    /**
     * La propriété de sécurité est préservée : un token qui n'a jamais été le token courant
     * de la série reste traité comme un vol.
     */
    @Test
    void unknownTokenIsStillTreatedAsTheft() {
        storeToken("token-initial");
        autoLogin("token-initial");

        assertThatThrownBy(() -> autoLogin("token-jamais-emis"))
                .isInstanceOf(CookieTheftException.class);
    }

    /**
     * CRYPTO-003 : le chemin de grâce court-circuite la méthode mère, il doit donc reproduire la
     * vérification d'expiration de Spring. Un jeton périmé ne doit rien ouvrir, même présenté
     * dans la fenêtre de grâce.
     */
    @Test
    void expiredTokensAreStillRecognisedAsExpiredOnTheGracePath() {
        services.setTokenValiditySeconds(1);

        assertThat(services.isTokenExpired(new PersistentRememberMeToken(
                USERNAME, SERIES, "token", new Date(System.currentTimeMillis() - 10_000L))))
                .as("un jeton plus vieux que la validité doit être reconnu périmé")
                .isTrue();
        assertThat(services.isTokenExpired(new PersistentRememberMeToken(
                USERNAME, SERIES, "token", new Date())))
                .as("un jeton tout juste posé doit être valide")
                .isFalse();
    }

    /**
     * CRYPTO-003 : la fenêtre est courte (2 s). Une fois retombée, la réutilisation d'un token
     * déjà tourné redevient ce que le schéma de Jaspan existe pour détecter : un vol.
     */
    @Test
    void replayOutsideTheGraceWindowIsTreatedAsTheft() throws Exception {
        storeToken("token-initial");
        autoLogin("token-initial");

        // Faire retomber la fenêtre sans attendre 2 secondes réelles.
        Field field = GraceWindowRememberMeServices.class.getDeclaredField("recentlyRotated");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> rotated = (Map<String, Object>) field.get(services);
        rotated.clear();

        assertThatThrownBy(() -> autoLogin("token-initial"))
                .isInstanceOf(CookieTheftException.class);
    }

    /** CRYPTO-003 : la fenêtre est de quelques secondes, plus de 30. */
    @Test
    void graceWindowIsAtMostTwoSeconds() {
        assertThat(GraceWindowRememberMeServices.GRACE_WINDOW.getSeconds()).isLessThanOrEqualTo(2);
    }
}
