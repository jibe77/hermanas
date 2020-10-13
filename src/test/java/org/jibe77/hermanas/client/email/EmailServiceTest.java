package org.jibe77.hermanas.client.email;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.File;

@SpringBootTest(classes = {EmailService.class})
class EmailServiceTest {

    @Autowired
    EmailService emailService;

    @MockBean
    JavaMailSender javaMailSender;

    @Test
    void testEmailEnabled() {
        emailService.setEnabled(true);

        emailService.sendMail("Subject Test", "Subject body");

        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailDisabled() {
        emailService.setEnabled(false);

        emailService.sendMail("Subject Test", "Subject body");

        Mockito.verify(javaMailSender, Mockito.times(0))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailEnabledWithException() {
        emailService.setEnabled(true);
        Mockito.doThrow(new MailSendException("Test Exception"))
                .when(javaMailSender)
                .send((MimeMessagePreparator) Mockito.any());

        emailService.sendMail("Subject Test", "Subject body");

        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailWithAttachmentEnabled() {
        emailService.setEnabled(true);

        emailService.sendMailWithAttachment("Subject Test", "Subject body", new File("test.txt"));

        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailWithAttachmentDisabled() {
        emailService.setEnabled(false);

        emailService.sendMailWithAttachment("Subject Test", "Subject body", new File("test.txt"));

        Mockito.verify(javaMailSender, Mockito.times(0))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailWithAttachmentWithException() {
        emailService.setEnabled(true);
        Mockito.doThrow(new MailSendException("Test Exception"))
                .when(javaMailSender)
                .send((MimeMessagePreparator) Mockito.any());

        emailService.sendMailWithAttachment("Subject Test", "Subject body", new File("test.txt"));

        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }
}
