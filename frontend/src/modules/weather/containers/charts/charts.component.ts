import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { ToastService } from '@common/services';
import { ConfigService } from '@modules/energy/services/config.service';
import { UserService } from '@modules/auth/services';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CommonCardsComponent } from '../../../app-common/components/common-cards/common-cards.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { WeatherTableAreaComponent } from '../../components/weather-table-area/weather-table-area.component';

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
    ],
})
export class ChartsComponent implements OnInit {
    private userService = inject(UserService);
    private configService = inject(ConfigService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    notificationSubject: Subject<void> = new Subject<void>();
    retrySubject: Subject<void> = new Subject<void>();

    isAdmin = false;
    weatherUrl = '';
    weatherKeyInput = '';
    weatherKeySet = false;
    weatherKeyLength = 0;
    weatherSaving = false;

    ngOnInit(): void {
        this.isAdmin = this.userService.isAdmin();
        if (this.isAdmin) {
            this.loadWeatherSettings();
        }
    }

    private loadWeatherSettings(): void {
        this.configService.getAll().subscribe({
            next: cfg => {
                this.weatherUrl = cfg.weather_settings.url;
                this.weatherKeySet = cfg.weather_settings.key_set;
                this.weatherKeyLength = cfg.weather_settings.key_length;
                this.cdr.markForCheck();
            },
            error: () => {
                /* silent — admin can retry */
            },
        });
    }

    /**
     * Two-step save: always save the URL (cheap, no secret), then optionally save the
     * key if the admin typed something in. Leaving the key field empty means "keep the
     * existing key" — matches the password-field placeholder.
     */
    saveWeatherSettings(): void {
        if (this.weatherSaving) {
            return;
        }
        this.weatherSaving = true;
        this.configService.setWeatherUrl(this.weatherUrl).subscribe({
            next: () => {
                if (this.weatherKeyInput && this.weatherKeyInput.trim().length > 0) {
                    this.configService.setWeatherKey(this.weatherKeyInput.trim()).subscribe({
                        next: () => this.onWeatherSaveSuccess(true),
                        error: (err: HttpErrorResponse) => this.onWeatherSaveError(err),
                    });
                } else {
                    this.onWeatherSaveSuccess(false);
                }
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
        this.toast.success(
            keyUpdated ? 'Réglages météo et clé enregistrés' : 'URL météo enregistrée',
            'Météo'
        );
        this.loadWeatherSettings();
    }

    private onWeatherSaveError(err: HttpErrorResponse): void {
        this.weatherSaving = false;
        this.toast.error(
            err.error?.message || err.message || 'Save failed',
            `Météo — HTTP ${err.status}`
        );
        this.cdr.markForCheck();
    }

    onServiceCommunicationError(_event: unknown) {
        this.notificationSubject.next();
    }

    onServiceRetry(_event: unknown) {
        this.retrySubject.next();
    }
}
