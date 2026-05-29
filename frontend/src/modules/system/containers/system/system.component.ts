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
import {
    ButtonName,
    ButtonStatus,
    ButtonStatusService,
} from '@modules/system/services/button-status.service';
import { DiskUsage, DiskUsageService } from '@modules/system/services/disk-usage.service';
import { EmailTestService } from '@modules/system/services/email-test.service';
import { SystemPowerService } from '@modules/system/services/system-power.service';
import { VersionInfo, VersionService } from '@modules/system/services/version.service';
import { ServoCalibrationService } from '@modules/system/services/servo-calibration.service';
import { ConfigService } from '@modules/energy/services/config.service';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CommonCardsComponent } from '../../../app-common/components/common-cards/common-cards.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu } from '@ng-bootstrap/ng-bootstrap';
import { DatePipe } from '@angular/common';

interface ButtonState {
    pressed?: boolean;
    timestamp?: number;
    error: boolean;
}

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
        FormsModule,
    ],
})
export class SystemComponent implements OnInit, OnDestroy {
    private _versionService = inject(VersionService);
    private _buttonStatusService = inject(ButtonStatusService);
    private _emailTestService = inject(EmailTestService);
    private _diskUsageService = inject(DiskUsageService);
    private _systemPowerService = inject(SystemPowerService);
    private _configService = inject(ConfigService);
    private _servoService = inject(ServoCalibrationService);
    private _userService = inject(UserService);
    private _toastService = inject(ToastService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    public backEndVersion: string;
    public backEndBuildTime: string;
    public backEndVersionOnError: boolean;

    public upButton: ButtonState = { error: false };
    public bottomButton: ButtonState = { error: false };
    public birdhouseButton: ButtonState = { error: false };

    public isAuthenticated = false;
    public isAdmin = false;
    public emailTestSending = false;
    public powerActionInFlight = false;
    public configRefreshing = false;

    // Servo calibration state
    public servoOpeningPosition = 16;
    public servoClosingPosition = 5;
    public servoSaving = false;
    public servoNudgeMs = 100;
    public servoNudging = false;

    public diskUsage?: DiskUsage;
    public diskUsageError = false;
    public diskUsageLoading = false;

    notificationSubject: Subject<void> = new Subject<void>();
    private destroy$ = new Subject<void>();

    ngOnInit() {
        this.createSubscriptionToBackendVersion();
        this.loadInitialButtonStatus();
        this.subscribeToButtonUpdates();
        this.subscribeToAuthState();
    }

    ngOnDestroy(): void {
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
        if (this.upButton.error || this.bottomButton.error) {
            this.loadInitialButtonStatus();
        }
        this.changeDetectorRef.detectChanges();
    }

    private loadInitialButtonStatus(): void {
        this._buttonStatusService
            .getInitialStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe(
                statuses => {
                    statuses.forEach(s => this.applyStatus(s));
                    this.upButton.error = false;
                    this.bottomButton.error = false;
                    this.birdhouseButton.error = false;
                    this.changeDetectorRef.detectChanges();
                },
                () => {
                    this.upButton = { error: true };
                    this.bottomButton = { error: true };
                    this.birdhouseButton = { error: true };
                    this.notificationSubject.next();
                    this.changeDetectorRef.detectChanges();
                }
            );
    }

    private subscribeToButtonUpdates(): void {
        this._buttonStatusService
            .observeUpdates()
            .pipe(takeUntil(this.destroy$))
            .subscribe(update => {
                this.applyStatus(update);
                this.changeDetectorRef.detectChanges();
            });
    }

    private applyStatus(status: ButtonStatus): void {
        const target = this.targetFor(status.button);
        target.pressed = status.pressed;
        target.timestamp = status.timestamp;
        target.error = false;
    }

    private targetFor(button: ButtonName): ButtonState {
        if (button === 'UP') return this.upButton;
        if (button === 'BIRDHOUSE') return this.birdhouseButton;
        return this.bottomButton;
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
                this.loadDiskUsage();
                this.loadServoPositions();
            }
            this.changeDetectorRef.detectChanges();
        });
    }

    private loadServoPositions(): void {
        this._configService
            .getAll()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: cfg => {
                    this.servoOpeningPosition = cfg.servo_positions.door_opening_position;
                    this.servoClosingPosition = cfg.servo_positions.door_closing_position;
                    this.changeDetectorRef.detectChanges();
                },
                error: () => {
                    /* keep defaults */
                },
            });
    }

    public saveOpeningPosition(): void {
        if (this.servoSaving) return;
        this.servoSaving = true;
        this._configService
            .setDoorOpeningPosition(this.servoOpeningPosition)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => {
                    this.servoSaving = false;
                    this._toastService.success(
                        `Position ouverte : ${this.servoOpeningPosition}`,
                        'Servo'
                    );
                    this.changeDetectorRef.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.servoSaving = false;
                    this._toastService.error(
                        err.error?.message || err.message || 'Save failed',
                        `Servo — HTTP ${err.status}`
                    );
                    this.changeDetectorRef.detectChanges();
                },
            });
    }

    public saveClosingPosition(): void {
        if (this.servoSaving) return;
        this.servoSaving = true;
        this._configService
            .setDoorClosingPosition(this.servoClosingPosition)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => {
                    this.servoSaving = false;
                    this._toastService.success(
                        `Position fermée : ${this.servoClosingPosition}`,
                        'Servo'
                    );
                    this.changeDetectorRef.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.servoSaving = false;
                    this._toastService.error(
                        err.error?.message || err.message || 'Save failed',
                        `Servo — HTTP ${err.status}`
                    );
                    this.changeDetectorRef.detectChanges();
                },
            });
    }

    public nudgeClockwise(): void {
        this.nudge(true);
    }

    public nudgeCounterClockwise(): void {
        this.nudge(false);
    }

    private nudge(clockwise: boolean): void {
        if (this.servoNudging) return;
        const ms = Math.max(1, Math.min(30000, this.servoNudgeMs || 100));
        this.servoNudging = true;
        const call$ = clockwise
            ? this._servoService.turnClockwise(ms)
            : this._servoService.turnCounterClockwise(ms);
        call$.pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.servoNudging = false;
                this._toastService.success(
                    `Servo ${clockwise ? 'clockwise' : 'counter-clockwise'} ${ms} ms`,
                    'Servo'
                );
                this.changeDetectorRef.detectChanges();
            },
            error: (err: HttpErrorResponse) => {
                this.servoNudging = false;
                this._toastService.error(
                    err.error?.message || err.message || 'Nudge failed',
                    `Servo — HTTP ${err.status}`
                );
                this.changeDetectorRef.detectChanges();
            },
        });
    }

    public loadDiskUsage(): void {
        this.diskUsageLoading = true;
        this.diskUsageError = false;
        this._diskUsageService
            .getDiskUsage()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (data: DiskUsage) => {
                    this.diskUsage = data;
                    this.diskUsageError = false;
                    this.diskUsageLoading = false;
                    this.changeDetectorRef.detectChanges();
                },
                error: () => {
                    this.diskUsage = undefined;
                    this.diskUsageError = true;
                    this.diskUsageLoading = false;
                    this.changeDetectorRef.detectChanges();
                },
            });
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
        const confirmMsg = $localize`:@@systemRebootConfirm:Reboot the Raspberry Pi now? The application will be unavailable for ~30 seconds.`;
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

    public sendTestEmail(): void {
        if (this.emailTestSending) {
            return;
        }
        this.emailTestSending = true;
        this.changeDetectorRef.detectChanges();
        this._emailTestService
            .sendTestEmail()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: response => {
                    this.emailTestSending = false;
                    this._toastService.success(response.message || 'Test email sent.', 'Email');
                    this.changeDetectorRef.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.emailTestSending = false;
                    const detail = err.error?.message || err.message || 'Unknown error';
                    this._toastService.error(detail, `Email — HTTP ${err.status}`);
                    this.changeDetectorRef.detectChanges();
                },
            });
    }
}
