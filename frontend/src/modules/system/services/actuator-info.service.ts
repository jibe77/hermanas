import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

/**
 * Shape of the {@code /actuator/info} payload we expose. The keys mirror the
 * {@code info.*} properties set in {@code application.properties}; Spring
 * Boot flattens dotted keys back into nested objects when serialising.
 *
 * <p>Anything we don't read is left as {@code any} on purpose — the endpoint
 * is meant to be extensible by Spring contributors (git, build, java), and a
 * tight type would force a recompile every time we add a new {@code info.*}
 * property.</p>
 */
export interface ActuatorInfo {
    app?: {
        name?: string;
        description?: string;
        encoding?: string;
        java?: {
            source?: string;
            target?: string;
        };
    };
    'java-vendor'?: string;
    build?: {
        version?: string;
        artifact?: string;
        name?: string;
        time?: string;
        group?: string;
    };
    git?: {
        commit?: { id?: string; time?: string };
        branch?: string;
    };
    [key: string]: unknown;
}

/**
 * Shape of {@code /actuator/metrics/{name}}. Each metric exposes a list of
 * measurements (most are single-valued, but timers also report COUNT,
 * TOTAL_TIME, MAX). We only use the first VALUE measurement.
 */
export interface ActuatorMetric {
    name: string;
    baseUnit?: string;
    measurements: { statistic: string; value: number }[];
}

@Injectable({ providedIn: 'root' })
export class ActuatorInfoService {
    private http = inject(HttpClient);

    /**
     * GET /actuator/info. Public endpoint (see SecurityConfig); we still send
     * the session cookie so contributions that key off authentication state
     * keep working consistently.
     */
    get(): Observable<ActuatorInfo> {
        return this.http.get<ActuatorInfo>('/actuator/info', { withCredentials: true });
    }

    /**
     * GET /actuator/metrics/{name}. Returns the raw value of the first
     * measurement (typically the VALUE statistic). Useful for snapshot-style
     * metrics like {@code process.uptime} or {@code jvm.memory.used}. For
     * metrics with multiple measurements (timers), use {@link rawMetric}.
     *
     * <p>An optional {@code tag} param narrows the query, e.g.
     * {@code area=heap} on {@code jvm.memory.used}. See:
     * https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#actuator.metrics.endpoint-format</p>
     */
    metricValue(name: string, tag?: string): Observable<number> {
        return this.rawMetric(name, tag).pipe(
            map(m => (m.measurements?.[0]?.value ?? 0))
        );
    }

    rawMetric(name: string, tag?: string): Observable<ActuatorMetric> {
        const url = tag
            ? `/actuator/metrics/${encodeURIComponent(name)}?tag=${encodeURIComponent(tag)}`
            : `/actuator/metrics/${encodeURIComponent(name)}`;
        return this.http.get<ActuatorMetric>(url, { withCredentials: true });
    }
}
