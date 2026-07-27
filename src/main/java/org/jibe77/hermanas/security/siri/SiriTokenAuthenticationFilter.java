package org.jibe77.hermanas.security.siri;

import org.jibe77.hermanas.security.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 */
@Component
public class SiriTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Siri-Token";
    private static final String PRINCIPAL_NAME = "siri";

    private static final Logger logger = LoggerFactory.getLogger(SiriTokenAuthenticationFilter.class);

    @Value("${hermanas.security.siri-token:}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (expectedToken != null && !expectedToken.isEmpty()) {
            String provided = request.getHeader(HEADER_NAME);
            if (provided != null && constantTimeEquals(provided, expectedToken)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        PRINCIPAL_NAME,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + SecurityConfig.ROLE_USER)));
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.debug("Authenticated request via Siri token from {}", request.getRemoteAddr());
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
