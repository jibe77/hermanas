package org.jibe77.hermanas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class AppConfig {

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setUseCodeAsDefaultMessage(true);
        source.setBasename("messages");
        // Force UTF-8 so accented characters in messages_fr.properties — used by the
        // outgoing notification mails — survive the round-trip. JDK 11 defaults
        // ResourceBundle to ISO-8859-1, which mangles "redémarré" into "redÃ©marrÃ©"
        // when the source file is encoded in UTF-8.
        source.setDefaultEncoding("UTF-8");
        // Without this Java's ResourceBundle.getBundle() falls back to the JVM's
        // default locale when the requested one is missing — so a French operator
        // running the server would also receive French templates for English-only
        // recipients. Pin it to the root bundle (English) instead.
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
