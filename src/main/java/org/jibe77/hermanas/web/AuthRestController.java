package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.dto.RegisterRequest;
import org.jibe77.hermanas.security.RegistrationService;
import org.jibe77.hermanas.security.RegistrationService.RegistrationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, logout, /me probe and self-service registration")
public class AuthRestController {

    private final RegistrationService registrationService;

    public AuthRestController(RegistrationService registrationService) {
        this.registrationService = registrationService;
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
        return body;
    }

    @Operation(summary = "Self-service registration. Account is created with NOT_VALIDATED_YET "
            + "and cannot log in until an administrator promotes it.")
    @PostMapping("/register")
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
