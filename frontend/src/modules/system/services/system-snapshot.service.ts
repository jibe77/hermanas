import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

import { DiskUsage } from './disk-usage.service';
import { MemoryUsage } from './memory-usage.service';
import { CpuUsage } from './cpu-usage.service';

/**
 * Stack section of the snapshot. Flat map mirroring what we used to fetch
 * via {@code /actuator/info} + {@code /actuator/metrics/*}, but produced
 * server-side in a single shot so the SPA only makes one HTTP call per
 * polling tick.
 */
export interface StackSnapshot {
    appName?: string;
    appDescription?: string;
    appEncoding?: string;
    // Build-time JVM identity (frozen at package time).
    javaSource?: string;
    javaVendor?: string;
    javaTarget?: string;
    // Runtime JVM identity (read fresh on every snapshot).
    javaRuntimeVersion?: string;
    javaRuntimeVendor?: string;
    javaRuntimeName?: string;
    hostname?: string;
    buildVersion?: string;
    buildTime?: string;
    uptimeSeconds?: number;
    jvmHeapUsed?: number;
    jvmHeapMax?: number;
    jvmThreads?: number;
    httpRequests?: number;
    processCpu?: number;
}

/**
 * Full diagnostics snapshot returned by {@code GET /api/v1/system/snapshot}.
 * One HTTP call replaces the 9 calls the System page used to do every 2 s.
 */
export interface SystemSnapshot {
    disk: DiskUsage;
    memory: MemoryUsage;
    cpu: CpuUsage;
    stack: StackSnapshot;
}

@Injectable({ providedIn: 'root' })
export class SystemSnapshotService extends AbstractService {
    private http = inject(HttpClient);

    public getSnapshot(): Observable<SystemSnapshot> {
        return this.http.get<SystemSnapshot>(this.domainBase + '/system/snapshot', {
            headers: this.getHeaders(),
        });
    }
}
