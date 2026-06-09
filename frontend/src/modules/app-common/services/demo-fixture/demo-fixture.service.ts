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
        // AI snapshot analysis — plausible chicken-coop report so the recruiter
        // sees the feature working end-to-end without needing an actual LLM behind.
        if (path.endsWith('/camera/analyze')) {
            const q = url.indexOf('?');
            const params = q >= 0 ? new URLSearchParams(url.substring(q + 1)) : null;
            const lang = (params?.get('lang') ?? 'en') as 'fr' | 'en' | 'ro';
            return {
                status: 'ok',
                lang,
                message: this.syntheticAnalysisText(lang),
            };
        }
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
            return { usedPercent: 42, path: '/ (demo)' };
        }

        // ── Info (public but included so demos work offline) ─────────────────
        if (path.endsWith('/info')) {
            return {
                build: { version: '0.8.1-demo', time: new Date().toISOString() },
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
        if (path.endsWith('/logs/files')) {
            return [
                { name: 'spring.log', size: 81920, modified: new Date().toISOString() },
                {
                    name: 'spring.log.2026-06-01.0.gz',
                    size: 12288,
                    modified: new Date(Date.now() - 86400 * 1000).toISOString(),
                },
            ];
        }
        // Tail content
        return [
            '2026-06-02 08:42:11.243  INFO 1234 --- [main] o.j.h.HermanasApplication       : Demo mode — synthetic log line',
            '2026-06-02 08:42:11.245  INFO 1234 --- [main] o.j.h.scheduler.job.PeriodicJob : Sensor scheduled job is taking temperature and humidity now.',
            '2026-06-02 08:42:14.901  INFO 1234 --- [main] o.j.h.service.sensor.SensorService : temperature 18.4 and humidity 62.1',
            '2026-06-02 08:43:05.012  INFO 1234 --- [main] o.j.h.service.door.DoorService     : Door opened at sunrise + 0 min.',
        ];
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

    /**
     * Public accessor so the Webcam page can short-circuit the async capture
     * pipeline in demo mode (no real POST, no polling) and still display the
     * same synthetic analysis users get via the legacy /camera/analyze fixture.
     */
    public buildAnalysisText(lang: 'fr' | 'en' | 'ro'): string {
        return this.syntheticAnalysisText(lang);
    }

    private syntheticAnalysisText(lang: 'fr' | 'en' | 'ro'): string {
        if (lang === 'fr') {
            return (
                'Analyse du poulailler :\n' +
                '*   Poules : 4 visibles (3 rousses + 1 noire dans le fond).\n' +
                '*   Œufs : 2 œufs visibles dans le pondoir.\n' +
                "*   Foin : niveau correct, l'épaisseur est suffisante.\n" +
                '*   Porte (coin bas gauche) : fermée (texture bois visible).\n' +
                '*   Saleté au sol : 2/5, légère.\n' +
                "*   Ventilateur (coin haut droit) : un peu de poussière, à dépoussiérer.\n\n" +
                '(Réponse factice — mode démo)'
            );
        }
        if (lang === 'ro') {
            return (
                'Analiza coteței:\n' +
                '*   Găini: 4 vizibile (3 roșcate + 1 neagră în fundal).\n' +
                '*   Ouă: 2 ouă vizibile în cuibar.\n' +
                '*   Fân: nivel corect, suficient pe sol.\n' +
                '*   Ușa (colțul stânga-jos): închisă (textura lemnului vizibilă).\n' +
                '*   Murdărie pe sol: 2/5, ușoară.\n' +
                '*   Ventilator (colțul dreapta-sus): puțin praf, ar trebui curățat.\n\n' +
                '(Răspuns fictiv — mod demo)'
            );
        }
        return (
            'Chicken coop analysis:\n' +
            '*   Chickens: 4 visible (3 brown + 1 black in the back).\n' +
            '*   Eggs: 2 eggs visible in the nesting box.\n' +
            '*   Hay: level looks correct, enough on the floor.\n' +
            '*   Door (lower left corner): closed (wooden texture visible).\n' +
            '*   Dirt on the floor: 2/5, light.\n' +
            '*   Fan (upper right corner): a bit dusty, should be cleaned.\n\n' +
            '(Synthetic answer — demo mode)'
        );
    }

    private syntheticUsers(): unknown {
        // Shape mirrors HermanasUser in notification/services/charts.service.ts:
        // {login, email, role (singular), notificationsEnabled, language}. The
        // older { roles: [...], pendingValidation } shape predated the Users
        // admin panel and did not render correctly in the table.
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
                login: 'gertrude',
                email: null,
                role: 'NOT_VALIDATED_YET',
                notificationsEnabled: false,
                language: 'fr',
            },
        ];
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
            camera_settings: { brightness: 60, rotation: 180 },
            weather_settings: { url: 'https://api.openweathermap.org', key_set: true, key_length: 32 },
            ai_settings: {
                inference_url: 'http://localhost:11434',
                inference_model: 'focus',
                cache_ttl_ms: 120000,
                prompt: '',
                prompt_default: 'Describe the chicken coop: hens, eggs, hay, door state, dirt.',
            },
            email_settings: { from: 'hermanas@demo.fr' },
            email_smtp: {
                host: 'smtp.demo.fr',
                port: 587,
                username: 'hermanas-demo',
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
