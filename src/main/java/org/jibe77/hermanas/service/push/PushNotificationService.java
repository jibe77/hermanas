package org.jibe77.hermanas.service.push;

import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.entity.PushSubscription;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.jibe77.hermanas.data.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;

/**
 * Generates a VAPID keypair on first boot (stored in the {@code Parameter} key-value
 * table so the same keys survive restarts), exposes the public key to the frontend, and
 * pushes Web Push notifications to every registered subscription.
 *
 * <h3>Why VAPID</h3>
 * Without VAPID, anonymous push services (FCM, Mozilla autopush) refuse our requests.
 * The keypair acts as the "from" identity of our server; the public part is shipped to
 * the browser and embedded in the subscription handshake.
 *
 * <h3>Auto-prune</h3>
 * When a push service returns 404 or 410 the subscription is permanently gone (browser
 * uninstalled, cleared site data, etc). The send loop deletes those rows so the table
 * does not grow forever.
 */
@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String VAPID_PUBLIC_KEY = "hermanas.push.vapid.public-key";
    private static final String VAPID_PRIVATE_KEY = "hermanas.push.vapid.private-key";

    private final ParameterRepository parameterRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final String vapidSubject;
    private WebPushSender pushService;
    private String publicKey;

    public PushNotificationService(ParameterRepository parameterRepository,
                                   PushSubscriptionRepository subscriptionRepository,
                                   @Value("${hermanas.push.vapid.subject:mailto:admin@hermanas.local}")
                                   String vapidSubject) {
        this.parameterRepository = parameterRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.vapidSubject = vapidSubject;
    }

    @PostConstruct
    public void initialize() {
        try {
            String pub = readParam(VAPID_PUBLIC_KEY);
            String priv = readParam(VAPID_PRIVATE_KEY);
            if (pub == null || priv == null) {
                logger.info("No VAPID keypair found, generating a new one.");
                KeyPair kp = WebPushSender.generateP256KeyPair();
                pub = base64UrlPublicKey((ECPublicKey) kp.getPublic());
                priv = base64UrlPrivateKey((ECPrivateKey) kp.getPrivate());
                writeParam(VAPID_PUBLIC_KEY, pub);
                writeParam(VAPID_PRIVATE_KEY, priv);
            }
            this.publicKey = pub;
            this.pushService = new WebPushSender(pub, priv, vapidSubject);
            logger.info("PushNotificationService initialized (vapid subject: {}).", vapidSubject);
        } catch (Exception e) {
            logger.error("Could not initialize PushNotificationService — push notifications disabled.", e);
        }
    }

    /** Public VAPID key, base64url-encoded. Shipped to the browser. */
    public String getPublicKey() {
        return publicKey;
    }

    public boolean isAvailable() {
        return pushService != null && publicKey != null;
    }

    /**
     * Sends the same payload to every registered subscription. Returns the number of
     * successful deliveries. Subscriptions that return 404/410 (gone) are deleted.
     */
    public int broadcast(String title, String body, String url) {
        if (!isAvailable()) {
            logger.debug("PushService unavailable, skipping broadcast.");
            return 0;
        }
        List<PushSubscription> all = (List<PushSubscription>) subscriptionRepository.findAll();
        if (all.isEmpty()) {
            return 0;
        }
        // Angular's ngsw-worker.js expects { notification: { title, body, data, ... } }
        // and will call self.registration.showNotification(title, options) automatically.
        // The `data.url` we put inside the notification is read by our SwPush click handler
        // to navigate to the right route when the user taps the notification.
        String payload = String.format(
                "{\"notification\":{\"title\":\"%s\",\"body\":\"%s\",\"icon\":\"/icons/icon-192x192.png\",\"badge\":\"/icons/icon-72x72.png\",\"data\":{\"url\":\"%s\"}}}",
                escape(title), escape(body), escape(url == null ? "/dashboard" : url));
        int sent = 0;
        for (PushSubscription sub : all) {
            try {
                int status = pushService.send(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(),
                        payload.getBytes(StandardCharsets.UTF_8));
                if (status == 404 || status == 410) {
                    logger.info("Pruning gone subscription {}", sub.getId());
                    subscriptionRepository.delete(sub);
                } else if (status >= 200 && status < 300) {
                    sent++;
                } else {
                    logger.warn("Push to {} returned HTTP {}", sub.getId(), status);
                }
            } catch (Exception e) {
                logger.warn("Failed to push to subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
        return sent;
    }

    private String readParam(String key) {
        Parameter p = parameterRepository.findByEntryKey(key);
        return p == null ? null : p.getEntryValue();
    }

    private void writeParam(String key, String value) {
        Parameter p = parameterRepository.findByEntryKey(key);
        if (p == null) {
            p = new Parameter();
            p.setEntryKey(key);
        }
        p.setEntryValue(value);
        parameterRepository.save(p);
    }

    private static String base64UrlPublicKey(ECPublicKey key) {
        // 65 bytes uncompressed: 0x04 || X (32) || Y (32)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(WebPushSender.encodePoint(key));
    }

    private static String base64UrlPrivateKey(ECPrivateKey key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(WebPushSender.unsigned32(key.getS()));
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    /** Used by tests only. */
    PushSubscriptionRepository subscriptionRepository() {
        return subscriptionRepository;
    }
}
