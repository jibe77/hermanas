package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.door.DoorNotClosedCorrectlyException;
import org.jibe77.hermanas.service.energy.WifiService;
import org.jibe77.hermanas.service.event.EventService;
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

    DoorService doorService;

    NotificationService notificationService;

    MessageSource messageSource;

    WifiService wifiService;

    ConsumptionModeController consumptionModeController;

    EventService eventService;

    private static final Logger logger = LoggerFactory.getLogger(ManageDoorClosingEvent.class);

    public ManageDoorClosingEvent(SunTimeManager sunTimeManager, DoorService doorService,
                                  NotificationService notificationService, MessageSource messageSource,
                                  WifiService wifiService,
                                  ConsumptionModeController consumptionModeController,
                                  EventService eventService) {
        this.sunTimeManager = sunTimeManager;
        this.doorService = doorService;
        this.notificationService = notificationService;
        this.messageSource = messageSource;
        this.wifiService = wifiService;
        this.consumptionModeController = consumptionModeController;
        this.eventService = eventService;
    }

    public void manageDoorClosingEvent(LocalDateTime currentTime) {
        if (currentTime.isAfter(sunTimeManager.getNextDoorClosingTime())) {
            if (!doorService.doorIsClosed()) {
                try {
                    wifiService.turnOn();
                    logger.info("start door closing job at sunset.");
                    doorService.closeDoorWithBottormButtonManagement(false);
                    eventService.recordAuto(EventType.DOOR_CLOSED, "auto: sunset");
                    notificationService.doorClosingEvent(true);
                    logger.info("take picture once the door is closed and send it by email.");

                } catch (DoorNotClosedCorrectlyException e) {
                    logger.error("Didn't close the door correctly.");
                    eventService.recordAuto(EventType.DOOR_CLOSE_FAILED, "auto: sunset");
                    notificationService.doorClosingEvent(false);
                }
            } else {
                logger.info("door has already been closed before, nothing to do in this event.");
            }
            if (consumptionModeController.isEcoMode(LocalDateTime.now())) {
                // turn off the wifi in 15 minutes
                wifiService.turnOffAfter(900);
            }
            sunTimeManager.reloadDoorClosingTime();
        }
    }
}
