package org.jibe77.hermanas.security.ratelimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AOP Aspect that enforces rate limiting on methods annotated with @RateLimited.
 * Uses a simple sliding window algorithm with per-IP tracking.
 * Designed to be lightweight for Raspberry Pi Zero deployment.
 */
@Aspect
@Component
public class RateLimiterAspect {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterAspect.class);

    // Map of IP -> (endpoint -> request tracker)
    private final Map<String, Map<String, RequestTracker>> requestTrackers = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimited)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            // No HTTP context, allow the request (e.g., internal calls)
            return joinPoint.proceed();
        }

        String clientIp = getClientIp(request);
        String endpoint = getEndpointKey(joinPoint);

        RequestTracker tracker = getOrCreateTracker(clientIp, endpoint);

        if (!tracker.allowRequest(rateLimited.maxRequests(), rateLimited.windowSeconds())) {
            logger.warn("Rate limit exceeded for IP {} on endpoint {}", clientIp, endpoint);
            throw new RateLimitExceededException(rateLimited.message());
        }

        logger.debug("Rate limit check passed for IP {} on endpoint {} ({}/{})",
                     clientIp, endpoint, tracker.getRequestCount(), rateLimited.maxRequests());
        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getEndpointKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private RequestTracker getOrCreateTracker(String clientIp, String endpoint) {
        return requestTrackers
            .computeIfAbsent(clientIp, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(endpoint, k -> new RequestTracker());
    }

    /**
     * Tracks requests for a specific IP and endpoint combination.
     * Uses a simple sliding window approach with automatic cleanup.
     */
    private static class RequestTracker {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStartTime = System.currentTimeMillis();

        public boolean allowRequest(int maxRequests, int windowSeconds) {
            long now = System.currentTimeMillis();
            long windowMillis = windowSeconds * 1000L;

            // Reset if window has expired
            if (now - windowStartTime >= windowMillis) {
                synchronized (this) {
                    if (now - windowStartTime >= windowMillis) {
                        requestCount.set(0);
                        windowStartTime = now;
                    }
                }
            }

            int currentCount = requestCount.incrementAndGet();
            return currentCount <= maxRequests;
        }

        public int getRequestCount() {
            return requestCount.get();
        }
    }

    /**
     * Cleanup task to remove stale trackers (can be scheduled if needed).
     * Currently, trackers are cleaned up naturally when their windows expire.
     */
    public void cleanup() {
        requestTrackers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
