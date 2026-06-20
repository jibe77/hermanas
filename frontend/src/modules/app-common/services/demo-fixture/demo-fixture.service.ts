import { Injectable } from '@angular/core';

/**
 * Local mirror of HermanasUser used to keep the demo fixtures' user-shaped
 * helpers self-typed without dragging the notification module's import here.
 * Kept in sync manually — diverging fields will surface immediately in the
 * Users admin panel.
 */
interface HermanasUserShape {
    login: string;
    email: string | null;
    role: string;
    notificationsEnabled: boolean;
    language: 'fr' | 'en' | 'ro';
}

/**
 * Static fixtures returned by the demo-mode HTTP interceptor when the real
 * backend refuses a protected GET. The data is intentionally plausible — same
 * shapes as real responses — so the UI behaves identically to a logged-in
 * admin session during showcases. Updating one of these fixtures is the right
 * move when a new screen / column / endpoint is added to the app.
 *
 * Lookups are by *path-suffix match* (URL path ending with the key), so the
 * shape works whether the SPA points to /api/v1/... locally or to a fully
 * qualified hostname in production.
 */
@Injectable({ providedIn: 'root' })
export class DemoFixtureService {
    /**
     * Returns the fixture body for a GET URL, or {@code undefined} if no
     * fixture matches — caller falls back to the real (failed) response.
     */
    matchGet(url: string): unknown | undefined {
        const path = this.normalize(url);

        // ── Auth ──────────────────────────────────────────────────────────────
        if (path.endsWith('/auth/me')) {
            return { authenticated: true, username: 'demo', roles: ['ADMIN'] };
        }

        // ── Electronics GPIO wiring (admin) ──────────────────────────────────
        if (path.endsWith('/electronics/gpio')) {
            return this.syntheticGpioPins();
        }

        // ── Buttons live status (admin) ──────────────────────────────────────
        if (path.endsWith('/buttons/status')) {
            const now = Date.now();
            return [
                { button: 'UP', pressed: false, timestamp: now },
                { button: 'BOTTOM', pressed: true, timestamp: now },
                { button: 'BIRDHOUSE', pressed: true, timestamp: now },
            ];
        }

        // ── Sensor info ──────────────────────────────────────────────────────
        if (path.endsWith('/sensor/info')) {
            return {
                temperature: 18.4,
                humidity: 62.1,
                dateTime: new Date().toISOString(),
                externalTemperature: 14.7,
                externalHumidity: 71.0,
            };
        }
        // Sensor history endpoints — return a 24h synthetic curve so the
        // chart isn't blank during showcases.
        if (path.includes('/sensor/history')) {
            return this.syntheticSensorHistory();
        }

        // ── Door & accessory statuses ────────────────────────────────────────
        if (path.endsWith('/door/status')) {
            return {
                status: 'CLOSED',
                timeStatusHasChanged: new Date(Date.now() - 3600 * 1000).toISOString(),
            };
        }
        if (path.endsWith('/light/status')) {
            return { statusEnum: 'OFF', timeOut: 0 };
        }
        if (path.endsWith('/fan/status')) {
            return { statusEnum: 'OFF', timeOut: 0 };
        }
        if (path.endsWith('/music/status')) {
            return { statusEnum: 'OFF', timeOut: 0 };
        }
        if (path.endsWith('/door/closingRate')) {
            return { rate: 0.97 };
        }
        if (path.endsWith('/camera/closingRate')) {
            return { rate: 0.97 };
        }

        // ── Scheduler ────────────────────────────────────────────────────────
        if (path.endsWith('/scheduler/doorOpeningTime')) {
            return { time: new Date().toISOString() };
        }
        if (path.endsWith('/scheduler/doorClosingTime')) {
            return { time: new Date().toISOString() };
        }
        if (path.endsWith('/scheduler/lightOnTime')) {
            return { time: new Date().toISOString() };
        }
        if (path.endsWith('/scheduler/nextEvents')) {
            return this.syntheticNextEvents();
        }

        // ── Energy ───────────────────────────────────────────────────────────
        if (path.endsWith('/energy/currentMode')) {
            return { mode: 'REGULAR' };
        }
        if (path.endsWith('/energy/currentConfigMode')) {
            return { mode: 'REGULAR', forced: false };
        }
        if (path.endsWith('/energy/wifi/wifiCardIsEnabled')) {
            return { enabled: true };
        }

        // ── Photo archive (admin/user only) ──────────────────────────────────
        // Browsable fake archive backed by ARCHIVE_TREE. Mirrors the directory
        // layout of the project's /photos/ sample tree (year/month/day) so the
        // demo behaves like a real installation that has been running for years.
        // The actual <img src> URLs for individual photos and the live snapshot
        // are handled by PhotosService directly — see PhotosService.fileUrl()
        // and PhotosService.snapshotUrl() — because Angular HTTP interceptors
        // do NOT see <img> requests (they bypass HttpClient).
        if (path.includes('/camera/photos')) {
            const sub = this.extractPathParam(url);
            return this.buildArchiveListing(sub);
        }

        // ── Logs / events (admin) ────────────────────────────────────────────
        if (path.includes('/logs')) {
            return this.syntheticLogs(path);
        }
        if (path.includes('/events/auth')) {
            return this.syntheticAuthEvents();
        }

        // ── Users (admin) ────────────────────────────────────────────────────
        if (path.endsWith('/users/me')) {
            return this.syntheticMe();
        }
        if (path.endsWith('/users') || path.endsWith('/users/')) {
            return this.syntheticUsers();
        }

        // ── Full configuration bundle (admin) ────────────────────────────────
        // GET /config is the one-shot read used by several admin panels (Users
        // page email/SMTP block, Music volume, Camera settings, etc.). The
        // fixture mirrors the AllConfig contract so every screen unfolds with
        // plausible values in demo mode.
        if (path.endsWith('/config')) {
            return this.syntheticAllConfig();
        }

        // ── Email diagnostics (admin) ────────────────────────────────────────
        if (path.includes('/email/test')) {
            return { ok: true, message: 'Demo mode — no real email sent.' };
        }

        // ── Actuator (admin) ─────────────────────────────────────────────────
        if (path.endsWith('/actuator/metrics')) {
            return { names: ['hermanas.door.opened', 'hermanas.door.closed'] };
        }
        // AI snapshot analysis intentionally has no fixture: in demo mode
        // the Webcam page should behave like an unauthenticated visitor —
        // no fake AI report, no fake live photo. The yellow "demo blocked"
        // toast surfaces when the visitor hits the live capture button.
        if (path.endsWith('/actuator/health')) {
            return {
                status: 'UP',
                components: {
                    db: { status: 'UP', details: { database: 'MariaDB (demo)' } },
                    weatherApi: { status: 'UP' },
                    diskSpace: { status: 'UP' },
                    ping: { status: 'UP' },
                },
            };
        }

        // ── System (admin) ───────────────────────────────────────────────────
        if (path.endsWith('/system/disk-usage')) {
            return {
                readable: true,
                usedPercent: 42,
                totalBytes: 64 * 1024 ** 3,
                usedBytes: 27 * 1024 ** 3,
                availableBytes: 37 * 1024 ** 3,
                path: '/ (demo)',
            };
        }
        if (path.endsWith('/system/cpu')) {
            return {
                readable: true,
                usedPercent: 18,
                coreCount: 1,
                loadAverage1m: 0.42,
                uptimeSeconds: 86400 * 6 + 3 * 3600,
            };
        }
        if (path.endsWith('/system/memory')) {
            return {
                readable: true,
                totalBytes: 512 * 1024 ** 2,
                usedBytes: 218 * 1024 ** 2,
                availableBytes: 294 * 1024 ** 2,
                usedPercent: 42,
                swapTotalBytes: 1024 ** 3,
                swapUsedBytes: 64 * 1024 ** 2,
                swapFreeBytes: 960 * 1024 ** 2,
                swapUsedPercent: 6,
            };
        }
        if (path.endsWith('/system/snapshot')) {
            return this.syntheticSystemSnapshot();
        }
        if (path.endsWith('/actuator/info')) {
            return {
                build: {
                    version: '0.8.2-demo',
                    time: new Date().toISOString(),
                    artifact: 'hermanas',
                    name: 'Hermanas',
                    group: 'org.jibe77',
                },
                app: { name: 'Hermanas', description: 'Chicken coop automation' },
                java: {
                    source: '11',
                    target: '11',
                    runtime: { name: 'OpenJDK Runtime Environment', version: '11.0.21' },
                    vendor: 'Eclipse Adoptium',
                },
            };
        }
        if (path.includes('/actuator/metrics/')) {
            return this.syntheticActuatorMetric(path);
        }

        // ── Info (public but included so demos work offline) ─────────────────
        if (path.endsWith('/info')) {
            return {
                build: { version: '0.8.2-demo', time: new Date().toISOString() },
                app: { name: 'Hermanas', description: 'Chicken coop automation' },
            };
        }

        return undefined;
    }

