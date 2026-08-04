import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    LOCALE_ID,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { LoggerService, ToastService } from '@common/services';
import {
    CaptureState,
    Detection,
    PhotoEntry,
    PhotoListing,
    PhotosService,
} from '@modules/camera/services/photos.service';
import { CaptureWebsocketService } from '@modules/camera/services/capture-websocket.service';
import { AWB_MODES, ConfigService } from '@modules/energy/services/config.service';
import { UserService } from '@modules/auth/services';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';

interface Crumb {
    label: string;
    /** Relative path this crumb navigates to. '' for the root. */
    path: string;
}

interface PhotoFile extends PhotoEntry {
    url: string;
}

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CardComponent,
        FaIconComponent,
        FormsModule,
    ],
})
export class ChartsComponent implements OnInit, OnDestroy {
    private photos = inject(PhotosService);
    private captureWs = inject(CaptureWebsocketService);
    private configService = inject(ConfigService);
    private userService = inject(UserService);
    private toast = inject(ToastService);
    private logger = inject(LoggerService);
    private sanitizer = inject(DomSanitizer);
    private cdr = inject(ChangeDetectorRef);
    private localeId = inject(LOCALE_ID);

    currentPath = '';
    crumbs: Crumb[] = [{ label: 'photos', path: '' }];
    directories: PhotoEntry[] = [];
    files: PhotoFile[] = [];
    loading = false;
    selected?: PhotoFile;

    isAdmin = false;
    /** True for any signed-in account (USER or ADMIN) or for the demo mode.
     *  Gates the historical "Archives" card — the live snapshot stays public. */
    isSignedIn = false;
    cameraBrightness = 60;
    cameraRotation = 180;
    cameraRegularQuality = 45;
    cameraHighQuality = 80;
    /** Mode de balance des blancs ; chaîne vide = automatique. */
    cameraAwb = '';
    /** Gains rouge/bleu imposés, "R,B". Prioritaires sur le mode ci-dessus. */
    cameraAwbGains = '';
    cameraRegularWidth = 1096;
    cameraRegularHeight = 822;
    cameraRegularDelay = 500;
    cameraHighWidth = 1640;
    cameraHighHeight = 1232;
    cameraHighDelay = 1000;
    /** Zone du capteur lue, "x,y,largeur,hauteur" en 0-1. Vide = capteur entier. */
    cameraRoi = '';
    /**
     * Mode capteur imposé, "largeur:hauteur". Vide = libcamera choisit — ce qui
     * sous ~790 px de hauteur donne le mode 640×480 recadré au centre, d'où une
     * image zoomée et pixelisée.
     */
    cameraMode = '';
    /** Temps de pose en µs. Vide = auto-exposition. */
    cameraShutter = '';
    /** Gain analogique (1 = aucun). Vide = gain automatique. */
    cameraGain = '';
    readonly awbModes = AWB_MODES;
    cameraSaving = false;

    aiInferenceUrl = '';
    aiInferenceModel = 'focus';
    /** Exposed in seconds in the UI — the backend stores milliseconds internally. */
    aiInferenceCacheTtlSec = 120;
    /** Custom prompt text. Empty string keeps the backend default. */
    aiInferencePrompt = '';
    /** Backend-supplied default prompt, used as the textarea placeholder. */
    aiInferencePromptDefault = '';
    aiInferenceSaving = false;

    // Timeouts & retry policy — exposed in seconds in the UI for readability,
    // but the backend stores everything in milliseconds.
    aiInferenceConnectTimeoutSec = 15;
    aiInferenceReadTimeoutSec = 180;
    aiInferenceRetryMaxAttempts = 3;
    aiInferenceRetryInitialBackoffSec = 2;
    aiInferenceRetryMaxBackoffSec = 10;
    aiInferenceTimeoutsSaving = false;

    /** Object URL of the currently displayed JPEG, or '' before the first capture. */
    snapshotUrl = '';
    /** True once {@link captureImageUrl} has resolved a blob. */
    snapshotLoaded = false;
    /** True if either the capture pipeline or the image download errored out. */
    snapshotFailed = false;
    /** True while a capture pipeline is in flight — disables the refresh + analyze buttons. */
    captureInFlight = false;
    /** Current capture id (kept so we can release the blob URL on destroy). */
    private currentCaptureId?: string;

