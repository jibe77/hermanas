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
    servo_positions: ServoPositions;
    audio_toggles: AudioToggles;
    notifications: NotificationToggles;
    camera_settings: CameraSettings;
}

export interface SunOffsets {
    light_on_minutes_before_sunset: number;
    door_close_minutes_after_sunset: number;
    door_open_minutes_after_sunrise: number;
    force_at_8: boolean;
}

export interface MusicSettings {
    volume_regular_percent: number;
}

export interface ServoPositions {
    door_opening_position: number;
    door_closing_position: number;
    door_opening_duration_ms: number;
    door_closing_duration_ms: number;
}

export interface AudioToggles {
    cocorico_at_sunrise: boolean;
    song_at_sunset: boolean;
}

export interface NotificationToggles {
    email_enabled: boolean;
    weather_enabled: boolean;
}

export interface CameraSettings {
    brightness: number;
    rotation: number;
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

    setDoorOpeningPosition(position: number): Observable<string> {
        const params = new HttpParams().set('position', String(position));
        return this.http.put(`${this.domainBase}/config/door/opening-position`, null, {
            params,
            responseType: 'text',
        });
    }

    setDoorClosingPosition(position: number): Observable<string> {
        const params = new HttpParams().set('position', String(position));
        return this.http.put(`${this.domainBase}/config/door/closing-position`, null, {
            params,
            responseType: 'text',
        });
    }

    setDoorOpeningDuration(durationMs: number): Observable<string> {
        const params = new HttpParams().set('durationMs', String(durationMs));
        return this.http.put(`${this.domainBase}/config/door/opening-duration`, null, {
            params,
            responseType: 'text',
        });
    }

    setDoorClosingDuration(durationMs: number): Observable<string> {
        const params = new HttpParams().set('durationMs', String(durationMs));
        return this.http.put(`${this.domainBase}/config/door/closing-duration`, null, {
            params,
            responseType: 'text',
        });
    }

    setCocoricoAtSunrise(enabled: boolean): Observable<string> {
        const params = new HttpParams().set('enabled', String(enabled));
        return this.http.put(`${this.domainBase}/config/audio/cocorico-at-sunrise`, null, {
            params,
            responseType: 'text',
        });
    }

    setSongAtSunset(enabled: boolean): Observable<string> {
        const params = new HttpParams().set('enabled', String(enabled));
        return this.http.put(`${this.domainBase}/config/audio/song-at-sunset`, null, {
            params,
            responseType: 'text',
        });
    }

    setEmailNotifications(enabled: boolean): Observable<string> {
        const params = new HttpParams().set('enabled', String(enabled));
        return this.http.put(`${this.domainBase}/config/notifications/email`, null, {
            params,
            responseType: 'text',
        });
    }

    setWeatherInfo(enabled: boolean): Observable<string> {
        const params = new HttpParams().set('enabled', String(enabled));
        return this.http.put(`${this.domainBase}/config/notifications/weather`, null, {
            params,
            responseType: 'text',
        });
    }

    setSunriseForceAt8(force: boolean): Observable<string> {
        const params = new HttpParams().set('force', String(force));
        return this.http.put(`${this.domainBase}/config/sun/force-at-8`, null, {
            params,
            responseType: 'text',
        });
    }

    setCameraBrightness(brightness: number): Observable<string> {
        const params = new HttpParams().set('brightness', String(brightness));
        return this.http.put(`${this.domainBase}/config/camera/brightness`, null, {
            params,
            responseType: 'text',
        });
    }

    setCameraRotation(degrees: number): Observable<string> {
        const params = new HttpParams().set('degrees', String(degrees));
        return this.http.put(`${this.domainBase}/config/camera/rotation`, null, {
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
