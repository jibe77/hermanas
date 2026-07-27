package org.jibe77.hermanas.scheduler.job;

import org.jibe77.hermanas.client.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.application.DiskSpaceHealthIndicator;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Component
public class DiskSpaceJob {

    DiskSpaceHealthIndicator diskSpaceHealthIndicator;

    EmailService emailService;

    MessageSource messageSource;

    private static final Logger logger = LoggerFactory.getLogger(DiskSpaceJob.class);
    private static final long ONE_MB = 1024L * 1024L;
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DiskSpaceJob(DiskSpaceHealthIndicator diskSpaceHealthIndicator, EmailService emailService, MessageSource messageSource) {
        this.diskSpaceHealthIndicator = diskSpaceHealthIndicator;
        this.emailService = emailService;
        this.messageSource = messageSource;
    }

    @Scheduled(fixedDelayString = "${diskspace.scheduler.delay.in.milliseconds}")
    public void verifyDiskSpace() {
        Health health = diskSpaceHealthIndicator.health(true);
        if (health.getStatus().equals(Status.UP)) {
            return;
        }
        Object[] args = extractArgs(health);
        logger.error("Send email now because disk space is below threshold: host={} path={} free={} MB total={} MB threshold={} MB usage={}%",
                args[0], args[1], args[2], args[3], args[4], args[5]);
        Locale locale = Locale.getDefault();
        emailService.sendMail(
                messageSource.getMessage("diskspace.down.title", args, locale),
                messageSource.getMessage("diskspace.down.message", args, locale));
    }

    /**
     * Reads the path / free / total / threshold the Spring actuator indicator
     * stashes in its {@code details} map and turns them into the placeholder
     * tuple consumed by the i18n bundle:
     * <pre>{@code {0}=host {1}=path {2}=free MB {3}=total MB {4}=threshold MB
     *           {5}=usage % {6}=timestamp}</pre>
     * Falls back to {@code "?"} for any missing field so a partial indicator
     * still produces a readable email.
     */
    private Object[] extractArgs(Health health) {
        Map<String, Object> details = health.getDetails();
        String path = stringDetail(details, "path");
        long free = longDetail(details, "free");
        long total = longDetail(details, "total");
        long threshold = longDetail(details, "threshold");
        long usagePercent = total > 0 ? Math.round((double) (total - free) * 100.0 / total) : -1;
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "?";
        }
        return new Object[]{
                host,
                path,
                free >= 0 ? free / ONE_MB : "?",
                total >= 0 ? total / ONE_MB : "?",
                threshold >= 0 ? threshold / ONE_MB : "?",
                usagePercent >= 0 ? usagePercent : "?",
                LocalDateTime.now().format(TS_FORMAT)
        };
    }

    private static String stringDetail(Map<String, Object> details, String key) {
        if (details == null) {
            return "?";
        }
        Object v = details.get(key);
        return v == null ? "?" : v.toString();
    }

    private static long longDetail(Map<String, Object> details, String key) {
        if (details == null) {
            return -1L;
        }
        Object v = details.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return -1L;
    }

}
