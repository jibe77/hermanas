import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

/**
 * OS-level CPU snapshot returned by {@code GET /api/v1/system/cpu}.
 *
 * <p>{@code usedPercent} is the delta between two consecutive
 * {@code /proc/stat} reads on the server. The very first call after the
 * backend starts always returns 0 because there is no previous snapshot to
 * diff against — subsequent calls (the SPA polls every 2 s) carry real
 * numbers. {@code loadAverage1m} is null when the JVM cannot read it
 * (rare; happens on some sandboxed Linux containers).</p>
 */
export interface CpuUsage {
    readable: boolean;
    usedPercent: number;
    coreCount: number;
    loadAverage1m: number | null;
    /** Seconds since the OS booted; null on non-Linux hosts (no /proc/uptime). */
    uptimeSeconds: number | null;
}

@Injectable({ providedIn: 'root' })
export class CpuUsageService extends AbstractService {
    private http = inject(HttpClient);

    public getCpuUsage(): Observable<CpuUsage> {
        return this.http.get<CpuUsage>(this.domainBase + '/system/cpu', {
            headers: this.getHeaders(),
        });
    }
}
