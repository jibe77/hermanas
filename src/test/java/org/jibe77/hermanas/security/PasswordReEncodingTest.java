package org.jibe77.hermanas.security;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CRYPTO-010 : un hash stocké sous le facteur courant doit être rattrapé à la connexion
 * suivante, sinon il reste au plancher jusqu'à ce que l'utilisateur change d'avis sur son
 * mot de passe — ce qui n'arrive pas.
 */
class PasswordReEncodingTest {

    private final HermanasUserRepository repository = mock(HermanasUserRepository.class);
    private final DbUserDetailsService service =
            new DbUserDetailsService(repository, "./does-not-exist.properties");

    private static HermanasUser user(String login, String hash) {
        HermanasUser user = new HermanasUser();
        user.setLogin(login);
        user.setPasswordHash(hash);
        user.setRole(SecurityConfig.ROLE_USER);
        user.setSessionEpoch(7L);
        return user;
    }

    @Test
    void updatePasswordPersistsTheNewHashWithoutTouchingTheSessionEpoch() {
        HermanasUser stored = user("jb", "$2a$04$ancien");
        when(repository.findByLogin("jb")).thenReturn(Optional.of(stored));

        UserDetails updated = service.updatePassword(
                new HermanasUserDetails("jb", "$2a$04$ancien", SecurityConfig.ROLE_USER, 7L),
                "$2a$12$nouveau");

        assertThat(updated.getPassword()).isEqualTo("$2a$12$nouveau");
        verify(repository).save(stored);
        assertThat(stored.getPasswordHash()).isEqualTo("$2a$12$nouveau");
        // Le mot de passe n'a pas changé, seule sa représentation : aucune session ne doit
        // tomber à cause de ce ré-encodage.
        assertThat(updated).isInstanceOf(HermanasUserDetails.class);
        assertThat(((HermanasUserDetails) updated).getSessionEpoch()).isEqualTo(7L);
    }

    /**
     * Le contrat sur lequel repose le crochet : {@code DaoAuthenticationProvider} ne demande
     * le ré-encodage que si {@code upgradeEncoding} reconnaît un hash plus faible que le
     * facteur configuré. Chiffres bas pour que le test reste instantané.
     */
    @Test
    void aWeakerStoredHashIsFlaggedForUpgradeAndACurrentOneIsNot() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(6);

        assertThat(encoder.upgradeEncoding(BCrypt.hashpw("mot-de-passe", BCrypt.gensalt(4))))
                .as("un hash posé à un facteur plus faible doit être re-haché")
                .isTrue();
        assertThat(encoder.upgradeEncoding(BCrypt.hashpw("mot-de-passe", BCrypt.gensalt(6))))
                .as("un hash déjà au facteur courant ne doit pas être re-haché à chaque connexion")
                .isFalse();
    }

    @Test
    void theConfiguredStrengthIsWhatEncodesNewHashes() {
        String hash = new BCryptPasswordEncoder(6).encode("mot-de-passe");

        assertThat(hash).startsWith("$2a$06$");
    }

    @Test
    void unknownUserIsNotSilentlyCreated() {
        when(repository.findByLogin(any())).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updatePassword(
                        new HermanasUserDetails("fantome", "$2a$12$x", SecurityConfig.ROLE_USER, 0L),
                        "$2a$12$y"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }
}
