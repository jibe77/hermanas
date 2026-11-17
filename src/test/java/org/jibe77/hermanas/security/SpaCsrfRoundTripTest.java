package org.jibe77.hermanas.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le contrat exact sur lequel repose le CSRF de ce SPA (CRYPTO-012) : Angular lit le cookie
 * {@code XSRF-TOKEN} et le renvoie tel quel dans {@code X-XSRF-TOKEN}. Il ne sait ni déchiffrer
 * un jeton, ni deviner un nom de champ.
 *
 * <p>Il est écrit pour les deux gestionnaires — le simple (celui de l'application) et le XOR
 * masqué (le défaut de Spring Security depuis la 6.0, que le commentaire de
 * {@code SecurityConfig} déclarait incompatible avec Angular sans qu'on l'ait jamais mesuré).
 * Résultat de la mesure : le premier remplit le contrat, le second le casse. La recommandation
 * CRYPTO-012 — « revenir au défaut du framework » — n'est donc pas applicable telle quelle ici,
 * et les deux tests qui suivent en gardent la trace chiffrée plutôt qu'une opinion.</p>
 */
class SpaCsrfRoundTripTest {

    /** Simule le SPA : renvoie exactement ce que le navigateur a reçu dans le cookie. */
    private static final class EchoChain implements FilterChain {
        private final AtomicBoolean reached = new AtomicBoolean();

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            reached.set(true);
        }

        boolean reached() {
            return reached.get();
        }
    }

    private void assertEchoedCookieIsAccepted(CsrfTokenRequestHandler handler) throws Exception {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfFilter filter = new CsrfFilter(repository);
        filter.setRequestHandler(handler);

        // 1. Une première requête doit poser le cookie que le SPA pourra lire.
        MockHttpServletRequest boot = new MockHttpServletRequest("GET", "/api/v1/door/status");
        // Le repository marque le cookie Secure quand la requête l'est ; MockHttpServletRequest
        // ne l'est pas par défaut, et setCookies refuserait un cookie Secure sur http.
        boot.setSecure(true);
        MockHttpServletResponse bootResponse = new MockHttpServletResponse();
        filter.doFilter(boot, bootResponse, new EchoChain());

        Cookie cookie = bootResponse.getCookie("XSRF-TOKEN");
        assertThat(cookie).as("le cookie XSRF-TOKEN doit être écrit, sinon tout POST part en 403")
                .isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly())
                .as("le SPA doit pouvoir le lire depuis JavaScript")
                .isFalse();

        // 2. Le mutating call : le navigateur renvoie le cookie, et le SPA l'a recopié tel
        // quel dans l'en-tête attendu. C'est exactement ce que fait HttpClient d'Angular.
        MockCookie echoed = new MockCookie(cookie.getName(), cookie.getValue());
        echoed.setPath(cookie.getPath());
        echoed.setSecure(cookie.getSecure());
        echoed.setHttpOnly(cookie.isHttpOnly());

        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/v1/door/open");
        post.setSecure(true);
        post.setSession(boot.getSession());
        post.setCookies(echoed);
        post.addHeader("X-XSRF-TOKEN", cookie.getValue());
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        EchoChain chain = new EchoChain();

        filter.doFilter(post, postResponse, chain);

        assertThat(chain.reached())
                .as("la requête a été rejetée par CsrfFilter (status %s)", postResponse.getStatus())
                .isTrue();
        assertThat(postResponse.getStatus()).isNotEqualTo(403);
    }

    /** Le gestionnaire en vigueur avant CRYPTO-012. */
    @Test
    void thePlainHandlerRoundTrips() throws Exception {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);

        assertEchoedCookieIsAccepted(handler);
    }

    /**
     * Le défaut masqué de Spring Security 6+ (CRYPTO-012), mesuré plutôt que cru sur parole :
     * il refuse la valeur du cookie réémise telle quelle. {@code CookieCsrfTokenRepository}
     * écrit le jeton <em>brut</em> dans le cookie — le masquage XOR ne porte que sur la vue
     * {@code request.getAttribute(CsrfToken)} exposée aux gabarits, que ce SPA ne lit jamais —
     * alors que ce gestionnaire attend dans l'en-tête une valeur masquée et la dé-masque avant
     * de comparer. Les deux ne se rencontrent pas.
     *
     * <p>Autrement dit, passer au gestionnaire par défaut sans rien changer d'autre casse
     * chaque requête mutante du frontend. Ce test est là pour que ce soit une découverte
     * explicite et non un 403 mystérieux en production. Le remède documenté par Spring, un
     * {@code SpaCsrfTokenRequestHandler} qui accepte aussi la valeur brute, n'apporterait ici
     * rien contre BREACH : puisque le jeton ne transite que par un en-tête {@code Set-Cookie}
     * et n'est jamais réfléchi dans un corps de réponse compressé, il n'y a rien à reconstituer
     * octet par octet. La recommandation supposait un gabarite qui imprime le jeton ; ce n'est
     * pas le cas de celui-ci.</p>
     */
    @Test
    void theMaskedXorHandlerRefusesTheVerbatimCookieEcho() throws Exception {
        XorCsrfTokenRequestAttributeHandler handler = new XorCsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);

        CsrfFilter filter = new CsrfFilter(CookieCsrfTokenRepository.withHttpOnlyFalse());
        filter.setRequestHandler(handler);

        MockHttpServletRequest boot = new MockHttpServletRequest("GET", "/api/v1/door/status");
        boot.setSecure(true);
        MockHttpServletResponse bootResponse = new MockHttpServletResponse();
        filter.doFilter(boot, bootResponse, new EchoChain());
        Cookie cookie = bootResponse.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();

        MockCookie echoed = new MockCookie(cookie.getName(), cookie.getValue());
        echoed.setPath(cookie.getPath());
        echoed.setSecure(cookie.getSecure());
        echoed.setHttpOnly(cookie.isHttpOnly());

        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/v1/door/open");
        post.setSecure(true);
        post.setSession(boot.getSession());
        post.setCookies(echoed);
        post.addHeader("X-XSRF-TOKEN", cookie.getValue());
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        EchoChain chain = new EchoChain();

        filter.doFilter(post, postResponse, chain);

        assertThat(chain.reached())
                .as("le masque XOR attend une autre valeur que celle du cookie")
                .isFalse();
        assertThat(postResponse.getStatus()).isEqualTo(403);
    }

    /** Sans jeton dans la requête, un mutating call doit toujours être refusé. */
    @Test
    void aMutationWithoutATokenIsStillRefused() throws Exception {
        CsrfFilter filter = new CsrfFilter(CookieCsrfTokenRepository.withHttpOnlyFalse());
        MockHttpServletRequest boot = new MockHttpServletRequest("GET", "/");
        filter.doFilter(boot, new MockHttpServletResponse(), new EchoChain());

        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/api/v1/door/open");
        post.setSession(boot.getSession());
        MockHttpServletResponse response = new MockHttpServletResponse();
        EchoChain chain = new EchoChain();

        filter.doFilter(post, response, chain);

        assertThat(chain.reached()).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
