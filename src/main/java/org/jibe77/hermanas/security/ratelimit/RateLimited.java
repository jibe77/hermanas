package org.jibe77.hermanas.security.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be rate limited.
 * Uses a simple token bucket algorithm with per-IP tracking.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    /**
     * Maximum number of requests allowed in the time window.
     * @return max requests
     */
    int maxRequests() default 5;

    /**
     * Time window in seconds for rate limiting.
     * @return window duration in seconds
     */
    int windowSeconds() default 60;

    /**
     * Custom error message to return when rate limit is exceeded.
     * @return error message
     */
    String message() default "Rate limit exceeded. Please try again later.";
}
