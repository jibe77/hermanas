package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload to create a new user account (admin only)")
public class UserCreateRequest {

    @Schema(description = "Login name", example = "jean")
    private String login;

    @Schema(description = "Cleartext password — hashed on the server before being persisted")
    private String password;

    @Schema(description = "Email address", example = "jean@example.com")
    private String email;

    @Schema(description = "Role (USER or ADMIN)", example = "USER")
    private String role;

    @Schema(description = "Whether the user should receive coop notifications")
    private boolean notificationsEnabled;

    @Schema(description = "Preferred UI / notification language (\"fr\" or \"en\")", example = "fr")
    private String language;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
