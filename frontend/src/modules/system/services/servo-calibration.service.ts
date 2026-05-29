import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

/**
 * Wraps the manual servo-control endpoints (turnClockwise / turnCounterClockwise
 * / turnServo) used by the diagnostic page to nudge the door motor by hand.
 *
 * NOTE on the verb: these are GET endpoints in the backend (legacy contract) but
 * they DO mutate hardware state. SecurityConfig has them under the same auth
 * gate as the POST mutators, so it's fine — we just match the legacy URL.
 */
@Injectable({ providedIn: 'root' })
export class ServoCalibrationService extends AbstractService {
    private http = inject(HttpClient);

    turnClockwise(durationMs: number): Observable<string> {
        const params = new HttpParams().set('duration', String(durationMs));
        return this.http.get(`${this.domainBase}/door/turnClockwise`, {
            params,
            responseType: 'text',
        });
    }

    turnCounterClockwise(durationMs: number): Observable<string> {
        const params = new HttpParams().set('duration', String(durationMs));
        return this.http.get(`${this.domainBase}/door/turnCounterClockwise`, {
            params,
            responseType: 'text',
        });
    }
}
