package org.jibe77.hermanas.data.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "hermanas_user")
public class HermanasUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String login;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 254)
    private String email;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(nullable = false)
    private boolean notificationsEnabled;

    /**
     * Preferred UI / notification language. Two-letter lower-case ISO code, currently
     * limited to {@code "fr"} and {@code "en"}. Drives the locale of the Angular SPA
     * after login and selects the matching template for outgoing notification mails.
     * Defaults to {@code "fr"} for historical accounts that never made a choice.
     */
    @Column(nullable = false, length = 8)
    private String language = "fr";

    public HermanasUser() {}

    public HermanasUser(String login, String passwordHash, String email, String role,
                        boolean notificationsEnabled) {
        this(login, passwordHash, email, role, notificationsEnabled, "fr");
    }

    public HermanasUser(String login, String passwordHash, String email, String role,
                        boolean notificationsEnabled, String language) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
        this.notificationsEnabled = notificationsEnabled;
        this.language = language != null ? language : "fr";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language != null ? language : "fr"; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HermanasUser that = (HermanasUser) o;
        return Objects.equals(login, that.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }
}
