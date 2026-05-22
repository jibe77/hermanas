package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.controller.camera.CameraService;
import org.jibe77.hermanas.controller.door.DoorService;
import org.jibe77.hermanas.controller.energy.WifiService;
import org.jibe77.hermanas.controller.fan.FanService;
import org.jibe77.hermanas.controller.music.MusicService;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ManageDoorOpeningEvent {

    SunTimeManager sunTimeManager;

    CameraService cameraService;

    DoorService doorService;

    MusicService musicService;

    FanService fanService;

    WifiService wifiService;

    NotificationService notificationService;

    ConsumptionModeController consumptionModeController;

    @Value("${play.cocorico.at.sunrise.enabled}")
    private boolean cocoricoAtSunriseEnabled;

    private static final Logger logger = LoggerFactory.getLogger(ManageDoorOpeningEvent.class);

    public ManageDoorOpeningEvent(SunTimeManager sunTimeManager, CameraService cameraService,
                                  DoorService doorService, MusicService musicService,
                                  FanService fanService, WifiService wifiService,
                                  NotificationService notificationService,
                                  ConsumptionModeController consumptionModeController) {
        this.sunTimeManager = sunTimeManager;
        this.cameraService = cameraService;
        this.doorService = doorService;
        this.musicService = musicService;
        this.fanService = fanService;
        this.wifiService = wifiService;
        this.notificationService = notificationService;
        this.consumptionModeController = consumptionModeController;
    }

    public void manageDoorOpeningEvent(LocalDateTime currentTime) {
        OpeningPlan openingPlan = OpeningPlan.from(
                currentTime,
                sunTimeManager.getNextDoorOpeningTime(),
                doorService.doorIsOpened(),
                consumptionModeController.isEcoMode(currentTime),
                cocoricoAtSunriseEnabled
        );

        if (!openingPlan.shouldRun()) {
            return;
        }

        if (openingPlan.shouldOpenDoor()) {
            playPreOpeningActions(openingPlan);
            runDoorOpening();
        }

        applyPostOpeningActions(openingPlan);
        sunTimeManager.reloadDoorOpeningTime();
    }

    private void playPreOpeningActions(OpeningPlan openingPlan) {
        if (openingPlan.shouldPlayCocorico()) {
            musicService.cocorico();
        }
    }

    private void runDoorOpening() {
        logger.info("door opening event is starting now.");
        wifiService.turnOn();

        Optional<File> picBeforeOpening = cameraService.takePictureNoException(true);
        boolean isCorrectlyOpened = doorService.openDoorWithUpButtonManagment(false, false);

        notificationService.doorOpeningEvent(isCorrectlyOpened, picBeforeOpening);
    }

    private void applyPostOpeningActions(OpeningPlan openingPlan) {
        if (openingPlan.shouldSwitchFanOn()) {
            fanService.switchOn();
            return;
        }

        if (openingPlan.shouldTurnWifiOffLater()) {
            // turn off the wifi in 15 minutes
            wifiService.turnOffAfter(900);
        }
    }

    static final class OpeningPlan {

        private final boolean shouldRun;
        private final boolean shouldOpenDoor;
        private final boolean shouldPlayCocorico;
        private final boolean shouldSwitchFanOn;
        private final boolean shouldTurnWifiOffLater;

        private OpeningPlan(boolean shouldRun,
                            boolean shouldOpenDoor,
                            boolean shouldPlayCocorico,
                            boolean shouldSwitchFanOn,
                            boolean shouldTurnWifiOffLater) {
            this.shouldRun = shouldRun;
            this.shouldOpenDoor = shouldOpenDoor;
            this.shouldPlayCocorico = shouldPlayCocorico;
            this.shouldSwitchFanOn = shouldSwitchFanOn;
            this.shouldTurnWifiOffLater = shouldTurnWifiOffLater;
        }

        static OpeningPlan from(LocalDateTime currentTime,
                                LocalDateTime nextOpeningTime,
                                boolean doorAlreadyOpened,
                                boolean ecoMode,
                                boolean cocoricoEnabled) {
            boolean shouldRun = currentTime.isAfter(nextOpeningTime);
            boolean shouldOpenDoor = shouldRun && !doorAlreadyOpened;
            boolean shouldPlayCocorico = shouldOpenDoor && cocoricoEnabled && !ecoMode;
            boolean shouldSwitchFanOn = shouldRun && !ecoMode;
            boolean shouldTurnWifiOffLater = shouldRun && ecoMode;

            return new OpeningPlan(
                    shouldRun,
                    shouldOpenDoor,
                    shouldPlayCocorico,
                    shouldSwitchFanOn,
                    shouldTurnWifiOffLater
            );
        }

        boolean shouldRun() {
            return shouldRun;
        }

        boolean shouldOpenDoor() {
            return shouldOpenDoor;
        }

        boolean shouldPlayCocorico() {
            return shouldPlayCocorico;
        }

        boolean shouldSwitchFanOn() {
            return shouldSwitchFanOn;
        }

        boolean shouldTurnWifiOffLater() {
            return shouldTurnWifiOffLater;
        }
    }
}
