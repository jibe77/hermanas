package org.jibe77.hermanas.service.push;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
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

    static {
        // BouncyCastle must be registered before any EC operation. Repeated registration
        // is a no-op so this is safe even if another component adds it too.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final ParameterRepository parameterRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final String vapidSubject;
    private PushService pushService;
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
                KeyPair kp = generateVapidKeyPair();
                pub = base64UrlPublicKey((ECPublicKey) kp.getPublic());
                priv = base64UrlPrivateKey((ECPrivateKey) kp.getPrivate());
                writeParam(VAPID_PUBLIC_KEY, pub);
                writeParam(VAPID_PRIVATE_KEY, priv);
            }
            this.publicKey = pub;
            this.pushService = new PushService(pub, priv, vapidSubject);
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
                Notification n = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(),
                        payload.getBytes(StandardCharsets.UTF_8));
                int status = pushService.send(n).getStatusLine().getStatusCode();
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

    private KeyPair generateVapidKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        return gen.generateKeyPair();
    }

    private static String base64UrlPublicKey(ECPublicKey key) {
        // 65 bytes uncompressed: 0x04 || X (32) || Y (32)
        byte[] x = bigIntegerToUnsigned32(key.getW().getAffineX());
        byte[] y = bigIntegerToUnsigned32(key.getW().getAffineY());
        byte[] encoded = new byte[65];
        encoded[0] = 0x04;
        System.arraycopy(x, 0, encoded, 1, 32);
        System.arraycopy(y, 0, encoded, 33, 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
    }

    private static String base64UrlPrivateKey(ECPrivateKey key) {
        byte[] s = bigIntegerToUnsigned32(key.getS());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s);
    }

    private static byte[] bigIntegerToUnsigned32(java.math.BigInteger bi) {
        byte[] raw = bi.toByteArray();
        byte[] out = new byte[32];
        if (raw.length == 32) {
            return raw;
        }
        if (raw.length == 33 && raw[0] == 0) {
            // Strip the leading sign byte that BigInteger.toByteArray() adds.
            System.arraycopy(raw, 1, out, 0, 32);
            return out;
        }
        // Left-pad if shorter than 32 bytes.
        System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        return out;
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