    /**
     * Returns a synthetic body for a mutating call (POST/PUT/DELETE/PATCH).
     * Most endpoints return plain text on success ("OK" or the new value);
     * a small number return JSON. We default to a 200 with an empty body —
     * the UI shows the warning toast already, so the body is largely cosmetic.
     */
    matchMutation(url: string, requestBody?: unknown): unknown {
        const path = this.normalize(url);
        if (path.endsWith('/config/refresh')) {
            return { message: 'Demo — caches not actually cleared', caches_cleared: 0 };
        }
        if (path.endsWith('/weather/test')) {
            return {
                ok: true,
                durationMs: 142,
                snippet: '{"main":{"temp":18.4,"humidity":62},…} (demo)',
            };
        }
        // Music actions surfaced as state-changing GETs by demoMode.interceptor —
        // the callers ignore the body itself but still emit a success toast, so
        // we hand back a shape compatible with the real SwitchStatus contract.
        if (path.endsWith('/music/switch')) {
            return { statusEnum: 'ON', timeOut: 0 };
        }
        if (path.endsWith('/music/cocorico')) {
            return true;
        }
        if (path.endsWith('/music/selected-playlist')) {
            return { playlist: this.extractPlaylistFromUrl(url) ?? 'Pop' };
        }
        // ── Users mutations: the Users admin panel expects a HermanasUser
        // back so it can splice the response into its local table. We echo
        // the request body's fields so the success toast and the row that
        // gets re-rendered carry the actual login/email/role the operator
        // typed in.
        const payload = (requestBody ?? {}) as Partial<HermanasUserShape>;
        if (/\/users\/me$/.test(path)) {
            const me = this.syntheticMe() as HermanasUserShape;
            return this.mergeUser(me, payload);
        }
        if (/\/users\/[^/]+$/.test(path)) {
            const login = decodeURIComponent(path.substring(path.lastIndexOf('/') + 1));
            return this.mergeUser(
                {
                    login,
                    email: `${login}@demo.hermanas.fr`,
                    role: 'USER',
                    notificationsEnabled: true,
                    language: 'fr',
                },
                payload
            );
        }
        if (path.endsWith('/users')) {
            return this.mergeUser(
                {
                    login: 'newcomer',
                    email: 'newcomer@demo.hermanas.fr',
                    role: 'USER',
                    notificationsEnabled: false,
                    language: 'fr',
                },
                payload
            );
        }
        // Email diagnostics — the Users page "Send a test email" button reads
        // {message} from the body to surface in its success toast.
        if (path.endsWith('/email/test')) {
            return { ok: true, message: 'Demo mode — no real email sent.' };
        }
        if (path.endsWith('/auth/login')) {
            // We don't fake real authentication. Let the real backend answer.
            return undefined;
        }
        return '';
    }

