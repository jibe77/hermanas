package org.jibe77.hermanas.security;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.data.repository.ResidentRepository;
import org.jibe77.hermanas.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La registration est le dernier oracle d'enumeration qui reste, depuis que le login
 * renvoie une reponse uniforme (AUTH-VULN-02). Ce que ces tests verrouillent :
 *
 * <ul>
 *   <li>une erreur de forme ou de mot de passe est renvoyee <em>avant</em> toute lookup,
 *       donc une sonde au mot de passe faible n'apprend rien ;</li>
 *   <li>le message de l'identifiant pris ne confirme plus textuellement l'existence du
 *       compte (le code, lui, reste — voir le commentaire dans le service).</li>
 * </ul>
 */
class RegistrationServiceEnumerationTest {

    private HermanasUserRepository repository;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        repository = mock(HermanasUserRepository.class);
        PasswordPolicy policy = new PasswordPolicy(mock(ResidentRepository.class));
        service = new RegistrationService(
                repository,
                mock(PasswordEncoder.class),
                mock(EmailService.class),
                mock(MessageSource.class),
                policy);
    }

    private static RegisterRequest request(String login, String password) {
        RegisterRequest body = new RegisterRequest();
        body.setLogin(login);
        body.setPassword(password);
        body.setEmail("quelquun@exemple.fr");
        return body;
    }

    @Test
    void aWeakPasswordNeverRevealsThatTheLoginExists() {
        when(repository.existsByLogin("marguerite")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request("marguerite", "court")))
                .isInstanceOf(RegistrationService.RegistrationException.class)
                .extracting("code")
                .isEqualTo("PASSWORD_TOO_SHORT");

        // Le service ne doit meme pas regarder si le login existe a ce stade.
        verify(repository, never()).existsByLogin(any());
    }

    @Test
    void aMalformedLoginIsRejectedBeforeAnyLookup() {
        assertThatThrownBy(() -> service.register(request("a", "CorrectHorseBattery9")))
                .isInstanceOf(RegistrationService.RegistrationException.class)
                .extracting("code")
                .isEqualTo("INVALID_LOGIN");

        verify(repository, never()).existsByLogin(any());
    }

    @Test
    void aTakenLoginIsRefusedWithoutSpellingOutThatItExists() {
        when(repository.existsByLogin("marguerite")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request("marguerite", "CorrectHorseBattery9")))
                .isInstanceOf(RegistrationService.RegistrationException.class)
                .satisfies(thrown -> {
                    RegistrationService.RegistrationException e =
                            (RegistrationService.RegistrationException) thrown;
                    assertThat(e.getCode()).isEqualTo("LOGIN_TAKEN");
                    assertThat(e.getMessage().toLowerCase())
                            .doesNotContain("taken", "already", "exists", "pris");
                });
    }
}
