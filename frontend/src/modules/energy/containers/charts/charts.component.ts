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
import { forkJoin } from 'rxjs';

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
    standalone: false,
})
export class ChartsComponent implements OnInit {
    private energyService = inject(EnergyService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    loading = true;
    saving = false;

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
                this.toast.success('Configuration enregistrée', 'Énergie');
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
            machineShutdown: false,
            wifiDisabled: false,
            durationOfFanInMilliseconds: 0,
            durationOfLightInMilliseconds: 0,
            durationOfMusicInMilliseconds: 0,
        };
    }
}
