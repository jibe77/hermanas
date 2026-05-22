package org.jibe77.hermanas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableAutoConfiguration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig
{
    public static final String ROLE_USER = "USER";
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Configures HTTP security using the modern SecurityFilterChain approach.
     *
     * <h3>Authentication model</h3>
     * <p>Form-based login backed by a file-based {@link FileBasedUserDetailsService}.
     * Successful login establishes a session cookie; the Angular SPA, served from the
     * same JAR, sends that cookie automatically on subsequent requests.</p>
     *
     * <h3>CSRF Protection</h3>
     * <p>CSRF is <strong>enabled</strong> because authentication relies on a session cookie
     * (cookies are automatically attached by the browser, hence vulnerable to CSRF).
     * The token is exposed via a cookie ({@code XSRF-TOKEN}) readable by JavaScript so the
     * SPA can echo it in the {@code X-XSRF-TOKEN} header on mutating requests.</p>
     *
     * <h3>CORS</h3>
     * <p>CORS configuration has been removed: the SPA is bundled into the same JAR and
     * served from the same origin, so cross-origin handling is no longer needed.</p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        logger.info("Configure authorizations.");
        http
                .headers().frameOptions().disable()
                .and()
                .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and()
                .authorizeRequests()
                // list of allowed urls for GUEST user - updated to /api/v1/* paths
                .antMatchers(HttpMethod.GET,
                        "/api/v1/light/status",
                        "/api/v1/music/status",
                        "/api/v1/door/status",
                        "/api/v1/camera/takePicture",
                        "/api/v1/camera/stream",
                        "/api/v1/camera/stopStream",
                        "/api/v1/fan/status",
                        "/api/v1/scheduler/doorClosingTime",
                        "/api/v1/scheduler/doorOpeningTime",
                        "/api/v1/scheduler/lightOnTime",
                        "/api/v1/scheduler/nextEvents",
                        "/api/v1/energy/currentMode",
                        "/api/v1/energy/dateRange",
                        "/api/v1/sensor/info",
                        "/api/v1/sensor/history/today",
                        "/api/v1/sensor/history/week",
                        "/api/v1/sensor/history/month",
                        "/api/v1/sensor/history/year",
                        "/api/v1/sensor/history/year/*",
                        "/api/v1/sensor/history/years",
                        "/api/v1/sensor/history/all",
                        "/api/v1/sensor/history/*/*",
                        "/api/v1/info",
                        "/sockjs-node/info"
                        ).anonymous()

                // web socket - updated to /api/v1/* path
                .antMatchers(HttpMethod.GET, "/socket/**").permitAll()
                .antMatchers(HttpMethod.GET, "/progress").anonymous()

                // swagger ui
                // https://poulailler57.ddns.net:5780/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config
                .antMatchers(HttpMethod.GET,"/v2/api-docs", "/v3/api-docs",
                        "/configuration/ui",
                        "/swagger-resources/**",
                        "/configuration/security",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/csrf",
                        "/swagger-ui/**",
                        "/v3/api-docs/swagger-config",
                        "/").permitAll()

                // Angular SPA static assets (bundled into the JAR under static/)
                // Login UI itself must be reachable without auth, plus JS/CSS/assets
                // that the browser pulls before the user is authenticated.
                .antMatchers(HttpMethod.GET,
                        "/index.html",
                        "/fr-FR/",
                        "/fr-FR/index.html",
                        "/en-US/",
                        "/en-US/index.html",
                        "/*.js",
                        "/*.css",
                        "/*.map",
                        "/*.ico",
                        "/*.png",
                        "/*.svg",
                        "/*.woff",
                        "/*.woff2",
                        "/*.ttf",
                        "/fr-FR/**/*.js",
                        "/fr-FR/**/*.css",
                        "/fr-FR/**/*.map",
                        "/fr-FR/assets/**",
                        "/en-US/**/*.js",
                        "/en-US/**/*.css",
                        "/en-US/**/*.map",
                        "/en-US/assets/**",
                        "/assets/**",
                        "/ngsw-worker.js",
                        "/ngsw.json",
                        "/manifest.webmanifest").permitAll()
                .antMatchers("/api/v1/stomp").permitAll()

                // login/logout/me endpoints must be reachable without auth
                .antMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .antMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                .antMatchers(HttpMethod.GET, "/api/v1/auth/me").permitAll()

                // user is allowed to call all the services
                .antMatchers("/**").hasRole(ROLE_USER)
                .and()
                .formLogin()
                    .loginProcessingUrl("/api/v1/auth/login")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .successHandler((req, res, auth) -> res.setStatus(200))
                    .failureHandler((req, res, ex) -> res.setStatus(401))
                .and()
                .logout()
                    .logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
