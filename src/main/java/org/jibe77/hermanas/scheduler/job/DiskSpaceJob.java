package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DiskSpaceJob {

    DiskSpaceHealthIndicator diskSpaceHealthIndicator;

    EmailService emailService;

    MessageSource messageSource;

    Logger logger = LoggerFactory.getLogger(DiskSpaceJob.class);

    public DiskSpaceJob(DiskSpaceHealthIndicator diskSpaceHealthIndicator, EmailService emailService, MessageSource messageSource) {
        this.diskSpaceHealthIndicator = diskSpaceHealthIndicator;
        this.emailService = emailService;
        this.messageSource = messageSource;
    }

    @Scheduled(fixedDelayString = "${diskspace.scheduler.delay.in.milliseconds}")
    public void verifyDiskSpace() {
        if (!diskSpaceHealthIndicator.getHealth(false).getStatus().equals(Status.UP)) {
            logger.error("Send email now because disk space is below threshold.");
            emailService.sendMail(
                    messageSource.getMessage("diskspace.down.title", null, Locale.getDefault()),
                    messageSource.getMessage("diskspace.down.message", null, Locale.getDefault()));
        }
    }

}
