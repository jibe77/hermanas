package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.dto.UserCreateRequest;
import org.jibe77.hermanas.dto.UserDTO;
import org.jibe77.hermanas.dto.UserUpdateRequest;
import org.jibe77.hermanas.security.PasswordPolicy;
import org.jibe77.hermanas.security.SecurityConfig;
import org.jibe77.hermanas.security.SessionRevocationService;
import org.jibe77.hermanas.service.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * User management REST endpoints.
 *
 * <ul>
 *   <li>{@code /me} — any authenticated user can read/update their own profile.</li>
 *   <li>everything else — restricted to {@link SecurityConfig#ROLE_ADMIN}.</li>
 * </ul>
 */
@Lazy
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User accounts: self-service profile (/me) and admin CRUD")
public class UserRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);
    private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final HermanasUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EventService eventService;
    private final PasswordPolicy passwordPolicy;
    private final SessionRevocationService sessionRevocationService;

    public UserRestController(HermanasUserRepository repository,
                              PasswordEncoder passwordEncoder,
                              EventService eventService,
                              PasswordPolicy passwordPolicy,
                              SessionRevocationService sessionRevocationService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.eventService = eventService;
        this.passwordPolicy = passwordPolicy;
        this.sessionRevocationService = sessionRevocationService;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Self-service endpoints (any authenticated user)
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Get the profile of the currently authenticated user")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> me(Authentication auth) {
        HermanasUser user = repository.findByLogin(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(user));
    }

    @Operation(summary = "Update the profile of the currently authenticated user (email, notifications, password)")
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateMe(Authentication auth, @RequestBody UserUpdateRequest body) {
        HermanasUser user = repository.findByLogin(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // self-update cannot change role — silently ignored to keep the API forgiving.
        body.setRole(null);
        ResponseEntity<?> response = applyUpdate(user, body);
        if (response.getStatusCode().is2xxSuccessful()) {
            eventService.record(EventType.USER_SELF_UPDATED,
                    "login=" + user.getLogin() + " " + describeUpdate(body));
        }
        return response;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Admin CRUD
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "List every user (admin only)")
    @GetMapping
    @PreAuthorize("hasRole('" + SecurityConfig.ROLE_ADMIN + "')")
    public List<UserDTO> list() {
        List<UserDTO> result = new java.util.ArrayList<>();
        repository.findAll().forEach(u -> result.add(toDto(u)));
        return result;
    }

    @Operation(summary = "Create a new user (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or duplicate login")
    })
    @PostMapping
    @PreAuthorize("hasRole('" + SecurityConfig.ROLE_ADMIN + "')")
    public ResponseEntity<?> create(@RequestBody UserCreateRequest body) {
        if (body.getLogin() == null || body.getLogin().trim().isEmpty()) {
            return badRequest("Login is required");
        }
        // AUTH-VULN-04: this endpoint happily minted a ROLE_ADMIN account with a
        // one-character secret. Same policy as self-registration now applies here.
        ResponseEntity<?> weak = checkPasswordStrength(body.getPassword(),
                body.getLogin(), body.getEmail());
        if (weak != null) {
            return weak;
        }
        if (repository.existsByLogin(body.getLogin())) {
            return badRequest("A user with this login already exists");
        }
        if (body.getEmail() != null && !body.getEmail().isEmpty() && !EMAIL_RE.matcher(body.getEmail()).matches()) {
            return badRequest("Invalid email address");
        }
        String role = normalizeRole(body.getRole());
        HermanasUser user = new HermanasUser(
                body.getLogin().trim(),
                passwordEncoder.encode(body.getPassword()),
                emptyToNull(body.getEmail()),
                role,
                body.isNotificationsEnabled(),
                normalizeLanguage(body.getLanguage()));
        repository.save(user);
        logger.info("Admin created user '{}' (role={})", user.getLogin(), user.getRole());
        eventService.record(EventType.USER_CREATED,
                "login=" + user.getLogin() + " role=" + user.getRole());
        return ResponseEntity.ok(toDto(user));
    }

    @Operation(summary = "Update an existing user (admin only)")
    @PutMapping("/{login}")
    @PreAuthorize("hasRole('" + SecurityConfig.ROLE_ADMIN + "')")
    public ResponseEntity<?> update(
            @Parameter(description = "Login of the user to update") @PathVariable("login") String login,
            @RequestBody UserUpdateRequest body) {
        HermanasUser user = repository.findByLogin(login).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // refuse to downgrade the last remaining admin
        if (body.getRole() != null && SecurityConfig.ROLE_ADMIN.equals(user.getRole())
                && !SecurityConfig.ROLE_ADMIN.equals(normalizeRole(body.getRole()))
                && repository.countByRole(SecurityConfig.ROLE_ADMIN) <= 1) {
            return badRequest("Cannot remove the last administrator");
        }
        ResponseEntity<?> response = applyUpdate(user, body);
        if (response.getStatusCode().is2xxSuccessful()) {
            eventService.record(EventType.USER_UPDATED,
                    "login=" + user.getLogin() + " " + describeUpdate(body));
        }
        return response;
    }

    @Operation(summary = "Delete a user (admin only)")
    @DeleteMapping("/{login}")
    @PreAuthorize("hasRole('" + SecurityConfig.ROLE_ADMIN + "')")
    public ResponseEntity<?> delete(
            @Parameter(description = "Login of the user to delete") @PathVariable("login") String login,
            Authentication auth) {
        if (auth != null && login.equals(auth.getName())) {
            return badRequest("Cannot delete yourself");
        }
        HermanasUser user = repository.findByLogin(login).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        if (SecurityConfig.ROLE_ADMIN.equals(user.getRole())
                && repository.countByRole(SecurityConfig.ROLE_ADMIN) <= 1) {
            return badRequest("Cannot delete the last administrator");
        }
        repository.delete(user);
        // AUTH-VULN-01: a deleted ADMIN kept ROLE_ADMIN in whatever session was already
        // open. The row is gone, so the epoch bump is a no-op, but the sessions and the
        // remember-me tokens really do go away.
        sessionRevocationService.revokeAllFor(login, "account deleted");
        logger.info("Admin deleted user '{}'", login);
        eventService.record(EventType.USER_DELETED,
                "login=" + login + " role=" + user.getRole());
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Runs a candidate password through {@link PasswordPolicy} and translates a rejection
     * into the 400 shape this controller already returns. Returns {@code null} when the
     * password is acceptable.
     */
    private ResponseEntity<?> checkPasswordStrength(String password, String login, String email) {
        try {
            passwordPolicy.validate(password, java.util.Arrays.asList(login, email));
            return null;
        } catch (PasswordPolicy.WeakPasswordException e) {
            logger.info("Rejected a weak password for '{}': {} ({})", login, e.getMessage(), e.getCode());
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage(), "code", e.getCode()));
        }
    }

    private ResponseEntity<?> applyUpdate(HermanasUser user, UserUpdateRequest body) {
        boolean passwordChanged = false;
        boolean roleChanged = false;
        if (body.getEmail() != null) {
            String email = body.getEmail().trim();
            if (!email.isEmpty() && !EMAIL_RE.matcher(email).matches()) {
                return badRequest("Invalid email address");
            }
            user.setEmail(email.isEmpty() ? null : email);
        }
        if (body.getNotificationsEnabled() != null) {
            user.setNotificationsEnabled(body.getNotificationsEnabled());
        }
        if (body.getPassword() != null && !body.getPassword().isEmpty()) {
            // AUTH-VULN-04: reached from PUT /users/me and PUT /users/{login}; it used to
            // accept any non-empty replacement, including a one-character one.
            ResponseEntity<?> weak = checkPasswordStrength(body.getPassword(),
                    user.getLogin(), user.getEmail());
            if (weak != null) {
                return weak;
            }
            user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
            passwordChanged = true;
        }
        if (body.getRole() != null) {
            String newRole = normalizeRole(body.getRole());
            roleChanged = !newRole.equals(user.getRole());
            user.setRole(newRole);
        }
        if (body.getLanguage() != null) {
            user.setLanguage(normalizeLanguage(body.getLanguage()));
        }
        repository.save(user);

        // AUTH-VULN-01: re-hashing the password and saving the row revoked nothing. The
        // SecurityContext — username plus the role cached at authentication time — lives in
        // the JDBC-backed session and was replayed verbatim, so a stolen session survived
        // the victim's own password change, and even a demotion or a deletion. Everything
        // previously issued for this login goes now: sessions, remember-me tokens, epoch.
        if (passwordChanged || roleChanged) {
            sessionRevocationService.revokeAllFor(user.getLogin(),
                    passwordChanged ? "password change" : "role change");
        }
        return ResponseEntity.ok(toDto(user));
    }

    /**
     * Same normalisation rules as {@code RegistrationService#normalizeLanguage} — kept
     * private here to avoid coupling the web layer to security/. Accepts the three
     * supported codes ("fr", "en", "ro"); anything else falls back to {@code "fr"}.
     */
    private static String normalizeLanguage(String input) {
        if (input == null) {
            return "fr";
        }
        String code = input.trim().toLowerCase();
        if (code.startsWith("en")) {
            return "en";
        }
        if (code.startsWith("ro")) {
            return "ro";
        }
        return "fr";
    }

    private static String normalizeRole(String input) {
        if (input == null) {
            return SecurityConfig.ROLE_USER;
        }
        String upper = input.trim().toUpperCase();
        if (SecurityConfig.ROLE_ADMIN.equals(upper)) {
            return SecurityConfig.ROLE_ADMIN;
        }
        if (SecurityConfig.ROLE_NOT_VALIDATED_YET.equals(upper)) {
            return SecurityConfig.ROLE_NOT_VALIDATED_YET;
        }
        return SecurityConfig.ROLE_USER;
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static UserDTO toDto(HermanasUser u) {
        return new UserDTO(u.getLogin(), u.getEmail(), u.getRole(), u.isNotificationsEnabled(),
                u.getLanguage());
    }

    private static ResponseEntity<java.util.Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", message));
    }

    /**
     * Builds a short, grep-friendly summary of which user fields were touched
     * by a request, suitable for the {@code details} column of an audit event.
     * The password is reported as "set" rather than its value so the journal
     * never carries credentials.
     */
    private static String describeUpdate(UserUpdateRequest body) {
        java.util.List<String> changed = new java.util.ArrayList<>();
        if (body.getEmail() != null) changed.add("email");
        if (body.getNotificationsEnabled() != null) changed.add("notifications");
        if (body.getPassword() != null && !body.getPassword().isEmpty()) changed.add("password");
        if (body.getRole() != null) changed.add("role=" + normalizeRole(body.getRole()));
        if (body.getLanguage() != null) changed.add("language=" + normalizeLanguage(body.getLanguage()));
        return "fields=[" + String.join(",", changed) + "]";
    }
}
