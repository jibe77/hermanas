package org.jibe77.hermanas.data.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * One row per browser session that opted into Web Push. The (endpoint, p256dh, auth)
 * triple is what the browser sends back from `pushManager.subscribe(...)`; the server
 * needs all three to encrypt and POST a notification.
 *
 * The endpoint URL identifies the push service (FCM for Chromium, Mozilla autopush for
 * Firefox, Apple WebPush for Safari). It is also used as a uniqueness key — re-subscribing
 * from the same browser produces the same endpoint, so we update the row in place rather
 * than accumulating duplicates.
 */
@Entity
@Table(name = "push_subscription",
        uniqueConstraints = @UniqueConstraint(columnNames = "endpoint"))
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** The full push-service URL the browser will receive notifications on. */
    @Column(nullable = false, length = 512)
    private String endpoint;

    /** ECDH P-256 public key from the browser, used to encrypt the payload. */
    @Column(nullable = false, length = 255)
    private String p256dh;

    /** Authentication secret shared with the push service. */
    @Column(nullable = false, length = 255)
    private String auth;

    /** Login of the user who created this subscription. May be null for anonymous. */
    @Column(length = 64)
    private String username;

    @Column(nullable = false)
    private Instant createdAt;

    public PushSubscription() {}

    public PushSubscription(String endpoint, String p256dh, String auth, String username) {
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.username = username;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }

    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PushSubscription that = (PushSubscription) o;
        return Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint);
    }
}
