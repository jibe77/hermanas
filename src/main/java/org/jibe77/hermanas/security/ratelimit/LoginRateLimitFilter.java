package org.jibe77.hermanas.security.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP rate limiter applied to {@code POST /api/v1/auth/login} <em>before</em>
 * the Spring Security {@code UsernamePasswordAuthenticationFilter} runs.
 *
 * <p>Cannot use {@link RateLimited} / {@link RateLimiterAspect} because the login
 * endpoint is handled by Spring Security's authentication filter chain, not by
 * a Spring MVC controller — AOP method-level proxies do not intercept it.</p>
 *
 * <p>Sliding-window algorithm, same shape as {@link RateLimiterAspect.RequestTracker}.
 * Counters reset on application restart, which is acceptable for a single-instance
 * Raspberry Pi Zero deployment.</p>
 *
 * <p>Defaults : 5 attempts per minute per IP, returning HTTP 429 with a JSON body.</p>
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    /** Path the Spring Security filter chain processes login form posts on. */
    public static final String LOGIN_PATH = "/api/v1/auth/login";

    private final Map<String, RequestTracker> trackers = new ConcurrentHashMap<>();

    @Value("${hermanas.security.login-rate-limit.max-requests:5}")
    private int maxRequests;

    @Value("${hermanas.security.login-rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isLoginAttempt(request)) {
            chain.doFilter(request, response);
            return;
        }
        String ip = clientIp(request);
        RequestTracker tracker = trackers.computeIfAbsent(ip, k -> new RequestTracker());
        if (!tracker.allowRequest(maxRequests, windowSeconds)) {
            logger.warn("Login rate limit exceeded for IP {} ({} in {}s)", ip, maxRequests, windowSeconds);
            writeTooManyRequests(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isLoginAttempt(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI());
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    /** HTTP 429 — not exposed as a constant on javax.servlet.http.HttpServletResponse (Servlet 4). */
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HTTP_TOO_MANY_REQUESTS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"RATE_LIMIT_EXCEEDED\","
                + "\"message\":\"Trop de tentatives de connexion depuis cette adresse. Réessayez dans une minute.\"}");
    }

    /** Identical sliding-window logic as {@link RateLimiterAspect}'s inner tracker. */
    private static final class RequestTracker {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStartTime = System.currentTimeMillis();

        boolean allowRequest(int maxRequests, int windowSeconds) {
            long now = System.currentTimeMillis();
            long windowMillis = windowSeconds * 1000L;
            if (now - windowStartTime >= windowMillis) {
                synchronized (this) {
                    if (now - windowStartTime >= windowMillis) {
                        requestCount.set(0);
                        windowStartTime = now;
                    }
                }
            }
            return requestCount.incrementAndGet() <= maxRequests;
        }
    }
}
