package org.jibe77.hermanas.image;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

class DoorImageAnalyzerTest {

    Logger logger = LoggerFactory.getLogger(DoorImageAnalyzerTest.class);

    @Test
    void testOpenedFolder() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer(null);
        File openedFolder = new File("src/test/resources/pictures/is opened");

        for (File openedDoorPicture : openedFolder.listFiles()) {
            if (!openedDoorPicture.getName().equals(".DS_Store")) {
                logger.info("Processing file {}.", openedDoorPicture.getAbsolutePath());
                assertFalse(doorPictureAnalizer.isDoorClosed(openedDoorPicture));
            }
        }
    }

    @Test
    void testClosedFolder() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer(null);
        File closedFolder = new File("src/test/resources/pictures/is closed");

        for (File closedDoorPicture : closedFolder.listFiles()) {
            if (!closedDoorPicture.getName().equals(".DS_Store")) {
                logger.info("Processing file {}.", closedDoorPicture.getAbsolutePath());
                assertTrue(doorPictureAnalizer.isDoorClosed(closedDoorPicture));
            }
        }
    }

}
