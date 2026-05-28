import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WeatherService } from './weather.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { MeteoInfo } from '@modules/dashboard/services';

describe('WeatherService', () => {
    let weatherService: WeatherService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                WeatherService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        weatherService = TestBed.inject(WeatherService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(weatherService).toBeTruthy();
    });

    describe('getInfoUsingDateRange', () => {
        it('should return Observable<MeteoInfo[]>', () => {
            const from = '2021-09-01';
            const to = '2021-10-01';
            const mockData: MeteoInfo[] = [
                {
                    temperature: '20',
                    externalTemperature: 18,
                    humidity: 60,
                    externalHumidity: 55,
                    dateTime: '2021-09-01T10:00:00Z',
                },
            ];

            weatherService.getInfoUsingDateRange(from, to).subscribe(response => {
                expect(response).toEqual(mockData);
                expect(response.length).toBe(1);
            });

            const req = httpMock.expectOne(request =>
                request.url.includes(`/sensor/history/${from}/${to}`)
            );
            expect(req.request.method).toBe('GET');
            req.flush(mockData);
        });

        it('should handle empty results', () => {
            const from = '2021-09-01';
            const to = '2021-09-02';

            weatherService.getInfoUsingDateRange(from, to).subscribe(response => {
                expect(response).toEqual([]);
                expect(response.length).toBe(0);
            });

            const req = httpMock.expectOne(request =>
                request.url.includes(`/sensor/history/${from}/${to}`)
            );
            req.flush([]);
        });

    });
});
