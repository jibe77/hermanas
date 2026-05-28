package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.energy.EnergyMode;
import org.jibe77.hermanas.service.energy.EnergyModeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.HashMap;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {ConsumptionModeController.class, ConfigService.class})
public class ConsumptionModeControllerTest {

    @MockBean
    ParameterRepository parameterRepository;

    @Autowired
    ConfigService configService;

    @Autowired
    ConsumptionModeController consumptionModeController;

    /**
     * In-memory shim over the mocked ParameterRepository: ConfigService persists to it
     * via setConfigValue() and reads it back via findByEntryKey(). Without this,
     * setMonthMode() / setConsumptionModeEcoForce() in a test do nothing.
     */
    private final java.util.Map<String, Parameter> paramStore = new HashMap<>();

    @BeforeEach
    public void wireParameterStore() {
        paramStore.clear();
        org.mockito.Mockito.when(parameterRepository.findByEntryKey(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> paramStore.get(invocation.<String>getArgument(0)));
        org.mockito.Mockito.when(parameterRepository.save(org.mockito.ArgumentMatchers.any(Parameter.class)))
                .thenAnswer(invocation -> {
                    Parameter p = invocation.getArgument(0);
                    paramStore.put(p.getEntryKey(), p);
                    return p;
                });
    }

    @AfterEach
    public void resetForce() {
        configService.setConsumptionModeEcoForce(false);
    }

    @Test
    public void monthlyMapping_defaultsFollowTheConfiguredCalendar() {
        // Calendar from application.properties: ECO Jan-Feb + Nov-Dec, REGULAR Mar + Oct,
        // SUNNY Apr-Sep.
        EnergyMode jan = consumptionModeController.getCurrentEnergyMode(LocalDateTime.of(2026, 1, 15, 12, 0));
        EnergyMode mar = consumptionModeController.getCurrentEnergyMode(LocalDateTime.of(2026, 3, 15, 12, 0));
        EnergyMode jun = consumptionModeController.getCurrentEnergyMode(LocalDateTime.of(2026, 6, 15, 12, 0));
        EnergyMode oct = consumptionModeController.getCurrentEnergyMode(LocalDateTime.of(2026, 10, 15, 12, 0));
        EnergyMode dec = consumptionModeController.getCurrentEnergyMode(LocalDateTime.of(2026, 12, 15, 12, 0));

        assertEquals("ECO", jan.getCurrentMode());
        assertEquals("REGULAR", mar.getCurrentMode());
        assertEquals("SUNNY", jun.getCurrentMode());
        assertEquals("REGULAR", oct.getCurrentMode());
        assertEquals("ECO", dec.getCurrentMode());
    }

    @Test
    public void currentEnergyMode_exposesTheTwelveMonthsAndForceFlag() {
        EnergyMode mode = consumptionModeController.getCurrentEnergyMode(LocalDateTime.of(2026, 5, 1, 12, 0));

        assertEquals("SUNNY", mode.getCurrentMode());
        assertFalse(mode.isForced());
        assertEquals(12, mode.getMonthlyMapping().size());
        assertEquals(EnergyModeEnum.ECO, mode.getMonthlyMapping().get(1));
        assertEquals(EnergyModeEnum.SUNNY, mode.getMonthlyMapping().get(7));
        assertEquals(EnergyModeEnum.REGULAR, mode.getMonthlyMapping().get(10));
    }

    @Test
    public void forceEco_overridesEveryMonth() {
        configService.setConsumptionModeEcoForce(true);

        assertTrue(consumptionModeController.getCurrentEnergyMode(LocalDateTime.of(2026, 6, 21, 12, 0))
                .isForced());
        assertEquals(1, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2026, 6, 21, 12, 0)));
        assertEquals(1, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2026, 10, 15, 12, 0)));
    }

    @Test
    public void getDuration_picksTheValueOfTheMonthMode() {
        // March -> REGULAR  ⇒ 10
        assertEquals(10, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2026, 3, 15, 12, 0)));
        // June -> SUNNY  ⇒ 100
        assertEquals(100, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2026, 6, 15, 12, 0)));
        // December -> ECO  ⇒ 1
        assertEquals(1, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2026, 12, 15, 12, 0)));
    }

    @Test
    public void updateMonthlyMapping_persistsTheNewSchedule() {
        Map<Integer, EnergyModeEnum> schedule = new LinkedHashMap<>();
        schedule.put(3, EnergyModeEnum.SUNNY);   // moved March from REGULAR to SUNNY
        schedule.put(10, EnergyModeEnum.SUNNY);  // and October too

        consumptionModeController.updateMonthlyMapping(schedule);

        assertEquals("SUNNY", consumptionModeController
                .getCurrentEnergyMode(LocalDateTime.of(2026, 3, 1, 12, 0)).getCurrentMode());
        assertEquals("SUNNY", consumptionModeController
                .getCurrentEnergyMode(LocalDateTime.of(2026, 10, 31, 12, 0)).getCurrentMode());
    }
}
