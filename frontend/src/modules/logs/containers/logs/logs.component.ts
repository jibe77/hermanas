import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import { UserService } from '@modules/auth/services';
import { EventEntry, EventsService } from '@modules/logs/services/events.service';
import { LogFileInfo, LogLevel, LogsService } from '@modules/logs/services/logs.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { DatePipe, NgClass } from '@angular/common';

/** Convenience labels for the time-window dropdown. */
type WindowPreset = '24h' | '7d' | '30d' | 'all';

interface CategoryFilter {
    key: string;
    labelKey: string;
    types: string[];
}

@Component({
    selector: 'sb-logs',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './logs.component.html',
    styleUrls: ['logs.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CardComponent,
        FaIconComponent,
        ReactiveFormsModule,
        FormsModule,
        NgClass,
        DatePipe,
    ],
})
export class LogsComponent implements OnInit, OnDestroy {
    private _logsService = inject(LogsService);
    private _eventsService = inject(EventsService);
    private _userService = inject(UserService);
    private _toastService = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    // ───────── Auth-derived flags ─────────
    isAdmin = false;
    /** True when the visitor is faking an admin session via the demo button.
     *  The log files + content are physical artefacts on the Pi that have no
     *  meaningful synthetic equivalent, so the two log-viewer cards collapse
     *  to a neutral "information not available" placeholder. */
    isDemoMode = false;

    // ───────── Business-event panel ─────────
    businessEvents: EventEntry[] = [];
    businessLoading = false;
    businessWindow: WindowPreset = '7d';
    /** When 'all' is selected we keep an empty selection meaning "every category". */
    businessCategoryKey = 'all';
    readonly businessCategories: CategoryFilter[] = [
        { key: 'all', labelKey: '@@journalCatAll', types: [] },
        {
            key: 'door',
            labelKey: '@@journalCatDoor',
            types: [
                'DOOR_OPENED',
                'DOOR_CLOSED',
                'DOOR_OPEN_FAILED',
                'DOOR_CLOSE_FAILED',
                'DOOR_POSITION_UNKNOWN',
            ],
        },
        { key: 'light', labelKey: '@@journalCatLight', types: ['LIGHT_ON', 'LIGHT_OFF'] },
        { key: 'fan', labelKey: '@@journalCatFan', types: ['FAN_ON', 'FAN_OFF'] },
        {
            key: 'music',
            labelKey: '@@journalCatMusic',
            types: ['MUSIC_STARTED', 'MUSIC_STOPPED', 'COCORICO'],
        },
        {
            key: 'resident',
            labelKey: '@@journalCatResident',
            types: ['RESIDENT_CREATED', 'RESIDENT_DELETED'],
        },
        {
            key: 'system',
            labelKey: '@@journalCatSystem',
            types: ['STARTUP', 'SHUTDOWN', 'SHUTDOWN_REQUESTED', 'REBOOT_REQUESTED'],
        },
    ];

    // ───────── Auth-event panel (admin only) ─────────
    authEvents: EventEntry[] = [];
    authLoading = false;
    authWindow: WindowPreset = '7d';
    /** 'all' | 'success' | 'failed' */
    authFilter: 'all' | 'success' | 'failed' = 'all';

    // ───────── Log-file viewer (admin only — unchanged) ─────────
    files: LogFileInfo[] = [];
    selectedFile?: string;

    lineOptions = [200, 500, 1000, 2000, 5000];
    levelOptions: LogLevel[] = ['ALL', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];

    selectedLines = 500;
    selectedLevel: LogLevel = 'ALL';
    searchText = '';

    lines: string[] = [];
    loading = false;
    listError = false;
    contentError?: string;

    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        this.isAdmin = this._userService.isAdmin();
        this.isDemoMode = this._userService.isDemoMode();
        this.loadBusinessEvents();
        if (this.isAdmin && !this.isDemoMode) {
            this.loadAuthEvents();
            this.refreshFiles();
        }
        // Refresh isAdmin if the user logs in while staying on the page.
        this._userService.user$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            const wasAdmin = this.isAdmin;
            this.isAdmin = this._userService.isAdmin();
            this.isDemoMode = this._userService.isDemoMode();
            if (this.isAdmin && !wasAdmin && !this.isDemoMode) {
                this.loadAuthEvents();
                this.refreshFiles();
            }
            this.cdr.detectChanges();
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Business events
    // ─────────────────────────────────────────────────────────────────

