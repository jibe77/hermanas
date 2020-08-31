package org.jibe77.hermanas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
    Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    @Value("${spring.security.user.name}")
    private String user;
    @Value("${spring.security.user.password}")
    private String password;
    @Value("${spring.security.demo.name}")
    private String demoUser;
    @Value("${spring.security.demo.password}")
    private String demoPassword;

    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        http
                .csrf().disable()
                .authorizeRequests()
                    .antMatchers("/**").hasAnyRole("USER")
                    .antMatchers(HttpMethod.GET, "/light/isSwitchedOn").hasAnyRole( "DEMO")
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
            .roles("USER")
            .and()
                .withUser(demoUser)
                .password("{noop}" + demoPassword)
                .roles("DEMO");
    }
}