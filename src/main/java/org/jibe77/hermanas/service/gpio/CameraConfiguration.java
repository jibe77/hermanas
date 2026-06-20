package org.jibe77.hermanas.service.gpio;

import org.jibe77.hermanas.service.config.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.co.caprica.picam.enums.Encoding;

import static uk.co.caprica.picam.CameraConfiguration.cameraConfiguration;

/**
 * Builds picam {@link uk.co.caprica.picam.CameraConfiguration} objects on demand.
 *
 * <p>Width/height/encoding come from {@code application.properties} (they are
 * not exposed to the admin UI). Quality, brightness and rotation are read from
 * {@link ConfigService} at every call so a hot-reload (via {@code @CacheEvict}
 * on the setters) takes effect on the very next picture — no app restart needed.
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

    public uk.co.caprica.picam.CameraConfiguration buildHighQuality() {
        return cameraConfiguration()
                .width(photoHighWidth)
                .height(photoHighHeight)
                .encoding(Encoding.valueOf(photoEncoding))
                .quality(configService.getCameraHighQuality())
                .rotation(configService.getCameraRotation())
                .brightness(configService.getCameraBrightness());
    }

    public uk.co.caprica.picam.CameraConfiguration buildRegularQuality() {
        return cameraConfiguration()
                .width(photoRegularWidth)
                .height(photoRegularHeight)
                .encoding(Encoding.valueOf(photoEncoding))
                .quality(configService.getCameraRegularQuality())
                .rotation(configService.getCameraRotation())
                .brightness(configService.getCameraBrightness());
    }
}
