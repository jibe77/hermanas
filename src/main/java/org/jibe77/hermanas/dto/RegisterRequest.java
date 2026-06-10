package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload for self-service registration ({@code POST /api/v1/auth/register}). No role is accepted —
 * the server always assigns {@code NOT_VALIDATED_YET} until an administrator promotes the account.
 */
@Schema(description = "Payload for self-service user registration")
public class RegisterRequest {

    @Schema(description = "Desired login", example = "alice", required = true)
    private String login;

    @Schema(description = "Clear-text password (bcrypt-hashed server-side)", required = true)
    private String password;

    @Schema(description = "Email address used both for the confirmation mail and for future notifications",
            example = "alice@example.com", required = true)
    private String email;

    @Schema(description = "Preferred UI / notification language (two-letter ISO code). "
            + "Accepted values: \"fr\", \"en\". Defaults to \"fr\" when omitted.",
            example = "fr")
    private String language;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