    aiAnalysisLoading = false;
    aiAnalysisResult = '';
    /** Sanitized HTML rendering of {@link aiAnalysisResult} — see renderMarkdown(). */
    aiAnalysisResultHtml: SafeHtml = '';
    /**
     * Normalized bounding boxes parsed from the model's hidden JSON tail.
     * Rendered as an absolutely-positioned SVG overlay above the snapshot
     * <img>. Empty array when the model emitted nothing usable.
     */
    detections: Detection[] = [];
    detectionsVisible = false;
    aiAnalysisStep = '';
    private aiAnalysisStepTimer?: ReturnType<typeof setInterval>;
    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        // Every visitor — anonymous included — gets the full async pipeline.
        // POST /api/v1/captures is open (rate-limited per IP server-side), so
        // anonymous abuse just hits a 429.
        this.startCapture();

        // Stay subscribed to auth changes so a login/logout that happens while
        // this page is open reveals or hides the admin-only / signed-in-only
        // panels (camera settings, archive listing) without a page reload.
        this.userService.user$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            const wasAdmin = this.isAdmin;
            const wasSignedIn = this.isSignedIn;
            this.isAdmin = this.userService.isAdmin();
            this.isSignedIn =
                this.isAdmin ||
                this.userService.getCurrentUser().authState === 'signedIn';
            if (this.isAdmin && !wasAdmin) {
                this.loadCameraSettings();
            }
            if (this.isSignedIn && !wasSignedIn) {
                this.load('');
            }
            this.cdr.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.stopAiAnalysisAnimation();
        this.releaseSnapshotUrl();
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Kicks the async capture + analysis pipeline on the backend. Single entry
     * point used by both the initial page load and the refresh button.
     *
     * <p>The previous synchronous flow ({@code GET /camera/takePicture} then
     * {@code GET /camera/analyze}) regularly timed out at the reverse proxy
     * because each request had to span the full Pi-side work. We now POST a
     * job, then poll its status while fetching the JPEG in parallel.</p>
     */
    startCapture(): void {
        if (this.captureInFlight) return;
        this.captureInFlight = true;
        this.snapshotLoaded = false;
        this.snapshotFailed = false;
        this.aiAnalysisResult = '';
        this.aiAnalysisResultHtml = '';
        this.detections = [];
        this.detectionsVisible = false;
        this.releaseSnapshotUrl();
        this.snapshotUrl = '';
        this.aiAnalysisLoading = true;
        this.startAiAnalysisAnimation();
        this.cdr.markForCheck();

        const lang: 'fr' | 'en' | 'ro' = this.localeId.startsWith('fr')
            ? 'fr'
            : this.localeId.startsWith('ro')
              ? 'ro'
              : 'en';

        // Demo mode falls through to the regular pipeline. The mutating POST
        // /captures will be blocked by demoModeInterceptor, which surfaces
        // the yellow "demo blocked" toast and rejects with status 0 — the
        // applyCaptureError path below clears the spinner and the live
        // panel ends up empty, matching what an unauthenticated visitor
        // sees (no live picture, no fake AI analysis).
        this.logger.info('startCapture: requesting new capture', { lang }, 'Webcam');
        this.photos.startCapture(lang).subscribe({
            next: captureId => {
                this.currentCaptureId = captureId;
                this.logger.info(
                    'startCapture: backend accepted job',
                    { captureId },
                    'Webcam'
                );
                this.photos.captureImageUrl(captureId).subscribe({
                    next: url => {
                        this.snapshotUrl = url;
                        this.snapshotLoaded = true;
                        this.logger.info(
                            'captureImageUrl: image received',
                            { captureId },
                            'Webcam'
                        );
                        this.cdr.markForCheck();
                    },
                    error: (err: HttpErrorResponse) => {
                        this.snapshotFailed = true;
                        this.logger.error(
                            'captureImageUrl: fetch failed',
                            { captureId, status: err.status, message: err.message },
                            'Webcam'
                        );
                        this.cdr.markForCheck();
                    },
                });
                // One-shot status fetch to cover the race where the backend
                // finishes the pipeline (cached analysis) before the STOMP
                // subscription is up. Errors are intentionally swallowed: the
                // STOMP stream below is the source of truth, and a 404 here
                // would just mean the job is still booting.
                this.photos.getCaptureStatus(captureId).subscribe({
                    next: state => this.applyCaptureState(state, captureId),
                    error: () => {
                        /* ignore — STOMP will deliver the real states */
                    },
                });
                this.captureWs
                    .watch(captureId)
                    .pipe(takeUntil(this.destroy$))
                    .subscribe({
                        next: state => this.applyCaptureState(state, captureId),
                        error: (err: Error) => {
                            this.logger.error(
                                'captureWs: subscription failed',
                                { captureId, message: err.message },
                                'Webcam'
                            );
                            this.applyCaptureError({
                                error: { message: err.message },
                                message: err.message,
                                status: 0,
                            } as HttpErrorResponse);
                        },
                    });
            },
            error: (err: HttpErrorResponse) => {
                this.captureInFlight = false;
                this.logger.error(
                    'startCapture: POST /captures failed',
                    { status: err.status, message: err.message, body: err.error },
                    'Webcam'
                );
                this.applyCaptureError(err);
            },
        });
    }

