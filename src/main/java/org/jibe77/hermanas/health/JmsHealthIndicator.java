package org.jibe77.hermanas.health;

import org.jibe77.hermanas.client.jms.EmailService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
public class JmsHealthIndicator implements HealthIndicator {

    EmailService emailService;

    public JmsHealthIndicator(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public Health health() {
        File file = new File(
                getClass().getClassLoader().getResource("jms-indicator-test-file.jpg").getFile()
        );
        emailService.sendPicture("Actuator Test", Optional.ofNullable(file));
        return Health.up().build();
    }
}