    /**
     * Overlays the fields the operator typed in on top of the canned defaults
     * so the success toast (Utilisateur "X" créé) and the table row that gets
     * spliced back into the SPA carry the real input rather than the fixture's
     * placeholder values.
     */
    private mergeUser(base: HermanasUserShape, payload: Partial<HermanasUserShape>): HermanasUserShape {
        return {
            login: payload.login ?? base.login,
            email: payload.email ?? base.email,
            role: payload.role ?? base.role,
            notificationsEnabled:
                typeof payload.notificationsEnabled === 'boolean'
                    ? payload.notificationsEnabled
                    : base.notificationsEnabled,
            language: payload.language ?? base.language,
        };
    }

    /**
     * Plucks the playlist name from a music URL when present in the query
     * string (the play-shortcut sends ?playlist=Pop). Used to keep the demo
     * confirmation toast meaningful — defaults to a plausible value upstream.
     */
    private extractPlaylistFromUrl(url: string): string | undefined {
        const q = url.indexOf('?');
        if (q < 0) {
            return undefined;
        }
        const params = new URLSearchParams(url.substring(q + 1));
        return params.get('playlist') ?? undefined;
    }

    /** Strip query string / trailing slash so suffix-matching stays robust. */
    private normalize(url: string): string {
        const q = url.indexOf('?');
        let p = q >= 0 ? url.substring(0, q) : url;
        if (p.length > 1 && p.endsWith('/')) {
            p = p.substring(0, p.length - 1);
        }
        return p;
    }

