import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MeteoInfo, MeteoService } from '@modules/dashboard/services/meteo.service';

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('MeteoService', () => {
    let service: MeteoService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                MeteoService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        service = TestBed.inject(MeteoService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getMeteoInfo', () => {
        it('should return meteo information', () => {
            const mockMeteoInfo: MeteoInfo = {
                temperature: '22.5',
                externalTemperature: 18.3,
                humidity: 65,
                externalHumidity: 72,
                dateTime: '2025-12-04T10:00:00Z',
            };

            service.getMeteoInfo().subscribe(response => {
                expect(response).toEqual(mockMeteoInfo);
                expect(response.temperature).toBe('22.5');
                expect(response.externalTemperature).toBe(18.3);
                expect(response.humidity).toBe(65);
                expect(response.externalHumidity).toBe(72);
                expect(response.dateTime).toBe('2025-12-04T10:00:00Z');
            });

            const req = httpMock.expectOne(request => request.url.includes('/sensor/info'));
            expect(req.request.method).toBe('GET');
            req.flush(mockMeteoInfo);
        });

        it('should handle different temperature values', () => {
            const mockMeteoInfo: MeteoInfo = {
                temperature: '15.0',
                externalTemperature: 10.5,
                humidity: 50,
                externalHumidity: 60,
                dateTime: '2025-12-04T12:00:00Z',
            };

            service.getMeteoInfo().subscribe(response => {
                expect(response.temperature).toBe('15.0');
                expect(response.externalTemperature).toBe(10.5);
            });

            const req = httpMock.expectOne(request => request.url.includes('/sensor/info'));
            req.flush(mockMeteoInfo);
        });

        it('should handle different humidity values', () => {
            const mockMeteoInfo: MeteoInfo = {
                temperature: '20.0',
                externalTemperature: 18.0,
                humidity: 45,
                externalHumidity: 55,
                dateTime: '2025-12-04T14:00:00Z',
            };

            service.getMeteoInfo().subscribe(response => {
                expect(response.humidity).toBe(45);
                expect(response.externalHumidity).toBe(55);
            });

            const req = httpMock.expectOne(request => request.url.includes('/sensor/info'));
            req.flush(mockMeteoInfo);
        });

        it('should include proper headers', () => {
            service.getMeteoInfo().subscribe();

            const req = httpMock.expectOne(request => request.url.includes('/sensor/info'));
            expect(req.request.headers).toBeDefined();
            req.flush({
                temperature: '20.0',
                externalTemperature: 18.0,
                humidity: 50,
                externalHumidity: 60,
                dateTime: '2025-12-04T10:00:00Z',
            });
        });

        it('should return complete MeteoInfo object', () => {
            const mockMeteoInfo: MeteoInfo = {
                temperature: '25.0',
                externalTemperature: 20.0,
                humidity: 70,
                externalHumidity: 75,
                dateTime: '2025-12-04T16:00:00Z',
            };

            service.getMeteoInfo().subscribe(response => {
                expect(response).toBeDefined();
                expect(typeof response.temperature).toBe('string');
                expect(typeof response.externalTemperature).toBe('number');
                expect(typeof response.humidity).toBe('number');
                expect(typeof response.externalHumidity).toBe('number');
                expect(typeof response.dateTime).toBe('string');
            });

            const req = httpMock.expectOne(request => request.url.includes('/sensor/info'));
            req.flush(mockMeteoInfo);
        });
    });
});
