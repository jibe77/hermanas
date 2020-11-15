package org.jibe77.hermanas.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class DoorPictureAnalizer {

    Logger logger = LoggerFactory.getLogger(DoorPictureAnalizer.class);

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
        logger.info("result is between {} and {}.", min, max);
        if (min > 50 && max < 85) {
            logger.info("door is closed");
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
