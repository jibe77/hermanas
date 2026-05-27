package org.jibe77.hermanas.security;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
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
import java.util.Properties;

/**
 * Loads users from the database. On first startup (empty table) it bootstraps the table from
 * {@code users.properties} so existing installations keep working without manual SQL.
 *
 * <p>The admin account ({@value SecurityConfig#ROLE_ADMIN}) named {@code marguerite} is given the
 * dedicated chicken-coop notification address by default; this value can be overridden in the
 * Users admin screen later.</p>
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(DbUserDetailsService.class);
    private static final String ADMIN_LOGIN = "marguerite";
    private static final String ADMIN_DEFAULT_EMAIL = "info@hermanas.fr";

    private final HermanasUserRepository repository;
    private final String legacyUsersFilePath;

    public DbUserDetailsService(
            HermanasUserRepository repository,
            @Value("${hermanas.security.users-file:./users.properties}") String legacyUsersFilePath) {
        this.repository = repository;
        this.legacyUsersFilePath = legacyUsersFilePath;
    }

    @PostConstruct
    public void bootstrapFromLegacyFileIfNeeded() {
        if (repository.count() > 0) {
            logger.info("User table already populated ({} user(s)).", repository.count());
            ensureMargueriteIsAdmin();
            return;
        }
        Path path = Paths.get(legacyUsersFilePath);
        if (!Files.exists(path)) {
            logger.warn("User table is empty and no legacy users file at {}. Login will be impossible "
                    + "until a user is created.", path.toAbsolutePath());
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            logger.error("Failed to read legacy users file {}", path.toAbsolutePath(), e);
            return;
        }
        int imported = 0;
        for (String key : props.stringPropertyNames()) {
            if (!key.endsWith(".password")) {
                continue;
            }
            String login = key.substring(0, key.length() - ".password".length());
            String passwordHash = props.getProperty(key);
            String rawRole = props.getProperty(login + ".roles", SecurityConfig.ROLE_USER).trim();
            String role = normalizeRole(rawRole);
            // force marguerite to ADMIN: she is the historical admin account and the
            // legacy file might not list her role explicitly.
            if (ADMIN_LOGIN.equals(login)) {
                role = SecurityConfig.ROLE_ADMIN;
            }
            String email = ADMIN_LOGIN.equals(login) ? ADMIN_DEFAULT_EMAIL : null;
            boolean notifications = ADMIN_LOGIN.equals(login);
            HermanasUser u = new HermanasUser(login, passwordHash, email, role, notifications);
            repository.save(u);
            logger.info("Bootstrapped user '{}' with role '{}' (notifications={}, email={}).",
                    login, role, notifications, email);
            imported++;
        }
        logger.info("Bootstrapped {} user(s) from {} into the database.", imported, path.toAbsolutePath());
    }

    /**
     * Repairs the marguerite account if a previous run stored her with the wrong role (e.g. a
     * legacy {@code ROLE_ADMIN} value that became double-prefixed) or with no email/notifications.
     * Idempotent — only updates fields that need fixing.
     */
    private void ensureMargueriteIsAdmin() {
        repository.findByLogin(ADMIN_LOGIN).ifPresent(u -> {
            boolean changed = false;
            if (!SecurityConfig.ROLE_ADMIN.equals(u.getRole())) {
                logger.warn("Account '{}' had role '{}', forcing it back to '{}'.",
                        ADMIN_LOGIN, u.getRole(), SecurityConfig.ROLE_ADMIN);
                u.setRole(SecurityConfig.ROLE_ADMIN);
                changed = true;
            }
            if (u.getEmail() == null || u.getEmail().isEmpty()) {
                u.setEmail(ADMIN_DEFAULT_EMAIL);
                changed = true;
            }
            if (changed) {
                repository.save(u);
                logger.info("Reconciled admin account '{}'.", ADMIN_LOGIN);
            }
        });
    }

    /**
     * Normalises a legacy role value into one of the canonical role names ({@code USER} /
     * {@code ADMIN}). Strips an optional {@code ROLE_} prefix and keeps only the first entry of
     * comma-separated lists.
     */
    private String normalizeRole(String raw) {
        if (raw == null || raw.isEmpty()) {
            return SecurityConfig.ROLE_USER;
        }
        String role = raw.trim();
        if (role.contains(",")) {
            role = role.split("\\s*,\\s*")[0].trim();
        }
        if (role.toUpperCase().startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        role = role.toUpperCase();
        if (SecurityConfig.ROLE_ADMIN.equals(role)) {
            return SecurityConfig.ROLE_ADMIN;
        }
        return SecurityConfig.ROLE_USER;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HermanasUser user = repository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + username));
        return User.withUsername(user.getLogin())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }
}
