package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.dto.RegisterRequest;
import org.jibe77.hermanas.security.RegistrationService;
import org.jibe77.hermanas.security.RegistrationService.RegistrationException;
import org.jibe77.hermanas.security.ratelimit.RateLimited;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, logout, /me probe and self-service registration")
public class AuthRestController {

    private final RegistrationService registrationService;
    private final HermanasUserRepository userRepository;

    public AuthRestController(RegistrationService registrationService,
                              HermanasUserRepository userRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> body = new HashMap<>();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            body.put("authenticated", false);
            return body;
        }
        body.put("authenticated", true);
        body.put("username", authentication.getName());
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        body.put("roles", roles);
        // Surface the preferred language so the SPA can switch locale right after login
        // (see frontend/UserService → checkAuthState). Falls back silently when the user
        // is not in the DB-backed repository (e.g. legacy users.properties bootstrap).
        Optional<HermanasUser> dbUser = userRepository.findByLogin(authentication.getName());
        body.put("language", dbUser.map(HermanasUser::getLanguage).orElse("fr"));
        return body;
    }

    @Operation(summary = "Self-service registration. Account is created with NOT_VALIDATED_YET "
            + "and cannot log in until an administrator promotes it.")
    @PostMapping("/register")
    @RateLimited(maxRequests = 3, windowSeconds = 3600,
            message = "Trop d'inscriptions depuis cette adresse. Réessayez dans une heure.")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        try {
            HermanasUser user = registrationService.register(body);
            Map<String, Object> response = new HashMap<>();
            response.put("login", user.getLogin());
            response.put("status", "pending-validation");
            return ResponseEntity.ok(response);
        } catch (RegistrationException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getCode());
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
