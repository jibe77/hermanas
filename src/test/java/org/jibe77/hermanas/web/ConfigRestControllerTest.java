package org.jibe77.hermanas.web;

import org.jibe77.hermanas.controller.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST layer tests for ConfigRestController using @WebMvcTest.
 * Tests HTTP request/response handling with mocked service layer.
 */
@WebMvcTest(ConfigRestController.class)
class ConfigRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConfigService configService;

    @MockBean
    private CacheManager cacheManager;

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
        when(configService.getEcoModeNbrDaysAroundWinterSolstice()).thenReturn(30);
        when(configService.getSunnyModeNbrDaysAroundSummerSolstice()).thenReturn(30);
        when(configService.isConsumptionModeEcoForce()).thenReturn(false);
        when(configService.isMachineShutdownInEcoMode()).thenReturn(true);
        when(configService.isWifiDisabledInEcoMode()).thenReturn(true);
        when(configService.isMachineShutdownInRegularMode()).thenReturn(false);
        when(configService.isWifiDisabledInRegularMode()).thenReturn(false);
        when(configService.isMachineShutdownInSunnyMode()).thenReturn(false);
        when(configService.isWifiDisabledInSunnyMode()).thenReturn(false);

        // When/Then
        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.light_timers.eco_delay_ms").value(300000))
                .andExpect(jsonPath("$.fan_timers.eco_delay_ms").value(300000))
                .andExpect(jsonPath("$.music_timers.eco_delay_ms").value(300000))
                .andExpect(jsonPath("$.consumption_mode.eco_days_around_winter_solstice").value(30))
                .andExpect(jsonPath("$.system_behavior.eco.machine_shutdown").value(true));
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

        // When/Then
        mockMvc.perform(post("/api/v1/config/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Configuration caches refreshed successfully"))
                .andExpect(jsonPath("$.caches_cleared").value(2))
                .andExpect(jsonPath("$.hot_reload_enabled").value(true));

        verify(mockCache, times(2)).clear();
    }

    @Test
    void refreshCaches_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/config/refresh"))
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
                        )
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
                        )
                .andExpect(status().isBadRequest());
    }

    // ============================================================================
    // PUT /api/v1/config/consumption/* - Update Consumption Mode
    // ============================================================================

    @Test
    @WithMockUser(roles = "USER")
    void setEcoDays_shouldUpdateDays() throws Exception {
        mockMvc.perform(put("/api/v1/config/consumption/eco-days")
                        .param("days", "45")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("Eco mode days around winter solstice updated to 45"));

        verify(configService).setEcoModeNbrDaysAroundWinterSolstice(45);
    }

    @Test
    @WithMockUser(roles = "USER")
    void setForceEco_shouldUpdateForceFlag() throws Exception {
        mockMvc.perform(put("/api/v1/config/consumption/force-eco")
                        .param("force", "true")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("Eco mode force set to true"));

        verify(configService).setConsumptionModeEcoForce(true);
    }

    // ============================================================================
    // PUT /api/v1/config/system/* - Update System Behavior
    // ============================================================================

    @Test
    @WithMockUser(roles = "USER")
    void setEcoShutdown_shouldUpdateShutdownBehavior() throws Exception {
        mockMvc.perform(put("/api/v1/config/system/eco/shutdown")
                        .param("enabled", "false")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("Machine shutdown in eco mode set to false"));

        verify(configService).setMachineShutdownInEcoMode(false);
    }

    @Test
    @WithMockUser(roles = "USER")
    void setEcoWifi_shouldUpdateWifiBehavior() throws Exception {
        mockMvc.perform(put("/api/v1/config/system/eco/wifi")
                        .param("disabled", "true")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("WiFi disabled in eco mode set to true"));

        verify(configService).setWifiDisabledInEcoMode(true);
    }
}
