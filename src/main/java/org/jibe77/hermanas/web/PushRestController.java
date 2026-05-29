package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.PushSubscription;
import org.jibe77.hermanas.data.repository.PushSubscriptionRepository;
import org.jibe77.hermanas.service.push.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints called by the Angular front-end to opt in/out of Web Push notifications.
 *
 * <ul>
 *   <li>{@code GET /api/v1/push/vapid-public-key} — bootstrap, public to anonymous so the
 *       browser can call <code>pushManager.subscribe()</code> before logging in.</li>
 *   <li>{@code POST /api/v1/push/subscribe} — store the browser's subscription. Requires
 *       authentication so we can associate it with a user.</li>
 *   <li>{@code POST /api/v1/push/unsubscribe} — remove a subscription by endpoint.</li>
 *   <li>{@code POST /api/v1/push/test} — admin-only, broadcasts a test notification.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Push", description = "Web Push notifications opt-in for the PWA")
public class PushRestController {

    private static final Logger logger = LoggerFactory.getLogger(PushRestController.class);

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
        PushSubscription sub = repository.findByEndpoint(req.endpoint).orElseGet(PushSubscription::new);
        sub.setEndpoint(req.endpoint);
        sub.setP256dh(req.p256dh);
        sub.setAuth(req.auth);
        sub.setUsername(username);
        if (sub.getCreatedAt() == null) {
            sub.setCreatedAt(java.time.Instant.now());
        }
        repository.save(sub);
        Map<String, Object> body = new HashMap<>();
        body.put("id", sub.getId());
        body.put("username", username);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Unregister a subscription")
    @PostMapping("/unsubscribe")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> unsubscribe(@RequestBody UnsubscribeRequest req) {
        if (req == null || req.endpoint == null) {
            return ResponseEntity.badRequest().build();
        }
        repository.deleteByEndpoint(req.endpoint);
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

    public static class SubscribeRequest {
        public String endpoint;
        public String p256dh;
        public String auth;
    }

    public static class UnsubscribeRequest {
        public String endpoint;
    }
}
