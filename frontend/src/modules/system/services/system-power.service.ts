import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

/**
 * Triggers the system-level shutdown / reboot endpoints. Both are rate-limited
 * server-side (2 hits / 5 min) and audit-logged. The endpoints accept no body.
 */
@Injectable({ providedIn: 'root' })
export class SystemPowerService extends AbstractService {
    private http = inject(HttpClient);

    shutdown(): Observable<void> {
        return this.http.post<void>(this.domainBase + '/system/shutdown', null);
    }

    reboot(): Observable<void> {
        return this.http.post<void>(this.domainBase + '/system/reboot', null);
    }
}
