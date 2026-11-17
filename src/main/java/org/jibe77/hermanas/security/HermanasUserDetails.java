package org.jibe77.hermanas.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The {@link UserDetails} Hermanas puts in the {@code SecurityContext}, carrying the
 * account's <em>session epoch</em> (AUTH-VULN-01).
 *
 * <p>Changing a password used to re-hash and save the row and nothing else: no
 * {@code session.invalidate()}, no remember-me logout, no deletion of the
 * {@code persistent_logins} rows. Because the {@code SecurityContext} — username plus
 * {@code ROLE_<role>} — is written into the JDBC-backed HTTP session at authentication
 * time and replayed verbatim afterwards, the privileges cached at login survived every
 * remediation the victim could perform, including a demotion and a deletion.</p>
 *
 * <p>The epoch is a counter on {@code hermanas_user} that is bumped whenever the account's
 * authority should stop being valid: password change, role change, deletion.
 * {@link SessionEpochFilter} compares the value cached in the session against the row on
 * every request and drops the session when they differ, so a token minted before the
 * change cannot outlive it even if some other revocation path is missed.</p>
 */
public class HermanasUserDetails implements UserDetails {

    private final String username;
    private final String passwordHash;
    private final String role;
    private final long sessionEpoch;

    public HermanasUserDetails(String username, String passwordHash, String role, long sessionEpoch) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.sessionEpoch = sessionEpoch;
    }

    /** Counter value the account row carried when this principal was authenticated. */
    public long getSessionEpoch() {
        return sessionEpoch;
    }

    /** The bare role name ({@code USER} / {@code ADMIN}), without the {@code ROLE_} prefix. */
    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HermanasUserDetails that)) {
            return false;
        }
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "HermanasUserDetails[" + username + ", role=" + role + ", epoch=" + sessionEpoch + "]";
    }
}
