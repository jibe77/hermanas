package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private List<MimeMessagePreparator> sendingQueue = new ArrayList<>();

    private final org.jibe77.hermanas.service.config.ConfigService configService;

    private String getEmailNotificationFrom() {
        return configService == null ? null : configService.getEmailNotificationFrom();
    }

    @Autowired(required = false)
    private HermanasUserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // @Lazy on configService: in 0.8.8 we discovered configService was being
    // injected as null at construction time — likely a hidden circular dependency
    // (ConfigService pulls EventService which pulls ... eventually EmailService?)
    // that Spring 2.7 was silently resolving by handing us a null. A @Lazy proxy
    // defers the actual bean lookup to the first method call, so the full context
    // is up before we touch configService, breaking the cycle without a rewrite.
    public EmailService(JavaMailSender mailSender,
                        @Lazy org.jibe77.hermanas.service.config.ConfigService configService) {
        this.mailSender = mailSender;
        this.configService = configService;
    }

    /**
     * Returns the JavaMailSender to use for the next send. Builds a fresh
     * {@link JavaMailSenderImpl} from {@link org.jibe77.hermanas.service.config.ConfigService}
     * whenever a host has been configured at runtime, so a change made through
     * /api/v1/config/mail/* takes effect on the next send without restart.
     * <p>Falls back to the injected Spring-Boot autoconfigured sender otherwise — that
     * keeps tests (which mock JavaMailSender) and the historical application.properties
     * path working unchanged.
     */
    private JavaMailSender resolveSender() {
        if (configService == null) {
            return mailSender;
        }
        String host = configService.getMailHost();
        if (host == null || host.trim().isEmpty()) {
            return mailSender;
        }
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(configService.getMailPort());
        String username = configService.getMailUsername();
        if (username != null && !username.isEmpty()) {
            impl.setUsername(username);
        }
        String password = configService.getMailPassword();
        if (password != null && !password.isEmpty()) {
            impl.setPassword(password);
        }
        Properties props = impl.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(configService.isMailSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(configService.isMailStartTlsEnable()));
        return impl;
    }

    public void sendMail(String subject, String body, Optional<File>... filesToAttach)
    {
        sendMailTo(resolveRecipients(), subject, body, filesToAttach);
    }

    /**
     * Sends a mail to an explicit list of recipients, bypassing the opt-in resolution. Used for
     * one-off transactional mails such as registration confirmation and the admin notification of
     * a pending account.
     */
    @SafeVarargs
    public final void sendMailTo(List<String> recipients, String subject, String body,
                                 Optional<File>... filesToAttach) {
        if (recipients == null || recipients.isEmpty()) {
            logger.info("No recipients for mail '{}' — skipping.", subject);
            return;
        }
        // From address is required by SMTP; bailing here keeps a misconfigured
        // ConfigService entry from crashing the scheduler's @Scheduled thread,
        // which had no chance to recover and left the morning mail dropped.
        String from = getEmailNotificationFrom();
        if (from == null || from.trim().isEmpty()) {
            logger.warn("Skipping mail '{}': email.notification.from is not set (resolved value is [{}]).",
                    subject, from);
            return;
        }
        MimeMessagePreparator preparator = mimeMessage -> {
            InternetAddress[] to = recipients.stream()
                    .map(address -> {
                        try {
                            return new InternetAddress(address);
                        } catch (javax.mail.internet.AddressException e) {
                            logger.warn("Skipping invalid recipient address '{}'.", address);
                            return null;
                        }
                    })
                    .filter(a -> a != null)
                    .toArray(InternetAddress[]::new);
            mimeMessage.setRecipients(Message.RecipientType.TO, to);
            mimeMessage.setFrom(new InternetAddress(from));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(body);

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            if (filesToAttach != null) {
                for (Optional<File> fileToAttach : filesToAttach) {
                    if (fileToAttach != null && fileToAttach.isPresent()) {
                        FileSystemResource file = new FileSystemResource(fileToAttach.get());
                        helper.addAttachment(file.getFilename(), file);
                    }
                }
            }

            helper.setText(body, true);
        };
        sendingQueue.add(preparator);
        processSendingQueue();
    }

    /**
     * Resolves the list of recipients for an automated notification: every user in the
     * {@code hermanas_user} table with {@code notificationsEnabled=true} and a non-blank
     * email address. Returns an empty list when no one is opted in — the queued send
     * path logs that fact and skips the message rather than fail.
     */
    private List<String> resolveRecipients() {
        if (userRepository == null) {
            return Collections.emptyList();
        }
        try {
            return userRepository.findByNotificationsEnabledTrue().stream()
                    .map(HermanasUser::getEmail)
                    .filter(e -> e != null && !e.trim().isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to load opted-in users from database; notification will be skipped.", e);
            return Collections.emptyList();
        }
    }

    public synchronized void processSendingQueue() {
        logger.info("start processing sending queue.");
        Iterator<MimeMessagePreparator> it = sendingQueue.iterator();
        while (it.hasNext()) {
            try {
                MimeMessagePreparator mimeMessagePreparator = it.next();
                send(mimeMessagePreparator);
                it.remove();
            } catch (MailException ex) {
                logger.error("Can't send email", ex);
            } catch (RuntimeException ex) {
                // Defensive: a NullPointerException raised inside the preparator
                // (e.g. invalid From address) used to escape this loop and crash
                // the scheduler thread that had triggered the send. Drop the
                // poisoned message so the queue keeps draining.
                logger.error("Dropping email due to unexpected error", ex);
                it.remove();
            }
        }
        logger.info("sending queue has been processed.");
    }

    @Retryable(
            value = {MailSendException.class, MailException.class, IOException.class, AuthenticationFailedException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 20000))
    private synchronized void send(MimeMessagePreparator mimeMessagePreparator) {
        logger.info("send mail now ...");
        resolveSender().send(mimeMessagePreparator);
        logger.info("mail has been sent.");
    }

    /**
     * Sends a synchronous test email and propagates any failure to the caller.
     *
     * <p>Bypasses the regular {@link #sendingQueue} so that the diagnostics UI can show whether
     * the SMTP configuration actually works (auth, TLS, From address allowed by the relay, etc.)
     * instead of silently swallowing the error like the queued path.</p>
     *
     * <p>When {@code picture} is present, it is embedded inline (CID) inside an HTML body so
     * the recipient sees the chicken-coop snapshot directly in the mail body, matching the
     * style of door open/close notifications.</p>
     *
     * @param picture optional snapshot to embed in the body
     * @param recipient explicit destination address — typically the email of the
     *                  authenticated admin clicking the "send test" button.
     * @throws IllegalStateException if the recipient is blank.
     * @throws MailException if the underlying SMTP send fails.
     */
    public void sendTestMail(Optional<File> picture, String recipient) {
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalStateException("No recipient email available for the test. Set your email address in your user profile.");
        }
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipient);
            helper.setFrom(getEmailNotificationFrom());
            helper.setSubject("Hermanas — test email");

            String introText = "This is a test email triggered from the diagnostics panel. "
                    + "If you read this, the SMTP configuration works.";
            if (picture.isPresent()) {
                String html = "<p>" + introText + "</p>"
                        + "<p>Latest chicken-coop snapshot:</p>"
                        + "<p><img src=\"cid:coopPicture\" style=\"max-width:100%;height:auto;\" /></p>";
                helper.setText(html, true);
                helper.addInline("coopPicture", picture.get());
            } else {
                helper.setText(introText + "\n(No picture available.)", false);
            }
        };
        logger.info("Sending diagnostics test email to {}.", recipient);
        resolveSender().send(preparator);
        logger.info("Diagnostics test email sent successfully.");
    }

    public boolean isSendingQueueEmpty() {
        return sendingQueue.isEmpty();
    }

    public void emptySendingQueue() {
        sendingQueue.clear();
    }
}
