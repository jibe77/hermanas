import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

/**
 * Shape of GET /api/v1/config. Only the sub-objects we actually consume in the
 * Angular UI are typed; the rest is kept as a passthrough.
 */
export interface AllConfig {
    light_timers: Record<string, number>;
    fan_timers: Record<string, number>;
    music_timers: Record<string, number>;
    consumption_mode: {
        monthly_mapping: Record<string, string>;
        eco_mode_forced: boolean;
    };
    sun_offsets: SunOffsets;
    music_settings: MusicSettings;
}

export interface SunOffsets {
    light_on_minutes_before_sunset: number;
    door_close_minutes_after_sunset: number;
    door_open_minutes_after_sunrise: number;
}

export interface MusicSettings {
    volume_regular_percent: number;
}

@Injectable({ providedIn: 'root' })
export class ConfigService extends AbstractService {
    private http = inject(HttpClient);

    getAll(): Observable<AllConfig> {
        return this.http.get<AllConfig>(`${this.domainBase}/config`);
    }

    setLightOnBeforeSunset(minutes: number): Observable<string> {
        const params = new HttpParams().set('minutes', String(minutes));
        return this.http.put(`${this.domainBase}/config/sun/light-on-before-sunset`, null, {
            params,
            responseType: 'text',
        });
    }

    setDoorCloseAfterSunset(minutes: number): Observable<string> {
        const params = new HttpParams().set('minutes', String(minutes));
        return this.http.put(`${this.domainBase}/config/sun/door-close-after-sunset`, null, {
            params,
            responseType: 'text',
        });
    }

    setDoorOpenAfterSunrise(minutes: number): Observable<string> {
        const params = new HttpParams().set('minutes', String(minutes));
        return this.http.put(`${this.domainBase}/config/sun/door-open-after-sunrise`, null, {
            params,
            responseType: 'text',
        });
    }

    setMusicVolume(percent: number): Observable<string> {
        const params = new HttpParams().set('percent', String(percent));
        return this.http.put(`${this.domainBase}/config/music/volume`, null, {
            params,
            responseType: 'text',
        });
    }

    /** Evicts every Spring cache so the next read picks up DB changes. */
    refresh(): Observable<{ message: string; caches_cleared: number }> {
        return this.http.post<{ message: string; caches_cleared: number }>(
            `${this.domainBase}/config/refresh`,
            null
        );
    }
}
