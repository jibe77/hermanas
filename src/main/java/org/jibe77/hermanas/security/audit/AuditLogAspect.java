package org.jibe77.hermanas.security.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AOP Aspect that logs audit events for security-sensitive operations.
 * Logs include timestamp, user, IP address, operation, category, and result.
 * Designed for regulatory compliance and security monitoring.
 */
@Aspect
@Component
public class AuditLogAspect {
    // Use a dedicated audit logger to allow separate log file configuration
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(AuditLogAspect.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Around("@annotation(auditLog)")
    public Object logAuditEvent(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String username = getCurrentUsername();
        String ipAddress = getClientIp();
        String operation = auditLog.operation();
        String category = auditLog.category();
        LocalDateTime timestamp = LocalDateTime.now();
        String result = "SUCCESS";
        Throwable exception = null;

        try {
            Object returnValue = joinPoint.proceed();
            return returnValue;
        } catch (Throwable e) {
            result = "FAILURE";
            exception = e;
            throw e;
        } finally {
            // Always log the audit event, regardless of success or failure
            logAudit(timestamp, username, ipAddress, category, operation, result, exception);
        }
    }

    private void logAudit(LocalDateTime timestamp, String username, String ipAddress,
                          String category, String operation, String result, Throwable exception) {
        String formattedTimestamp = timestamp.format(formatter);
        String errorDetails = exception != null ? " | Error: " + exception.getClass().getSimpleName() + " - " + exception.getMessage() : "";

        // Structured log format for easy parsing
        String auditMessage = String.format(
            "[AUDIT] %s | User: %s | IP: %s | Category: %s | Operation: %s | Result: %s%s",
            formattedTimestamp, username, ipAddress, category, operation, result, errorDetails
        );

        if ("FAILURE".equals(result)) {
            auditLogger.error(auditMessage);
        } else {
            auditLogger.info(auditMessage);
        }

        logger.debug("Audit event logged: {}", auditMessage);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
