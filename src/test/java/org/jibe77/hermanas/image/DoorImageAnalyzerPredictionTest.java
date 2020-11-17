package org.jibe77.hermanas.image;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DoorImageAnalyzerPredictionTest {
/*
    @Test
    public void predict() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer();
        File openedTargetFolder = new File("target/opened");
        File closedTargetFolder = new File("target/closed");

         FileUtils.deleteDirectory(openedTargetFolder);
         FileUtils.deleteDirectory(closedTargetFolder);

        if (!openedTargetFolder.mkdir() || !closedTargetFolder.mkdir()) {
            System.out.println("Can't create target folders");
            return;
        }

        File tryFolder = new File("src/test/resources/pictures/try");
        File[] tryDayFolders = tryFolder.listFiles();
        for (File tryDayFolder : tryDayFolders) {
            if (tryDayFolder.isDirectory()) {
                for (File fileToAnalyse : tryDayFolder.listFiles()) {
                    System.out.println("Analysing file " + fileToAnalyse.getName());
                    try {
                        if (doorPictureAnalizer.isDoorClosed(fileToAnalyse)) {
                            FileUtils.copyFile(fileToAnalyse,
                                    new File(openedTargetFolder, fileToAnalyse.getName()));
                        } else {
                            FileUtils.copyFile(fileToAnalyse,
                                    new File(closedTargetFolder, fileToAnalyse.getName()));
                        }
                    } catch (IIOException | ArrayIndexOutOfBoundsException e) {
                        System.out.println("Error reading this file.");
                    }
                }
            }
        }
    }*/
}
