package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.PushSubscription;
import org.jibe77.hermanas.data.repository.PushSubscriptionRepository;
import org.jibe77.hermanas.service.push.PushNotificationService;
import org.jibe77.hermanas.security.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints called by the Angular front-end to opt in/out of Web Push notifications.
 *
 * <ul>
 *   <li>{@code GET /api/v1/push/vapid-public-key} — bootstrap, public to anonymous so the
 *       browser can call <code>pushManager.subscribe()</code> before logging in.</li>
 *   <li>{@code POST /api/v1/push/subscribe} — store the browser's subscription. Requires
 *       authentication so we can associate it with a user. The first caller owns the row;
 *       a later login from the same browser refreshes the keys without moving ownership.</li>
 *   <li>{@code POST /api/v1/push/unsubscribe} — remove a subscription, but only your own
 *       (an admin may remove any, to prune subscriptions of deleted accounts).</li>
 *   <li>{@code POST /api/v1/push/test} — admin-only, broadcasts a test notification.</li>
 * </ul>
 */
@Lazy
@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Push", description = "Web Push notifications opt-in for the PWA")
public class PushRestController {

    private static final Logger logger = LoggerFactory.getLogger(PushRestController.class);

    private static final String ADMIN_AUTHORITY = "ROLE" + SecurityConfig.ROLE_ADMIN;

    private final PushNotificationService pushService;
    private final PushSubscriptionRepository repository;

    public PushRestController(PushNotificationService pushService,
                              PushSubscriptionRepository repository) {
        this.pushService = pushService;
        this.repository = repository;
    }

    @Operation(summary = "Public VAPID key", description = "Returns the server's VAPID public key so the browser can register a push subscription.")
    @GetMapping("/vapid-public-key")
    public Map<String, String> vapidPublicKey() {
        Map<String, String> out = new HashMap<>();
        out.put("publicKey", pushService.getPublicKey());
        return out;
    }

    @Operation(summary = "Register a subscription", description = "Stores the browser's push subscription so the server can later notify it.")
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody SubscribeRequest req) {
        if (req == null || req.endpoint == null || req.p256dh == null || req.auth == null) {
            return ResponseEntity.badRequest().build();
        }
        String username = currentUsername();
        if (username == null) {
            // Defensive: POST /api/v1/** already requires a role, so this is only
            // reachable through a future rule change. An anonymous row would be an
            // orphan nobody can maintain.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // SSRF-VULN-03: refuse to store a subscription that points the notifier at an
        // internal address. Checked at subscribe time so the operator gets a clean 400
        // instead of a silently skipped push.
        String refusal = pushService.endpointRefusalReason(req.endpoint);
        if (refusal != null) {
            logger.warn("Rejected push subscription endpoint: {}", refusal);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "INVALID_ENDPOINT");
            error.put("message", refusal);
            return ResponseEntity.badRequest().body(error);
        }

        PushSubscription sub = repository.findByEndpoint(req.endpoint).orElse(null);
        if (sub == null) {
            sub = new PushSubscription();
            sub.setEndpoint(req.endpoint);
            sub.setUsername(username);
            sub.setCreatedAt(java.time.Instant.now());
        } else if (sub.getUsername() == null) {
            // Rows written before the username column existed are unowned: the first
            // caller to present one gets to keep it.
            sub.setUsername(username);
        } else if (!sub.getUsername().equals(username)) {
            // AUTHZ-VULN-04: ownership used to follow the last writer, so any
            // authenticated user who replayed somebody else's endpoint string took the
            // subscription over — and could then delete it. The owner is now claimed
            // once and never transferred implicitly. The key material is still
            // refreshed, because the push services rotate p256dh/auth on the *browser*
            // side: a shared family tablet is one endpoint used by several logins, and
            // refusing that write would silently stop everyone's notifications.
            logger.info("Push endpoint already owned by '{}', refreshed keys without "
                    + "transferring ownership to '{}'", sub.getUsername(), username);
        }
        sub.setP256dh(req.p256dh);
        sub.setAuth(req.auth);
        repository.save(sub);
        Map<String, Object> body = new HashMap<>();
        body.put("id", sub.getId());
        body.put("username", sub.getUsername());
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Unregister a subscription")
    @PostMapping("/unsubscribe")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> unsubscribe(@RequestBody UnsubscribeRequest req) {
        if (req == null || req.endpoint == null) {
            return ResponseEntity.badRequest().build();
        }
        String username = currentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (isAdmin()) {
            // An admin has to be able to prune subscriptions left behind by an account
            // that was deleted — nobody else can claim them any more.
            repository.deleteByEndpoint(req.endpoint);
        } else {
            // AUTHZ-VULN-04: deleting by endpoint alone let any authenticated user
            // switch off another user's notifications.
            repository.deleteByUsernameAndEndpoint(username, req.endpoint);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Send a test notification", description = "Admin-only. Pushes a test notification to every registered subscription.")
    @PostMapping("/test")
    public Map<String, Object> test() {
        int sent = pushService.broadcast("Hermanas", "Notification de test", "/dashboard");
        logger.info("Test push broadcasted to {} subscriptions", sent);
        Map<String, Object> out = new HashMap<>();
        out.put("sent", sent);
        return out;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    /**
     * There is no role hierarchy in this application, so ADMIN is not a USER and has to
     * be recognised on its own authority (see SecurityConfig).
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities().stream().anyMatch(a -> ADMIN_AUTHORITY.equals(a.getAuthority()));
    }

    public static class SubscribeRequest {
        public String endpoint;
        public String p256dh;
        public String auth;
    }

    public static class UnsubscribeRequest {
        public String endpoint;
    }
}