    private applyCaptureState(state: CaptureState, captureId?: string): void {
        // We only care about terminal states for logging — intermediate
        // ANALYZING frames are bookkeeping and don't change the UI surface.
        if (state.status === 'DONE') {
            this.aiAnalysisLoading = false;
            this.captureInFlight = false;
            this.aiAnalysisResult = state.message ?? '';
            this.aiAnalysisResultHtml = this.renderMarkdown(this.aiAnalysisResult);
            this.detections = state.detections ?? [];
            this.stopAiAnalysisAnimation();
            this.logger.info(
                'captureState: DONE',
                {
                    captureId,
                    messageLength: this.aiAnalysisResult.length,
                    detectionCount: this.detections.length,
                },
                'Webcam'
            );
        } else if (state.status === 'ERROR') {
            this.aiAnalysisLoading = false;
            this.captureInFlight = false;
            this.aiAnalysisResult =
                state.errorMessage || state.errorCode || 'Analysis unavailable';
            this.aiAnalysisResultHtml = this.renderMarkdown(this.aiAnalysisResult);
            this.stopAiAnalysisAnimation();
            this.logger.warn(
                'captureState: ERROR',
                { captureId, errorCode: state.errorCode, errorMessage: state.errorMessage },
                'Webcam'
            );
        }
        this.cdr.markForCheck();
    }

    private applyCaptureError(err: HttpErrorResponse): void {
        this.aiAnalysisLoading = false;
        this.captureInFlight = false;
        this.aiAnalysisResult =
            err.error?.message || err.error?.error || err.message || 'Analysis unavailable';
        this.aiAnalysisResultHtml = this.renderMarkdown(this.aiAnalysisResult);
        this.stopAiAnalysisAnimation();
        this.cdr.markForCheck();
    }

    /**
     * Releases the previous blob: URL so the browser can free the underlying
     * memory. createObjectURL leaks the blob until URL.revokeObjectURL is
     * called explicitly.
     */
    private releaseSnapshotUrl(): void {
        if (this.snapshotUrl && this.snapshotUrl.startsWith('blob:')) {
            URL.revokeObjectURL(this.snapshotUrl);
        }
    }

