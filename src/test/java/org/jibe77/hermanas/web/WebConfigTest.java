package org.jibe77.hermanas.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie que les routes SPA sans préfixe de locale sont redirigées vers la
 * locale par défaut.
 *
 * <p>Régression : le manifest PWA déclarait {@code "start_url": "/dashboard"},
 * un chemin que le serveur ne sert pas (la SPA vit sous /fr-FR/, /en-US/,
 * /ro-RO/). Les icônes installées sur l'écran d'accueil iOS ouvraient donc
 * {@code /dashboard} et recevaient le JSON 404 de
 * {@code GlobalExceptionHandler#handleNoResourceFound}.</p>
 */
// Contexte web minimal : WebConfig n'enregistre que des view controllers et ne
// dépend d'aucun bean applicatif. Un @WebMvcTest complet déclencherait le scan
// de composants (dont ButtonNotificationController, qui exige toute la stack
// WebSocket) pour ne vérifier que des redirections.
@SpringJUnitWebConfig
@ContextConfiguration(classes = WebConfigTest.TestConfig.class)
class WebConfigTest {

    @EnableWebMvc
    static class TestConfig extends WebConfig {
    }

    private MockMvc mockMvc;

    WebConfigTest(@Autowired WebApplicationContext wac) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/dashboard", "/camera", "/energy", "/logs", "/music", "/notification",
        "/residents", "/scheduler", "/system", "/utility", "/version", "/weather",
        "/electronics", "/error"
    })
    void redirectsUnprefixedSpaRoutesToDefaultLocale(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fr-FR" + path));
    }

    /** Le cas exact rapporté : l'icône iPhone ouvrait /dashboard. */
    @Test
    void redirectsPwaStartUrlToDefaultLocale() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fr-FR/dashboard"));
    }

    /**
     * Une icône déjà installée continue de demander /manifest.webmanifest :
     * iOS ne relit pas le manifest d'une app ajoutée à l'écran d'accueil.
     */
    @Test
    void redirectsRootManifestToDefaultLocale() throws Exception {
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fr-FR/manifest.webmanifest"));
    }

    /** Les sous-chemins profonds retombent sur la racine de la feature. */
    @Test
    void redirectsNestedSpaRoutesToDefaultLocale() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fr-FR/auth/login"));
    }

    @Test
    void redirectsRootToDefaultLocale() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fr-FR/"));
    }
}
