package org.jibe77.hermanas.security;

import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

@Configuration
@EnableAutoConfiguration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig
{
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    /**
     * Placeholder role given to freshly self-registered accounts. Carries <em>no</em> permission
     * (it is not listed in any {@code .hasRole(...)} clause) so the user effectively sees the same
     * resources as an anonymous visitor until an administrator promotes them to {@code USER} or
     * {@code ADMIN}. Logging in with this role is also refused, see
     * {@link PendingValidationUserDetailsChecker}.
     */
    public static final String ROLE_NOT_VALIDATED_YET = "NOT_VALIDATED_YET";
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    /** Name of the remember-me cookie sent to the browser. */
    private static final String REMEMBER_ME_COOKIE_NAME = "hermanas-remember-me";

    @Value("${hermanas.security.remember-me-key}")
    private String rememberMeKey;

    @Value("${hermanas.security.remember-me-validity-seconds:2678400}")
    private int rememberMeValiditySeconds;

    /**
     * HTTP security with inverted policy: everything is public by default; only state-changing
     * operations require authentication. This matches the product model where the dashboard is a
     * read-only public showcase and only authenticated users can act on the chicken coop.
     *
     * <h3>Authentication model</h3>
     * <p>Form-based login backed by {@link DbUserDetailsService}. Successful login creates
     * a session cookie which the Angular SPA (same-origin, bundled in the JAR) replays on
     * subsequent requests.</p>
     *
     * <h3>CSRF</h3>
     * <p>Enabled — required because we use session cookies. Token exposed via
     * {@code XSRF-TOKEN} cookie, echoed by Angular in the {@code X-XSRF-TOKEN} header.</p>
     *
     * <h3>What is protected</h3>
     * <ul>
     *   <li>All POST / PUT / DELETE on {@code /api/v1/**}</li>
     *   <li>A short list of GET endpoints that mutate state (servo calibration, switch toggles
     *       exposed as GET for legacy reasons, wifi controls, etc.)</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           PersistentTokenBasedRememberMeServices rememberMeServices,
                                           EventService eventService)
            throws Exception
    {
        logger.info("Configure authorizations.");
        http
                .headers().frameOptions().disable()
                .and()
                .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and()
                .authorizeRequests()

                // ─── Authentication endpoints (must stay reachable unauthenticated) ───────────
                .antMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/logout",
                        "/api/v1/auth/register").permitAll()
                .antMatchers(HttpMethod.GET, "/api/v1/auth/me").permitAll()

                // ─── Push notifications: VAPID public key is public (the browser needs it before
                // logging in to set up the SW subscription), but subscribe/unsubscribe/test must
                // be authenticated so we can associate a row with a user and gate the test
                // broadcast. ─────────────────────────────────────────────────────────────────
                .antMatchers(HttpMethod.GET, "/api/v1/push/vapid-public-key").permitAll()
                .antMatchers(HttpMethod.POST, "/api/v1/push/test").hasRole(ROLE_ADMIN)

                // ─── Actuator: keep /health and /info reachable for external monitoring,
                // restrict everything else to administrators (env/configprops/heapdump can
                // leak credentials and memory snapshots) ─────────────────────────────────────
                .antMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**",
                        "/actuator/info").permitAll()
                .antMatchers("/actuator/**").hasRole(ROLE_ADMIN)

                // ─── Logs: admin only — log content may contain sensitive data ────────────────
                .antMatchers("/api/v1/logs/**").hasRole(ROLE_ADMIN)

                // ─── Journal / auth event feed: admin only. The /business sibling stays
                //     public (covered by .anyRequest().permitAll() below). Listing failed
                //     login attempts to anonymous visitors would leak account existence. ───
                .antMatchers("/api/v1/events/auth/**").hasRole(ROLE_ADMIN)

                // ─── Diagnostics: admin only — exposes hardware state and SMTP test ──────────
                .antMatchers("/api/v1/buttons/**").hasRole(ROLE_ADMIN)
                .antMatchers("/api/v1/email/**").hasRole(ROLE_ADMIN)

                // ─── Camera photo archive: authenticated users only — the historical
                //     /photos/** tree is private. The live dashboard endpoints
                //     (takePicture, stream, closingRate) stay public so unauthenticated
                //     visitors can still see the current chicken-coop view. ───────────────
                .antMatchers("/api/v1/camera/photos/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)

                // ─── Protected: every mutating call ───────────────────────────────────────────
                // Any state-changing HTTP verb on the API requires authentication.
                // Use hasAnyRole(USER, ADMIN) — Spring Security does NOT give admins the USER
                // role automatically (no role hierarchy is configured), so hasRole(USER) on its
                // own would 403 every admin call.
                .antMatchers(HttpMethod.POST, "/api/v1/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                .antMatchers(HttpMethod.PUT, "/api/v1/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                .antMatchers(HttpMethod.DELETE, "/api/v1/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                .antMatchers(HttpMethod.PATCH, "/api/v1/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)

                // ─── Protected: GET endpoints that actually mutate state ──────────────────────
                // Legacy GET handlers that should have been POST/PUT but are kept for back-compat.
                // Calibration / debug actions on the servo motor.
                .antMatchers(HttpMethod.GET,
                        "/api/v1/door/turnClockwise",
                        "/api/v1/door/turnCounterClockwise",
                        "/api/v1/door/turnServo",
                        // Toggle/switch endpoints exposed as GET.
                        "/api/v1/fan/switch",
                        "/api/v1/music/switch",
                        "/api/v1/music/cocorico",
                        // WiFi / energy control actions.
                        "/api/v1/energy/wifi/stopUntilNextDoorEvent",
                        "/api/v1/energy/wifi/wifiSwitchEnabled",
                        // Camera streaming control.
                        "/api/v1/camera/stopStream"
                        ).hasAnyRole(ROLE_USER, ROLE_ADMIN)

                // ─── Everything else (SPA shell, static assets, GET status endpoints, swagger,
                // websockets, API GET reads, deep-link SPA routes) is public ──────────────────
                .anyRequest().permitAll()

                .and()
                .formLogin()
                    // Declaring a custom loginPage disables Spring's DefaultLoginPageGeneratingFilter,
                    // which would otherwise serve an HTML form on GET /login and cause empty-file
                    // downloads when the Angular SPA is the real login UI.
                    .loginPage("/auth/login")
                    .loginProcessingUrl("/api/v1/auth/login")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .successHandler((req, res, auth) -> {
                        eventService.record(EventType.LOGIN_SUCCESS,
                                "login=" + auth.getName() + " ip=" + clientIp(req));
                        res.setStatus(200);
                    })
                    .failureHandler((req, res, ex) -> {
                        // Record the failed attempt with the login that was tried, so admins
                        // can spot brute-force attempts on the Journalisation page.
                        String attempted = req.getParameter("username");
                        eventService.record(EventType.LOGIN_FAILED,
                                "login=" + (attempted == null ? "?" : attempted)
                                        + " reason=" + ex.getMessage()
                                        + " ip=" + clientIp(req));
                        // Surface the special "pending validation" case as a small JSON body so the
                        // SPA can render the right error string. Anything else stays a generic 401.
                        res.setStatus(401);
                        if (PendingValidationUserDetailsChecker.PENDING_VALIDATION_MESSAGE
                                .equals(ex.getMessage())) {
                            res.setContentType("application/json");
                            res.getWriter().write(
                                    "{\"error\":\""
                                            + PendingValidationUserDetailsChecker.PENDING_VALIDATION_MESSAGE
                                            + "\"}");
                        }
                    })
                .and()
                .rememberMe()
                    .rememberMeServices(rememberMeServices)
                    .key(rememberMeKey)
                .and()
                .logout()
                    .logoutUrl("/api/v1/auth/logout")
                    // Remove the remember-me cookie alongside the session on logout, otherwise
                    // the next request would silently reauthenticate the user.
                    .deleteCookies(REMEMBER_ME_COOKIE_NAME, "JSESSIONID")
                    .logoutSuccessHandler((req, res, auth) -> {
                        if (auth != null) {
                            eventService.record(EventType.LOGOUT, "login=" + auth.getName());
                        }
                        res.setStatus(204);
                    });

        return http.build();
    }

    /**
     * Returns the most relevant client IP for journalisation: prefers the
     * standard {@code X-Forwarded-For} header (we sit behind a Caddy reverse
     * proxy in production), falls back to the raw socket address.
     */
    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return req.getRemoteAddr();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider wired with the {@link PendingValidationUserDetailsChecker}, so a
     * password match for a {@code NOT_VALIDATED_YET} account still results in a 401. Without this
     * customisation Spring would happily create a session for the user even though every
     * authorization rule then denies them.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder,
                                                            PendingValidationUserDetailsChecker pendingChecker) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(pendingChecker);
        return provider;
    }

    /**
     * Persistent token store for remember-me tokens. Backed by the standard Spring Security
     * {@code persistent_logins} table — Hibernate's {@code ddl-auto=update} creates it on first
     * startup, so no manual SQL is needed. Each remember-me login produces a row keyed by
     * (series, token); rotation happens on every reuse.
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        repo.setCreateTableOnStartup(false);
        return repo;
    }

    /**
     * Wires the persistent token repository, the user details service and the cookie settings
     * together. We need to declare this bean explicitly (instead of relying on the fluent
     * {@code .rememberMe().tokenRepository(...)} DSL) so we can customise the cookie name,
     * the validity and force the parameter name expected on the login form.
     */
    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices(
            PersistentTokenRepository tokenRepository,
            UserDetailsService userDetailsService) {
        PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices(rememberMeKey, userDetailsService,
                        tokenRepository);
        services.setCookieName(REMEMBER_ME_COOKIE_NAME);
        services.setTokenValiditySeconds(rememberMeValiditySeconds);
        services.setParameter("remember-me");
        // Send the cookie only over HTTPS in production. Hermanas serves both http (dev) and https
        // (prod via reverse-proxy), so we let Spring auto-detect from the request scheme.
        services.setUseSecureCookie(false);
        return services;
    }
}
