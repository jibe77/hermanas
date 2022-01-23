package org.jibe77.hermanas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableAutoConfiguration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
    public static final String ROLE_USER = "USER";
    Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    @Value("${security.user.name}")
    private String user;
    @Value("${security.user.password}")
    private String password;

    /**
     * See doc about configuration
     * https://www.baeldung.com/spring-security-expressions
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        logger.info("Configure authorizations.");
        http.cors().and().headers().frameOptions().disable().and().csrf().disable().authorizeRequests()
                // list of allowed urls for GUEST user.
                .antMatchers(HttpMethod.GET,
                        "/light/status",
                        "/music/status",
                        "/door/status",
                        "/camera/takePicture",
                        "/camera/stream",
                        "/camera/stopStream",
                        "/fan/status",
                        "/scheduler/doorClosingTime",
                        "/scheduler/doorOpeningTime",
                        "/scheduler/lightOnTime",
                        "/scheduler/nextEvents",
                        "/energy/currentMode",
                        "/energy/dateRange",
                        "/sensor/info",
                        "/sensor/history/today",
                        "/sensor/history/week",
                        "/sensor/history/month",
                        "/sensor/history/year",
                        "/sensor/history/year/*",
                        "/sensor/history/years",
                        "/sensor/history/all",
                        "/sensor/history/*/*",
                        "/info",
                        "/system/version",
                        "/sockjs-node/info"
                        ).anonymous()

                // web socket
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
    }

    /**
     * Configures the users.
     * For the moment, the password are stored in plain text.
     * If we need to crypt them, see
     * https://info.michael-simons.eu/2018/01/13/spring-security-5-new-password-storage-format/
     * @param auth
     * @throws Exception
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth)
            throws Exception
    {
        logger.info("Init security.");
        logger.info("Configure user and password for main user");
        auth.inMemoryAuthentication()
            .withUser(user)
            .password("{noop}" + password)
            .roles(ROLE_USER);
    }
}
