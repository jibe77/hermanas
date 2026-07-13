package org.jibe77.hermanas.websocket;

import org.jibe77.hermanas.service.capture.CaptureStateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

/**
 * Pushes {@link CaptureStateDto} updates onto a per-capture STOMP topic
 * ({@code /topic/captures/{id}}). Lets the SPA replace its 1-second poll on
 * {@code GET /captures/{id}/status} with a single subscription, which removes
 * the last reverse-proxy-timeout-sensitive HTTP round-trip from the capture
 * pipeline on flaky WiFi.
 */
@Controller
public class CaptureNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(CaptureNotificationController.class);

    private static final String TOPIC_PREFIX = "/topic/captures/";

    private final MessageSendingOperations<String> wsTemplate;

    public CaptureNotificationController(MessageSendingOperations<String> wsTemplate) {
        this.wsTemplate = wsTemplate;
    }

    public void notify(String captureId, CaptureStateDto state) {
        if (captureId == null || state == null) {
            return;
        }
        logger.debug("Notifying capture {} state={} imageAvailable={}.",
                captureId, state.getStatus(), state.isImageAvailable());
        this.wsTemplate.convertAndSend(TOPIC_PREFIX + captureId, state);
    }
}
