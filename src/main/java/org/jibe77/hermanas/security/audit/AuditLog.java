package org.jibe77.hermanas.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audit logged.
 * Logs security-sensitive operations with user, IP, timestamp, and result.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /**
     * Category of the operation for grouping audit logs.
     * @return operation category
     */
    String category() default "GENERAL";

    /**
     * Description of the operation being audited.
     * @return operation description
     */
    String operation();
}
