package org.jibe77.hermanas.websocket;

import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController {

    Logger logger = LoggerFactory.getLogger(NotificationController.class);

    MessageSendingOperations<String> wsTemplate;

    private static final String TOPIC = "/socket/progress";

    public NotificationController(MessageSendingOperations<String> wsTemplate) {
        this.wsTemplate = wsTemplate;
    }

    private StatusEnum statusEnum = StatusEnum.OFF;


    public void notify(CoopStatus coopStatus) {
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
                try {
                    Thread.sleep(3*1000); // Each 3 sec.
                    logger.info("publishing test {} ... ");
                    if (statusEnum == StatusEnum.OFF) {
                        statusEnum = StatusEnum.ON;
                    } else {
                        statusEnum = StatusEnum.OFF;
                    }
                    this.wsTemplate.convertAndSend(TOPIC, new CoopStatus(Appliance.WEBCAM, statusEnum));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
