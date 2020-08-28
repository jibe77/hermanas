package org.jibe77.hermanas.client.jms;

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

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.Optional;

@Service("emailService")
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${email.notification.to}")
    private String emailNotificationTo;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${email.notification.enabled}")
    private boolean enabled;

    Logger logger = LoggerFactory.getLogger(EmailService.class);

    private void sendMailWithAttachment(String to, String subject, String body, File fileToAttach)
    {
        MimeMessagePreparator preparator = mimeMessage -> {
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress(from));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(body);

            FileSystemResource file = new FileSystemResource(fileToAttach);
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.addAttachment("logo.jpg", file);
            helper.setText("", true);
        };

        try {
            mailSender.send(preparator);
        }
        catch (MailException ex) {
           logger.error("Can't send email", ex);
        }
    }

    public void sendPicture(String emailNotificationSubject, Optional<File> pic) {
        if (enabled) {
            if (pic.isPresent()) {
                logger.info("send email notification with subject {}", emailNotificationSubject);
                sendMailWithAttachment(
                        emailNotificationTo,
                        emailNotificationSubject,
                        "Voici une photo des cocottes.",
                        pic.get());
            } else {
                logger.warn("can't send email notification with subject {} because pic is not available.",
                        emailNotificationSubject);
            }
        }
    }
}
