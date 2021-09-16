package org.jibe77.hermanas.scheduler.sun;

import org.jibe77.hermanas.controller.config.ConfigController;
import org.jibe77.hermanas.controller.energy.EnergyMode;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {ConsumptionModeController.class, ConfigController.class})
public class ConsumptionModeControllerTest {

    @MockBean
    ParameterRepository parameterRepository;

    @Autowired
    ConfigController configController;

    @Autowired
    ConsumptionModeController consumptionModeController;

    @Test
    public void testEcoMode() {
        LocalDateTime d = LocalDateTime.of(2020, 12, 05, 12, 00);
        assertTrue(consumptionModeController.isEcoMode(d));
        assertTrue(!consumptionModeController.isSunnyMode(d));
    }

    @Test
    public void testEcoModeNextYear() {
        LocalDateTime d = LocalDateTime.of(2021, 1, 5, 12, 00);
        assertTrue(consumptionModeController.isEcoMode(d));
        assertTrue(!consumptionModeController.isSunnyMode(d));
    }

    @Test
    public void testSolsticeDay() {
        assertEquals(355,
                consumptionModeController.getWinterSolsticeDay(2021).getDayOfYear());
        assertEquals(172,
                consumptionModeController.getSummerSolsticeDay(2021).getDayOfYear());
    }

