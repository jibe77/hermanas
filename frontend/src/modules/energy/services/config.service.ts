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
    weather_settings: WeatherSettings;
    ai_settings: AiSettings;
    email_settings: EmailSettings;
    email_smtp: EmailSmtpSettings;
}

export interface AiSettings {
    /** URL of the local LLM endpoint used by /camera/analyze. Empty string = not configured. */
    inference_url: string;
    /** Name of the multimodal model exposed by that server. Defaults to "focus" (qwen2.5-vl). */
    inference_model: string;
    /** TTL (in milliseconds) of the server-side cache for successful analyses. 0 disables it. */
    cache_ttl_ms: number;
    /** Custom prompt sent to the model. Empty string means "use the built-in default". */
    prompt: string;
    /** Built-in default prompt — used to pre-fill the textarea when nothing is configured. */
    prompt_default: string;
    /** HTTP connect timeout (ms) when reaching the inference server. */
    connect_timeout_ms: number;
    /** HTTP read timeout (ms) — covers actual model inference time. */
    read_timeout_ms: number;
    /** Total attempts (initial + retries) on connect-phase failures. */
    retry_max_attempts: number;
    /** Initial exponential backoff (ms) between retry attempts. */
    retry_initial_backoff_ms: number;
    /** Cap on the exponential backoff (ms) between retry attempts. */
    retry_max_backoff_ms: number;
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
    weather_enabled: boolean;
}

export interface CameraSettings {
    brightness: number;
    rotation: number;
}

export interface WeatherSettings {
    url: string;
    /** true if a non-default API key is set on the server. The key itself is never sent. */
    key_set: boolean;
    /** Length of the stored key in characters (0 if unset). Used as a visual sanity check. */
    key_length: number;
    // latitude/longitude are write-only — the server does not return them
    // (chicken-coop location is sensitive). Use setLatitude()/setLongitude()
    // to push new values.
}

export interface EmailSettings {
    from: string;
}

