package org.jibe77.hermanas.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController {

    Logger logger = LoggerFactory.getLogger(NotificationController.class);

    MessageSendingOperations<String> wsTemplate;

    private static final String TOPIC = "/topic/progress";

    public NotificationController(MessageSendingOperations<String> wsTemplate) {
        this.wsTemplate = wsTemplate;
    }

    public void notify(CoopStatus coopStatus) {
        logger.info("notifying on web-socket on appliance {} with status {}.",
                coopStatus.getAppliance(),
                coopStatus.getState());
        this.wsTemplate.convertAndSend(TOPIC, coopStatus);
    }
}
