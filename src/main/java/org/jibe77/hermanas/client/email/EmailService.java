package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import javax.annotation.PostConstruct;
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

    /**
     * Optional direct handle on the {@code parameter} table. Chosen over
     * {@link org.jibe77.hermanas.service.config.ConfigService} injection because,
     * in prod we observed on Spring Boot 2.7 that a {@code @Lazy ConfigService}
     * proxy was reliably resolved at {@code @PostConstruct} time but then
     * silently reverted to a null-behaving stub several hours later — most
     * likely a fallout of the circular chain
     * {@code ConfigService → EventService → … → EmailService} combined with
     * mixed constructor + field injection. Reading the DB row ourselves is
     * simple, has no cache surface to invalidate, and removes the cycle
     * entirely. Optional so the fallback keeps working in tests that don't
     * bring the JPA context up.
     */
    @Autowired(required = false)
    private ParameterRepository parameterRepository;

    /**
     * Static fallback pulled from application.properties. Used only when the
     * DB row does not exist (or the repository is not available in tests) —
     * matches the historical @Value pathway that was reachable through
     * ConfigService.
     */
    @Value("${email.notification.from:}")
    private String emailNotificationFromDefault;

    private String getEmailNotificationFrom() {
        String fromDb = readFromParameterRow();
        if (fromDb != null) {
            return fromDb;
        }
        if (emailNotificationFromDefault == null || emailNotificationFromDefault.trim().isEmpty()) {
            return null;
        }
        return emailNotificationFromDefault.trim();
    }

    private String readFromParameterRow() {
        if (parameterRepository == null) {
            return null;
        }
        try {
            Parameter row = parameterRepository.findByEntryKey("email.notification.from");
            if (row == null) {
                return null;
            }
            String value = row.getEntryValue();
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        } catch (Exception e) {
            logger.warn("Failed to read 'email.notification.from' from the parameter table; falling back to application.properties.", e);
            return null;
        }
    }

    @Autowired(required = false)
    private HermanasUserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Startup diagnostic: prints the resolved From address once the context is
    // fully up. Catches misconfigurations (missing application.properties,
    // empty DB row) at boot time instead of at the first scheduled send
    // several hours later.
    @PostConstruct
    void logResolvedFromAtStartup() {
        try {
            String from = getEmailNotificationFrom();
            if (from == null || from.trim().isEmpty()) {
                logger.warn("Email notification init: no 'From' address resolved — outgoing mails will be skipped until email.notification.from is set.");
            } else {
                logger.info("Email notification init: 'From' address resolved to '{}'.", from);
            }
        } catch (Exception e) {
            logger.warn("Email notification init: failed to resolve 'From' address at startup.", e);
        }
    }

    /**
     * Returns the JavaMailSender to use for the next send.
     *
     * <p>Previously this rebuilt a fresh {@link JavaMailSenderImpl} on every call
     * from ConfigService so a change made through /api/v1/config/mail/* took
     * effect without restart. We dropped that path when we removed the
     * ConfigService injection (see the class-level rationale) — the runtime
     * mail settings now come straight from {@code spring.mail.*} in
     * application.properties, honoured by Spring Boot's autoconfigured
     * JavaMailSender. Changes still take effect on restart.</p>
     */
    private JavaMailSender resolveSender() {
        return mailSender;
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
