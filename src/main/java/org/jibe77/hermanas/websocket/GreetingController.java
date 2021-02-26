package org.jibe77.hermanas.websocket;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class GreetingController {

    @MessageMapping("/progress")
    @SendTo("/socket/progress")
    public Greeting websocketDemo(HelloMessage message) throws Exception {
        Thread.sleep(1000); // simulated delay
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

    int i = 0;

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
                    websocketDemo(new HelloMessage("ceci est un test : " + i++));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
