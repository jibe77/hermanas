package org.jibe77.hermanas.client.email;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.jibe77.hermanas.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Structure of the outgoing MIME messages — what spam filters actually look at. */
class EmailServiceMimeTest {

    private final JavaMailSender fallbackSender = mock(JavaMailSender.class);
    private final ConfigService configService = mock(ConfigService.class);
    private final ParameterRepository parameterRepository = mock(ParameterRepository.class);

    private EmailService newService(String smtpHost) {
        when(configService.getMailHost()).thenReturn(smtpHost);
        Parameter from = new Parameter();
        from.setEntryKey("email.notification.from");
        from.setEntryValue("poulailler@r3n4.uk");
        when(parameterRepository.findByEntryKey("email.notification.from")).thenReturn(from);
        EmailService service = new EmailService(fallbackSender, configService, parameterRepository);
        ReflectionTestUtils.setField(service, "mailSessionHost", "r3n4.uk");
        return service;
    }

    @Test
    void messageIdUsesConfiguredMailHost() throws Exception {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) newService("smtp.example.com").resolveSender();

        assertEquals("r3n4.uk", sender.getJavaMailProperties().getProperty("mail.host"));
        MimeMessage message = sender.createMimeMessage();
        message.setText("x");
        message.saveChanges();
        assertTrue(message.getMessageID().endsWith("@r3n4.uk>"), message.getMessageID());
    }

    @Test
    void notificationCarriesPlainTextAndHtmlAlternatives() throws Exception {
        EmailService service = newService(null);
        HermanasUserRepository users = mock(HermanasUserRepository.class);
        HermanasUser alice = new HermanasUser();
        alice.setEmail("alice@example.com");
        when(users.findByNotificationsEnabledTrue()).thenReturn(List.of(alice));
        ReflectionTestUtils.setField(service, "userRepository", users);

        service.sendMail("Porte fermée", "<p>La porte est fermée.</p><p>Bonne nuit &amp; à demain</p>");

        ArgumentCaptor<MimeMessagePreparator> captor = ArgumentCaptor.forClass(MimeMessagePreparator.class);
        verify(fallbackSender).send(captor.capture());
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        captor.getValue().prepare(message);
        message.saveChanges();

        List<Part> leaves = new ArrayList<>();
        collectLeaves(message, leaves);
        Part plain = leaves.stream().filter(p -> isType(p, "text/plain")).findFirst().orElseThrow();
        Part html = leaves.stream().filter(p -> isType(p, "text/html")).findFirst().orElseThrow();
        assertEquals("La porte est fermée.\nBonne nuit & à demain", plain.getContent());
        assertTrue(((String) html.getContent()).contains("<p>La porte est fermée.</p>"));
        assertTrue(plain.getContentType().toLowerCase().contains("utf-8"), plain.getContentType());
    }

    @Test
    void htmlToPlainTextKeepsLineStructure() {
        assertEquals("Ligne 1\nLigne 2\n- un\n- deux",
                EmailService.htmlToPlainText("Ligne 1<br/>Ligne 2<ul><li>un</li><li>deux</li></ul>"));
        assertEquals("déjà du texte", EmailService.htmlToPlainText("déjà du texte"));
    }

    private static boolean isType(Part part, String type) {
        try {
            return part.isMimeType(type);
        } catch (Exception e) {
            return false;
        }
    }

    private static void collectLeaves(Part part, List<Part> leaves) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                collectLeaves(child, leaves);
            }
        } else {
            leaves.add(part);
        }
    }
}
