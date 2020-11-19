package org.jibe77.hermanas.image;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

@Component
public class DoorPictureAnalizer {

    private final CameraController cameraController;

    Logger logger = LoggerFactory.getLogger(DoorPictureAnalizer.class);

    public DoorPictureAnalizer(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    @Retryable(
            value = { PredictionException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000))
    public boolean isDoorClosed() {
        Optional<File> picture = cameraController.takePictureNoException(true);
        try {
            return (picture.isPresent() && isDoorClosed(picture.get()));
        } catch (IOException e) {
            logger.error("Can't process picture.", e);
            // impossible to analyse, the door is supposed to be closed correctly.
            return true;
        }
    }

    /**
     * Analyze image
     * @param sourceFile file to analyse
     * @return true if the door is closed
     * @throws IOException thrown if the file is not readable
     */
    public boolean isDoorClosed(File sourceFile) throws IOException {
        BufferedImage originalImgage = ImageIO.read(sourceFile);
        return isDoorClosed(originalImgage);
    }

    protected boolean isDoorClosed(String sourcePath) throws IOException {
        logger.info("Analyse of source path {}.", sourcePath);
        File picture = new File(sourcePath);
        return isDoorClosed(picture);
    }

    /**
     * Analyse image
     * @param bufferedImage picture to process.
     * @return true if the door is closed, false if the door is closed
     */
    private boolean isDoorClosed(BufferedImage bufferedImage) {
        java.util.List<Double> results = new ArrayList<>(184);
        // analyser toutes les lignes depuis
        // x = 60   y = 155
        // x =  165 y = 339
        //    x -> +105     y -> +184
        // pour chaque 2 ajout en abscisse, on ajoute 1 en ordonnée, on va donc retomber sur x = 152
        // on récupère les 10 pixels en ordonnées,  on prend le rouge, et on fait la moyenne sur 10
        // ça doit être entre 60 et 80
        int offset = 0;
        for (int y = 855 ; y <=1079 ; y++) {
            int[] list = new int[10];
            for (int x = 0 ; x < 10 ; x++) {
                int computedX = 200 + x + (offset/2);
                list[x] = new Color(bufferedImage.getRGB(computedX, y)).getRed();
            }
            double average = computeAverage(list);
            results.add(average);
            offset++;
        }
        double max = Collections.max(results);
        double min = Collections.min(results);
        double dif = max - min;
        logger.info("result is between {} and {}.", min, max);
        if (min > 55 && max < 85 && dif < 25 && dif >= 5) {
            logger.info("door is closed.");
            return true;
        } else if (min > 50 && max < 70 && dif <= 16 && dif >= 8) {
            logger.info("door seems to be closed but there is probably a chick in front.");
            return true;
        } else if (min > 50 && max < 58 && dif < 5) {
            logger.info("door seems to be closed but there is a problem with light.");
            return true;
        } else {
            logger.info("door is opened");
            return false;
        }
    }

    public static double computeAverage(int[] array) {
        int sum = 0;
        for (int value : array) {
            sum += value;
        }
        return (double) sum / array.length;
    }
}