export interface EmailSmtpSettings {
    host: string;
    port: number;
    username: string;
    /** true if a non-default password is stored. The password itself is never sent. */
    password_set: boolean;
    auth: boolean;
    starttls: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfigService extends AbstractService {
    private http = inject(HttpClient);

    getAll(): Observable<AllConfig> {
        return this.http.get<AllConfig>(`${this.domainBase}/config`);
    }

    /**
     * Fetches just the built-in default AI prompt from a public endpoint. Used
     * by the camera config panel as a fallback when {@link getAll} returns 401
     * (anonymous visitor or front-only demo mode), so the operator can still
     * see and copy the default prompt even without being authenticated.
     */
    getAiInferencePromptDefault(): Observable<{ prompt_default: string }> {
        return this.http.get<{ prompt_default: string }>(
            `${this.domainBase}/config/ai/prompt-default`
        );
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

    setWeatherUrl(url: string): Observable<string> {
        const params = new HttpParams().set('url', url);
        return this.http.put(`${this.domainBase}/config/weather/url`, null, {
            params,
            responseType: 'text',
        });
    }

    setWeatherKey(key: string): Observable<string> {
        const params = new HttpParams().set('key', key);
        return this.http.put(`${this.domainBase}/config/weather/key`, null, {
            params,
            responseType: 'text',
        });
    }

    /**
     * Sets the URL of the local LLM inference service used by /camera/analyze.
     * Empty string clears the configuration (analyze endpoint then stays in WIP / 501 mode).
     */
    setAiInferenceUrl(url: string): Observable<string> {
        const params = new HttpParams().set('url', url);
        return this.http.put(`${this.domainBase}/config/ai/inference-url`, null, {
            params,
            responseType: 'text',
        });
    }

    /** Updates the model name passed in the OpenAI-compatible chat/completions payload. */
    setAiInferenceModel(model: string): Observable<string> {
        const params = new HttpParams().set('model', model);
        return this.http.put(`${this.domainBase}/config/ai/inference-model`, null, {
            params,
            responseType: 'text',
        });
    }

    /** Updates the server-side AI vision cache TTL in milliseconds. 0 disables the cache. */
    setAiInferenceCacheTtlMs(ttlMs: number): Observable<string> {
        const params = new HttpParams().set('ttlMs', String(ttlMs));
        return this.http.put(`${this.domainBase}/config/ai/cache-ttl-ms`, null, {
            params,
            responseType: 'text',
        });
    }

    /**
     * Updates the prompt sent to the multimodal model. Empty string restores
     * the built-in default. The body is sent as raw text so the operator can
     * keep multi-line content without escaping anything client-side.
     */
    setAiInferencePrompt(prompt: string): Observable<string> {
        return this.http.put(`${this.domainBase}/config/ai/prompt`, prompt, {
            headers: { 'Content-Type': 'text/plain' },
            responseType: 'text',
        });
    }

    /** HTTP connect timeout (ms) for the inference call. Takes effect on next reboot. */
    setAiInferenceConnectTimeoutMs(ms: number): Observable<string> {
        const params = new HttpParams().set('ms', String(ms));
        return this.http.put(`${this.domainBase}/config/ai/connect-timeout-ms`, null, {
            params,
            responseType: 'text',
        });
    }

    /** HTTP read timeout (ms) for the inference call. Takes effect on next reboot. */
    setAiInferenceReadTimeoutMs(ms: number): Observable<string> {
        const params = new HttpParams().set('ms', String(ms));
        return this.http.put(`${this.domainBase}/config/ai/read-timeout-ms`, null, {
            params,
            responseType: 'text',
        });
    }

    /** Total number of attempts (initial + retries). Takes effect on next reboot. */
    setAiInferenceRetryMaxAttempts(attempts: number): Observable<string> {
        const params = new HttpParams().set('attempts', String(attempts));
        return this.http.put(`${this.domainBase}/config/ai/retry-max-attempts`, null, {
            params,
            responseType: 'text',
        });
    }

    /** Initial exponential backoff (ms) between retries. Takes effect on next reboot. */
    setAiInferenceRetryInitialBackoffMs(ms: number): Observable<string> {
        const params = new HttpParams().set('ms', String(ms));
        return this.http.put(`${this.domainBase}/config/ai/retry-initial-backoff-ms`, null, {
            params,
            responseType: 'text',
        });
    }

    /** Cap on the exponential backoff (ms) between retries. Takes effect on next reboot. */
    setAiInferenceRetryMaxBackoffMs(ms: number): Observable<string> {
        const params = new HttpParams().set('ms', String(ms));
        return this.http.put(`${this.domainBase}/config/ai/retry-max-backoff-ms`, null, {
            params,
            responseType: 'text',
        });
    }

    setLatitude(value: number): Observable<string> {
        const params = new HttpParams().set('value', String(value));
        return this.http.put(`${this.domainBase}/config/location/latitude`, null, {
            params,
            responseType: 'text',
        });
    }

    setLongitude(value: number): Observable<string> {
        const params = new HttpParams().set('value', String(value));
        return this.http.put(`${this.domainBase}/config/location/longitude`, null, {
            params,
            responseType: 'text',
        });
    }

    setEmailFrom(email: string): Observable<string> {
        const params = new HttpParams().set('email', email);
        return this.http.put(`${this.domainBase}/config/email/from`, null, {
            params,
            responseType: 'text',
        });
    }

    setMailHost(host: string): Observable<string> {
        const params = new HttpParams().set('host', host);
        return this.http.put(`${this.domainBase}/config/mail/host`, null, {
            params,
            responseType: 'text',
        });
    }

    setMailPort(port: number): Observable<string> {
        const params = new HttpParams().set('port', String(port));
        return this.http.put(`${this.domainBase}/config/mail/port`, null, {
            params,
            responseType: 'text',
        });
    }

    setMailUsername(username: string): Observable<string> {
        const params = new HttpParams().set('username', username);
        return this.http.put(`${this.domainBase}/config/mail/username`, null, {
            params,
            responseType: 'text',
        });
    }

    setMailPassword(password: string): Observable<string> {
        const params = new HttpParams().set('password', password);
        return this.http.put(`${this.domainBase}/config/mail/password`, null, {
            params,
            responseType: 'text',
        });
    }

    setMailAuth(enabled: boolean): Observable<string> {
        const params = new HttpParams().set('enabled', String(enabled));
        return this.http.put(`${this.domainBase}/config/mail/auth`, null, {
            params,
            responseType: 'text',
        });
    }

    setMailStartTls(enabled: boolean): Observable<string> {
        const params = new HttpParams().set('enabled', String(enabled));
        return this.http.put(`${this.domainBase}/config/mail/starttls`, null, {
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
