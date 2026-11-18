package org.jibe77.hermanas.security.siri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;

/**
 * One place that answers "does this request carry a valid Siri token?".
 *
 * <p>Before this existed, two independent pieces of code looked at the same header and
 * disagreed (CRYPTO-004). {@link SiriTokenAuthenticationFilter} authenticated only on an
 * exact match, but the CSRF exemption in {@code SecurityConfig} was keyed on header
 * <em>presence</em>:</p>
 *
 * <pre>{@code
 * .requireCsrfProtectionMatcher(req -> CsrfFilter.DEFAULT_CSRF_MATCHER.matches(req)
 *         && req.getHeader(HEADER_NAME) == null)
 * }</pre>
 *
 * <p>So any state-changing request carrying {@code X-Siri-Token: anything} walked past
 * {@code CsrfFilter} entirely — including {@code POST /system/shutdown},
 * {@code /door/open} and every other mutating endpoint. The exemption is supposed to be
 * the price of the static-token mechanism (a Shortcuts client cannot echo a cookie into a
 * header); it must therefore be granted only when that mechanism actually authenticated
 * the caller.</p>
 *
 * <p>Note that the CSRF matcher cannot read the {@code SecurityContext}: the Siri filter
 * is registered before {@code UsernamePasswordAuthenticationFilter}, which runs
 * <em>after</em> {@code CsrfFilter}. Re-deriving the answer from the header, with the same
 * comparison the filter uses, is what keeps the two in agreement.</p>
 */
@Component
public class SiriTokenVerifier {

    public static final String HEADER_NAME = "X-Siri-Token";

    private static final Logger logger = LoggerFactory.getLogger(SiriTokenVerifier.class);

    /** Recommended minimum entropy for the shared token, in bytes. */
    static final int MIN_TOKEN_BYTES = 32;

    @Value("${hermanas.security.siri-token:}")
    private String expectedToken;

    @PostConstruct
    void warnOnWeakConfiguration() {
        if (expectedToken == null || expectedToken.isEmpty()) {
            logger.info("Siri/Shortcuts integration is disabled (hermanas.security.siri-token is empty).");
            return;
        }
        int bytes = expectedToken.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_TOKEN_BYTES) {
            logger.warn("hermanas.security.siri-token is only {} bytes; a static shared bearer token "
                    + "should carry at least {} bytes of entropy (openssl rand -hex 32).",
                    bytes, MIN_TOKEN_BYTES);
        }
    }

    /** True when the integration is configured at all. Unconfigured means no-op, as before. */
    public boolean isEnabled() {
        return expectedToken != null && !expectedToken.isEmpty();
    }

    /**
     * True when this request presents the configured token. A request that merely carries
     * the header is <em>not</em> authenticated and gets no exemption anywhere.
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        if (!isEnabled()) {
            return false;
        }
        String provided = request.getHeader(HEADER_NAME);
        return provided != null && constantTimeEquals(provided, expectedToken);
    }

    /**
     * Length-independent constant-time comparison. The previous hand-rolled loop returned
     * early on a length mismatch, which leaks the token length through timing;
     * {@link MessageDigest#isEqual} pads internally and does not (CRYPTO-005).
     */
    private static boolean constantTimeEquals(String provided, String expected) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
