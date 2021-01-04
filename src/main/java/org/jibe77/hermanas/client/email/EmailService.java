package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.notification.to}")
    private String emailNotificationTo;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${email.notification.enabled}")
    private boolean enabled;

    Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender, CameraController cameraController) {
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

            try {
                mailSender.send(preparator);
            } catch (MailException ex) {
                logger.error("Can't send email", ex);
            }
        }
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
