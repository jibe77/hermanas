import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EnergyMode, EnergyModeConfig, EnergyService } from './energy.service';

describe('EnergyService', () => {
    let service: EnergyService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(EnergyService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('getCurrentMode', () => {
        it('GETs /energy/currentMode and forwards the payload as-is', async () => {
            const payload: EnergyMode = {
                currentMode: 'ECO',
                forced: false,
                monthlyMapping: {
                    1: 'ECO',
                    2: 'ECO',
                    3: 'REGULAR',
                    4: 'SUNNY',
                    5: 'SUNNY',
                    6: 'SUNNY',
                    7: 'SUNNY',
                    8: 'SUNNY',
                    9: 'SUNNY',
                    10: 'REGULAR',
                    11: 'ECO',
                    12: 'ECO',
                },
            };
            const result = new Promise<EnergyMode>(resolve =>
                service.getCurrentMode().subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/energy/currentMode'));
            expect(req.request.method).toBe('GET');
            req.flush(payload);

            await expect(result).resolves.toEqual(payload);
        });
    });

    describe('getConfig', () => {
        it('passes the mode as ?energyMode= query string', async () => {
            const result = new Promise<EnergyModeConfig>(resolve =>
                service.getConfig('SUNNY').subscribe(resolve)
            );

            const req = httpMock.expectOne(
                r => r.url.endsWith('/energy/configMode') && r.params.get('energyMode') === 'SUNNY'
            );
            expect(req.request.method).toBe('GET');
            req.flush({
                energyMode: 'SUNNY',
                wifiDisabled: false,
                durationOfFanInMilliseconds: 1200000,
                durationOfLightInMilliseconds: 3600000,
                durationOfMusicInMilliseconds: 4800000,
            } as EnergyModeConfig);

            await result;
        });
    });

    describe('updateConfig', () => {
        it('PUTs the full config body to /energy/updateMode', async () => {
            const config: EnergyModeConfig = {
                energyMode: 'REGULAR',
                wifiDisabled: false,
                durationOfFanInMilliseconds: 600000,
                durationOfLightInMilliseconds: 1800000,
                durationOfMusicInMilliseconds: 2400000,
            };
            const result = new Promise<void>(resolve =>
                service.updateConfig(config).subscribe(() => resolve())
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/energy/updateMode'));
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual(config);
            req.flush(null);

            await result;
        });
    });

    describe('updateMonthlyMapping', () => {
        it('PUTs the 12-entry month → mode record', async () => {
            const mapping = {
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
            const result = new Promise<void>(resolve =>
                service.updateMonthlyMapping(mapping).subscribe(() => resolve())
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/energy/monthlyMapping'));
            expect(req.request.method).toBe('PUT');
            expect(req.request.body).toEqual(mapping);
            req.flush(null);

            await result;
        });
    });

    describe('setEcoForced', () => {
        it('PUTs /energy/forceEco with the flag as a query parameter', async () => {
            const result = new Promise<void>(resolve =>
                service.setEcoForced(true).subscribe(() => resolve())
            );

            const req = httpMock.expectOne(
                r => r.url.endsWith('/energy/forceEco') && r.params.get('forced') === 'true'
            );
            expect(req.request.method).toBe('PUT');
            // The endpoint takes the flag in the query string, no body.
            expect(req.request.body).toBeNull();
            req.flush(null);

            await result;
        });

        it('serialises false as "false"', async () => {
            const result = new Promise<void>(resolve =>
                service.setEcoForced(false).subscribe(() => resolve())
            );

            const req = httpMock.expectOne(
                r => r.url.endsWith('/energy/forceEco') && r.params.get('forced') === 'false'
            );
            req.flush(null);

            await result;
        });
    });
});
