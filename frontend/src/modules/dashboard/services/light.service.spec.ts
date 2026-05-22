import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LightService, LightStatus } from '@modules/dashboard/services/light.service';

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { User } from '@modules/auth/models';
import { SwitchResponse } from '../models';

describe('LightService', () => {
    let service: LightService;
    let httpMock: HttpTestingController;

    const mockUser: User = {
        login: 'testuser',
        email: 'test@test.com',
        backEndUser: 'admin',
        backEndPassword: 'password',
    } as User;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                LightService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        service = TestBed.inject(LightService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getStatus', () => {
        it('should return light status', () => {
            const mockStatus: LightStatus = {
                statusEnum: 'ON',
                timeOut: 300,
            };

            service.getStatus().subscribe(response => {
                expect(response.statusEnum).toBe('ON');
                expect(response.timeOut).toBe(300);
            });

            const req = httpMock.expectOne(request => request.url.includes('/light/status'));
            expect(req.request.method).toBe('GET');
            req.flush(mockStatus);
        });

        it('should handle OFF status', () => {
            const mockStatus: LightStatus = {
                statusEnum: 'OFF',
                timeOut: 0,
            };

            service.getStatus().subscribe(response => {
                expect(response.statusEnum).toBe('OFF');
                expect(response.timeOut).toBe(0);
            });

            const req = httpMock.expectOne(request => request.url.includes('/light/status'));
            req.flush(mockStatus);
        });

        it('should include proper headers', () => {
            service.getStatus().subscribe();

            const req = httpMock.expectOne(request => request.url.includes('/light/status'));
            expect(req.request.headers).toBeDefined();
            req.flush({ statusEnum: 'ON', timeOut: 300 });
        });
    });

    describe('switch', () => {
        it('should send POST request to switch light ON', () => {
            const mockResponse: SwitchResponse = {
                success: true,
                message: 'Light turned on',
            };

            service.switch(true, mockUser).subscribe(response => {
                expect(response.success).toBe(true);
                expect(response.message).toBe('Light turned on');
            });

            const req = httpMock.expectOne(request =>
                request.url.includes('/light/switch?param=true')
            );
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toBeNull();
            req.flush(mockResponse);
        });

        it('should send POST request to switch light OFF', () => {
            const mockResponse: SwitchResponse = {
                success: true,
                message: 'Light turned off',
            };

            service.switch(false, mockUser).subscribe(response => {
                expect(response.success).toBe(true);
                expect(response.message).toBe('Light turned off');
            });

            const req = httpMock.expectOne(request =>
                request.url.includes('/light/switch?param=false')
            );
            expect(req.request.method).toBe('POST');
            req.flush(mockResponse);
        });

        it('should include auth headers', () => {
            service.switch(true, mockUser).subscribe();

            const req = httpMock.expectOne(request => request.url.includes('/light/switch'));
            expect(req.request.headers.has('Authorization')).toBe(true);
            req.flush({ success: true });
        });

        it('should handle success response', () => {
            const mockResponse: SwitchResponse = {
                success: true,
            };

            service.switch(true, mockUser).subscribe(response => {
                expect(response.success).toBe(true);
            });

            const req = httpMock.expectOne(request => request.url.includes('/light/switch'));
            req.flush(mockResponse);
        });

        it('should handle failure response', () => {
            const mockResponse: SwitchResponse = {
                success: false,
                message: 'Failed to switch light',
            };

            service.switch(true, mockUser).subscribe(response => {
                expect(response.success).toBe(false);
                expect(response.message).toBe('Failed to switch light');
            });

            const req = httpMock.expectOne(request => request.url.includes('/light/switch'));
            req.flush(mockResponse);
        });

        it('should include param in query string', () => {
            service.switch(true, mockUser).subscribe();

            const req = httpMock.expectOne(request => {
                return request.url.includes('/light/switch') && request.url.includes('param=true');
            });
            expect(req.request.url).toContain('param=true');
            req.flush({ success: true });
        });
    });
});