    /**
     * Minimal Markdown → HTML pass tailored for the chicken-coop LLM output:
     * `**bold**`, `*italic*`, `# headings`, bullets (`* ` or `- `) and blank
     * lines as paragraph breaks. Everything is HTML-escaped before any
     * transformation, so even if the model returns `<script>` it appears as
     * literal text. The final string still goes through DomSanitizer for
     * defence in depth.
     */
    private renderMarkdown(src: string): SafeHtml {
        if (!src) {
            return '';
        }
        const escape = (s: string): string =>
            s
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;');
        const inline = (s: string): string =>
            s
                .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
                .replace(/__([^_]+)__/g, '<strong>$1</strong>')
                .replace(/(^|[\s(])\*([^*\s][^*]*?[^*\s])\*(?=[\s).,!?:;]|$)/g, '$1<em>$2</em>')
                .replace(/`([^`]+)`/g, '<code>$1</code>');

        const lines = escape(src).split(/\r?\n/);
        const out: string[] = [];
        let inList = false;
        for (const raw of lines) {
            const line = raw.trimEnd();
            const bullet = line.match(/^\s*[*\-]\s+(.*)$/);
            const heading = line.match(/^(#{1,6})\s+(.*)$/);
            if (bullet) {
                if (!inList) {
                    out.push('<ul>');
                    inList = true;
                }
                out.push(`<li>${inline(bullet[1])}</li>`);
                continue;
            }
            if (inList) {
                out.push('</ul>');
                inList = false;
            }
            if (!line.trim()) {
                continue;
            }
            if (heading) {
                const level = heading[1].length;
                out.push(`<h${level}>${inline(heading[2])}</h${level}>`);
                continue;
            }
            out.push(`<p>${inline(line)}</p>`);
        }
        if (inList) {
            out.push('</ul>');
        }
        return this.sanitizer.bypassSecurityTrustHtml(out.join(''));
    }

    /**
     * Cycles through a handful of localized "step" messages while the LLM is
     * thinking, so the visitor sees the panel actively breathing rather than a
     * frozen spinner for 30s. Stays the same step for ~2.5 s then rotates.
     */
    private startAiAnalysisAnimation(): void {
        const steps = this.analysisSteps();
        let i = 0;
        this.aiAnalysisStep = steps[0];
        this.aiAnalysisStepTimer = setInterval(() => {
            i = (i + 1) % steps.length;
            this.aiAnalysisStep = steps[i];
            this.cdr.markForCheck();
        }, 2500);
    }

    private stopAiAnalysisAnimation(): void {
        if (this.aiAnalysisStepTimer) {
            clearInterval(this.aiAnalysisStepTimer);
            this.aiAnalysisStepTimer = undefined;
        }
        this.aiAnalysisStep = '';
    }

    /**
     * Returns the stroke color for a detection. Chickens are red, eggs are
     * blue, anything else falls back to a neutral grey so a future label
     * doesn't render invisibly. Confidence (0..1) shifts the lightness so
     * uncertain boxes appear paler — visually obvious without a legend.
     */
    detectionStroke(d: Detection): string {
        const lightness = Math.round(60 - Math.max(0, Math.min(1, d.confidence)) * 25);
        switch (d.type) {
            case 'chicken':
                return `hsl(0, 85%, ${lightness}%)`;
            case 'egg':
                return `hsl(215, 85%, ${lightness}%)`;
            default:
                return `hsl(0, 0%, ${lightness}%)`;
        }
    }

    /** Localized short label drawn next to each bounding box. */
    detectionLabel(d: Detection): string {
        const pct = Math.round(Math.max(0, Math.min(1, d.confidence)) * 100);
        switch (d.type) {
            case 'chicken':
                return $localize`:@@detectionLabelChicken:Hen` + ' ' + pct + '%';
            case 'egg':
                return $localize`:@@detectionLabelEgg:Egg` + ' ' + pct + '%';
            default:
                return d.type + ' ' + pct + '%';
        }
    }

    /** Track function for the *ngFor over detections. */
    trackDetection(index: number, _d: Detection): number {
        return index;
    }

    toggleDetections(): void {
        if (this.detections.length === 0) return;
        this.detectionsVisible = !this.detectionsVisible;
        this.cdr.markForCheck();
    }

    private analysisSteps(): string[] {
        return [
            $localize`:@@aiStepSendingPicture:Sending the picture to the model`,
            $localize`:@@aiStepCountingHens:Counting hens`,
            $localize`:@@aiStepLookingForEggs:Looking for eggs`,
            $localize`:@@aiStepCheckingDoor:Checking the door state`,
            $localize`:@@aiStepInspectingFloor:Inspecting hay and dirt levels`,
            $localize`:@@aiStepFinalizing:Wrapping up the analysis`,
        ];
    }

    private loadCameraSettings(): void {
        this.configService.getAll().subscribe({
            next: cfg => {
                this.cameraBrightness = cfg.camera_settings.brightness;
                this.cameraRotation = cfg.camera_settings.rotation;
                this.cameraRegularQuality = cfg.camera_settings.regular_quality ?? 45;
                this.cameraHighQuality = cfg.camera_settings.high_quality ?? 80;
                this.cameraAwb = cfg.camera_settings.awb ?? '';
                this.cameraAwbGains = cfg.camera_settings.awb_gains ?? '';
                this.cameraRegularWidth = cfg.camera_settings.regular_width ?? 1096;
                this.cameraRegularHeight = cfg.camera_settings.regular_height ?? 822;
                this.cameraRegularDelay = cfg.camera_settings.regular_delay ?? 500;
                this.cameraHighWidth = cfg.camera_settings.high_width ?? 1640;
                this.cameraHighHeight = cfg.camera_settings.high_height ?? 1232;
                this.cameraHighDelay = cfg.camera_settings.high_delay ?? 1000;
                this.cameraRoi = cfg.camera_settings.roi ?? '';
                this.cameraMode = cfg.camera_settings.mode ?? '';
                this.cameraShutter = cfg.camera_settings.shutter ?? '';
                this.cameraGain = cfg.camera_settings.gain ?? '';
                this.aiInferenceUrl = cfg.ai_settings?.inference_url ?? '';
                this.aiInferenceModel = cfg.ai_settings?.inference_model ?? 'focus';
                this.aiInferenceCacheTtlSec = Math.round(
                    (cfg.ai_settings?.cache_ttl_ms ?? 120000) / 1000
                );
                this.aiInferencePromptDefault = cfg.ai_settings?.prompt_default ?? '';
                // Pre-fill the textarea with the default when no custom prompt
                // is set, so the admin sees the actual text instead of an empty
                // box. saveAiInferenceUrl() compares back against the default
                // verbatim and sends an empty string when they match, so this
                // is purely cosmetic and does not freeze the prompt to the
                // current default value.
                const stored = cfg.ai_settings?.prompt ?? '';
                this.aiInferencePrompt = stored || this.aiInferencePromptDefault;
                // Timeouts & retry — store as seconds in the UI; milliseconds go
                // over the wire. The ?? fallbacks match the backend defaults so
                // an old config payload without these keys still renders sane values.
                this.aiInferenceConnectTimeoutSec = Math.round(
                    (cfg.ai_settings?.connect_timeout_ms ?? 15000) / 1000
                );
                this.aiInferenceReadTimeoutSec = Math.round(
                    (cfg.ai_settings?.read_timeout_ms ?? 180000) / 1000
                );
                this.aiInferenceRetryMaxAttempts = cfg.ai_settings?.retry_max_attempts ?? 3;
                this.aiInferenceRetryInitialBackoffSec = Math.round(
                    (cfg.ai_settings?.retry_initial_backoff_ms ?? 2000) / 1000
                );
                this.aiInferenceRetryMaxBackoffSec = Math.round(
                    (cfg.ai_settings?.retry_max_backoff_ms ?? 10000) / 1000
                );
                this.cdr.markForCheck();
            },
            error: () => {
                // GET /config is admin-only, so an anonymous visitor (including
                // the front-only demo mode) lands here with a 401. Fall back to
                // the public /config/ai/prompt-default endpoint so the prompt
                // textarea still shows the actual default text instead of a
                // blank box — every other field stays gated.
                this.loadAiInferencePromptDefaultPublic();
            },
        });
    }

    private loadAiInferencePromptDefaultPublic(): void {
        this.configService.getAiInferencePromptDefault().subscribe({
            next: res => {
                this.aiInferencePromptDefault = res.prompt_default ?? '';
                if (!this.aiInferencePrompt) {
                    this.aiInferencePrompt = this.aiInferencePromptDefault;
                }
                this.cdr.markForCheck();
            },
            error: () => {
                /* keep defaults */
            },
        });
    }

    /**
     * Contrôle la région d'intérêt avant l'envoi, avec les mêmes règles que le
     * serveur : quatre valeurs, bornes 0-1, largeur/hauteur non nulles, et zone
     * ne débordant pas du capteur.
     *
     * <p>Doubler la validation côté client n'est pas redondant : elle donne un
     * retour immédiat pendant la saisie, là où le serveur ne répond qu'après
     * l'envoi. Le serveur reste l'autorité — l'API est accessible sans passer
     * par cette page.</p>
     *
     * @returns le message d'erreur, ou `null` si la valeur est acceptable
     */
    get cameraRoiError(): string | null {
        const raw = (this.cameraRoi ?? '').trim();
        if (raw === '') return null; // vide = capteur entier

        const parts = raw.split(',');
        if (parts.length !== 4) {
            return $localize`:@@cameraRoiErrCount:Four values are required: x, y, width, height.`;
        }
        const values = parts.map(p => Number(p.trim()));
        if (values.some(v => Number.isNaN(v))) {
            return $localize`:@@cameraRoiErrNumber:Values must be decimal numbers.`;
        }
        if (values.some(v => v < 0 || v > 1)) {
            return $localize`:@@cameraRoiErrRange:Values must be between 0 and 1.`;
        }
        const [x, y, w, h] = values;
        if (w <= 0 || h <= 0) {
            return $localize`:@@cameraRoiErrZero:Width and height must be greater than 0.`;
        }
        if (x + w > 1.0001 || y + h > 1.0001) {
            return $localize`:@@cameraRoiErrOverflow:The region extends past the sensor: x+width and y+height must not exceed 1.`;
        }
        return null;
    }

    /**
     * Hauteur de sortie qui préserverait l'échelle avec la région saisie.
     *
     * <p>rpicam-still recadre puis rééchantillonne : sans ajuster la hauteur, une
     * région plus courte que large est étirée verticalement. Cette valeur est
     * suggérée à l'opérateur, pas imposée — un zoom délibéré reste possible.</p>
     */
    get suggestedRegularHeight(): number | null {
        return this.suggestedHeightFor(Number(this.cameraRegularWidth));
    }

    get suggestedHighHeight(): number | null {
        return this.suggestedHeightFor(Number(this.cameraHighWidth));
    }

    private suggestedHeightFor(width: number): number | null {
        if (this.cameraRoiError !== null) return null;
        const raw = (this.cameraRoi ?? '').trim();
        if (raw === '') return null;
        const [, , w, h] = raw.split(',').map(p => Number(p.trim()));
        if (!w || !h) return null;
        // Le capteur est en 4:3 : la hauteur suit la largeur, corrigée du rapport
        // entre les dimensions de la région.
        return Math.round((width * 3) / 4 / (w / h));
    }

    saveCameraSettings(): void {
        if (this.cameraSaving) return;
        // Le serveur refuserait de toute façon, mais autant l'annoncer tout de
        // suite plutôt que d'afficher une erreur HTTP après un aller-retour.
        const roiError = this.cameraRoiError;
        if (roiError !== null) {
            this.toast.error(roiError, $localize`:@@cameraToastTitle:Camera`);
            return;
        }
        this.cameraSaving = true;
        // <input type="range"> binds as a string under ngModel; coerce here so
        // the backend never receives a stringified value (it would still parse
        // it, but the log lines and the local model would diverge in type).
        const brightness = Number(this.cameraBrightness);
        const rotation = Number(this.cameraRotation);
        const regularQuality = Number(this.cameraRegularQuality);
        const highQuality = Number(this.cameraHighQuality);
        const regularWidth = Number(this.cameraRegularWidth);
        const regularHeight = Number(this.cameraRegularHeight);
        const regularDelay = Number(this.cameraRegularDelay);
        const highWidth = Number(this.cameraHighWidth);
        const highHeight = Number(this.cameraHighHeight);
        const highDelay = Number(this.cameraHighDelay);
        this.logger.info(
            'saveCameraSettings: sending settings',
            {
                brightness,
                rotation,
                regularQuality,
                highQuality,
                awb: this.cameraAwb,
                awbGains: this.cameraAwbGains,
                mode: this.cameraMode || '(auto)',
                shutter: this.cameraShutter || '(auto)',
                gain: this.cameraGain || '(auto)',
                regularSize: `${regularWidth}x${regularHeight}`,
                highSize: `${highWidth}x${highHeight}`,
            },
            'Webcam'
        );
        forkJoin({
            brightness: this.configService.setCameraBrightness(brightness),
            rotation: this.configService.setCameraRotation(rotation),
            regularQuality: this.configService.setCameraRegularQuality(regularQuality),
            highQuality: this.configService.setCameraHighQuality(highQuality),
            awb: this.configService.setCameraAwb(this.cameraAwb),
            awbGains: this.configService.setCameraAwbGains(this.cameraAwbGains),
            regularSize: this.configService.setCameraRegularSize(regularWidth, regularHeight),
            highSize: this.configService.setCameraHighSize(highWidth, highHeight),
            regularDelay: this.configService.setCameraRegularDelay(regularDelay),
            highDelay: this.configService.setCameraHighDelay(highDelay),
            roi: this.configService.setCameraRoi(this.cameraRoi),
            mode: this.configService.setCameraMode(this.cameraMode),
            shutter: this.configService.setCameraShutter(this.cameraShutter),
            gain: this.configService.setCameraGain(this.cameraGain),
        }).subscribe({
            next: () => {
                this.cameraSaving = false;
                // Reload from the backend after a successful save so the form
                // shows what was actually persisted — same contract as the AI
                // settings card. Without this, a save that the backend silently
                // clamped (or that hit a stale cache) would still display the
                // original textarea value and the operator could not tell.
                this.loadCameraSettings();
                this.toast.success(
                    $localize`:@@cameraSettingsSaved:Settings saved — effective on the next picture.`,
                    $localize`:@@cameraToastTitle:Camera`
                );
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.cameraSaving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Caméra — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    /**
     * Returns the prompt value to persist. Empty string when the textarea still
     * holds the default verbatim — that way the backend keeps the "follow the
     * built-in default" behaviour and a save that touches only the URL or model
     * does not freeze the prompt to the version currently shipped with the SPA.
     */
    private normalizePromptForSave(): string {
        const current = (this.aiInferencePrompt || '').trim();
        const def = (this.aiInferencePromptDefault || '').trim();
        return current === def ? '' : current;
    }

    saveAiInferenceUrl(): void {
        if (this.aiInferenceSaving) return;
        this.aiInferenceSaving = true;
        // Clamp the TTL to a sane range — anything negative is a UI typo and
        // anything above an hour would defeat the "freshness" intent.
        const ttlSec = Math.max(0, Math.min(3600, this.aiInferenceCacheTtlSec || 0));
        const promptToSend = this.normalizePromptForSave();
        // Explicit log so a missed save can be triaged from the browser console
        // without having to inspect the network tab: we see what the SPA
        // actually sent vs. what the textarea showed.
        this.logger.info(
            'saveAiInferenceUrl: sending settings',
            {
                urlLength: this.aiInferenceUrl.trim().length,
                model: (this.aiInferenceModel || '').trim(),
                cacheTtlSec: ttlSec,
                promptLength: promptToSend.length,
                promptMatchesDefault: promptToSend.length === 0,
            },
            'Webcam'
        );
        forkJoin({
            url: this.configService.setAiInferenceUrl(this.aiInferenceUrl.trim()),
            model: this.configService.setAiInferenceModel(
                (this.aiInferenceModel || '').trim()
            ),
            cacheTtl: this.configService.setAiInferenceCacheTtlMs(ttlSec * 1000),
            prompt: this.configService.setAiInferencePrompt(promptToSend),
        }).subscribe({
            next: () => {
                this.aiInferenceSaving = false;
                // Drop the on-screen analysis so the user does not mistake an
                // old cached result for the output of the new prompt. The
                // backend's AiVisionCache is also cleared server-side by
                // setAiInferencePrompt, so the next analyze will go to the
                // model with the new prompt.
                this.aiAnalysisResult = '';
                this.aiAnalysisResultHtml = '';
                // Reload settings so the textarea reflects what was actually
                // persisted (and the default, in case the backend ever evolves
                // it). Without this, an admin who pressed Save twice quickly
                // could be confused about which version is now live.
                this.loadCameraSettings();
                this.toast.success(
                    $localize`:@@cameraAiSettingsSaved:AI inference settings saved.`,
                    $localize`:@@cameraToastTitle:Camera`
                );
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.aiInferenceSaving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Caméra — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    /**
     * Persists the timeouts + retry policy. Values are entered in seconds in
     * the UI but sent in milliseconds. The success toast reminds the admin
     * that AiVisionClient reads these on boot so changes only take effect
     * on the next reboot — same contract as the camera settings.
     */
    saveAiInferenceTimeouts(): void {
        if (this.aiInferenceTimeoutsSaving) return;
        this.aiInferenceTimeoutsSaving = true;
        const connectMs = Math.max(1, Math.min(600, this.aiInferenceConnectTimeoutSec || 1)) * 1000;
        const readMs = Math.max(1, Math.min(1800, this.aiInferenceReadTimeoutSec || 1)) * 1000;
        const attempts = Math.max(1, Math.min(10, this.aiInferenceRetryMaxAttempts || 1));
        const initialBackoffMs = Math.max(0, Math.min(60, this.aiInferenceRetryInitialBackoffSec || 0)) * 1000;
        const maxBackoffMs = Math.max(0, Math.min(120, this.aiInferenceRetryMaxBackoffSec || 0)) * 1000;
        forkJoin({
            connect: this.configService.setAiInferenceConnectTimeoutMs(connectMs),
            read: this.configService.setAiInferenceReadTimeoutMs(readMs),
            attempts: this.configService.setAiInferenceRetryMaxAttempts(attempts),
            initialBackoff: this.configService.setAiInferenceRetryInitialBackoffMs(initialBackoffMs),
            maxBackoff: this.configService.setAiInferenceRetryMaxBackoffMs(maxBackoffMs),
        }).subscribe({
            next: () => {
                this.aiInferenceTimeoutsSaving = false;
                this.loadCameraSettings();
                this.toast.success(
                    $localize`:@@cameraAiTimeoutsSaved:Timeouts & retry saved (effective on next reboot).`,
                    $localize`:@@cameraToastTitle:Camera`
                );
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.aiInferenceTimeoutsSaving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Caméra — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    enterDir(dir: PhotoEntry): void {
        const next = this.currentPath ? `${this.currentPath}/${dir.name}` : dir.name;
        this.load(next);
    }

    goTo(path: string): void {
        if (path === this.currentPath) {
            return;
        }
        this.load(path);
    }

    selectFile(file: PhotoFile): void {
        this.selected = file;
        this.cdr.markForCheck();
    }

    clearSelection(): void {
        this.selected = undefined;
    }

    private load(path: string): void {
        this.loading = true;
        this.clearSelection();
        this.photos.list(path).subscribe({
            next: (listing: PhotoListing) => {
                this.currentPath = listing.path;
                const byNameAsc = (a: { name: string }, b: { name: string }) =>
                    a.name.localeCompare(b.name);
                this.directories = [...listing.directories].sort(byNameAsc);
                this.files = listing.files
                    .map(f => ({
                        ...f,
                        url: this.photos.fileUrl(
                            listing.path ? `${listing.path}/${f.name}` : f.name
                        ),
                    }))
                    .sort(byNameAsc);
                this.crumbs = this.buildCrumbs(listing.path);
                this.loading = false;
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.loading = false;
                const msg = err.error?.error || err.message || 'Cannot list photos';
                this.toast.error(msg, `Camera — HTTP ${err.status}`);
                this.cdr.markForCheck();
            },
        });
    }

    private buildCrumbs(path: string): Crumb[] {
        const result: Crumb[] = [{ label: 'photos', path: '' }];
        if (!path) {
            return result;
        }
        const parts = path.split('/').filter(p => p.length > 0);
        let acc = '';
        for (const part of parts) {
            acc = acc ? `${acc}/${part}` : part;
            result.push({ label: part, path: acc });
        }
        return result;
    }
}
