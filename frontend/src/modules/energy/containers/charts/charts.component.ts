import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    OnInit,
} from '@angular/core';
import { ToastService } from '@common/services';
import {
    EnergyModeConfig,
    EnergyModeEnum,
    EnergyService,
} from '@modules/energy/services/energy.service';
import { ConfigService, SunOffsets } from '@modules/energy/services/config.service';
import { forkJoin } from 'rxjs';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

interface MonthEntry {
    /** 1-12 */
    month: number;
    /** French label for display only. */
    label: string;
    mode: EnergyModeEnum;
}

const FRENCH_MONTHS = [
    'Janvier',
    'Février',
    'Mars',
    'Avril',
    'Mai',
    'Juin',
    'Juillet',
    'Août',
    'Septembre',
    'Octobre',
    'Novembre',
    'Décembre',
];

const FAN_MAX_MINUTES = 60;
const LIGHT_MAX_MINUTES = 180;
const MUSIC_MAX_MINUTES = 180;

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
        ReactiveFormsModule,
        FormsModule,
    ],
})
export class ChartsComponent implements OnInit {
    private energyService = inject(EnergyService);
    private configService = inject(ConfigService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    loading = true;
    saving = false;
    savingSun = false;

    sunOffsets: SunOffsets = {
        light_on_minutes_before_sunset: 0,
        door_close_minutes_after_sunset: 0,
        door_open_minutes_after_sunrise: 0,
    };


    /** Active mode reported by the backend (read-only display). */
    currentMode: EnergyModeEnum = 'REGULAR';

    /** Radio binding: 'auto' or 'force-eco'. */
    forcedSelection: 'auto' | 'force-eco' = 'auto';

    months: MonthEntry[] = FRENCH_MONTHS.map((label, idx) => ({
        month: idx + 1,
        label,
        mode: 'REGULAR' as EnergyModeEnum,
    }));

    /** Tab selection for the per-mode configuration block. */
    selectedTab: EnergyModeEnum = 'ECO';

    /** Cached configs for each mode — minutes, not ms. */
    configs: Record<EnergyModeEnum, EnergyModeConfig> = {
        ECO: this.emptyConfig('ECO'),
        SUNNY: this.emptyConfig('SUNNY'),
        REGULAR: this.emptyConfig('REGULAR'),
    };

    readonly fanMax = FAN_MAX_MINUTES;
    readonly lightMax = LIGHT_MAX_MINUTES;
    readonly musicMax = MUSIC_MAX_MINUTES;
    readonly modes: EnergyModeEnum[] = ['ECO', 'REGULAR', 'SUNNY'];

    /**
     * Translated label for an energy mode. The backend enum values (ECO / REGULAR / SUNNY) are
     * left untouched — only the UI display goes through i18n. New locales just need to provide
     * a translation for the three IDs below.
     */
    modeLabel(mode: EnergyModeEnum): string {
        switch (mode) {
            case 'ECO':
                return $localize`:@@energyModeEco:Economy`;
            case 'REGULAR':
                return $localize`:@@energyModeRegular:Normal`;
            case 'SUNNY':
                return $localize`:@@energyModeSunny:Sunny`;
        }
    }

    ngOnInit(): void {
        this.reload();
    }

    reload(): void {
        this.loading = true;
        forkJoin({
            current: this.energyService.getCurrentMode(),
            eco: this.energyService.getConfig('ECO'),
            regular: this.energyService.getConfig('REGULAR'),
            sunny: this.energyService.getConfig('SUNNY'),
            config: this.configService.getAll(),
        }).subscribe({
            next: result => {
                this.currentMode = result.current.currentMode;
                this.forcedSelection = result.current.forced ? 'force-eco' : 'auto';
                this.months = this.months.map(entry => ({
                    ...entry,
                    mode: result.current.monthlyMapping[entry.month] ?? entry.mode,
                }));
                this.configs = {
                    ECO: this.toMinutes(result.eco),
                    REGULAR: this.toMinutes(result.regular),
                    SUNNY: this.toMinutes(result.sunny),
                };
                this.sunOffsets = result.config.sun_offsets;
                this.loading = false;
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.loading = false;
                this.toast.error(
                    err.error?.message || err.message || 'Cannot load energy state',
                    `Energy — HTTP ${err.status}`
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
                    $localize`:@@energySunOffsetsSaved:Sun-related offsets saved`,
                    $localize`:@@energyToastTitle:Energy`
                );
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.savingSun = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Energy — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    selectTab(mode: EnergyModeEnum): void {
        this.selectedTab = mode;
    }

    /**
     * Single "Save" button → fan out three PUT requests in parallel: force-flag,
     * monthly schedule, and the config currently shown in the tab. If any fails,
     * a toast surfaces the error but the others still apply.
     */
    save(): void {
        if (this.saving) {
            return;
        }
        this.saving = true;
        const monthlyMapping: Record<number, EnergyModeEnum> = {};
        for (const entry of this.months) {
            monthlyMapping[entry.month] = entry.mode;
        }
        const activeConfig = this.toMilliseconds(this.configs[this.selectedTab]);
        const forced = this.forcedSelection === 'force-eco';

        forkJoin({
            force: this.energyService.setEcoForced(forced),
            mapping: this.energyService.updateMonthlyMapping(monthlyMapping),
            config: this.energyService.updateConfig(activeConfig),
        }).subscribe({
            next: () => {
                this.saving = false;
                this.toast.success(
                    $localize`:@@energyConfigSaved:Configuration saved`,
                    $localize`:@@energyToastTitle:Energy`
                );
                this.reload();
            },
            error: (err: HttpErrorResponse) => {
                this.saving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Energy — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    private toMinutes(config: EnergyModeConfig): EnergyModeConfig {
        return {
            ...config,
            durationOfFanInMilliseconds: Math.round(config.durationOfFanInMilliseconds / 60000),
            durationOfLightInMilliseconds: Math.round(config.durationOfLightInMilliseconds / 60000),
            durationOfMusicInMilliseconds: Math.round(config.durationOfMusicInMilliseconds / 60000),
        };
    }

    private toMilliseconds(config: EnergyModeConfig): EnergyModeConfig {
        return {
            ...config,
            durationOfFanInMilliseconds: config.durationOfFanInMilliseconds * 60000,
            durationOfLightInMilliseconds: config.durationOfLightInMilliseconds * 60000,
            durationOfMusicInMilliseconds: config.durationOfMusicInMilliseconds * 60000,
        };
    }

    private emptyConfig(mode: EnergyModeEnum): EnergyModeConfig {
        return {
            energyMode: mode,
            wifiDisabled: false,
            durationOfFanInMilliseconds: 0,
            durationOfLightInMilliseconds: 0,
            durationOfMusicInMilliseconds: 0,
        };
    }
}
