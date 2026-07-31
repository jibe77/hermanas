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

    @Value("${camera.high.width}")
    private int photoHighWidth;

    @Value("${camera.high.height}")
    private int photoHighHeight;

    @Value("${camera.regular.width}")
    private int photoRegularWidth;

    @Value("${camera.regular.height}")
    private int photoRegularHeight;

    @Value("${camera.encoding}")
    private String photoEncoding;

    private final ConfigService configService;

    public CameraConfiguration(ConfigService configService) {
        this.configService = configService;
    }

    /** Largeur en pixels selon le profil demandé. */
    public int width(boolean highQuality) {
        return highQuality ? photoHighWidth : photoRegularWidth;
    }

    /** Hauteur en pixels selon le profil demandé. */
    public int height(boolean highQuality) {
        return highQuality ? photoHighHeight : photoRegularHeight;
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
