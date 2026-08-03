package org.jibe77.hermanas.service.config;

import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validation des réglages de capture saisis librement — région d'intérêt, gains de
 * balance des blancs, mode AWB.
 *
 * <p>Ces valeurs partent telles quelles dans la ligne de commande
 * {@code rpicam-still}. Une saisie malformée y ferait échouer la capture entière
 * avec un message obscur : mieux vaut la refuser à l'écriture, avec une explication.</p>
 */
class ConfigServiceRoiTest {

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        ParameterRepository repository = mock(ParameterRepository.class);
        // Aucune valeur en base : setConfigValue insère au lieu de mettre à jour.
        when(repository.findByEntryKey(anyString())).thenReturn(null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        configService = new ConfigService(repository, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0,0,1,1",            // capteur entier
            "0,0.2,1,0.8",        // retire 20 % en haut
            "0.25,0.25,0.5,0.5",  // quart central
            "0.1,0.1,0.9,0.9",    // x+largeur = 1 pile
            " 0 , 0.2 , 1 , 0.8 " // espaces tolérés
    })
    @DisplayName("Régions valides acceptées")
    void acceptsValidRegions(String roi) {
        assertDoesNotThrow(() -> configService.setCameraRoi(roi));
    }

    @Test
    @DisplayName("Vide accepté — lit le capteur entier")
    void acceptsEmptyRegion() {
        assertDoesNotThrow(() -> configService.setCameraRoi(""));
        assertDoesNotThrow(() -> configService.setCameraRoi(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0,0,1",              // trois valeurs seulement
            "0,0,1,1,1",          // cinq valeurs
            "a,b,c,d",            // non numérique
            "0,0,,1",             // champ vide
            "-0.1,0,1,1",         // hors bornes, négatif
            "0,0,1.5,1",          // hors bornes, > 1
            "0,0,0,1",            // largeur nulle
            "0,0,1,0",            // hauteur nulle
            "0.5,0,0.8,1",        // x+largeur = 1.3, déborde
            "0,0.5,1,0.8"         // y+hauteur = 1.3, déborde
    })
    @DisplayName("Régions invalides refusées")
    void rejectsInvalidRegions(String roi) {
        assertThrows(IllegalArgumentException.class, () -> configService.setCameraRoi(roi));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2,2.0", "1,2", "0.5,3.25"})
    @DisplayName("Gains AWB valides acceptés")
    void acceptsValidAwbGains(String gains) {
        assertDoesNotThrow(() -> configService.setCameraAwbGains(gains));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2", "1.2,2.0,3.0", "a,b", "-1,2", "1.2;2.0"})
    @DisplayName("Gains AWB invalides refusés")
    void rejectsInvalidAwbGains(String gains) {
        assertThrows(IllegalArgumentException.class, () -> configService.setCameraAwbGains(gains));
    }

    @Test
    @DisplayName("Mode AWB : seuls les modes connus de rpicam-still passent")
    void validatesAwbMode() {
        assertDoesNotThrow(() -> configService.setCameraAwb("incandescent"));
        assertDoesNotThrow(() -> configService.setCameraAwb("TUNGSTEN"));  // casse ignorée
        assertDoesNotThrow(() -> configService.setCameraAwb(""));          // vide = automatique
        assertThrows(IllegalArgumentException.class, () -> configService.setCameraAwb("neon"));
    }

    @Test
    @DisplayName("Dimensions et délais bornés")
    void validatesSizesAndDelays() {
        assertDoesNotThrow(() -> configService.setCameraRegularWidth(1096));
        assertThrows(IllegalArgumentException.class, () -> configService.setCameraRegularWidth(32));
        assertThrows(IllegalArgumentException.class, () -> configService.setCameraRegularWidth(8192));

        assertDoesNotThrow(() -> configService.setCameraHighDelay(1000));
        assertThrows(IllegalArgumentException.class, () -> configService.setCameraHighDelay(-1));
        assertThrows(IllegalArgumentException.class, () -> configService.setCameraHighDelay(60000));
    }
}