    private syntheticSensorHistory(): unknown {
        const now = Date.now();
        const points: Array<{
            dateTime: string;
            temperature: number;
            humidity: number;
            externalTemperature: number;
            externalHumidity: number;
        }> = [];
        for (let h = 23; h >= 0; h--) {
            const t = new Date(now - h * 3600 * 1000).toISOString();
            const wave = Math.sin((h / 24) * Math.PI * 2);
            points.push({
                dateTime: t,
                temperature: 17 + wave * 3,
                humidity: 60 + wave * 8,
                externalTemperature: 12 + wave * 5,
                externalHumidity: 75 - wave * 10,
            });
        }
        return points;
    }

    private syntheticNextEvents(): unknown {
        const now = Date.now();
        return [
            {
                eventType: 'DOOR_OPENING',
                timestamp: new Date(now + 6 * 3600 * 1000).toISOString(),
            },
            {
                eventType: 'DOOR_CLOSING',
                timestamp: new Date(now + 14 * 3600 * 1000).toISOString(),
            },
            {
                eventType: 'LIGHT_ON',
                timestamp: new Date(now + 13.5 * 3600 * 1000).toISOString(),
            },
        ];
    }

    private syntheticLogs(path: string): unknown {
        // `GET /logs` lists the available log files. We also accept
        // /logs/files for compatibility with older callers.
        if (path.endsWith('/logs') || path.endsWith('/logs/files')) {
            return [
                { name: 'spring.log', size: 81920, modified: new Date().toISOString() },
                {
                    name: 'spring.log.2026-06-01.0.gz',
                    size: 12288,
                    modified: new Date(Date.now() - 86400 * 1000).toISOString(),
                },
                {
                    name: 'spring.log.2026-05-31.0.gz',
                    size: 14336,
                    modified: new Date(Date.now() - 2 * 86400 * 1000).toISOString(),
                },
            ];
        }
        // `GET /logs/{filename}` returns the tail of the named file as a
        // plain string array. Synthetic lines cover the typical mix of
        // INFO/WARN/ERROR so the level filter has something to grade.
        const now = Date.now();
        const ts = (offsetMin: number) =>
            new Date(now - offsetMin * 60_000)
                .toISOString()
                .replace('T', ' ')
                .substring(0, 23);
        return [
            `${ts(15)}  INFO 1234 --- [main] o.j.h.HermanasApplication       : Demo mode — synthetic log line`,
            `${ts(14)}  INFO 1234 --- [scheduler-1] o.j.h.scheduler.job.PeriodicJob : Sensor scheduled job is taking temperature and humidity now.`,
            `${ts(14)}  INFO 1234 --- [scheduler-1] o.j.h.service.sensor.SensorService : temperature 18.4 and humidity 62.1`,
            `${ts(12)}  INFO 1234 --- [http-nio-8080-exec-3] o.j.h.security.audit.AuditLogger : user=demo ip=127.0.0.1 op=DOOR_STATUS result=OK`,
            `${ts(10)}  WARN 1234 --- [main] o.j.h.client.weather.WeatherClient : OpenWeather replied 502 — falling back to cached value.`,
            `${ts(8)}  INFO 1234 --- [scheduler-2] o.j.h.service.door.DoorService     : Door opened at sunrise + 0 min.`,
            `${ts(5)} ERROR 1234 --- [http-nio-8080-exec-5] o.j.h.client.ai.AiVisionClient : POST http://alyssa:8080/v1/chat/completions failed: read timeout (will not retry).`,
            `${ts(3)}  INFO 1234 --- [main] o.j.h.service.camera.CameraService : Picture cache hit (highQuality=true, age=4831 ms).`,
            `${ts(1)}  INFO 1234 --- [scheduler-1] o.j.h.scheduler.job.SunRelatedJob : Sun checks done — next door close in 6h12m.`,
        ];
    }

