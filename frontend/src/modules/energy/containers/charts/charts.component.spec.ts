import { ChangeDetectorRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ToastService } from '@common/services';
import {
    EnergyMode,
    EnergyModeConfig,
    EnergyService,
} from '@modules/energy/services/energy.service';
import { ConfigService } from '@modules/energy/services/config.service';

import { ChartsComponent } from './charts.component';

const baseConfig: EnergyModeConfig = {
    energyMode: 'REGULAR',
    wifiDisabled: false,
    durationOfFanInMilliseconds: 1200000, // 20 min
    durationOfLightInMilliseconds: 3600000, // 60 min
    durationOfMusicInMilliseconds: 4800000, // 80 min
};

const sampleMonthlyMapping = {
    1: 'ECO' as const,
    2: 'ECO' as const,
    3: 'REGULAR' as const,
    4: 'SUNNY' as const,
    5: 'SUNNY' as const,
    6: 'SUNNY' as const,
    7: 'SUNNY' as const,
    8: 'SUNNY' as const,
    9: 'SUNNY' as const,
    10: 'REGULAR' as const,
    11: 'ECO' as const,
    12: 'ECO' as const,
};

describe('Energy ChartsComponent', () => {
    let component: ChartsComponent;
    let mockEnergyService: {
        getCurrentMode: ReturnType<typeof vi.fn>;
        getConfig: ReturnType<typeof vi.fn>;
        updateConfig: ReturnType<typeof vi.fn>;
        updateMonthlyMapping: ReturnType<typeof vi.fn>;
        setEcoForced: ReturnType<typeof vi.fn>;
    };
    let mockToastService: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
    let mockConfigService: {
        getAll: ReturnType<typeof vi.fn>;
        setLightOnBeforeSunset: ReturnType<typeof vi.fn>;
        setDoorCloseAfterSunset: ReturnType<typeof vi.fn>;
        setDoorOpenAfterSunrise: ReturnType<typeof vi.fn>;
    };

    beforeEach(() => {
        const currentMode: EnergyMode = {
            currentMode: 'ECO',
            forced: false,
            monthlyMapping: sampleMonthlyMapping,
        };

        mockEnergyService = {
            getCurrentMode: vi.fn().mockReturnValue(of(currentMode)),
            getConfig: vi
                .fn()
                .mockImplementation((mode: 'ECO' | 'SUNNY' | 'REGULAR') =>
                    of({ ...baseConfig, energyMode: mode })
                ),
            updateConfig: vi.fn().mockReturnValue(of(undefined)),
            updateMonthlyMapping: vi.fn().mockReturnValue(of(undefined)),
            setEcoForced: vi.fn().mockReturnValue(of(undefined)),
        };

        mockToastService = { success: vi.fn(), error: vi.fn() };

        mockConfigService = {
            getAll: vi.fn().mockReturnValue(
                of({
                    light_timers: {},
                    fan_timers: {},
                    music_timers: {},
                    consumption_mode: { monthly_mapping: {}, eco_mode_forced: false },
                    sun_offsets: {
                        light_on_minutes_before_sunset: 15,
                        door_close_minutes_after_sunset: 45,
                        door_open_minutes_after_sunrise: 0,
                    },
                })
            ),
            setLightOnBeforeSunset: vi.fn().mockReturnValue(of('ok')),
            setDoorCloseAfterSunset: vi.fn().mockReturnValue(of('ok')),
            setDoorOpenAfterSunrise: vi.fn().mockReturnValue(of('ok')),
        };

        TestBed.configureTestingModule({
            providers: [
                ChartsComponent,
                { provide: EnergyService, useValue: mockEnergyService },
                { provide: ConfigService, useValue: mockConfigService },
                { provide: ToastService, useValue: mockToastService },
                {
                    provide: ChangeDetectorRef,
                    useValue: { markForCheck: vi.fn(), detectChanges: vi.fn() },
                },
            ],
        });

        component = TestBed.inject(ChartsComponent);
    });

    describe('initial load (ngOnInit / reload)', () => {
        it('fans out one /currentMode and three /configMode calls', () => {
            component.ngOnInit();

            expect(mockEnergyService.getCurrentMode).toHaveBeenCalledTimes(1);
            expect(mockEnergyService.getConfig).toHaveBeenCalledWith('ECO');
            expect(mockEnergyService.getConfig).toHaveBeenCalledWith('REGULAR');
            expect(mockEnergyService.getConfig).toHaveBeenCalledWith('SUNNY');
        });

        it('populates the months grid from the backend mapping', () => {
            component.ngOnInit();

            expect(component.months).toHaveLength(12);
            expect(component.months[0]).toEqual({ month: 1, label: 'Janvier', mode: 'ECO' });
            expect(component.months[2]).toEqual({ month: 3, label: 'Mars', mode: 'REGULAR' });
            expect(component.months[5]).toEqual({ month: 6, label: 'Juin', mode: 'SUNNY' });
            expect(component.months[9]).toEqual({ month: 10, label: 'Octobre', mode: 'REGULAR' });
        });

        it('converts ms durations to minutes on load', () => {
            component.ngOnInit();

            expect(component.configs.ECO.durationOfFanInMilliseconds).toBe(20);
            expect(component.configs.REGULAR.durationOfLightInMilliseconds).toBe(60);
            expect(component.configs.SUNNY.durationOfMusicInMilliseconds).toBe(80);
        });

        it('mirrors the "forced" flag into the radio binding', () => {
            mockEnergyService.getCurrentMode.mockReturnValue(
                of({ currentMode: 'ECO', forced: true, monthlyMapping: sampleMonthlyMapping })
            );

            component.ngOnInit();
            expect(component.forcedSelection).toBe('force-eco');
        });

        it('surfaces a toast and stops loading on HTTP failure', () => {
            mockEnergyService.getCurrentMode.mockReturnValue(
                throwError(() => ({ status: 500, message: 'boom' }))
            );

            component.ngOnInit();

            expect(component.loading).toBe(false);
            expect(mockToastService.error).toHaveBeenCalled();
        });
    });

    describe('selectTab', () => {
        it('changes the active mode tab', () => {
            component.selectTab('SUNNY');
            expect(component.selectedTab).toBe('SUNNY');
        });
    });

    describe('save', () => {
        it('persists the three sections in parallel and converts minutes → ms', () => {
            component.ngOnInit();

            // pretend the admin tweaked February and a slider before clicking Save
            component.months[1].mode = 'REGULAR';
            component.selectedTab = 'ECO';
            component.configs.ECO.durationOfFanInMilliseconds = 25; // minutes

            component.save();

            expect(mockEnergyService.setEcoForced).toHaveBeenCalledWith(false);

            const mappingArg = mockEnergyService.updateMonthlyMapping.mock.calls.at(-1)?.[0];
            expect(mappingArg[2]).toBe('REGULAR'); // changed February
            expect(mappingArg[1]).toBe('ECO'); // January untouched

            const configArg = mockEnergyService.updateConfig.mock.calls.at(-1)?.[0];
            expect(configArg.energyMode).toBe('ECO');
            // minutes converted back to milliseconds: 25 * 60_000
            expect(configArg.durationOfFanInMilliseconds).toBe(1500000);
        });

        it('translates the force-eco radio to setEcoForced(true)', () => {
            component.ngOnInit();
            component.forcedSelection = 'force-eco';

            component.save();
            expect(mockEnergyService.setEcoForced).toHaveBeenCalledWith(true);
        });

        it('shows a success toast and reloads on success', () => {
            component.ngOnInit();
            mockEnergyService.getCurrentMode.mockClear();

            component.save();

            expect(mockToastService.success).toHaveBeenCalled();
            // After saving we reload — the second /currentMode hit confirms it.
            expect(mockEnergyService.getCurrentMode).toHaveBeenCalled();
        });
    });
});
