package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.dto.UserCreateRequest;
import org.jibe77.hermanas.dto.UserDTO;
import org.jibe77.hermanas.dto.UserUpdateRequest;
import org.jibe77.hermanas.security.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User accounts: self-service profile (/me) and admin CRUD")
public class UserRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);
    private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final HermanasUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserRestController(HermanasUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
        return applyUpdate(user, body);
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
        if (body.getPassword() == null || body.getPassword().isEmpty()) {
            return badRequest("Password is required");
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
                body.isNotificationsEnabled());
        repository.save(user);
        logger.info("Admin created user '{}' (role={})", user.getLogin(), user.getRole());
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
        return applyUpdate(user, body);
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
        logger.info("Admin deleted user '{}'", login);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────────

    private ResponseEntity<?> applyUpdate(HermanasUser user, UserUpdateRequest body) {
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
            user.setPasswordHash(passwordEncoder.encode(body.getPassword()));
        }
        if (body.getRole() != null) {
            user.setRole(normalizeRole(body.getRole()));
        }
        repository.save(user);
        return ResponseEntity.ok(toDto(user));
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
        return new UserDTO(u.getLogin(), u.getEmail(), u.getRole(), u.isNotificationsEnabled());
    }

    private static ResponseEntity<java.util.Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", message));
    }
}
