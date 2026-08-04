package org.jibe77.hermanas.service.gpio;

import org.jibe77.hermanas.service.config.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Expose les réglages de prise de vue, que l'appelant traduit en options
 * {@code rpicam-still}.
 *
 * <p>Cette classe construisait auparavant des objets picam ; picam reposait sur
 * MMAL, absent de Raspberry Pi OS arm64. Elle ne fournit plus que des valeurs
 * brutes, sans dépendance à une librairie de capture.</p>
 *
 * <p>Largeur, hauteur et encodage viennent d'{@code application.properties} (non
 * exposés dans l'admin). Qualité, luminosité et rotation sont relues depuis
 * {@link ConfigService} à chaque appel, pour qu'un changement à chaud (via
 * {@code @CacheEvict} sur les setters) s'applique dès la photo suivante.</p>
 */
@Component
public class CameraConfiguration {

    @Value("${camera.encoding}")
    private String photoEncoding;

    private final ConfigService configService;

    public CameraConfiguration(ConfigService configService) {
        this.configService = configService;
    }

    /** Largeur en pixels, relue à chaque appel. */
    public int width(boolean highQuality) {
        return highQuality
                ? configService.getCameraHighWidth()
                : configService.getCameraRegularWidth();
    }

    /** Hauteur en pixels, relue à chaque appel. */
    public int height(boolean highQuality) {
        return highQuality
                ? configService.getCameraHighHeight()
                : configService.getCameraRegularHeight();
    }

    /**
     * Temps laissé à l'auto-exposition avant déclenchement, en millisecondes
     * ({@code --timeout} de rpicam-still).
     */
    public int delay(boolean highQuality) {
        return highQuality
                ? configService.getCameraHighDelay()
                : configService.getCameraRegularDelay();
    }

    /** Mode de balance des blancs ({@code --awb}), vide si automatique. */
    public String awb() {
        return configService.getCameraAwb();
    }

    /** Gains rouge/bleu imposés ({@code --awbgains}), vide si non utilisés. */
    public String awbGains() {
        return configService.getCameraAwbGains();
    }

    /** Zone du capteur lue ({@code --roi}), vide pour le capteur entier. */
    public String roi() {
        return configService.getCameraRoi();
    }

    /**
     * Mode capteur imposé ({@code --mode}), vide si libcamera choisit.
     *
     * <p>Empêche la bascule automatique vers un mode recadré quand la hauteur
     * de sortie descend sous ~790 px.</p>
     */
    public String mode() {
        return configService.getCameraMode();
    }

    /**
     * Temps de pose en microsecondes ({@code --shutter}), vide si automatique.
     *
     * <p>Fixer l'exposition supprime le temps de convergence de l'AEC, ce qui
     * permet de réduire {@code camera.*.delay}. Contrairement à la balance des
     * blancs, la valeur juste dépend de la lumière ambiante : elle varie entre
     * midi et le crépuscule, même lampe allumée.</p>
     */
    public String shutter() {
        return configService.getCameraShutter();
    }

    /** Gain analogique ({@code --gain}), vide si automatique. 1 = aucun gain. */
    public String gain() {
        return configService.getCameraGain();
    }

    /** Qualité JPEG (0-100), relue à chaque appel pour suivre les changements à chaud. */
    public int quality(boolean highQuality) {
        return highQuality
                ? configService.getCameraHighQuality()
                : configService.getCameraRegularQuality();
    }

    /** Rotation en degrés, relue à chaque appel. */
    public int rotation() {
        return configService.getCameraRotation();
    }

    /** Luminosité sur l'échelle historique 0-100, relue à chaque appel. */
    public int brightness() {
        return configService.getCameraBrightness();
    }

    /** Encodage configuré ({@code JPEG}). Conservé pour information. */
    public String encoding() {
        return photoEncoding;
    }

}