    /**
     * GPIO wiring shown on the Electronics page. Pin numbers mirror the
     * production application.properties so the demo board diagram lines up
     * with what a real Hermanas user would see on their own hardware.
     */
    private syntheticGpioPins(): unknown {
        return [
            {
                key: 'doorServo', label: 'Door servomotor', labelFr: 'Servomoteur de la porte',
                direction: 'OUTPUT', kind: 'servo', pin: 25, boardPin: '22',
            },
            {
                key: 'upperEndStop', label: 'Upper end-stop button',
                labelFr: 'Bouton de fin de course haut', direction: 'INPUT',
                kind: 'button', pin: 15, boardPin: '10',
            },
            {
                key: 'lowerEndStop', label: 'Lower end-stop button',
                labelFr: 'Bouton de fin de course bas', direction: 'INPUT',
                kind: 'button', pin: 18, boardPin: '12',
            },
            {
                key: 'light', label: 'Coop light', labelFr: 'Lumière du poulailler',
                direction: 'OUTPUT', kind: 'light', pin: 14, boardPin: '8',
            },
            {
                key: 'fan', label: 'Ventilation fan', labelFr: 'Ventilateur',
                direction: 'OUTPUT', kind: 'fan', pin: 23, boardPin: '16',
            },
            {
                key: 'sensor', label: 'Temperature & humidity sensor',
                labelFr: 'Capteur température / humidité', direction: 'INPUT',
                kind: 'sensor', pin: 4, boardPin: '7',
            },
        ];
    }

    /** GET /system/snapshot — composite payload consumed by the System page. */
    private syntheticSystemSnapshot(): unknown {
        return {
            disk: {
                readable: true,
                usedPercent: 42,
                totalBytes: 64 * 1024 ** 3,
                usedBytes: 27 * 1024 ** 3,
                availableBytes: 37 * 1024 ** 3,
                path: '/ (demo)',
            },
            memory: {
                readable: true,
                totalBytes: 512 * 1024 ** 2,
                usedBytes: 218 * 1024 ** 2,
                availableBytes: 294 * 1024 ** 2,
                usedPercent: 42,
                swapTotalBytes: 1024 ** 3,
                swapUsedBytes: 64 * 1024 ** 2,
                swapFreeBytes: 960 * 1024 ** 2,
                swapUsedPercent: 6,
            },
            cpu: {
                readable: true,
                usedPercent: 18,
                coreCount: 1,
                loadAverage1m: 0.42,
                uptimeSeconds: 86400 * 6 + 3 * 3600,
            },
            stack: {
                appName: 'Hermanas',
                appDescription: 'Chicken coop automation',
                appEncoding: 'UTF-8',
                javaSource: '11',
                javaTarget: '11',
                javaVendor: 'Eclipse Adoptium',
                javaRuntimeVersion: '11.0.21',
                javaRuntimeVendor: 'Eclipse Adoptium',
                javaRuntimeName: 'OpenJDK Runtime Environment',
                hostname: 'hermanas-demo',
                buildVersion: '0.8.2-demo',
                buildTime: new Date().toISOString(),
                uptimeSeconds: 86400 * 6 + 3 * 3600,
                jvmHeapUsed: 96 * 1024 ** 2,
                jvmHeapMax: 256 * 1024 ** 2,
                jvmThreads: 38,
                httpRequests: 14_512,
            },
        };
    }

