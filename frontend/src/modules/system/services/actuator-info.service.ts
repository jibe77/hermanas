import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

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
}
