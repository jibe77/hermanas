import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { forkJoin } from 'rxjs';
import { ToastService } from '@common/services';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import {
    ConfigService,
    DoorForceSchedule,
    SunOffsets,
} from '@modules/energy/services/config.service';
import { SystemTimeService } from '@modules/system/services';

@Component({
    selector: 'sb-scheduler',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './scheduler.component.html',
    styleUrls: ['scheduler.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CardComponent,
        FaIconComponent,
        ReactiveFormsModule,
        FormsModule,
    ],
})
export class SchedulerComponent implements OnInit, OnDestroy {
    private configService = inject(ConfigService);
    private systemTimeService = inject(SystemTimeService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    loading = true;
    saving = false;
    savingSun = false;

    /** Two-way bound model for the two force blocks. */
    force: DoorForceSchedule = {
        opening_enabled: false,
        opening_time: '08:00',
        closing_enabled: false,
        closing_time: '20:00',
    };

    /** Two-way bound model for the sun-relative offsets block. */
    sunOffsets: SunOffsets = {
        light_on_minutes_before_sunset: 0,
        door_close_minutes_after_sunset: 0,
        door_open_minutes_after_sunrise: 0,
    };

    /**
     * Coop clock displayed at the top of the page. We fetch once, then tick
     * locally every second by adding {@code Date.now() - browserSyncMs} to
     * {@code serverEpochMs}. Every 60 s we re-sync from the endpoint to
     * absorb clock drift and avoid caching stale values across long sessions.
     */
    coopTimeLabel = '';
    coopZoneId = '';
    /** Server epoch ms captured at last sync. */
    private serverEpochMs = 0;
    /** Browser Date.now() captured at last sync — used as the reference for the local tick. */
    private browserSyncMs = 0;
    private tickHandle: ReturnType<typeof setInterval> | null = null;
    private resyncHandle: ReturnType<typeof setInterval> | null = null;

    ngOnInit(): void {
        this.reload();
        this.syncCoopTime();
        this.tickHandle = setInterval(() => this.tickCoopTime(), 1000);
        this.resyncHandle = setInterval(() => this.syncCoopTime(), 60000);
    }

    ngOnDestroy(): void {
        if (this.tickHandle !== null) {
            clearInterval(this.tickHandle);
        }
        if (this.resyncHandle !== null) {
            clearInterval(this.resyncHandle);
        }
    }

    private syncCoopTime(): void {
        this.systemTimeService.getSystemTime().subscribe({
            next: t => {
                this.serverEpochMs = t.epochMs;
                this.browserSyncMs = Date.now();
                this.coopZoneId = t.zoneId;
                this.tickCoopTime();
            },
            error: () => {
                // Silent: the widget is a nice-to-have. Keep the last label
                // rather than surface a scary error toast that would fire
                // again every 60 s until connectivity comes back.
            },
        });
    }

    private tickCoopTime(): void {
        if (this.serverEpochMs === 0) {
            return;
        }
        const nowOnCoop = new Date(this.serverEpochMs + (Date.now() - this.browserSyncMs));
        this.coopTimeLabel = nowOnCoop.toLocaleTimeString(undefined, {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
        });
        this.cdr.markForCheck();
    }

    reload(): void {
        this.loading = true;
        this.configService.getAll().subscribe({
            next: config => {
                this.force = { ...config.door_force_schedule };
                this.sunOffsets = { ...config.sun_offsets };
                this.loading = false;
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.loading = false;
                this.toast.error(
                    err.error?.message || err.message || 'Cannot load scheduler state',
                    `Scheduler — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    saveSunOffsets(): void {
        if (this.savingSun) {
            return;
        }
        this.savingSun = true;
        forkJoin({
            lightOn: this.configService.setLightOnBeforeSunset(
                this.sunOffsets.light_on_minutes_before_sunset
            ),
            doorClose: this.configService.setDoorCloseAfterSunset(
                this.sunOffsets.door_close_minutes_after_sunset
            ),
            doorOpen: this.configService.setDoorOpenAfterSunrise(
                this.sunOffsets.door_open_minutes_after_sunrise
            ),
        }).subscribe({
            next: () => {
                this.savingSun = false;
                this.toast.success(
                    $localize`:@@schedulerSunOffsetsSaved:Sun-related offsets saved`,
                    $localize`:@@schedulerToastTitle:Scheduler`
                );
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.savingSun = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Scheduler — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    save(): void {
        if (this.saving) {
            return;
        }
        this.saving = true;
        forkJoin({
            openingEnabled: this.configService.setDoorOpeningForceEnabled(
                this.force.opening_enabled
            ),
            openingTime: this.configService.setDoorOpeningForceTime(this.force.opening_time),
            closingEnabled: this.configService.setDoorClosingForceEnabled(
                this.force.closing_enabled
            ),
            closingTime: this.configService.setDoorClosingForceTime(this.force.closing_time),
        }).subscribe({
            next: () => {
                this.saving = false;
                this.toast.success(
                    $localize`:@@schedulerSaved:Schedule saved`,
                    $localize`:@@schedulerToastTitle:Scheduler`
                );
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.saving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Scheduler — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }
}
