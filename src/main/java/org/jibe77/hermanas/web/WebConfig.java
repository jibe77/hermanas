package org.jibe77.hermanas.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Top-level Angular routes, as declared in {@code frontend/src/app/app.routes.ts}.
     * The SPA is only ever served under a locale prefix, so these bare paths have no
     * mapping of their own — see {@link #addViewControllers} for why we redirect them.
     */
    private static final String[] SPA_ROOT_ROUTES = {
        "auth", "camera", "dashboard", "electronics", "energy", "error", "logs",
        "music", "notification", "residents", "scheduler", "system", "utility",
        "version", "weather"
    };

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/fr-FR/");

        // Spring Security's loginPage redirects unauthenticated navigations to /auth/login;
        // forward that to the default-locale SPA shell so the Angular login screen renders.
        registry.addRedirectViewController("/auth/login", "/fr-FR/auth/login");

        // Un-prefixed SPA routes (/dashboard, /camera, ...) reach us from links shared
        // without a locale and from PWA icons installed before the manifest carried a
        // locale-aware start_url. Nothing maps them, so they used to surface the bare
        // JSON 404 from GlobalExceptionHandler#handleNoResourceFound. Send them to the
        // default locale instead; the Angular router takes over from there.
        for (String route : SPA_ROOT_ROUTES) {
            registry.addRedirectViewController("/" + route, "/fr-FR/" + route);
            registry.addRedirectViewController("/" + route + "/**", "/fr-FR/" + route);
        }

        // Same rationale for the manifest itself: an already-installed icon may still
        // request /manifest.webmanifest, and iOS will not re-read it from a new path.
        registry.addRedirectViewController("/manifest.webmanifest", "/fr-FR/manifest.webmanifest");

        registry.addViewController("/fr-FR/").setViewName("forward:/fr-FR/index.html");
        registry.addViewController("/fr-FR/{path:[^\\.]*}").setViewName("forward:/fr-FR/index.html");
        registry.addViewController("/fr-FR/{path:^(?!api|actuator|swagger-ui|v3|stomp).*}/{subpath:[^\\.]*}")
                .setViewName("forward:/fr-FR/index.html");

        registry.addViewController("/en-US/").setViewName("forward:/en-US/index.html");
        registry.addViewController("/en-US/{path:[^\\.]*}").setViewName("forward:/en-US/index.html");
        registry.addViewController("/en-US/{path:^(?!api|actuator|swagger-ui|v3|stomp).*}/{subpath:[^\\.]*}")
                .setViewName("forward:/en-US/index.html");

        registry.addViewController("/ro-RO/").setViewName("forward:/ro-RO/index.html");
        registry.addViewController("/ro-RO/{path:[^\\.]*}").setViewName("forward:/ro-RO/index.html");
        registry.addViewController("/ro-RO/{path:^(?!api|actuator|swagger-ui|v3|stomp).*}/{subpath:[^\\.]*}")
                .setViewName("forward:/ro-RO/index.html");
    }
}
