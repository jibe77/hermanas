import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
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
} from '@modules/energy/services/config.service';

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
export class SchedulerComponent implements OnInit {
    private configService = inject(ConfigService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    loading = true;
    saving = false;

    /** Two-way bound model for the two force blocks. */
    force: DoorForceSchedule = {
        opening_enabled: false,
        opening_time: '08:00',
        closing_enabled: false,
        closing_time: '20:00',
    };

    ngOnInit(): void {
        this.reload();
    }

    reload(): void {
        this.loading = true;
        this.configService.getAll().subscribe({
            next: config => {
                this.force = { ...config.door_force_schedule };
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
