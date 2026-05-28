import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
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
import { VersionInfo, VersionService } from '@modules/system/services/version.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

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
})
export class SystemComponent implements OnInit, OnDestroy {
    public backEndVersion: string;
    public backEndBuildTime: string;
    public backEndVersionOnError: boolean;

    public upButton: ButtonState = { error: false };
    public bottomButton: ButtonState = { error: false };
    public birdhouseButton: ButtonState = { error: false };

    public isAuthenticated = false;
    public isAdmin = false;
    public emailTestSending = false;

    public diskUsage?: DiskUsage;
    public diskUsageError = false;
    public diskUsageLoading = false;

    notificationSubject: Subject<void> = new Subject<void>();
    private destroy$ = new Subject<void>();

    constructor(
        private _versionService: VersionService,
        private _buttonStatusService: ButtonStatusService,
        private _emailTestService: EmailTestService,
        private _diskUsageService: DiskUsageService,
        private _userService: UserService,
        private _toastService: ToastService,
        private changeDetectorRef: ChangeDetectorRef
    ) {}

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
            }
            this.changeDetectorRef.detectChanges();
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
