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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(emailService.isSendingQueueEmpty());

        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailDisabled() {
        emailService.setEnabled(false);

        emailService.sendMail("Subject Test", "Subject body");
        assertTrue(emailService.isSendingQueueEmpty());
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
        assertFalse(emailService.isSendingQueueEmpty());
        emailService.emptySendingQueue();
        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailWithAttachmentEnabled() {
        emailService.setEnabled(true);

        emailService.sendMail("Subject Test", "Subject body", Optional.of(new File("test.txt")));
        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailWithAttachmentDisabled() {
        emailService.setEnabled(false);

        emailService.sendMail("Subject Test", "Subject body", Optional.of(new File("test.txt")));
        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(0))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void testEmailWithAttachmentWithException() {
        emailService.setEnabled(true);
        Mockito.doThrow(new MailSendException("Test Exception"))
                .when(javaMailSender)
                .send((MimeMessagePreparator) Mockito.any());

        emailService.sendMail("Subject Test", "Subject body", Optional.of(new File("test.txt")));
        assertFalse(emailService.isSendingQueueEmpty());
        emailService.emptySendingQueue();
        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }
}
