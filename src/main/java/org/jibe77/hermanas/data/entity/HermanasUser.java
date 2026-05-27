package org.jibe77.hermanas.data.entity;

import javax.persistence.*;
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

    public HermanasUser() {}

    public HermanasUser(String login, String passwordHash, String email, String role, boolean notificationsEnabled) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
        this.notificationsEnabled = notificationsEnabled;
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
