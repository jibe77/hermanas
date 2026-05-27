package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API representation of a user. The password hash never leaves the server.
 */
@Schema(description = "Hermanas user account")
public class UserDTO {

    @Schema(description = "Login name", example = "marguerite")
    private String login;

    @Schema(description = "Email address used for notifications", example = "info@hermanas.fr")
    private String email;

    @Schema(description = "Role granted to the user", example = "ADMIN")
    private String role;

    @Schema(description = "Whether this user should receive coop notifications by email")
    private boolean notificationsEnabled;

    public UserDTO() {}

    public UserDTO(String login, String email, String role, boolean notificationsEnabled) {
        this.login = login;
        this.email = email;
        this.role = role;
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
}
