package org.jibe77.hermanas;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void messageSource() {
        MessageSource messageSource = new AppConfig().messageSource();
        assertEquals("Hermanas has restarted incorrecly",
                messageSource.getMessage(
                        "restarted.incorrectly.title", null, Locale.getDefault()));
    }
}