package org.jibe77.hermanas.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class ButtonNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(ButtonNotificationController.class);

    private static final String TOPIC = "/topic/buttons";

    private final MessageSendingOperations<String> wsTemplate;

    public ButtonNotificationController(MessageSendingOperations<String> wsTemplate) {
        this.wsTemplate = wsTemplate;
    }

    public void notify(ButtonStatus status) {
        logger.debug("notifying on web-socket button {} pressed={}.",
                status.getButton(), status.isPressed());
        this.wsTemplate.convertAndSend(TOPIC, status);
    }
}
