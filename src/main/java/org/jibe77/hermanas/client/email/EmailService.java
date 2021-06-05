package org.jibe77.hermanas.client.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private List<MimeMessagePreparator> sendingQueue = new ArrayList<>();

    @Value("${email.notification.to}")
    private String emailNotificationTo;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${email.notification.enabled}")
    private boolean enabled;

    Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMail(String subject, String body, Optional<File>... filesToAttach)
    {
        if (enabled) {
            MimeMessagePreparator preparator = mimeMessage -> {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(emailNotificationTo));
                mimeMessage.setFrom(new InternetAddress(from));
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
            value = {MailSendException.class, MailException.class, IOException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 20000))
    private synchronized void send(MimeMessagePreparator mimeMessagePreparator) {
        logger.info("send mail now ...");
        mailSender.send(mimeMessagePreparator);
        logger.info("mail has been sent.");
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
