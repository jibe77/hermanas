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
    public static final String ROLE_GUEST = "GUEST";
    public static final String ROLE_USER = "USER";
    Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    @Value("${security.user.name}")
    private String user;
    @Value("${security.user.password}")
    private String password;
    @Value("${security.guest.name}")
    private String guestUser;
    @Value("${security.guest.password}")
    private String guestPassword;

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
        http.authorizeRequests()
                // list of allowed urls for GUEST user.
                .antMatchers(HttpMethod.GET, "/light/isSwitchedOn").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/camera/takePicture").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/fan/isSwitchedOn").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/scheduler/doorClosingTime").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/scheduler/doorOpeningTime").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/scheduler/lightOffTime").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/scheduler/lightOnTime").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/sensor/info").hasAnyRole(ROLE_USER, ROLE_GUEST)
                .antMatchers(HttpMethod.GET, "/camera/takePicture").hasAnyRole(ROLE_USER, ROLE_GUEST)
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
            .roles(ROLE_USER)
            .and()
                .withUser(guestUser)
                .password("{noop}" + guestPassword)
                .roles(ROLE_GUEST);
    }
}