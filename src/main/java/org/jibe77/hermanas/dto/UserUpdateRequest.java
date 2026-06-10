package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Partial-update payload. Each field is optional — only non-null values are applied.
 * Used by both /users/me (self update) and /users/{login} (admin update).
 */
@Schema(description = "Partial user update (only non-null fields are applied)")
public class UserUpdateRequest {

    @Schema(description = "New email address — set to empty string to clear")
    private String email;

    @Schema(description = "Whether the user should receive notifications")
    private Boolean notificationsEnabled;

    @Schema(description = "New cleartext password (will be hashed). Leave null to keep current password.")
    private String password;

    @Schema(description = "New role (admin only)")
    private String role;

    @Schema(description = "Preferred UI / notification language (\"fr\" or \"en\"). "
            + "Unrecognised values fall back to \"fr\".")
    private String language;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
