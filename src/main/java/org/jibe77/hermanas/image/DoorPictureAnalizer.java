package org.jibe77.hermanas.image;

import org.jibe77.hermanas.controller.camera.CameraController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DoorPictureAnalizer {

    public static final int ORDINATE_START = 810;
    public static final int ORDINATE_END = 1040;
    public static final int ABSCISSA_START = 163;
    public static final int ABSCISSA_END = 320;

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
     * This method is called when "isDoorClosed" doesn't know if the door is closed or not,
     * because PredictionException has been thrown too many times.
     * @param e thrown exception
     * @return
     */
    @Recover
    public boolean isDoorClosedRecover(PredictionException e) {
        logger.warn("Can't predict if the door is closed or not, so it's supposed to be {}.", e.getDoorStatus());
        return e.getDoorStatus().equals(DoorStatus.SEEMS_CLOSED);
    }

    /**
     * Analyze image
     * @param sourceFile file to analyse
     * @return true if the door is closed
     * @throws IOException thrown if the file is not readable
     */
    public boolean isDoorClosed(File sourceFile) throws IOException {
        BufferedImage originalImgage = ImageIO.read(sourceFile);
        DoorStatus doorStatus = getDoorStatus(originalImgage);
        switch (doorStatus) {
            case CLOSED:
                return true;
            case OPENED:
                return false;
            default:
                throw new PredictionException(doorStatus);
        }
    }

    /**
     * Analyse image
     * @param bufferedImage picture to process.
     * @return true if the door is closed, false if the door is closed
     */
    private DoorStatus getDoorStatus(BufferedImage bufferedImage) {
        java.util.List<Color> results = computeColorAverageList(bufferedImage);
        // fetch the index of the last blue pixel
        int lastIndexWithBluePixel = getLastIndexWithBluePixel(results);

        if (lastIndexWithBluePixel == results.size()-1) {
            return DoorStatus.CLOSED;
        } else if (lastIndexWithBluePixel < (results.size() -1)) {
            return DoorStatus.OPENED;
        } else {
            return DoorStatus.SEEMS_CLOSED;
        }
    }

    public static Color computeAverage(Color[] array) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (Color c : array) {
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        return new Color(
                 r / array.length,
                 g / array.length,
                 b / array.length);
    }

    public int getClosedStatus(File file) throws IOException {
        BufferedImage originalImgage = ImageIO.read(file);

        // analyser toutes les lignes depuis
        // x = 60   y = 155
        // x =  165 y = 339
        //    x -> +105     y -> +184
        // pour chaque 2 ajout en abscisse, on ajoute 1 en ordonnée, on va donc retomber sur x = 152
        // on récupère les 10 pixels en ordonnées,  on prend le rouge, et on fait la moyenne sur 10
        // ça doit être entre 60 et 80
        List<Color> results = computeColorAverageList(originalImgage);

        // fetch the index of the last blue pixel
        int lastIndexWithBluePixel = getLastIndexWithBluePixel(results);
        return (lastIndexWithBluePixel * 100) / (results.size() -1);
    }

    private int getLastIndexWithBluePixel(List<Color> results) {
        int lastIndexWithBluePixel = 0;
        for (int i = 0 ;  i < results.size() ; i++) {
            if (results.get(i).getBlue() > 75 && results.get(i).getBlue() < 110
             && results.get(i).getRed() > 45 && results.get(i).getRed() < 65
             && results.get(i).getGreen() > 45 && results.get(i).getGreen() < 65) {
                lastIndexWithBluePixel = i;
            }
        }
        return lastIndexWithBluePixel;
    }

    private List<Color> computeColorAverageList(BufferedImage originalImgage) {
        List<Color> results = new ArrayList<>(ORDINATE_END - ORDINATE_START);
        double m = (double)(ABSCISSA_END - ABSCISSA_START) / (ORDINATE_END - ORDINATE_START);
        for (int y = ORDINATE_START; y <= ORDINATE_END; y++) {
            Color[] list = new Color[10];

            for (int x = 0 ; x < 10 ; x++) {
                double offset = ((y - ORDINATE_START) * m);
                int computedX = ABSCISSA_START + x + (int)offset;
                list[x] = new Color(originalImgage.getRGB(computedX, y));
            }
            Color average = computeAverage(list);
            results.add(average);
        }
        return results;
    }
}
