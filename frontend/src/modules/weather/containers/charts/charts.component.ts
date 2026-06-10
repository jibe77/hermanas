import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { ToastService } from '@common/services';
import { ConfigService } from '@modules/energy/services/config.service';
import { UserService } from '@modules/auth/services';
import { WeatherTestService } from '@modules/weather/services/weather-test.service';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CommonCardsComponent } from '../../../app-common/components/common-cards/common-cards.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { WeatherTableAreaComponent } from '../../components/weather-table-area/weather-table-area.component';
import { MonthlyTrendChartComponent } from '../../components/monthly-trend-chart/monthly-trend-chart.component';

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CommonCardsComponent,
        CardComponent,
        FaIconComponent,
        FormsModule,
        WeatherTableAreaComponent,
        MonthlyTrendChartComponent,
    ],
})
export class ChartsComponent implements OnInit, OnDestroy {
    private _configService = inject(ConfigService);
    private _userService = inject(UserService);
    private _toastService = inject(ToastService);
    private _weatherTestService = inject(WeatherTestService);
    private cdr = inject(ChangeDetectorRef);

    notificationSubject: Subject<void> = new Subject<void>();
    retrySubject: Subject<void> = new Subject<void>();

    isAdmin = false;

    weatherUrl = '';
    weatherKeyInput = '';
    weatherKeySet = false;
    weatherKeyLength = 0;
    weatherSaving = false;
    weatherTesting = false;
    /**
     * Persistent badge shown next to the "Test configuration" button:
     *   'idle'    — no test run yet, no badge.
     *   'success' — last test came back ok; badge shows duration.
     *   'failure' — last test failed; badge shows the short error.
     * Reset to 'idle' on any field change so a stale outcome never lingers
     * once the admin types something different.
     */
    weatherTestResult: 'idle' | 'success' | 'failure' = 'idle';
    weatherTestDurationMs: number | null = null;
    weatherTestError = '';
    /** Write-only: the backend never returns lat/long (sensitive). Empty unless
     * the admin types a new value in the form. */
    latitudeInput: number | null = null;
    longitudeInput: number | null = null;

    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        // Re-evaluate isAdmin on every user$ emission so the admin-only
        // provider-settings panel appears/disappears when the operator
        // logs in or out without leaving this page.
        this._userService.user$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            const wasAdmin = this.isAdmin;
            this.isAdmin = this._userService.isAdmin();
            if (this.isAdmin && !wasAdmin) {
                this.loadWeatherSettings();
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Calls POST /api/v1/weather/test with whatever the admin currently typed
     * in the form (or stored values, if the inputs are blank). Reports the
     * outcome via a toast — never persists the values.
     */
    testWeatherSettings(): void {
        if (this.weatherTesting) {
            return;
        }
        this.weatherTesting = true;
        // Reset the previous outcome so the badge briefly disappears during
        // the call — clearer than showing yesterday's success while we re-test.
        this.weatherTestResult = 'idle';
        this.weatherTestError = '';
        this.weatherTestDurationMs = null;
        const payload = {
            url: this.weatherUrl?.trim() || undefined,
            key: this.weatherKeyInput?.trim() || undefined,
            latitude: this.latitudeInput ?? undefined,
            longitude: this.longitudeInput ?? undefined,
        };
        const title = $localize`:@@weatherToastTitle:Weather`;
        this._weatherTestService
            .test(payload)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: result => {
                    this.weatherTesting = false;
                    if (result.ok) {
                        this.weatherTestResult = 'success';
                        this.weatherTestDurationMs = result.durationMs ?? null;
                        this._toastService.success(
                            $localize`:@@weatherTestOk:Connection successful (${result.durationMs}:ms: ms)`,
                            title
                        );
                    } else {
                        this.weatherTestResult = 'failure';
                        this.weatherTestError =
                            result.message ||
                            $localize`:@@weatherTestKo:Connection failed.`;
                        this._toastService.error(this.weatherTestError, title);
                    }
                    this.cdr.markForCheck();
                },
                error: (err: HttpErrorResponse) => {
                    this.weatherTesting = false;
                    this.weatherTestResult = 'failure';
                    this.weatherTestError =
                        err.error?.message ||
                        err.message ||
                        $localize`:@@weatherTestKo:Connection failed.`;
                    this._toastService.error(
                        this.weatherTestError,
                        `${title} — HTTP ${err.status}`
                    );
                    this.cdr.markForCheck();
                },
            });
    }

    /**
     * Called by every `(ngModelChange)` on the provider/key/lat/long inputs so
     * a previous outcome never lingers once the admin has typed something new.
     */
    onWeatherFieldChange(): void {
        if (this.weatherTestResult !== 'idle') {
            this.weatherTestResult = 'idle';
            this.weatherTestError = '';
            this.weatherTestDurationMs = null;
        }
    }

    onServiceCommunicationError(_event: unknown) {
        this.notificationSubject.next();
    }

    onServiceRetry(_event: unknown) {
        this.retrySubject.next();
    }

    private loadWeatherSettings(): void {
        this._configService
            .getAll()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: cfg => {
                    this.weatherUrl = cfg.weather_settings.url;
                    this.weatherKeySet = cfg.weather_settings.key_set;
                    this.weatherKeyLength = cfg.weather_settings.key_length;
                    this.cdr.detectChanges();
                },
                error: () => {
                    /* silent — admin can retry */
                },
            });
    }

    saveWeatherSettings(): void {
        if (this.weatherSaving) return;
        this.weatherSaving = true;
        const tail: Observable<unknown>[] = [];
        if (this.weatherKeyInput && this.weatherKeyInput.trim().length > 0) {
            tail.push(this._configService.setWeatherKey(this.weatherKeyInput.trim()));
        }
        if (this.latitudeInput !== null && !Number.isNaN(this.latitudeInput)) {
            tail.push(this._configService.setLatitude(this.latitudeInput));
        }
        if (this.longitudeInput !== null && !Number.isNaN(this.longitudeInput)) {
            tail.push(this._configService.setLongitude(this.longitudeInput));
        }
        this._configService
            .setWeatherUrl(this.weatherUrl)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => {
                    if (tail.length === 0) {
                        this.onWeatherSaveSuccess(false);
                        return;
                    }
                    forkJoin(tail)
                        .pipe(takeUntil(this.destroy$))
                        .subscribe({
                            next: () =>
                                this.onWeatherSaveSuccess(
                                    !!this.weatherKeyInput &&
                                        this.weatherKeyInput.trim().length > 0
                                ),
                            error: (err: HttpErrorResponse) => this.onWeatherSaveError(err),
                        });
                },
                error: (err: HttpErrorResponse) => this.onWeatherSaveError(err),
            });
    }

    private onWeatherSaveSuccess(keyUpdated: boolean): void {
        this.weatherSaving = false;
        if (keyUpdated) {
            this.weatherKeyInput = '';
            this.weatherKeySet = true;
        }
        // Latitude / longitude are write-only — clear the inputs after save
        // so a stale value is not re-submitted on the next click.
        this.latitudeInput = null;
        this.longitudeInput = null;
        this._toastService.success(
            keyUpdated ? 'Réglages météo et clé enregistrés' : 'URL météo enregistrée',
            'Météo'
        );
        this.loadWeatherSettings();
    }

    private onWeatherSaveError(err: HttpErrorResponse): void {
        this.weatherSaving = false;
        this._toastService.error(
            err.error?.message || err.message || 'Save failed',
            `Météo — HTTP ${err.status}`
        );
        this.cdr.detectChanges();
    }
}
