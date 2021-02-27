package org.jibe77.hermanas.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.util.HtmlUtils;

@Controller
@CrossOrigin(origins = "*")
public class GreetingController {

    Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @MessageMapping("/progress")
    @SendTo("/socket/progress")
    public Greeting websocketDemo(HelloMessage message) throws Exception {
        logger.info("sending message from /process, send to /socket/progress with message {}.", message.getName());
        Thread.sleep(1000); // simulated delay
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }
}
