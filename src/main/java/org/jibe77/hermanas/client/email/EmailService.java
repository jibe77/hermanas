package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

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

    @Value("${email.notification.to}")
    private String emailNotificationTo;

    @Value("${email.notification.from}")
    private String emailNotificationFrom;

    @Value("${email.notification.enabled}")
    private boolean enabled;

    @Autowired(required = false)
    private HermanasUserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
        if (enabled) {
            if (recipients == null || recipients.isEmpty()) {
                logger.info("No recipients for mail '{}' — skipping.", subject);
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
                mimeMessage.setFrom(new InternetAddress(emailNotificationFrom));
                mimeMessage.setSubject(subject);
                mimeMessage.setText(body);

                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

                for (Optional<File> fileToAttach : filesToAttach) {
                    if (fileToAttach.isPresent()) {
                        FileSystemResource file = new FileSystemResource(fileToAttach.get());
                        helper.addAttachment(file.getFilename(), file);
                    }
                }

                helper.setText(body, true);
            };
            sendingQueue.add(preparator);
            processSendingQueue();
        }
    }

    /**
     * Resolves the list of recipients for an automated notification. The source of truth is the
     * {@code hermanas_user} table — every user with {@code notificationsEnabled=true} and a
     * non-blank email address. When the repository is not available or the list is empty,
     * falls back to the historical {@code email.notification.to} property so that an isolated
     * misconfiguration of the user database never causes notifications to vanish silently.
     */
    private List<String> resolveRecipients() {
        if (userRepository != null) {
            try {
                List<String> optedIn = userRepository.findByNotificationsEnabledTrue().stream()
                        .map(HermanasUser::getEmail)
                        .filter(e -> e != null && !e.trim().isEmpty())
                        .collect(Collectors.toList());
                if (!optedIn.isEmpty()) {
                    return optedIn;
                }
            } catch (Exception e) {
                logger.warn("Failed to load opted-in users from database, falling back to configured address.", e);
            }
        }
        if (emailNotificationTo == null || emailNotificationTo.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(emailNotificationTo);
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
        mailSender.send(mimeMessagePreparator);
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
     * @throws IllegalStateException if email notifications are disabled.
     * @throws MailException if the underlying SMTP send fails.
     */
    public void sendTestMail(Optional<File> picture) {
        if (!enabled) {
            throw new IllegalStateException("Email notifications are disabled (email.notification.enabled=false).");
        }
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(emailNotificationTo);
            helper.setFrom(emailNotificationFrom);
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
        logger.info("Sending diagnostics test email to {}.", emailNotificationTo);
        mailSender.send(preparator);
        logger.info("Diagnostics test email sent successfully.");
    }

    public boolean isSendingQueueEmpty() {
        return sendingQueue.isEmpty();
    }

    public void emptySendingQueue() {
        sendingQueue.clear();
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
