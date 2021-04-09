package org.jibe77.hermanas.controller.gpio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.caprica.picam.enums.Encoding;

import static uk.co.caprica.picam.CameraConfiguration.cameraConfiguration;

@Configuration
public class CameraConfiguration {

    @Value("${camera.high.width}")
    private int photoHighWidth;

    @Value("${camera.high.height}")
    private int photoHighHeight;

    @Value("${camera.high.quality}")
    private int photoHighQuality;

    @Value("${camera.regular.width}")
    private int photoRegularWidth;

    @Value("${camera.regular.height}")
    private int photoRegularHeight;

    @Value("${camera.regular.quality}")
    private int photoRegularQuality;

    @Value("${camera.encoding}")
    private String photoEncoding;

    @Value("${camera.rotation}")
    private int photoRotation;

    @Value("${camera.brightness}")
    private int photoBrightness;

    @Bean(name = "CameraHighQualityConfig")
    public uk.co.caprica.picam.CameraConfiguration initHighQuality() {
        return cameraConfiguration()
                .width(photoHighWidth)
                .height(photoHighHeight)
                .encoding(Encoding.valueOf(photoEncoding))
                .quality(photoHighQuality)
                .rotation(photoRotation)
                .brightness(photoBrightness);
    }

    @Bean(name = "CameraRegularQualityConfig")
    public uk.co.caprica.picam.CameraConfiguration initReguarQuality() {
        return cameraConfiguration()
                .width(photoRegularWidth)
                .height(photoRegularHeight)
                .encoding(Encoding.valueOf(photoEncoding))
                .quality(photoRegularQuality)
                .rotation(photoRotation)
                .brightness(photoBrightness);
    }
}
