package org.jibe77.hermanas.scheduler.event;

import org.jibe77.hermanas.client.ai.AiVisionClient;
import org.jibe77.hermanas.client.ai.AiVisionException;
import org.jibe77.hermanas.client.email.NotificationService;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.door.DoorService;
import org.jibe77.hermanas.service.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Morning and evening sanity check on the chicken-coop door.
 *
 * <p>Fires from {@link org.jibe77.hermanas.scheduler.job.SunRelatedJob} once per
 * minute. At {@code sunrise + door.verification.delay_after_event.minutes} the
 * door must be open; at {@code sunset + ...} the door must be closed.</p>
 *
 * <p>The check reads the limit switches first (cheap, instant). If the
 * switches disagree with the expected state OR are ambiguous (both released —
 * door possibly stuck mid-travel or a switch is faulty), we capture a snapshot
 * and ask the AI vision model whether the door (bottom-left corner of the
 * frame) is in the expected state. The AI reply is parsed for the leading
 * keyword {@code OPEN} / {@code CLOSED} / {@code UNCERTAIN}; only a clear
 * mismatch triggers the email notification.</p>
 *
 * <p>A cooldown ({@code door.verification.cooldown.hours}, default 6 h)
 * prevents the per-minute scheduler from re-firing the AI call and the email
 * inside the same morning/evening window.</p>
 */
@Component
public class ManageDoorVerificationEvent {

    private static final Logger logger = LoggerFactory.getLogger(ManageDoorVerificationEvent.class);

    private final SunTimeManager sunTimeManager;
    private final DoorService doorService;
    private final CameraService cameraService;
    private final AiVisionClient aiVisionClient;
    private final NotificationService notificationService;
    private final EventService eventService;

    @Value("${door.verification.delay_after_event.minutes:30}")
    private int delayAfterEventMinutes;

    @Value("${door.verification.cooldown.hours:6}")
    private int cooldownHours;

    // Per-window deduplication. Reset to null when the relevant sun event is
    // recomputed for the next day; otherwise we'd keep silencing legitimate
    // alarms 24 h later.
    private LocalDateTime lastMorningCheck;
    private LocalDateTime lastEveningCheck;

    public ManageDoorVerificationEvent(SunTimeManager sunTimeManager,
                                       DoorService doorService,
                                       CameraService cameraService,
                                       AiVisionClient aiVisionClient,
                                       NotificationService notificationService,
                                       EventService eventService) {
        this.sunTimeManager = sunTimeManager;
        this.doorService = doorService;
        this.cameraService = cameraService;
        this.aiVisionClient = aiVisionClient;
        this.notificationService = notificationService;
        this.eventService = eventService;
    }

    public void manageDoorVerificationEvent(LocalDateTime currentTime) {
        LocalDateTime morningDeadline = sunTimeManager.getNextDoorOpeningTime()
                .plusMinutes(delayAfterEventMinutes);
        LocalDateTime eveningDeadline = sunTimeManager.getNextDoorClosingTime()
                .plusMinutes(delayAfterEventMinutes);

        // The "next" door-opening time flips to tomorrow as soon as it passes,
        // so we look at how recent it is relative to "now". The morning window
        // is "deadline reached, not yet in cooldown".
        if (currentTime.isAfter(morningDeadline) && shouldRunMorning(currentTime)) {
            lastMorningCheck = currentTime;
            runCheck(true);
        }
        if (currentTime.isAfter(eveningDeadline) && shouldRunEvening(currentTime)) {
            lastEveningCheck = currentTime;
            runCheck(false);
        }
    }

    private boolean shouldRunMorning(LocalDateTime now) {
        return lastMorningCheck == null
                || lastMorningCheck.plusHours(cooldownHours).isBefore(now);
    }

    private boolean shouldRunEvening(LocalDateTime now) {
        return lastEveningCheck == null
                || lastEveningCheck.plusHours(cooldownHours).isBefore(now);
    }

    private void runCheck(boolean isMorning) {
        String window = isMorning ? "morning (sunrise+" + delayAfterEventMinutes + "min)"
                                  : "evening (sunset+" + delayAfterEventMinutes + "min)";
        logger.info("door verification: starting {} check.", window);

        // Limit-switch reading — never moves the servo.
        boolean isOpened = doorService.doorIsOpened();
        boolean isClosed = doorService.doorIsClosed();

        if (isMorning && isOpened) {
            logger.info("door verification: morning check OK — door is open.");
            return;
        }
        if (!isMorning && isClosed) {
            logger.info("door verification: evening check OK — door is closed.");
            return;
        }

        // From here on, either the wrong switch is pressed (firmly wrong state)
        // or neither switch is pressed (ambiguous). In both cases we ask the AI
        // to confirm before firing the email — the limit switches occasionally
        // misread (the bottom switch can stay released when the latch is just
        // shy of the contact), and we don't want to spam users on a sensor blip.
        logger.warn("door verification: switches say opened={} closed={} — escalating to AI.",
                isOpened, isClosed);

        Optional<File> snapshot = cameraService.takePictureNoException(true);
        if (snapshot.isEmpty()) {
            logger.warn("door verification: snapshot unavailable — skipping AI check, no email sent.");
            eventService.recordAuto(EventType.DOOR_POSITION_UNKNOWN,
                    "verification: " + window + " — switches ambiguous, snapshot unavailable");
            return;
        }

        String reply;
        try {
            reply = aiVisionClient.analyzeDoorState(snapshot.get(), isMorning);
        } catch (AiVisionException e) {
            logger.warn("door verification: AI analysis failed — skipping email.", e);
            eventService.recordAuto(EventType.DOOR_POSITION_UNKNOWN,
                    "verification: " + window + " — switches ambiguous, AI error: " + e.getMessage());
            return;
        }

        String verdict = leadingKeyword(reply);
        logger.info("door verification: AI verdict='{}' (raw reply head: '{}').",
                verdict, headSnippet(reply));

        boolean mismatch = (isMorning && "CLOSED".equals(verdict))
                || (!isMorning && "OPEN".equals(verdict));
        if (!mismatch) {
            // OPEN-in-the-morning or CLOSED-in-the-evening match the expected
            // state — even though the switches were ambiguous, the door is
            // physically fine. UNCERTAIN also lands here: we'd rather skip the
            // email than send a false positive.
            logger.info("door verification: no mismatch confirmed by AI ({} expected, AI said {}).",
                    isMorning ? "OPEN" : "CLOSED", verdict);
            return;
        }

        logger.warn("door verification: confirmed mismatch — sending notification email.");
        eventService.recordAuto(EventType.DOOR_POSITION_UNKNOWN,
                "verification: " + window + " — AI confirmed door is " + verdict);
        notificationService.doorStateMismatchEvent(isMorning, snapshot, reply);
    }

    /**
     * Returns the first uppercase keyword on the first line of {@code reply},
     * restricted to the three values we asked the model to use. Anything else
     * collapses to {@code UNCERTAIN} — we'd rather skip the alert than send a
     * false positive built on a malformed reply.
     */
    static String leadingKeyword(String reply) {
        if (reply == null) {
            return "UNCERTAIN";
        }
        String firstLine = reply.split("\\R", 2)[0].trim().toUpperCase();
        if (firstLine.startsWith("OPEN")) {
            return "OPEN";
        }
        if (firstLine.startsWith("CLOSED")) {
            return "CLOSED";
        }
        return "UNCERTAIN";
    }

    private static String headSnippet(String reply) {
        if (reply == null) {
            return "";
        }
        String trimmed = reply.trim();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120) + "…";
    }
}
