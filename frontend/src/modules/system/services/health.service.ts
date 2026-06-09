import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Subset of /actuator/health we actually care about in the System smoke-test
 * panel. The endpoint exposes a {@code status} field plus a {@code components}
 * map; we only read two component names: {@code db} (Spring's
 * DataSourceHealthIndicator, bundled with Spring Boot's actuator JDBC
 * autoconfig) and {@code weatherApi} (our resilience4j circuit breaker).
 *
 * When the visitor is admin, Spring exposes the per-component details (see
 * {@code management.endpoint.health.show-details=when-authorized} in
 * application.properties); anonymous users only get the global status, in
 * which case {@code components} is undefined.
 */
export interface HealthStatus {
    status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN' | string;
}

export interface HealthResponse extends HealthStatus {
    components?: Record<string, HealthStatus>;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
    private http = inject(HttpClient);

    /**
     * GET /actuator/health. The endpoint is not under /api/v1 — it sits at the
     * application root — so we don't go through {@code AbstractService}.
     */
    get(): Observable<HealthResponse> {
        return this.http.get<HealthResponse>('/actuator/health', { withCredentials: true });
    }
}
