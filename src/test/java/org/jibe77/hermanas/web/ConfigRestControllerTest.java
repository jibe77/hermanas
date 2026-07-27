package org.jibe77.hermanas.web;

import org.jibe77.hermanas.client.ai.AiVisionCache;
import org.jibe77.hermanas.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST layer tests for ConfigRestController using @WebMvcTest.
 * Tests HTTP request/response handling with mocked service layer.
 */
@WebMvcTest(ConfigRestController.class)
// Spring Boot 4 : la slice @WebMvcTest ne charge plus SecurityConfig, donc les
// @PreAuthorize du contrôleur seraient inertes et les tests d'accès anonyme
// passeraient au travers. On réactive juste la method security ici — importer
// SecurityConfig en entier obligerait à mocker toute sa chaîne de dépendances.
@Import(ConfigRestControllerTest.MethodSecurityTestConfig.class)
class ConfigRestControllerTest {

    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigService configService;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private AiVisionCache aiVisionCache;

    @MockitoBean
    private org.jibe77.hermanas.service.camera.CameraService cameraService;

    // ============================================================================
    // GET /api/v1/config - View Configuration
    // ============================================================================

    @Test
    @WithMockUser(roles = "USER")
    void getAllConfig_shouldReturnConfiguration() throws Exception {
        // Given
        when(configService.getLightSecurityTimerDelayEco()).thenReturn(300000L);
        when(configService.getLightSecurityTimerDelayRegular()).thenReturn(600000L);
        when(configService.getLightSecurityTimerDelaySunny()).thenReturn(900000L);
        when(configService.getFanSecurityTimerDelayEco()).thenReturn(300000L);
        when(configService.getFanSecurityTimerDelayRegular()).thenReturn(600000L);
        when(configService.getFanSecurityTimerDelaySunny()).thenReturn(900000L);
        when(configService.getMusicSecurityTimerDelayEco()).thenReturn(300000L);
        when(configService.getMusicSecurityTimerDelayRegular()).thenReturn(600000L);
        when(configService.getMusicSecurityTimerDelaySunny()).thenReturn(900000L);
        when(configService.getMonthMode(anyInt())).thenReturn(
                org.jibe77.hermanas.service.energy.EnergyModeEnum.REGULAR);
        when(configService.isConsumptionModeEcoForce()).thenReturn(false);

        // When/Then. system_behavior used to expose wifi_disabled per mode, but the
        // section was removed when the matching admin endpoints were dropped.
        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.light_timers.eco_delay_ms").value(300000))
                .andExpect(jsonPath("$.fan_timers.eco_delay_ms").value(300000))
                .andExpect(jsonPath("$.music_timers.eco_delay_ms").value(300000))
                .andExpect(jsonPath("$.consumption_mode.monthly_mapping").exists())
                .andExpect(jsonPath("$.system_behavior").doesNotExist());
    }

    @Test
    void getAllConfig_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================================
    // POST /api/v1/config/refresh - Cache Refresh
    // ============================================================================

    @Test
    @WithMockUser(roles = "USER")
    void refreshCaches_shouldClearAllCaches() throws Exception {
        // Given
        org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
        when(cacheManager.getCacheNames()).thenReturn(java.util.Set.of("cache1", "cache2"));
        when(cacheManager.getCache("cache1")).thenReturn(mockCache);
        when(cacheManager.getCache("cache2")).thenReturn(mockCache);

        // When/Then — Spring caches (cache1, cache2) plus the custom AiVisionCache
        // and the CameraService picture cache bring the total to 4 cleared entries.
        mockMvc.perform(post("/api/v1/config/refresh")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Configuration caches refreshed successfully"))
                .andExpect(jsonPath("$.caches_cleared").value(4))
                .andExpect(jsonPath("$.hot_reload_enabled").value(true));

        verify(mockCache, times(2)).clear();
        verify(aiVisionCache, times(1)).clear();
        verify(cameraService, times(1)).clearPictureCache();
    }

    @Test
    void refreshCaches_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/config/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================================
    // PUT /api/v1/config/light/* - Update Light Timers
    // ============================================================================

    @Test
    @WithMockUser(roles = "USER")
    void setLightEco_shouldUpdateTimer() throws Exception {
        mockMvc.perform(put("/api/v1/config/light/eco")
                        .param("delayMs", "300000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Light eco timer updated to 300000 ms"));

        verify(configService).setLightSecurityTimerDelayEco(300000L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void setLightEco_shouldReturn400ForInvalidValue() throws Exception {
        doThrow(new IllegalArgumentException("Timer delay cannot be negative"))
                .when(configService).setLightSecurityTimerDelayEco(-1L);

        mockMvc.perform(put("/api/v1/config/light/eco")
                        .param("delayMs", "-1")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ============================================================================
    // PUT /api/v1/config/consumption/* - Update Consumption Mode
    // ============================================================================

    @Test
    @WithMockUser(roles = "USER")
    void setForceEco_shouldUpdateForceFlag() throws Exception {
        mockMvc.perform(put("/api/v1/config/consumption/force-eco")
                        .param("force", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Eco mode force set to true"));

        verify(configService).setConsumptionModeEcoForce(true);
    }

    // PUT /api/v1/config/system/<mode>/wifi and /shutdown endpoints have been
    // removed — see ConfigRestController for the rationale.
}
