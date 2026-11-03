package org.jibe77.hermanas.service.push;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * Minimal Web Push sender built on the JDK only: payload encryption per RFC 8291
 * ({@code aes128gcm}, RFC 8188) and VAPID authentication per RFC 8292 (ES256 JWT).
 *
 * <p>Replaces {@code nl.martijndwars:web-push}, which dragged in async-http-client,
 * Netty (event-loop threads + native buffers) and BouncyCastle (7 MB) — heavy for
 * the Pi Zero when the JDK already ships P-256 ECDH, AES-GCM, HMAC and an
 * {@link HttpClient}.</p>
 */
class WebPushSender {

    /** Same default as the former library: 4 weeks. */
    static final int TTL_SECONDS = 2_419_200;
    private static final int RECORD_SIZE = 4096;
    private static final Duration JWT_VALIDITY = Duration.ofHours(12);
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    private static final ECParameterSpec P256 = p256();

    private final ECPrivateKey vapidPrivateKey;
    private final String vapidPublicKey;
    private final String vapidSubject;
    private final HttpClient httpClient;
    private final SecureRandom random = new SecureRandom();

    /**
     * @param vapidPublicKey  base64url, 65-byte uncompressed point
     * @param vapidPrivateKey base64url, 32-byte scalar
     */
    WebPushSender(String vapidPublicKey, String vapidPrivateKey, String vapidSubject)
            throws GeneralSecurityException {
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = privateKey(decode(vapidPrivateKey));
        this.vapidSubject = vapidSubject;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Encrypts {@code payload} for the subscription and POSTs it. Returns the HTTP status. */
    int send(String endpoint, String p256dh, String auth, byte[] payload) throws Exception {
        byte[] body = encrypt(payload, decode(p256dh), decode(auth),
                generateP256KeyPair(), randomSalt());
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("TTL", String.valueOf(TTL_SECONDS))
                .header("Content-Encoding", "aes128gcm")
                .header("Content-Type", "application/octet-stream")
                .header("Authorization", "vapid t=" + vapidJwt(endpoint) + ", k=" + vapidPublicKey)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    // ---------------------------------------------------------------- RFC 8291

    /**
     * Builds the {@code aes128gcm} body: header (salt, record size, sender public key)
     * followed by a single encrypted record. Ephemeral key pair and salt are parameters
     * so the RFC 8291 test vector can be replayed.
     */
    static byte[] encrypt(byte[] payload, byte[] uaPublic, byte[] authSecret,
                          KeyPair ephemeral, byte[] salt) throws GeneralSecurityException {
        byte[] asPublic = encodePoint((ECPublicKey) ephemeral.getPublic());

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(publicKey(uaPublic), true);
        byte[] ecdhSecret = ka.generateSecret();

        // IKM = HKDF(auth_secret, ecdh_secret, "WebPush: info" || 0x00 || ua_public || as_public, 32)
        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);
        byte[] cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        // 0x02 = padding delimiter of the last (and only) record.
        byte[] ciphertext = cipher.doFinal(concat(payload, new byte[]{2}));

        ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length);
        header.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic);
        return concat(header.array(), ciphertext);
    }

    /** HKDF-SHA256 with a single expand block (length <= 32). */
    private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        mac.update(info);
        mac.update((byte) 1);
        byte[] okm = mac.doFinal();
        byte[] out = new byte[length];
        System.arraycopy(okm, 0, out, 0, length);
        return out;
    }

    // ---------------------------------------------------------------- RFC 8292

    private String vapidJwt(String endpoint) throws GeneralSecurityException {
        URI uri = URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        long exp = System.currentTimeMillis() / 1000 + JWT_VALIDITY.toSeconds();
        String header = B64URL.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = B64URL.encodeToString(("{\"aud\":\"" + audience + "\",\"exp\":" + exp
                + ",\"sub\":\"" + vapidSubject + "\"}").getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + claims;
        // P1363 format = raw R || S, which is what JWS ES256 expects (not DER).
        Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
        signature.initSign(vapidPrivateKey);
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + B64URL.encodeToString(signature.sign());
    }

    // ---------------------------------------------------------------- keys

    static KeyPair generateP256KeyPair() throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        return gen.generateKeyPair();
    }

    private byte[] randomSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    static ECPublicKey publicKey(byte[] uncompressed) throws GeneralSecurityException {
        if (uncompressed.length != 65 || uncompressed[0] != 0x04) {
            throw new GeneralSecurityException("Expected a 65-byte uncompressed P-256 point");
        }
        BigInteger x = new BigInteger(1, java.util.Arrays.copyOfRange(uncompressed, 1, 33));
        BigInteger y = new BigInteger(1, java.util.Arrays.copyOfRange(uncompressed, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
    }

    static ECPrivateKey privateKey(byte[] scalar) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, scalar), P256));
    }

    static byte[] encodePoint(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 0x04;
        System.arraycopy(unsigned32(key.getW().getAffineX()), 0, out, 1, 32);
        System.arraycopy(unsigned32(key.getW().getAffineY()), 0, out, 33, 32);
        return out;
    }

    static byte[] unsigned32(BigInteger bi) {
        byte[] raw = bi.toByteArray();
        if (raw.length == 32) {
            return raw;
        }
        byte[] out = new byte[32];
        if (raw.length > 32) {
            // Strip the leading sign byte that BigInteger.toByteArray() adds.
            System.arraycopy(raw, raw.length - 32, out, 0, 32);
        } else {
            System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        }
        return out;
    }

    /** Accepts base64url or standard base64, padded or not — browsers vary. */
    static byte[] decode(String value) {
        String s = value.trim().replace('+', '-').replace('/', '_');
        int pad = s.indexOf('=');
        return Base64.getUrlDecoder().decode(pad >= 0 ? s.substring(0, pad) : s);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            return params.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 curve unavailable", e);
        }
    }
}