    /**
     * GET /actuator/metrics/<name> — minimal Spring Boot Actuator shape. The
     * Hermanas System page reads `measurements[0].value` only, so a single-
     * measurement payload is enough; the actual value is derived from the
     * metric name so the demo isn't filled with identical zeros.
     */
    private syntheticActuatorMetric(path: string): unknown {
        const name = path.substring(path.lastIndexOf('/') + 1);
        let value = 0;
        if (name.includes('uptime')) value = 86400 * 6 + 3 * 3600;
        else if (name.includes('cpu')) value = 0.16;
        else if (name.includes('memory.used')) value = 96 * 1024 ** 2;
        else if (name.includes('memory.max')) value = 256 * 1024 ** 2;
        else if (name.includes('threads')) value = 38;
        else if (name.includes('http')) value = 14_512;
        else value = 1;
        return {
            name,
            measurements: [{ statistic: 'VALUE', value }],
            availableTags: [],
        };
    }

    private syntheticAuthEvents(): unknown {
        const now = Date.now();
        return [
            {
                id: 1,
                eventType: 'LOGIN_SUCCESS',
                timestamp: new Date(now - 30 * 60 * 1000).toISOString(),
                details: 'login=demo ip=127.0.0.1',
            },
            {
                id: 2,
                eventType: 'LOGIN_FAILED',
                timestamp: new Date(now - 2 * 3600 * 1000).toISOString(),
                details: 'login=alice reason=Bad credentials ip=10.0.0.42',
            },
            {
                id: 3,
                eventType: 'LOGOUT',
                timestamp: new Date(now - 5 * 3600 * 1000).toISOString(),
                details: 'login=marguerite',
            },
        ];
    }

    private syntheticUsers(): unknown {
        // Shape mirrors HermanasUser in notification/services/charts.service.ts:
        // {login, email, role (singular), notificationsEnabled, language}. The
        // older { roles: [...], pendingValidation } shape predated the Users
        // admin panel and did not render correctly in the table.
        // Mix of roles, languages and notification preferences so the table
        // exercises every badge, every locale column and both notification
        // states — the empty-email + Pending row at the end keeps the
        // "approve / placeholder" rendering paths visible.
        return [
            {
                login: 'marguerite',
                email: 'marguerite@demo.hermanas.fr',
                role: 'ADMIN',
                notificationsEnabled: true,
                language: 'fr',
            },
            {
                login: 'henriette',
                email: 'henriette@demo.hermanas.fr',
                role: 'USER',
                notificationsEnabled: true,
                language: 'en',
            },
            {
                login: 'ileana',
                email: 'ileana@demo.hermanas.fr',
                role: 'USER',
                notificationsEnabled: false,
                language: 'ro',
            },
            {
                login: 'colette',
                email: 'colette@demo.hermanas.fr',
                role: 'USER',
                notificationsEnabled: true,
                language: 'fr',
            },
            {
                login: 'josephine',
                email: 'josephine@demo.hermanas.fr',
                role: 'ADMIN',
                notificationsEnabled: false,
                language: 'en',
            },
            {
                login: 'gertrude',
                email: null,
                role: 'NOT_VALIDATED_YET',
                notificationsEnabled: false,
                language: 'fr',
            },
        ];
    }

    /**
     * Synthetic AI settings shown in the Webcam configuration panel. The
     * inference URL host, port and model name are randomised once per service
     * instance — keeps the value plausible and avoids any accidental hint at
     * the real production hostname. Timeouts/retry mirror the production
     * defaults so the operator sees the actual tunable surface. The
     * {@code prompt_default} matches the Java {@code CameraPromptBuilder
     * #DEFAULT_PROMPT} verbatim so the textarea pre-fills with the real
     * built-in prompt, not a placeholder.
     */
    private cachedAiSettings: unknown | null = null;
    private syntheticAiSettings(): unknown {
        if (this.cachedAiSettings) {
            return this.cachedAiSettings;
        }
        const hosts = ['alyssa', 'helga', 'kelda', 'astrid', 'birgit', 'sigrid'];
        const models = ['focus', 'vision-pro', 'qwen-vl', 'multimodal-v2', 'farm-watch'];
        const ports = [8080, 11434, 5050, 7860, 9090];
        const host = hosts[Math.floor(Math.random() * hosts.length)];
        const port = ports[Math.floor(Math.random() * ports.length)];
        const model = models[Math.floor(Math.random() * models.length)];
        this.cachedAiSettings = {
            inference_url: `http://${host}:${port}/v1`,
            inference_model: model,
            cache_ttl_ms: 120000,
            prompt: '',
            prompt_default: DEMO_DEFAULT_AI_PROMPT,
            connect_timeout_ms: 15000,
            read_timeout_ms: 180000,
            retry_max_attempts: 3,
            retry_initial_backoff_ms: 2000,
            retry_max_backoff_ms: 10000,
        };
        return this.cachedAiSettings;
    }

