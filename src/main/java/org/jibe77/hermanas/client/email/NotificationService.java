package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.scheduler.sun.SunTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds the localized body of every coop notification mail and hands it off to
 * {@link EmailService} for delivery.
 *
 * <p>Each opt-in user has a preferred language ({@link HermanasUser#getLanguage()})
 * — we group recipients by that language and render the template once per group so
 * a French opt-in receives the FR version while an English opt-in receives the EN
 * version for the same physical event.</p>
 */
@Service
public class NotificationService {

    public static final String RETURN_TO_NEXT_LINE = "\r\n";

    @Autowired(required = false)
    private HermanasUserRepository userRepository;

    private CameraService cameraService;

    private EmailService emailService;

    private SunTimeManager sunTimeManager;

    private final MessageSource messageSource;

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(EmailService emailService, CameraService cameraService,
                               MessageSource messageSource, SunTimeManager sunTimeManager) {
        this.cameraService = cameraService;
        this.emailService = emailService;
        this.messageSource = messageSource;
        this.sunTimeManager = sunTimeManager;
    }

    /**
     * Returns opt-in users grouped by language code ("fr", "en", …). Empty when no one
     * has notifications enabled — the caller short-circuits the snapshot capture in
     * that case (expensive on the Pi Zero).
     */
    private Map<String, List<String>> recipientsByLanguage() {
        if (userRepository == null) {
            return Collections.emptyMap();
        }
        try {
            List<HermanasUser> users = userRepository.findByNotificationsEnabledTrue();
            // LinkedHashMap so iteration order is deterministic (FR first when several
            // languages are present in a single run). Unsupported codes are bucketed under
            // "fr" so the message bundle always has a matching properties file.
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (HermanasUser u : users) {
                String email = u.getEmail();
                if (email == null || email.trim().isEmpty()) {
                    continue;
                }
                String lang = u.getLanguage();
                if (!"en".equals(lang) && !"ro".equals(lang)) {
                    lang = "fr";
                }
                grouped.computeIfAbsent(lang, k -> new java.util.ArrayList<>()).add(email);
            }
            return grouped;
        } catch (Exception e) {
            logger.warn("Failed to query opt-in users; skipping notification.", e);
            return Collections.emptyMap();
        }
    }

    public void doorOpeningEvent(boolean isOpenedCorrectly, Optional<File> picBeforeOpening) {
        Map<String, List<String>> recipients = recipientsByLanguage();
        if (recipients.isEmpty()) {
            logger.info("Notification 'doorOpeningEvent' not activated.");
            return;
        }

        // Snapshot is expensive — take it once and reuse it across language groups.
        Optional<File> picWithClosedDoor = cameraService.takePictureNoException(true);
        String pictureKey = pickPictureKey(picBeforeOpening, picWithClosedDoor);

        for (Map.Entry<String, List<String>> entry : recipients.entrySet()) {
            Locale locale = Locale.forLanguageTag(entry.getKey());
            String title = messageSource.getMessage(
                    isOpenedCorrectly ? "event.mail.opening.ok.title" : "event.mail.opening.ko.title",
                    null, locale);
            StringBuilder message = new StringBuilder();
            message.append(messageSource.getMessage(
                    isOpenedCorrectly ? "event.mail.opening.ok.message" : "event.mail.opening.ko.message",
                    null, locale));
            message.append(RETURN_TO_NEXT_LINE);
            message.append(messageSource.getMessage(
                    "event.mail.opening.closing.time",
                    new Object[]{sunTimeManager.getNextDoorClosingTime().format(
                            DateTimeFormatter.ofPattern(SunTimeManager.HH_MM))},
                    locale));
            message.append(messageSource.getMessage(pictureKey, null, locale));

            emailService.sendMailTo(entry.getValue(), title, message.toString(),
                    picBeforeOpening, picWithClosedDoor);
        }
    }

    public void doorClosingEvent(boolean isClosedCorrectly) {
        Map<String, List<String>> recipients = recipientsByLanguage();
        if (recipients.isEmpty()) {
            logger.info("Notification 'doorClosingEvent' not activated.");
            return;
        }

        Optional<File> picWithClosedDoor = cameraService.takePictureNoException(true);
        String pictureKey = picWithClosedDoor.isPresent()
                ? "event.mail.with_picture.message"
                : "event.mail.without_picture.message";

        for (Map.Entry<String, List<String>> entry : recipients.entrySet()) {
            Locale locale = Locale.forLanguageTag(entry.getKey());
            String title = messageSource.getMessage(
                    isClosedCorrectly ? "event.mail.closing.ok.title" : "event.mail.closing.ko.title",
                    null, locale);
            StringBuilder message = new StringBuilder();
            message.append(messageSource.getMessage(
                    isClosedCorrectly ? "event.mail.closing.ok.message" : "event.mail.closing.ko.message",
                    null, locale));
            message.append(RETURN_TO_NEXT_LINE);
            message.append(messageSource.getMessage(
                    "event.mail.closing.opening.time",
                    new Object[]{sunTimeManager.getNextDoorOpeningTime().format(
                            DateTimeFormatter.ofPattern(SunTimeManager.HH_MM))},
                    locale));
            message.append(messageSource.getMessage(pictureKey, null, locale));

            emailService.sendMailTo(entry.getValue(), title, message.toString(), picWithClosedDoor);
        }
    }

    /**
     * Fires when the morning/evening door-verification scheduler detects that
     * the door is not in the expected state (open in the morning, closed in
     * the evening). The {@code aiRationale} string is the raw LLM reply we used
     * to confirm the mismatch — kept verbatim in the body so the operator can
     * judge whether the model was right.
     *
     * @param isMorning    true for the post-sunrise check (expected: open),
     *                     false for the post-sunset check (expected: closed).
     * @param snapshot     the snapshot we sent to the AI, attached to the mail
     *                     when present.
     * @param aiRationale  short text returned by the AI, trimmed by the caller.
     */
    public void doorStateMismatchEvent(boolean isMorning, Optional<File> snapshot, String aiRationale) {
        Map<String, List<String>> recipients = recipientsByLanguage();
        if (recipients.isEmpty()) {
            logger.info("Notification 'doorStateMismatchEvent' not activated.");
            return;
        }
        for (Map.Entry<String, List<String>> entry : recipients.entrySet()) {
            Locale locale = Locale.forLanguageTag(entry.getKey());
            String titleKey = isMorning
                    ? "event.mail.verification.morning.title"
                    : "event.mail.verification.evening.title";
            String messageKey = isMorning
                    ? "event.mail.verification.morning.message"
                    : "event.mail.verification.evening.message";
            String title = messageSource.getMessage(titleKey, null, locale);
            StringBuilder message = new StringBuilder();
            message.append(messageSource.getMessage(messageKey, null, locale));
            message.append(RETURN_TO_NEXT_LINE).append(RETURN_TO_NEXT_LINE);
            message.append(messageSource.getMessage(
                    "event.mail.verification.ai.rationale",
                    new Object[]{aiRationale == null ? "" : aiRationale},
                    locale));
            message.append(RETURN_TO_NEXT_LINE).append(RETURN_TO_NEXT_LINE);
            String pictureKey = snapshot.isPresent()
                    ? "event.mail.with_picture.message"
                    : "event.mail.without_picture.message";
            message.append(messageSource.getMessage(pictureKey, null, locale));

            emailService.sendMailTo(entry.getValue(), title, message.toString(), snapshot);
        }
    }

    /** Chooses the picture paragraph based on how many snapshots are actually available. */
    private static String pickPictureKey(Optional<File> a, Optional<File> b) {
        if (a.isPresent() && b.isPresent()) {
            return "event.mail.with_pictures.message";
        }
        if (a.isPresent() || b.isPresent()) {
            return "event.mail.with_picture.message";
        }
        return "event.mail.without_picture.message";
    }
}
