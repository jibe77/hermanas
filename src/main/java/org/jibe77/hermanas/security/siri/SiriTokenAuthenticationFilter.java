package org.jibe77.hermanas.security.siri;

import org.jibe77.hermanas.security.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Authenticates requests carrying a static {@code X-Siri-Token} header so iOS
 * Shortcuts (and Siri voice commands wired on top of them) can call the API
 * without going through the form-login + session-cookie + CSRF dance.
 *
 * <p>When the header matches {@code hermanas.security.siri-token}, the request
 * is populated with a synthetic {@code siri} principal carrying {@code ROLE_USER}.
 * The rest of the security chain then applies normally — the request is still
 * subject to the {@code authorizeRequests} rules in
 * {@link SecurityConfig}.</p>
 *
 * <p>When the property is empty or unset, the filter is a no-op. This keeps the
 * door closed by default: an unconfigured deployment cannot be reached by anyone
 * waving a guessed header value.</p>
 *
 * <p>The comparison itself lives in {@link SiriTokenVerifier}, shared with the CSRF
 * exemption so that "authenticated via the Siri token" and "exempt from CSRF" can never
 * mean different things again (CRYPTO-004).</p>
 */
@Component
public class SiriTokenAuthenticationFilter extends OncePerRequestFilter {

    /** Kept for callers that referenced the header name before the verifier existed. */
    public static final String HEADER_NAME = SiriTokenVerifier.HEADER_NAME;

    private static final String PRINCIPAL_NAME = "siri";

    private static final Logger logger = LoggerFactory.getLogger(SiriTokenAuthenticationFilter.class);

    private final SiriTokenVerifier verifier;

    public SiriTokenAuthenticationFilter(SiriTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (verifier.isEnabled() && verifier.isAuthenticated(request)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    PRINCIPAL_NAME,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + SecurityConfig.ROLE_USER)));
            SecurityContextHolder.getContext().setAuthentication(auth);
            // INFO rather than debug: a static shared token is a credential that cannot be
            // attributed or revoked per device, so its use belongs in the record (CRYPTO-005).
            logger.info("Authenticated request via Siri token from {}", request.getRemoteAddr());
        }
        chain.doFilter(request, response);
    }
}