    /** GET /users/me — synthetic profile for the demo "admin" visitor. */
    private syntheticMe(): unknown {
        return {
            login: 'demo',
            email: 'demo@hermanas.fr',
            role: 'ADMIN',
            notificationsEnabled: true,
            language: 'fr',
        };
    }

    /**
     * GET /config — full bundle consumed by every admin screen. Values are
     * plausible defaults that mirror the production shape so the UI does not
     * have to handle missing sub-objects defensively. Sensitive bits
     * (password, API key) are surfaced as "*_set" booleans only, exactly like
     * the real backend.
     */
    private syntheticAllConfig(): unknown {
        return {
            light_timers: { regular: 600000, eco: 180000 },
            fan_timers: { regular: 600000, eco: 180000 },
            music_timers: { regular: 600000, eco: 180000 },
            consumption_mode: {
                monthly_mapping: {
                    JANUARY: 'ECO',
                    FEBRUARY: 'ECO',
                    MARCH: 'REGULAR',
                    APRIL: 'REGULAR',
                    MAY: 'REGULAR',
                    JUNE: 'REGULAR',
                    JULY: 'REGULAR',
                    AUGUST: 'REGULAR',
                    SEPTEMBER: 'REGULAR',
                    OCTOBER: 'REGULAR',
                    NOVEMBER: 'ECO',
                    DECEMBER: 'ECO',
                },
                eco_mode_forced: false,
            },
            sun_offsets: {
                light_on_minutes_before_sunset: 20,
                door_close_minutes_after_sunset: 30,
                door_open_minutes_after_sunrise: 0,
                force_at_8: true,
            },
            music_settings: { volume_regular_percent: 78 },
            servo_positions: {
                door_opening_position: 16,
                door_closing_position: 5,
                door_opening_duration_ms: 10000,
                door_closing_duration_ms: 2350,
            },
            audio_toggles: { cocorico_at_sunrise: true, song_at_sunset: false },
            notifications: { weather_enabled: true },
            camera_settings: { brightness: 60, rotation: 180, regular_quality: 45, high_quality: 80 },
            weather_settings: {
                // Mirrors the shape of the real OpenWeatherMap template so the
                // demo visitor sees a realistic URL (with the placeholder tokens
                // the backend substitutes at call time) instead of a generic
                // example.com domain.
                url: 'https://api.weather.demo/data/2.5/weather?lat={latitude}&lon={longitude}&units=metric&appid={key}',
                key_set: true,
                key_length: 32,
            },
            ai_settings: this.syntheticAiSettings(),
            // Sender + SMTP block mirrors a typical Pi deployment (Gmail relay
            // on 587 + STARTTLS) so the demo visitor sees realistic values
            // instead of obvious "demo.fr" placeholders. The password is never
            // returned by the real backend either — only the password_set flag,
            // which surfaces as the green "Set" badge in the UI.
            email_settings: { from: 'hermanas.demo@gmail.com' },
            email_smtp: {
                host: 'smtp.gmail.com',
                port: 587,
                username: 'hermanas.demo@gmail.com',
                password_set: true,
                auth: true,
                starttls: true,
            },
        };
    }

    // ── Camera archive ────────────────────────────────────────────────────────

    /**
     * Synthetic year/month/day archive layout. Mirrors the project's /photos/
     * sample tree so the demo browser feels like a long-running installation.
     * Days listed here generate four pretend photos at 06:00 / 12:00 / 16:00 /
     * 20:00 each — enough to populate the grid without bloating the bundle.
     */
    private static readonly ARCHIVE_TREE: Record<string, string[]> = {
        '': ['2020', '2021'],
        '2020': ['7', '8', '9', '10', '12'],
        '2020/7': ['5', '6', '7', '9', '11'],
        '2020/8': ['9', '28', '31'],
        '2020/9': ['1', '2', '3'],
        '2020/10': ['17', '27'],
        '2020/12': ['6', '11', '13'],
        '2021': ['2', '4', '6', '11'],
        '2021/2': ['21', '22', '23', '26'],
        '2021/4': ['17'],
        '2021/6': ['21', '22'],
        '2021/11': ['3'],
    };

