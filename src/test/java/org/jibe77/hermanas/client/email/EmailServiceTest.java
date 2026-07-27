package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {EmailService.class})
class EmailServiceTest {

    @Autowired
    EmailService emailService;

    @MockitoBean
    JavaMailSender javaMailSender;

    @MockitoBean
    ParameterRepository parameterRepository;

    @MockitoBean
    HermanasUserRepository userRepository;

    @org.junit.jupiter.api.BeforeEach
    void wireMocks() {
        Parameter fromRow = new Parameter();
        fromRow.setEntryKey("email.notification.from");
        fromRow.setEntryValue("test-from@example.com");
        Mockito.when(parameterRepository.findByEntryKey("email.notification.from"))
                .thenReturn(fromRow);

        // Default: one opted-in user — sendMail will queue and send.
        // Tests that need the "no recipient" scenario override this themselves.
        HermanasUser user = new HermanasUser();
        user.setLogin("alice");
        user.setEmail("alice@example.com");
        user.setNotificationsEnabled(true);
        Mockito.when(userRepository.findByNotificationsEnabledTrue())
                .thenReturn(Collections.singletonList(user));
    }

    @Test
    void sendsMailWhenAtLeastOneUserOptedIn() {
        emailService.sendMail("Subject Test", "Subject body");

        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void skipsMailWhenNoUserOptedIn() {
        Mockito.when(userRepository.findByNotificationsEnabledTrue())
                .thenReturn(Collections.emptyList());

        emailService.sendMail("Subject Test", "Subject body");

        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(0))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void keepsFailedMailInQueueOnSmtpException() {
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
    void sendsMailWithAttachmentWhenAtLeastOneUserOptedIn() {
        emailService.sendMail("Subject Test", "Subject body", Optional.of(new File("test.txt")));

        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(1))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void skipsMailWithAttachmentWhenNoUserOptedIn() {
        Mockito.when(userRepository.findByNotificationsEnabledTrue())
                .thenReturn(Collections.emptyList());

        emailService.sendMail("Subject Test", "Subject body", Optional.of(new File("test.txt")));

        assertTrue(emailService.isSendingQueueEmpty());
        Mockito.verify(javaMailSender, Mockito.times(0))
                .send((MimeMessagePreparator) Mockito.any());
    }

    @Test
    void keepsFailedMailWithAttachmentInQueueOnSmtpException() {
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
