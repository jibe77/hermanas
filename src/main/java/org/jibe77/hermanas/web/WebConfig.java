package org.jibe77.hermanas.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/fr-FR/");

        // Spring Security's loginPage redirects unauthenticated navigations to /auth/login;
        // forward that to the default-locale SPA shell so the Angular login screen renders.
        registry.addRedirectViewController("/auth/login", "/fr-FR/auth/login");

        registry.addViewController("/fr-FR/").setViewName("forward:/fr-FR/index.html");
        registry.addViewController("/fr-FR/{path:[^\\.]*}").setViewName("forward:/fr-FR/index.html");
        registry.addViewController("/fr-FR/{path:^(?!api|actuator|swagger-ui|v3|stomp).*}/{subpath:[^\\.]*}")
                .setViewName("forward:/fr-FR/index.html");

        registry.addViewController("/en-US/").setViewName("forward:/en-US/index.html");
        registry.addViewController("/en-US/{path:[^\\.]*}").setViewName("forward:/en-US/index.html");
        registry.addViewController("/en-US/{path:^(?!api|actuator|swagger-ui|v3|stomp).*}/{subpath:[^\\.]*}")
                .setViewName("forward:/en-US/index.html");
    }
}
