package org.jibe77.hermanas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class FileBasedUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedUserDetailsService.class);

    private final String usersFilePath;
    private volatile Map<String, UserDetails> users = Collections.emptyMap();

    public FileBasedUserDetailsService(
            @Value("${hermanas.security.users-file:./users.properties}") String usersFilePath) {
        this.usersFilePath = usersFilePath;
    }

    @PostConstruct
    public void loadUsers() {
        Path path = Paths.get(usersFilePath);
        Properties props = new Properties();

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
                logger.info("Loaded users from {}", path.toAbsolutePath());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read users file: " + path.toAbsolutePath(), e);
            }
        } else {
            logger.warn("Users file not found at {}. No users will be authenticated until it is created. "
                    + "See README for the format and how to generate a bcrypt hash.", path.toAbsolutePath());
        }

        Map<String, UserDetails> parsed = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (!key.endsWith(".password")) {
                continue;
            }
            String username = key.substring(0, key.length() - ".password".length());
            String password = props.getProperty(key);
            String rolesProp = props.getProperty(username + ".roles", SecurityConfig.ROLE_USER);
            String[] roles = rolesProp.split("\\s*,\\s*");

            UserDetails ud = User.withUsername(username)
                    .password(password)
                    .roles(roles)
                    .build();
            parsed.put(username, ud);
        }
        this.users = Collections.unmodifiableMap(parsed);
        logger.info("Configured {} user(s) from file.", users.size());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails ud = users.get(username);
        if (ud == null) {
            throw new UsernameNotFoundException("Unknown user: " + username);
        }
        return ud;
    }
}
