package org.jibe77.hermanas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-request check that the authority cached in the session is still the authority the
 * account has (AUTH-VULN-01).
 *
 * <p>{@code DbUserDetailsService.loadUserByUsername} runs at login and at remember-me
 * auto-login — never per request — so the {@code SecurityContext} stored in the
 * JDBC-backed session is replayed verbatim for its whole lifetime. A demotion, a password
 * rotation or even a deletion therefore left an already-authenticated attacker serving
 * {@code ROLE_ADMIN} until the idle timeout happened to expire, and 30 minutes is an
 * <em>idle</em> timeout: an active attacker never reaches it.</p>
 *
 * <p>This filter compares the {@link HermanasUserDetails#getSessionEpoch()} baked into the
 * session against the row. Any difference — or a missing row — means the session was
 * minted before the account changed, so it is invalidated and the caller gets a clean 401
 * like any other unauthenticated API request.</p>
 *
 * <p>The lookup is one indexed {@code SELECT} on a one-row-by-one-column read, and
 * {@code spring-session-jdbc} already hits the database on every request to load the
 * session itself, so this adds no meaningful cost on the Pi.</p>
 */
@Component
public class SessionEpochFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SessionEpochFilter.class);

    private final HermanasUserRepository userRepository;

    public SessionEpochFilter(HermanasUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof HermanasUserDetails principal)) {
            // Anonymous, or a synthetic principal (the Siri token) that has no row to check.
            chain.doFilter(request, response);
            return;
        }

        Long currentEpoch = userRepository.findByLogin(principal.getUsername())
                .map(user -> user.getSessionEpoch())
                .orElse(null);

        if (currentEpoch != null && currentEpoch == principal.getSessionEpoch()) {
            chain.doFilter(request, response);
            return;
        }

        String reason = currentEpoch == null ? "the account no longer exists"
                : "the account changed (epoch " + principal.getSessionEpoch() + " -> " + currentEpoch + ")";
        logger.info("Dropping the session of '{}': {}.", principal.getUsername(), reason);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"SESSION_REVOKED\"}");
    }
}
