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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final ParameterRepository parameterRepository;

    private final List<MimeMessagePreparator> sendingQueue = new ArrayList<>();

    @Value("${email.notification.from:}")
    private String emailNotificationFromDefault;

    @Autowired(required = false)
    private HermanasUserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender, ParameterRepository parameterRepository) {
        this.mailSender = mailSender;
        this.parameterRepository = parameterRepository;
    }

    private String getEmailNotificationFrom() {
        Parameter row = parameterRepository.findByEntryKey("email.notification.from");
        if (row != null && row.getEntryValue() != null && !row.getEntryValue().trim().isEmpty()) {
            return row.getEntryValue().trim();
        }
        if (emailNotificationFromDefault != null && !emailNotificationFromDefault.trim().isEmpty()) {
            return emailNotificationFromDefault.trim();
        }
        return null;
    }

    public void sendMail(String subject, String body, Optional<File>... filesToAttach) {
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
        String from = getEmailNotificationFrom();
        if (from == null) {
            logger.warn("Skipping mail '{}': email.notification.from is not set.", subject);
            return;
        }
        MimeMessagePreparator preparator = mimeMessage -> {
            InternetAddress[] to = recipients.stream()
                    .map(address -> {
                        try {
                            return new InternetAddress(address);
                        } catch (jakarta.mail.internet.AddressException e) {
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
                mailSender.send(it.next());
                it.remove();
            } catch (MailException ex) {
                logger.error("Can't send email", ex);
            }
        }
        logger.info("sending queue has been processed.");
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
        mailSender.send(preparator);
        logger.info("Diagnostics test email sent successfully.");
    }

    public boolean isSendingQueueEmpty() {
        return sendingQueue.isEmpty();
    }

    public void emptySendingQueue() {
        sendingQueue.clear();
    }
}
