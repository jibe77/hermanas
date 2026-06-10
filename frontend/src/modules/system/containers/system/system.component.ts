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
import { User, AuthState } from '@modules/auth/models';
import { UserService } from '@modules/auth/services';
import { DiskUsage } from '@modules/system/services/disk-usage.service';
import { MemoryUsage } from '@modules/system/services/memory-usage.service';
import { CpuUsage } from '@modules/system/services/cpu-usage.service';
import {
    StackSnapshot,
    SystemSnapshot,
    SystemSnapshotService,
} from '@modules/system/services/system-snapshot.service';
import { SystemPowerService } from '@modules/system/services/system-power.service';
import { VersionInfo, VersionService } from '@modules/system/services/version.service';
import { ConfigService } from '@modules/energy/services/config.service';
import { Subject, interval } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CommonCardsComponent } from '../../../app-common/components/common-cards/common-cards.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu } from '@ng-bootstrap/ng-bootstrap';
import { DatePipe } from '@angular/common';

@Component({
    selector: 'sb-system',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './system.component.html',
    styleUrls: ['system.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CommonCardsComponent,
        CardComponent,
        FaIconComponent,
        NgbDropdown,
        NgbDropdownToggle,
        NgbDropdownMenu,
        DatePipe,
    ],
})
export class SystemComponent implements OnInit, OnDestroy {
    private _versionService = inject(VersionService);
    private _snapshotService = inject(SystemSnapshotService);
    private _systemPowerService = inject(SystemPowerService);
    private _configService = inject(ConfigService);
    private _userService = inject(UserService);
    private _toastService = inject(ToastService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    public backEndVersion: string;
    public backEndBuildTime: string;
    public backEndVersionOnError: boolean;

    public isAuthenticated = false;
    public isAdmin = false;
    public powerActionInFlight = false;
    public configRefreshing = false;

    public diskUsage?: DiskUsage;
    public diskUsageError = false;
    public diskUsageLoading = false;

    public memoryUsage?: MemoryUsage;
    public memoryUsageError = false;
    public memoryUsageLoading = false;

    public cpuUsage?: CpuUsage;
    public cpuUsageError = false;
    /** Subject used to stop the snapshot polling stream — separate from
     *  destroy$ so logging out can kill the loop without ending all
     *  subscriptions held by this component. */
    private snapshotPollStop$ = new Subject<void>();

    // Software stack panel (admin only): pulled from the snapshot's `stack`
    // section. {@link stackInfo} is the flat structure assembled server-side
    // and is now the single source of truth for the Software stack card.
    public stackInfo?: StackSnapshot;
    public stackLoading = false;
    public stackError = false;

    notificationSubject: Subject<void> = new Subject<void>();
    private destroy$ = new Subject<void>();

    ngOnInit() {
        this.createSubscriptionToBackendVersion();
        // Button-status calls /api/v1/buttons (admin-only) — defer until auth state is known.
        this.subscribeToAuthState();
    }

    ngOnDestroy(): void {
        this.stopSnapshotPolling();
        this.snapshotPollStop$.complete();
        this.destroy$.next();
        this.destroy$.complete();
    }

    createSubscriptionToBackendVersion() {
        this._versionService
            .getVersionInfo()
            .pipe(takeUntil(this.destroy$))
            .subscribe(
                (data: VersionInfo) => {
                    this.refreshBackEndVersion(data);
                },
                (error: any) => {
                    this.refreshBackEndVersion(undefined, error);
                }
            );
    }

    refreshBackEndVersion(data?: VersionInfo, error?: any) {
        this.backEndVersionOnError = error !== undefined;
        this.backEndVersion = data !== undefined ? data.version : undefined;
        this.backEndBuildTime = data !== undefined ? data.time : undefined;
        if (error !== undefined) {
            this.notificationSubject.next();
        }
        this.changeDetectorRef.detectChanges();
    }

    onServiceRetry(_event: any) {
        if (this.backEndVersionOnError) {
            this.createSubscriptionToBackendVersion();
        }
        this.changeDetectorRef.detectChanges();
    }

    setCardChangeDetectorRef(_changeDetectorRef: ChangeDetectorRef) {
        this.changeDetectorRef = _changeDetectorRef;
    }

    private subscribeToAuthState(): void {
        this._userService.user$.pipe(takeUntil(this.destroy$)).subscribe((user: User) => {
            this.isAuthenticated = !!user && user.authState === AuthState.SignedIn;
            const wasAdmin = this.isAdmin;
            this.isAdmin = this._userService.isAdmin();
            if (this.isAdmin && !wasAdmin) {
                this.startSnapshotPolling();
            }
            if (!this.isAdmin && wasAdmin) {
                this.stopSnapshotPolling();
            }
            this.changeDetectorRef.detectChanges();
        });
    }

    /**
     * Starts polling {@code /api/v1/system/snapshot} every 2 seconds. One
     * request fetches disk, memory, CPU + OS uptime, and the entire Software
     * stack section — replaces the 9 separate requests we used to fire on
     * every tick. Each section is dispatched to the existing state fields so
     * the template does not change.
     *
     * <p>The endpoint also computes the CPU percentage from a delta between
     * two {@code /proc/stat} reads, so the very first response always
     * reports 0%; we prime the loop with an immediate call so the second
     * tick already shows a meaningful number.</p>
     */
    public startSnapshotPolling(): void {
        // Defensive: in dev a quick logout/login cycle could otherwise leave
        // two concurrent polls running.
        this.stopSnapshotPolling();
        this.diskUsageLoading = true;
        this.memoryUsageLoading = true;
        this.stackLoading = true;
        const stream$ = interval(2000)
            .pipe(
                switchMap(() => this._snapshotService.getSnapshot()),
                takeUntil(this.snapshotPollStop$),
                takeUntil(this.destroy$)
            );
        stream$.subscribe({
            next: snap => this.applySnapshot(snap),
            error: () => this.applySnapshotError(),
        });
        // Prime so the first tick already carries data (and so the CPU
        // backend has its previous /proc/stat sample by tick #2).
        this._snapshotService.getSnapshot().subscribe({
            next: snap => this.applySnapshot(snap),
            error: () => this.applySnapshotError(),
        });
    }

    public stopSnapshotPolling(): void {
        this.snapshotPollStop$.next();
    }

    private applySnapshot(snap: SystemSnapshot): void {
        this.diskUsage = snap.disk;
        this.diskUsageError = false;
        this.diskUsageLoading = false;
        this.memoryUsage = snap.memory;
        this.memoryUsageError = false;
        this.memoryUsageLoading = false;
        this.cpuUsage = snap.cpu;
        this.cpuUsageError = false;
        this.stackInfo = snap.stack;
        this.stackError = false;
        this.stackLoading = false;
        this.changeDetectorRef.detectChanges();
    }

    private applySnapshotError(): void {
        // Don't blank out the cards — keep the last good values and only flip
        // the error flags so the user knows the live feed died. The template
        // gates rendering on the *Loading flags which we leave false so the
        // last frame stays visible.
        this.diskUsageError = !this.diskUsage;
        this.memoryUsageError = !this.memoryUsage;
        this.cpuUsageError = !this.cpuUsage;
        this.stackError = !this.stackInfo;
        this.diskUsageLoading = false;
        this.memoryUsageLoading = false;
        this.stackLoading = false;
        this.changeDetectorRef.detectChanges();
    }

    /** Formats a byte count as a human-readable MB or GB string. */
    public formatBytes(bytes: number): string {
        if (!isFinite(bytes) || bytes <= 0) return '0 MB';
        const mb = bytes / (1024 * 1024);
        if (mb >= 1024) {
            return (mb / 1024).toFixed(2) + ' GB';
        }
        return mb.toFixed(0) + ' MB';
    }

    /** Formats {@code process.uptime} (seconds) as "3d 12h 5min". */
    public formatUptime(seconds?: number): string {
        if (seconds === undefined || !isFinite(seconds) || seconds < 0) return '';
        const total = Math.round(seconds);
        const d = Math.floor(total / 86400);
        const h = Math.floor((total % 86400) / 3600);
        const m = Math.floor((total % 3600) / 60);
        const parts: string[] = [];
        if (d > 0) parts.push(`${d}d`);
        if (h > 0 || d > 0) parts.push(`${h}h`);
        parts.push(`${m}min`);
        return parts.join(' ');
    }

    /** Formats a fraction (0..1) returned by metrics like process.cpu.usage as a percentage. */
    public formatPercent(ratio?: number): string {
        if (ratio === undefined || !isFinite(ratio)) return '';
        return (ratio * 100).toFixed(1) + ' %';
    }


    /**
     * Shuts down the Raspberry Pi via the audit-logged, rate-limited
     * POST /api/v1/system/shutdown endpoint. Confirms first with a native
     * dialog — losing the host is destructive enough that the friction is
     * worth it. The HTTP response often arrives after the OS has already
     * killed the JVM, so we treat any non-rate-limit error as best-effort.
     */
    public shutdownMachine(): void {
        if (this.powerActionInFlight) {
            return;
        }
        // eslint-disable-next-line max-len -- $localize template literals must keep their @@id and message on one line.
        const confirmMsg = $localize`:@@systemShutdownConfirm:Shut down the Raspberry Pi now? This will kill the application until the machine is powered back on manually.`;
        if (!window.confirm(confirmMsg)) {
            return;
        }
        this.powerActionInFlight = true;
        this.changeDetectorRef.detectChanges();
        this._systemPowerService.shutdown().subscribe({
            next: () => {
                this.powerActionInFlight = false;
                this._toastService.success(
                    $localize`:@@systemShutdownInProgress:Shutdown command sent. The machine is going down.`,
                    'System'
                );
                this.changeDetectorRef.detectChanges();
            },
            error: (err: HttpErrorResponse) => {
                this.powerActionInFlight = false;
                // 429 = too many shutdown attempts → show the backend message verbatim
                const detail =
                    err.status === 429
                        ? err.error?.message ||
                          $localize`:@@systemRateLimited:Too many attempts. Please wait a few minutes.`
                        : err.error?.message ||
                          err.message ||
                          $localize`:@@systemShutdownFailed:Shutdown failed.`;
                this._toastService.error(detail, `System — HTTP ${err.status}`);
                this.changeDetectorRef.detectChanges();
            },
        });
    }

    public rebootMachine(): void {
        if (this.powerActionInFlight) {
            return;
        }
        // eslint-disable-next-line max-len -- $localize template literal kept on one line on purpose.
        const confirmMsg = $localize`:@@systemRebootConfirm:Reboot the Raspberry Pi now? The application will be unavailable for up to 10 minutes.`;
        if (!window.confirm(confirmMsg)) {
            return;
        }
        this.powerActionInFlight = true;
        this.changeDetectorRef.detectChanges();
        this._systemPowerService.reboot().subscribe({
            next: () => {
                this.powerActionInFlight = false;
                this._toastService.success(
                    $localize`:@@systemRebootInProgress:Reboot command sent. The machine is restarting.`,
                    'System'
                );
                this.changeDetectorRef.detectChanges();
            },
            error: (err: HttpErrorResponse) => {
                this.powerActionInFlight = false;
                const detail =
                    err.status === 429
                        ? err.error?.message ||
                          $localize`:@@systemRateLimited:Too many attempts. Please wait a few minutes.`
                        : err.error?.message ||
                          err.message ||
                          $localize`:@@systemRebootFailed:Reboot failed.`;
                this._toastService.error(detail, `System — HTTP ${err.status}`);
                this.changeDetectorRef.detectChanges();
            },
        });
    }

    /**
     * Forces ConfigService to drop every Spring cache so the next read
     * picks up changes made directly in the DB (e.g. someone edited a row
     * via mysql cli). Idempotent and very cheap.
     */
    public refreshConfigCaches(): void {
        if (this.configRefreshing) {
            return;
        }
        this.configRefreshing = true;
        this.changeDetectorRef.detectChanges();
        this._configService
            .refresh()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: response => {
                    this.configRefreshing = false;
                    this._toastService.success(
                        $localize`:@@reloadConfigDone:${response.caches_cleared} caches vidés.`,
                        'Configuration'
                    );
                    this.changeDetectorRef.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.configRefreshing = false;
                    this._toastService.error(
                        err.error?.message || err.message || 'Refresh failed',
                        `Configuration — HTTP ${err.status}`
                    );
                    this.changeDetectorRef.detectChanges();
                },
            });
    }

}
