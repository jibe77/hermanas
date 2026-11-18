import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

/**
 * Body returned by POST /api/v1/weather/test. {@code ok} reflects whether the
 * call to the OpenWeather endpoint succeeded; on failure the {@code error}
 * field carries a stable code and {@code message} a generic explanation.
 *
 * <p>There is deliberately no excerpt of the provider's answer and no exception
 * text (SSRF-VULN-01): both were readable channels for an internal URL. The
 * details stay in the server log.</p>
 */
export interface WeatherTestResult {
    ok: boolean;
    durationMs?: number;
    error?: string;
    message?: string;
}

/**
 * Optional payload — every field falls back to what is currently stored on the
 * server when missing, so a "test what's already configured" call can pass an
 * empty body.
 */
export interface WeatherTestRequest {
    url?: string;
    key?: string;
    latitude?: number;
    longitude?: number;
}

@Injectable({ providedIn: 'root' })
export class WeatherTestService extends AbstractService {
    private http = inject(HttpClient);

    test(payload: WeatherTestRequest = {}): Observable<WeatherTestResult> {
        return this.http.post<WeatherTestResult>(
            `${this.domainBase}/weather/test`,
            payload,
            { headers: this.getHeaders() }
        );
    }
}
