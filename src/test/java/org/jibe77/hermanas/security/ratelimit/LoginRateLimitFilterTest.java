package org.jibe77.hermanas.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le throttle de login doit voir la meme porte que Spring Security.
 *
 * <p>Le filtre comparait autrefois {@code getRequestURI()} a la chaine brute, ce qui
 * laissait {@code /api/v1/auth/login;x=1} (parametre de chemin) passer sans comptage
 * alors que le matcher de Spring Security, lui, traite bien cette URL comme un login.
 */
class LoginRateLimitFilterTest {

    private static final int MAX = 3;

    private final LoginRateLimitFilter filter = new LoginRateLimitFilter();

    @BeforeEach
    void setUp() {
        // Les valeurs sont injectees par @Value en production ; en test unitaire pur,
        // elles valent 0 et tout serait refuse.
        ReflectionTestUtils.setField(filter, "maxRequests", MAX);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60);
    }

    /** @return le code HTTP produit par le filtre (200 quand la suite est atteinte). */
    private int post(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr("203.0.113.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void loginAttemptsAreCountedPerIp() throws Exception {
        for (int i = 0; i < MAX; i++) {
            assertThat(post("/api/v1/auth/login"))
                    .as("tentative %d dans la fenetre", i + 1)
                    .isEqualTo(200);
        }
        assertThat(post("/api/v1/auth/login")).isEqualTo(429);
    }

    @Test
    void aPathParameterDoesNotEscapeTheThrottle() throws Exception {
        // La variante qui contournait le filtre : Spring Security la traite bien comme
        // un login, le throttle doit donc la compter aussi.
        for (int i = 0; i < MAX; i++) {
            assertThat(post("/api/v1/auth/login;probe=1")).isEqualTo(200);
        }
        assertThat(post("/api/v1/auth/login;probe=1")).isEqualTo(429);
    }

    @Test
    void aTrailingSlashIsNotALoginForEitherSide() throws Exception {
        // PathPattern ne fait pas correspondre le motif avec un slash final : Spring
        // Security ne traite pas cette URL comme un login, le filtre ne la compte donc
        // pas non plus. Les deux cotes sont d'accord, c'est ce qui compte.
        for (int i = 0; i < MAX + 2; i++) {
            assertThat(post("/api/v1/auth/login/")).isEqualTo(200);
        }
    }

    @Test
    void otherPostsAreNotCounted() throws Exception {
        for (int i = 0; i < MAX + 2; i++) {
            assertThat(post("/api/v1/door/open")).isEqualTo(200);
        }
        assertThat(post("/api/v1/auth/login")).isEqualTo(200);
    }

    @Test
    void getOnTheLoginPathIsNotCounted() throws Exception {
        for (int i = 0; i < MAX + 2; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
            request.setRemoteAddr("203.0.113.7");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
