package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.controller.door.DoorController;
import org.jibe77.hermanas.controller.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ManageDoorClosingEvent {

    SunTimeManager sunTimeManager;

    DoorController doorController;

    NotificationService notificationService;

    MessageSource messageSource;

    WifiController wifiController;

    ConsumptionModeController consumptionModeController;

    Logger logger = LoggerFactory.getLogger(ManageDoorClosingEvent.class);

    public ManageDoorClosingEvent(SunTimeManager sunTimeManager, DoorController doorController,
                                  NotificationService notificationService, MessageSource messageSource,
                                  WifiController wifiController,
                                  ConsumptionModeController consumptionModeController) {
        this.sunTimeManager = sunTimeManager;
        this.doorController = doorController;
        this.notificationService = notificationService;
        this.messageSource = messageSource;
        this.wifiController = wifiController;
        this.consumptionModeController = consumptionModeController;
    }

    public void manageDoorClosingEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorClosingTime())) {
            if (!doorController.doorIsClosed()) {
                try {
                    wifiController.turnOn();
                    logger.info("start door closing job at sunset.");
                    doorController.closeDoorWithBottormButtonManagement(false);
                    notificationService.doorClosingEvent(true);
                    logger.info("take picture once the door is closed and send it by email.");

                } catch (DoorNotClosedCorrectlyException e) {
                    logger.error("Didn't close the door correctly.");
                    notificationService.doorClosingEvent(false);
                }
            } else {
                logger.info("door has already been closed before, nothing to do in this event.");
            }
            if (consumptionModeController.isEcoMode(LocalDateTime.now())) {
                // turn off the wifi in 15 minutes
                wifiController.turnOffAfter(900);
            }
            sunTimeManager.reloadDoorClosingTime();
        }
    }
}
