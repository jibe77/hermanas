package org.jibe77.hermanas.image;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("image_processing")
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

    /*@Test
    void testSeemsClosedFolder() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer(null);
        File closedFolder = new File("src/test/resources/pictures/seems closed");

        for (File closedDoorPicture : closedFolder.listFiles()) {
            if (!closedDoorPicture.getName().equals(".DS_Store")) {
                logger.info("Processing file {}.", closedDoorPicture.getAbsolutePath());
                assertThrows(PredictionException.class, () -> doorPictureAnalizer.isDoorClosed(closedDoorPicture));
            }
        }
    }*/

    /*@Test
    void testOpenedStatus() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer(null);

        int closedStatus, previousClosedStatus = 0;
        for (int i = 0 ; i <= 20 ; i++) {
            File file = new File(
                    "src/test/resources/pictures/closing_status_by_day/" +
                            StringUtils.leftPad(i + "", 2, "0") + ".png");
            boolean isDoorClosed = doorPictureAnalizer.isDoorClosed(file);
            closedStatus = doorPictureAnalizer.getClosedStatus(file);
            if (i >= 20) {
                assertTrue(isDoorClosed);
            } else {
                if (i < 7) {
                    assertEquals(0, closedStatus);
                } else {
                    int dif = closedStatus - previousClosedStatus;
                    assertTrue(dif >= 1 && dif <= 15);
                }
                assertFalse(isDoorClosed);
            }
            logger.info("on index {} closed status is {}, previous closed status was {}.", i, closedStatus, previousClosedStatus);
            previousClosedStatus = closedStatus;
        }
    }*/

    @Test
    void testOpenedStatusByNight() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer(null);

        File file = new File(
                "src/test/resources/pictures/closing_status_by_night/2020-11-27-22-17.png");
        assertFalse(doorPictureAnalizer.isDoorClosed(file));
        int status = doorPictureAnalizer.getClosedStatus(file);
        assertTrue(status > 80);
        logger.info("File {} closing status is {}.", file.getName(), status);
    }

    @Test
    void testOpenedStatusByNight2() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer(null);

        File file = new File(
                "src/test/resources/pictures/closing_status_by_night/2020-11-27-22-17-2.png");
        assertFalse(doorPictureAnalizer.isDoorClosed(file));
        int status = doorPictureAnalizer.getClosedStatus(file);
        assertTrue(status < 20);
        logger.info("File {} closing status is {}.", file.getName(), status);
    }
}