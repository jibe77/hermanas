import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

/**
 * OS-level memory snapshot returned by {@code GET /api/v1/system/memory}.
 *
 * <p>RAM accounting uses {@code MemAvailable} from {@code /proc/meminfo}
 * (the kernel's own "memory available for new workloads" estimate, which
 * includes reclaimable page cache). On non-Linux hosts the file is missing
 * and the backend returns {@code readable=false} with every byte at 0 — the
 * UI should hide the panel in that case.</p>
 */
export interface MemoryUsage {
    readable: boolean;
    totalBytes: number;
    usedBytes: number;
    availableBytes: number;
    usedPercent: number;
    swapTotalBytes: number;
    swapUsedBytes: number;
    swapFreeBytes: number;
    swapUsedPercent: number;
}

@Injectable({ providedIn: 'root' })
export class MemoryUsageService extends AbstractService {
    private http = inject(HttpClient);

    public getMemoryUsage(): Observable<MemoryUsage> {
        return this.http.get<MemoryUsage>(this.domainBase + '/system/memory', {
            headers: this.getHeaders(),
        });
    }
}
