package org.jibe77.hermanas.security;

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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableAutoConfiguration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig
{
    public static final String ROLE_USER = "USER";
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    @Value("${security.user.name}")
    private String user;
    @Value("${security.user.password}")
    private String password;

    /**
     * Configures HTTP security using the modern SecurityFilterChain approach.
     * See doc about configuration: https://www.baeldung.com/spring-security-expressions
     *
     * <h3>CSRF Protection Decision</h3>
     * <p>CSRF protection is disabled for the following reasons:</p>
     * <ul>
     *   <li>This is a stateless REST API consumed by a separate SPA (https://www.hermanas.fr)</li>
     *   <li>Primary authentication uses HTTP Basic (credentials explicitly sent in headers, not cookies)</li>
     *   <li>The frontend makes explicit API calls with credentials, not browser-automated requests</li>
     *   <li>CORS is properly configured with allowed origins only</li>
     * </ul>
     * <p><strong>Note:</strong> Form login is enabled for manual browser access. If browser-based
     * session management becomes the primary auth method, CSRF should be re-enabled.</p>
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        logger.info("Configure authorizations.");
        // CSRF disabled - see method JavaDoc for reasoning
        http.cors().and().headers().frameOptions().disable().and().csrf().disable().authorizeRequests()
                // Allow all OPTIONS requests for CORS preflight
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
                .antMatchers("/api/v1/stomp").permitAll()

                // user is allowed to call all the services
                .antMatchers("/**").hasRole(ROLE_USER)
                .and()
                .formLogin()
                .permitAll()
                .and()
                .logout()
                .permitAll()
                .and()
                .httpBasic();

        return http.build();
    }

    /**
     * Configures the in-memory user details service.
     * For the moment, the password are stored in plain text.
     * If we need to encrypt them, see
     * https://info.michael-simons.eu/2018/01/13/spring-security-5-new-password-storage-format/
     *
     * @return the configured InMemoryUserDetailsManager
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        logger.info("Init security.");
        logger.info("Configure user and password for main user");
        UserDetails userDetails = User.withUsername(user)
                .password("{noop}" + password)
                .roles(ROLE_USER)
                .build();
        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://www.hermanas.fr", "https://dev.d2ylqblswoz84y.amplifyapp.com", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
