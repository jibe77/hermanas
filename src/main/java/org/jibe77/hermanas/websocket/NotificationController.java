package org.jibe77.hermanas.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.HashMap;

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

    /**
     * Generate random numbers publish with WebSocket protocol each 3 seconds.
     * @return a command line runner.
     */
    @Bean
    public CommandLineRunner websocketDemo() {
        return (args) -> {
            while (true) {
                logger.info("send a message on web socket.");
                try {
                    Thread.sleep(3*1000); // Each 3 sec.
                    HashMap<String, Integer> progress = new HashMap();
                    progress.put("num1", 0);
                    progress.put("num2", 100);
                    this.wsTemplate.convertAndSend(TOPIC, progress);
                    logger.info("sent a message on web socket.");
                } catch (Exception e) {
                    logger.error("Error sending web-socket message.", e);
                }
            }
        };
    }
}