    @Test
    public void testDuration() {
        assertEquals(1, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2020, 12, 21, 12, 00)));
        assertEquals(100, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2020, 6, 21, 12, 00)));
        assertEquals(10, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2020, 3, 1, 12, 00)));
    }

    @Test
    public void testDurationEcoModeForce() {
        configController.setConsumptionModeEcoForce(true);
        assertTrue(configController.isConsumptionModeEcoForce());
        assertEquals(1, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2020, 12, 21, 12, 00)));
        assertEquals(1, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2020, 6, 21, 12, 00)));
        assertEquals(1, consumptionModeController.getDuration(
                1, 10, 100,
                LocalDateTime.of(2020, 3, 21, 12, 00)));
        configController.setConsumptionModeEcoForce(false);
    }

    @Test
    public void testSolsticeBeforeWinterInRegularMode() {
        configController.setConsumptionModeEcoForce(false);
        assertTrue(!configController.isConsumptionModeEcoForce());
        LocalDateTime beforeWinterSolsticeInRegularMode = LocalDateTime.of(2020, 11, 10, 12, 00);
        assertEquals(LocalDateTime.of(2020, 12, 21, 12, 00), consumptionModeController.getWinterSolstice(
                beforeWinterSolsticeInRegularMode));
        assertEquals(LocalDateTime.of(2021, 6, 21, 12, 00), consumptionModeController.getSummerSolstice(
                beforeWinterSolsticeInRegularMode));
    }

    @Test
    public void testSolsticeBeforeWinter() {
        configController.setConsumptionModeEcoForce(false);
        LocalDateTime beforeWinterSolstice = LocalDateTime.of(2020, 12, 10, 12, 00);
        assertEquals(LocalDateTime.of(2020, 12, 21, 12, 00), consumptionModeController.getWinterSolstice(
                beforeWinterSolstice));
        assertEquals(LocalDateTime.of(2021, 6, 21, 12, 00), consumptionModeController.getSummerSolstice(
                beforeWinterSolstice));
    }

    @Test
    public void testSolsticeAfterWinter() {
        LocalDateTime afterWinterSolstice = LocalDateTime.of(2020, 12, 31, 12, 00);
        assertEquals(
                LocalDateTime.of(2020, 12, 21, 12, 00),
                consumptionModeController.getWinterSolstice(afterWinterSolstice));
        assertEquals(LocalDateTime.of(2021, 6, 21, 12, 00),
                consumptionModeController.getSummerSolstice(afterWinterSolstice));
    }

    @Test
    public void testSolsticeAfterWinterNextYear() {
        LocalDateTime afterWinterSolsticeNextYear = LocalDateTime.of(2021, 1, 5, 12, 00);
        assertEquals(LocalDateTime.of(2020, 12, 21, 12, 00), consumptionModeController.getWinterSolstice(
                afterWinterSolsticeNextYear));
        assertEquals(LocalDateTime.of(2021, 6, 21, 12, 00), consumptionModeController.getSummerSolstice(
                afterWinterSolsticeNextYear));
    }

    @Test
    public void testSolsticeAfterWinterRegularMode() {
        LocalDateTime afterWinterSolsticeRegularMode = LocalDateTime.of(2021, 2, 25, 12, 00);
        assertEquals(LocalDateTime.of(2021, 12, 21, 12, 00), consumptionModeController.getWinterSolstice(
                afterWinterSolsticeRegularMode));
        assertEquals(LocalDateTime.of(2021, 6, 21, 12, 00), consumptionModeController.getSummerSolstice(
                afterWinterSolsticeRegularMode));
    }

    @Test
    public void testSolsticeBeforeSunnyMode() {
        LocalDateTime beforeSunnyMode = LocalDateTime.of(2021, 5, 1, 12, 00);
        assertEquals(LocalDateTime.of(2021, 12, 21, 12, 00), consumptionModeController.getWinterSolstice(
                beforeSunnyMode));
        assertEquals(LocalDateTime.of(2021, 6, 21, 12, 00), consumptionModeController.getSummerSolstice(
                beforeSunnyMode));
    }

    @Test
    public void testSolsticeAfterSunnyMode() {
        LocalDateTime afterSunnyMode = LocalDateTime.of(2021, 8, 1, 12, 00);
        assertEquals(LocalDateTime.of(2021, 12, 21, 12, 00), consumptionModeController.getWinterSolstice(
                afterSunnyMode));
        assertEquals(LocalDateTime.of(2021, 6, 21, 12, 00), consumptionModeController.getSummerSolstice(
                afterSunnyMode));
    }

    @Test
    public void testSolsticeAfterSunnyModeAsRegularMode() {
        LocalDateTime afterSunnyMode = LocalDateTime.of(2021, 10, 15, 12, 00);
        assertEquals(LocalDateTime.of(2021, 12, 21, 12, 00), consumptionModeController.getWinterSolstice(
                afterSunnyMode));
        assertEquals(LocalDateTime.of(2022, 6, 21, 12, 00), consumptionModeController.getSummerSolstice(
                afterSunnyMode));
    }

    @Test
    public void testCurrentEnergyMode() {
        LocalDateTime time = LocalDateTime.of(2021, 12, 21, 12, 00);
        EnergyMode energyMode = consumptionModeController.getCurrentEnergyMode(time);
        assertEquals("ECO", energyMode.getCurrentMode());

        assertEquals(25, energyMode.getEcoModeDaysAroundWinterSolstice());
        assertEquals(100, energyMode.getSunnyModeDaysAroundSummerSolstice());

        assertEquals(26, energyMode.getEcoModeStartDate().getDayOfMonth());
        assertEquals(Month.NOVEMBER, energyMode.getEcoModeStartDate().getMonth());
        assertEquals(2021, energyMode.getEcoModeStartDate().getYear());

        assertEquals(15, energyMode.getEcoModeEndDate().getDayOfMonth());
        assertEquals(Month.JANUARY, energyMode.getEcoModeEndDate().getMonth());
        assertEquals(2022, energyMode.getEcoModeEndDate().getYear());

        assertEquals(13, energyMode.getSunnyModeStartDate().getDayOfMonth());
        assertEquals(Month.MARCH, energyMode.getSunnyModeStartDate().getMonth());
        assertEquals(2022, energyMode.getSunnyModeStartDate().getYear());

        assertEquals(29, energyMode.getSunnyModeEndDate().getDayOfMonth());
        assertEquals(Month.SEPTEMBER, energyMode.getSunnyModeEndDate().getMonth());
        assertEquals(2022, energyMode.getSunnyModeEndDate().getYear());
    }

    @Test
    public void nbrDaysInYear() {
        assertEquals(366, consumptionModeController.getNumberOfDaysInYear(2020));
        assertEquals(365, consumptionModeController.getNumberOfDaysInYear(2021));
    }
}
