import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

/**
 * Server-side wall-clock snapshot returned by {@code GET /api/v1/system/time}.
 *
 * <p>The SPA uses {@code epochMs} to compute the offset between the browser
 * and the Pi, then ticks locally every second so the displayed clock stays
 * live without hammering the endpoint. A periodic re-sync corrects any
 * drift.</p>
 */
export interface SystemTime {
    /** ISO-8601 with offset, e.g. "2026-07-13T16:52:34.123+02:00". */
    iso: string;
    /** Zone identifier, e.g. "Europe/Paris". */
    zoneId: string;
    /** Server epoch millis at the moment the response was built. */
    epochMs: number;
}

@Injectable({ providedIn: 'root' })
export class SystemTimeService extends AbstractService {
    private http = inject(HttpClient);

    public getSystemTime(): Observable<SystemTime> {
        return this.http.get<SystemTime>(this.domainBase + '/system/time', {
            headers: this.getHeaders(),
        });
    }
}