    /** Times of day used to populate a leaf "day" directory with pretend photos. */
    private static readonly LEAF_HOURS = [6, 10, 14, 18];

    private buildArchiveListing(subPath: string): unknown {
        const normalized = subPath.replace(/^\/+|\/+$/g, '');
        const children = DemoFixtureService.ARCHIVE_TREE[normalized];

        if (children) {
            // Intermediate directory: only sub-directories.
            return {
                path: normalized,
                directories: children.map(name => ({
                    name,
                    type: 'DIRECTORY',
                    size: 0,
                    lastModified: Date.now(),
                })),
                files: [],
            };
        }

        // Leaf "day" — generate a handful of plausible-looking JPEG entries.
        const segments = normalized.split('/').filter(Boolean);
        const isDayLeaf = segments.length === 3 && segments.every(s => /^\d+$/.test(s));
        if (!isDayLeaf) {
            return { path: normalized, directories: [], files: [] };
        }
        const [year, month, day] = segments.map(s => parseInt(s, 10));
        const files = DemoFixtureService.LEAF_HOURS.map(h => {
            const ts = new Date(year, month - 1, day, h, 0, 0).getTime();
            const hh = h.toString().padStart(2, '0');
            return {
                name: `IMG_${year}${month.toString().padStart(2, '0')}${day
                    .toString()
                    .padStart(2, '0')}_${hh}0000.jpg`,
                type: 'FILE' as const,
                size: 540_000 + ((h * 1213) % 60_000),
                lastModified: ts,
            };
        });
        return { path: normalized, directories: [], files };
    }

    /** Extracts the `path=` query parameter from a /camera/photos GET URL. */
    private extractPathParam(url: string): string {
        const q = url.indexOf('?');
        if (q < 0) {
            return '';
        }
        const params = new URLSearchParams(url.substring(q + 1));
        return params.get('path') ?? '';
    }
}

/**
 * Mirror of CameraPromptBuilder.DEFAULT_PROMPT (Java side). Kept verbatim so
 * the demo Webcam config panel pre-fills the textarea with the actual
 * production default, not a placeholder. Hand-synced — drift will only
 * affect what the demo visitor sees, never real inference behaviour.
 */
const DEMO_DEFAULT_AI_PROMPT =
    "Can you analyze this pictures from the inside of my chicken coop ? Be very" +
    " straightforward in your answers, and don't reply like it's questions, make real" +
    " answer because the person who will read this text can't see the prompt.\n" +
    "*   how many chicken can you see ? (watch out, some chicken are black so they can be" +
    " difficult to notice, if you see a tiny glimpse of another chicken then take it in" +
    " account, if this is just a silhouette or a shadow it's a false positive so skip" +
    " it) However, be very strict: if a black shape looks like a shadow, a dark piece of" +
    " wood, or a pile of feathers without a distinct head or eye, do not count it. If" +
    " you are not 100% sure it is a living bird, assume it is a shadow.\n" +
    "*   how many eggs can you see on the floor ? (watch out, an egg is different from a" +
    " poop, even if they are both round oval, a poop is dark and sometimes white, but an" +
    " egg is cuckoo maran. If you see a round dark shape in the shadows of the nesting" +
    " box, verify if it has the smooth, glossy texture of an egg before counting it. If" +
    " it's blue, it's not an egg. Verify it's not a shadow) (note : if you see something" +
    " blue/green it's probably a glove, skip it)\n" +
    "*   is they enough hay on the ground  ?\n" +
    "*   is the door on the lower left corner opened or closed ? (when the door is" +
    " closed, it has a wooden color, when it's opened you can see the outside) (Note: If" +
    " it is night time, an opened door will appear as a dark void or shadow similar to" +
    " the outside, whereas a closed door will show the visible wooden texture/panel of" +
    " the door itself).\n" +
    "*   is there poop / dirt on the floor ? (grade from 1 to 5)\n" +
    "*   there is a fan on the upper right corner, is it dusty ?";
