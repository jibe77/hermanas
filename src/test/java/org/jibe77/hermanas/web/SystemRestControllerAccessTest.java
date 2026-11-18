package org.jibe77.hermanas.web;

import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.security.siri.SiriTokenVerifier;
import org.jibe77.hermanas.service.system.SystemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AUTHZ-VULN-06 : eteindre ou redemarrer la machine est une decision d'administration.
 *
 * <p>Avant ce correctif, les deux appels tombaient sous la regle generique
 * « verbe mutatif sur /api/v1/** → USER ou ADMIN » de SecurityConfig, donc n'importe
 * quel compte USER pouvait couper le Raspberry Pi.
 */
@WebMvcTest(SystemRestController.class)
// Comme dans ConfigRestControllerTest : la slice n'embarque pas SecurityConfig, il faut
// reactiver la method security pour que @PreAuthorize soit réellement évalué.
@Import(SystemRestControllerAccessTest.MethodSecurityTestConfig.class)
class SystemRestControllerAccessTest {

    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemService systemService;

    @MockitoBean
    private org.jibe77.hermanas.service.event.EventService eventService;

    /**
     * Simulacres imposes par les filtres servlet de l'application que la slice charge
     * (SessionEpochFilter, SiriTokenAuthenticationFilter) — voir ConfigRestControllerTest.
     */
    @MockitoBean
    private HermanasUserRepository userRepository;

    @MockitoBean
    private SiriTokenVerifier siriTokenVerifier;

    /** Réclamé au démarrage de la slice, comme dans ConfigRestControllerTest. */
    @MockitoBean
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    @WithMockUser(username = "jean", roles = "USER")
    void aPlainUserCannotShutTheMachineDown() throws Exception {
        mockMvc.perform(post("/api/v1/system/shutdown").with(csrf()))
                .andExpect(status().isForbidden());

        verify(systemService, never()).shutdown();
    }

    @Test
    @WithMockUser(username = "jean", roles = "USER")
    void aPlainUserCannotRebootTheMachine() throws Exception {
        mockMvc.perform(post("/api/v1/system/reboot").with(csrf()))
                .andExpect(status().isForbidden());

        verify(systemService, never()).reboot();
    }

    @Test
    @WithMockUser(username = "marguerite", roles = "ADMIN")
    void anAdminCanStillShutDown() throws Exception {
        mockMvc.perform(post("/api/v1/system/shutdown").with(csrf()))
                .andExpect(status().isOk());

        verify(systemService).shutdown();
    }

    @Test
    @WithMockUser(username = "marguerite", roles = "ADMIN")
    void anAdminCanStillReboot() throws Exception {
        mockMvc.perform(post("/api/v1/system/reboot").with(csrf()))
                .andExpect(status().isOk());

        verify(systemService).reboot();
    }
}
