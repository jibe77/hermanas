package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.jibe77.hermanas.security.ssrf.DestinationValidator;
import org.jibe77.hermanas.service.config.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Une destination SMTP refusée est un problème de configuration, jamais un motif pour
 * ne pas démarrer.
 *
 * <p>Le 18 novembre 2026, <code>mail.r3n4.uk</code> a commencé à répondre
 * 192.168.1.116 depuis le réseau local (DNS split-horizon). Le validateur de
 * destinations l'a refusé, l'exception est remontée par
 * <code>processSendingQueue</code> appelé depuis le <code>@PostConstruct</code> de
 * WifiService, et le poulailler n'a plus démarré du tout — redémarrage après
 * redémarrage. L'envoi de mails se dégrade désormais en « mail désactivé ».</p>
 */
class EmailServiceRefusedRelayTest {

    private static final String REFUSAL =
            "Destination mail.r3n4.uk is not allowed for the SMTP relay: resolves to a "
                    + "private address (192.168.1.116)";

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ConfigService configService = mock(ConfigService.class);
        when(configService.getMailHost()).thenReturn("mail.r3n4.uk");
        when(configService.getMailPort()).thenReturn(587);

        DestinationValidator validator = mock(DestinationValidator.class);
        doThrow(new DestinationValidator.InvalidDestinationException(REFUSAL))
                .when(validator).validateSmtpDestination(anyString(), anyInt());

        emailService = new EmailService(
                mock(JavaMailSender.class), configService, mock(ParameterRepository.class));
        ReflectionTestUtils.setField(emailService, "destinationValidator", validator);
    }

    @Test
    void theStartupPathSurvivesARefusedRelay() {
        // C'est l'appel qui tuait le contexte au démarrage.
        assertThatCode(emailService::processSendingQueue).doesNotThrowAnyException();
    }

    @Test
    void theDiagnosticsTestReportsWhyNothingIsSent() {
        assertThatThrownBy(() -> emailService.sendTestMail(Optional.empty(), "admin@example.org"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refused");
    }
}
