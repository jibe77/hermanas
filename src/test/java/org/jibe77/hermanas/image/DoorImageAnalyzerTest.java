package org.jibe77.hermanas.image;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

class DoorImageAnalyzerTest {

    @Test
    void testOpenPicture() throws IOException {
        DoorPictureAnalizer doorPictureAnalizer = new DoorPictureAnalizer();
        assertFalse(doorPictureAnalizer.isDoorClosed(
                "src/test/resources/pictures/is opened/logo.jpg"));
        assertFalse(doorPictureAnalizer.isDoorClosed(
                "src/test/resources/pictures/is opened/logo copie.jpg"));
        assertFalse(doorPictureAnalizer.isDoorClosed(
                "src/test/resources/pictures/is opened/logo copie 2.jpg"));
        assertTrue(doorPictureAnalizer.isDoorClosed(
                "src/test/resources/pictures/is closed/logo 7.jpg"));
    }
}