    loadBusinessEvents(): void {
        this.businessLoading = true;
        this.cdr.detectChanges();
        this._eventsService
            .listBusiness({
                from: this.computeFrom(this.businessWindow),
                limit: 500,
            })
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: events => {
                    this.businessEvents = this.applyCategoryFilter(events);
                    this.businessLoading = false;
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.businessLoading = false;
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot load events',
                        `Journal — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    /**
     * Server returns every business type within the time window; we filter to
     * the chosen category client-side so changing the dropdown is instant.
     */
    private applyCategoryFilter(events: EventEntry[]): EventEntry[] {
        const cat = this.businessCategories.find(c => c.key === this.businessCategoryKey);
        if (!cat || cat.types.length === 0) {
            return events;
        }
        const allowed = new Set(cat.types);
        return events.filter(e => allowed.has(e.eventType));
    }

    onBusinessWindowChange(): void {
        this.loadBusinessEvents();
    }

    onBusinessCategoryChange(): void {
        // No re-fetch needed: filtering is client-side over the already-loaded set.
        // But if the user picked an empty bucket while the cached set is empty, refetch.
        this.loadBusinessEvents();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Auth events (admin only)
    // ─────────────────────────────────────────────────────────────────

    loadAuthEvents(): void {
        this.authLoading = true;
        this.cdr.detectChanges();
        this._eventsService
            .listAuth({ from: this.computeFrom(this.authWindow), limit: 500 })
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: events => {
                    this.authEvents = this.applyAuthFilter(events);
                    this.authLoading = false;
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.authLoading = false;
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot load auth events',
                        `Journal — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    private applyAuthFilter(events: EventEntry[]): EventEntry[] {
        if (this.authFilter === 'success') {
            return events.filter(e => e.eventType === 'LOGIN_SUCCESS');
        }
        if (this.authFilter === 'failed') {
            return events.filter(e => e.eventType === 'LOGIN_FAILED');
        }
        return events;
    }

    onAuthWindowChange(): void {
        this.loadAuthEvents();
    }

    onAuthFilterChange(): void {
        this.loadAuthEvents();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Shared helpers
    // ─────────────────────────────────────────────────────────────────

    private computeFrom(window: WindowPreset): string | undefined {
        const now = new Date();
        let offsetMs: number;
        switch (window) {
            case '24h':
                offsetMs = 24 * 3600 * 1000;
                break;
            case '7d':
                offsetMs = 7 * 24 * 3600 * 1000;
                break;
            case '30d':
                offsetMs = 30 * 24 * 3600 * 1000;
                break;
            case 'all':
            default:
                // 5 years back is effectively "all" for this dataset.
                offsetMs = 5 * 365 * 24 * 3600 * 1000;
        }
        return new Date(now.getTime() - offsetMs).toISOString().split('.')[0];
    }

    /**
     * Human-readable, localized label for an event type. Built lazily and cached
     * because $localize strings are resolved at runtime. Falls back to the raw
     * enum value when a new backend type is not yet known to the frontend.
     */
    eventLabel(type: string): string {
        return this.localizedLabels()[type] ?? type;
    }

    private _labelsCache?: Record<string, string>;

    private localizedLabels(): Record<string, string> {
        if (!this._labelsCache) {
            this._labelsCache = {
                STARTUP: $localize`:@@eventStartup:Application started`,
                SHUTDOWN: $localize`:@@eventShutdown:Application stopped`,
                SHUTDOWN_REQUESTED: $localize`:@@eventShutdownRequested:Shutdown requested`,
                REBOOT_REQUESTED: $localize`:@@eventRebootRequested:Reboot requested`,
                DOOR_OPENED: $localize`:@@eventDoorOpened:Door opened`,
                DOOR_CLOSED: $localize`:@@eventDoorClosed:Door closed`,
                DOOR_OPEN_FAILED: $localize`:@@eventDoorOpenFailed:Door failed to open`,
                DOOR_CLOSE_FAILED: $localize`:@@eventDoorCloseFailed:Door failed to close`,
                DOOR_POSITION_UNKNOWN: $localize`:@@eventDoorPositionUnknown:Door position unknown`,
                LIGHT_ON: $localize`:@@eventLightOn:Light switched on`,
                LIGHT_OFF: $localize`:@@eventLightOff:Light switched off`,
                FAN_ON: $localize`:@@eventFanOn:Fan switched on`,
                FAN_OFF: $localize`:@@eventFanOff:Fan switched off`,
                MUSIC_STARTED: $localize`:@@eventMusicStarted:Music started`,
                MUSIC_STOPPED: $localize`:@@eventMusicStopped:Music stopped`,
                COCORICO: $localize`:@@eventCocorico:Cocorico played`,
                RESIDENT_CREATED: $localize`:@@eventResidentCreated:Resident added`,
                RESIDENT_DELETED: $localize`:@@eventResidentDeleted:Resident removed`,
                LOGIN_SUCCESS: $localize`:@@eventLoginSuccess:Successful login`,
                LOGIN_FAILED: $localize`:@@eventLoginFailed:Failed login`,
                LOGOUT: $localize`:@@eventLogout:Logout`,
            };
        }
        return this._labelsCache;
    }

    eventIcon(type: string): string {
        if (type.startsWith('DOOR_')) return 'door-open';
        if (type.startsWith('LIGHT_')) return 'sun';
        if (type.startsWith('FAN_')) return 'fan';
        if (type.startsWith('MUSIC_') || type === 'COCORICO') return 'music';
        if (type.startsWith('RESIDENT_')) return 'feather';
        if (type === 'STARTUP' || type === 'SHUTDOWN' || type.endsWith('_REQUESTED'))
            return 'power-off';
        if (type === 'LOGIN_SUCCESS' || type === 'LOGOUT') return 'user';
        if (type === 'LOGIN_FAILED') return 'exclamation-triangle';
        return 'info-circle';
    }

    eventCssClass(type: string): string {
        if (
            type.endsWith('_FAILED') ||
            type === 'DOOR_POSITION_UNKNOWN' ||
            type === 'LOGIN_FAILED'
        ) {
            return 'event-row event-danger';
        }
        if (type === 'STARTUP' || type === 'LOGIN_SUCCESS') return 'event-row event-success';
        if (type === 'SHUTDOWN' || type.endsWith('_REQUESTED') || type === 'LOGOUT')
            return 'event-row event-warn';
        return 'event-row';
    }

    // ─────────────────────────────────────────────────────────────────
    //  Log-file viewer (admin only — original behaviour)
    // ─────────────────────────────────────────────────────────────────

    refreshFiles(): void {
        this.listError = false;
        this._logsService
            .listFiles()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: files => {
                    this.files = files;
                    if (!this.selectedFile && files.length > 0) {
                        this.selectedFile = files[0].name;
                        this.refreshContent();
                    }
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.listError = true;
                    this.files = [];
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot list log files',
                        `Logs — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    onFileChange(filename: string): void {
        this.selectedFile = filename;
        this.refreshContent();
    }

    refreshContent(): void {
        if (!this.selectedFile) {
            return;
        }
        this.loading = true;
        this.contentError = undefined;
        this.cdr.detectChanges();
        this._logsService
            .tail(this.selectedFile, {
                lines: this.selectedLines,
                level: this.selectedLevel,
                search: this.searchText,
            })
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: lines => {
                    this.lines = lines;
                    this.loading = false;
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.loading = false;
                    this.contentError = err.error?.message || err.message || 'Cannot read log file';
                    this.lines = [];
                    this._toastService.error(this.contentError, `Logs — HTTP ${err.status}`);
                    this.cdr.detectChanges();
                },
            });
    }

    cssClassForLine(line: string): string {
        if (/\bERROR\b/.test(line)) return 'log-error';
        if (/\bWARN\b/.test(line)) return 'log-warn';
        if (/\bINFO\b/.test(line)) return 'log-info';
        if (/\bDEBUG\b/.test(line)) return 'log-debug';
        if (/\bTRACE\b/.test(line)) return 'log-trace';
        return 'log-other';
    }

    formatSize(bytes: number): string {
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }
}
