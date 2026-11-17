package org.jibe77.hermanas.data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
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

    /**
     * Counter bumped whenever everything the application already issued for this account
     * must stop working — password change, role change, deletion (AUTH-VULN-01).
     *
     * <p>The value is copied into the {@code SecurityContext} at authentication time and
     * compared against this row by {@link org.jibe77.hermanas.security.SessionEpochFilter}
     * on every request, so a session or remember-me token minted before the change cannot
     * outlive it.</p>
     */
    @Column(name = "session_epoch", nullable = false)
    private long sessionEpoch = 0L;

    /**
     * Consecutive failed logins against this account, reset by a successful one
     * (AUTH-VULN-03).
     *
     * <p>The only friction the target used to offer was a per-source-IP bucket held in a
     * {@code ConcurrentHashMap}, destroyed on every restart — and the public journal showed
     * 959 {@code STARTUP} events, so the budget refilled on the operator's own restarts.
     * A named, privilege-tagged account could be sprayed with zero account-side cost. These
     * two columns move the counter onto the account and onto disk.</p>
     */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    /** Until when the account refuses logins. {@code null} means not locked. */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

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

    public long getSessionEpoch() { return sessionEpoch; }
    public void setSessionEpoch(long sessionEpoch) { this.sessionEpoch = sessionEpoch; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

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
