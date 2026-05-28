import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DoorService, DoorStatus } from './door.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { User } from '@modules/auth/models';

describe('DoorService', () => {
    let service: DoorService;
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
                DoorService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        service = TestBed.inject(DoorService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getDoorStatus', () => {
        it('should return door status', () => {
            const _mockResponse: DoorStatus = {
                status: 'OPEN',
                timeStatusHasChanged: '2025-12-04T10:00:00Z',
                timeStatusHasChangedAsDate: new Date('2025-12-04T10:00:00Z'),
            };

            service.getDoorStatus().subscribe(response => {
                expect(response.status).toBe('OPEN');
                expect(response.timeStatusHasChanged).toBe('2025-12-04T10:00:00Z');
                expect(response.timeStatusHasChangedAsDate).toBeInstanceOf(Date);
            });

            const req = httpMock.expectOne(request => request.url.includes('/door/status'));
            expect(req.request.method).toBe('GET');
            req.flush({
                status: 'OPEN',
                timeStatusHasChanged: '2025-12-04T10:00:00Z',
            });
        });

        it('should convert timeStatusHasChanged string to Date object', () => {
            const timeString = '2025-12-04T15:30:00Z';

            service.getDoorStatus().subscribe(response => {
                expect(response.timeStatusHasChangedAsDate).toBeInstanceOf(Date);
                expect(response.timeStatusHasChangedAsDate.toISOString()).toContain(
                    '2025-12-04T15:30:00'
                );
            });

            const req = httpMock.expectOne(request => request.url.includes('/door/status'));
            req.flush({
                status: 'CLOSED',
                timeStatusHasChanged: timeString,
            });
        });

        it('should handle CLOSED status', () => {
            service.getDoorStatus().subscribe(response => {
                expect(response.status).toBe('CLOSED');
            });

            const req = httpMock.expectOne(request => request.url.includes('/door/status'));
            req.flush({
                status: 'CLOSED',
                timeStatusHasChanged: '2025-12-04T10:00:00Z',
            });
        });

        it('should include proper headers', () => {
            service.getDoorStatus().subscribe();

            const req = httpMock.expectOne(request => request.url.includes('/door/status'));
            expect(req.request.headers).toBeDefined();
            req.flush({
                status: 'OPEN',
                timeStatusHasChanged: '2025-12-04T10:00:00Z',
            });
        });
    });

    describe('closeDoor', () => {
        it('should send POST request to close door', () => {
            service.closeDoor(mockUser).subscribe(response => {
                expect(response).toBeDefined();
            });

            const req = httpMock.expectOne(request => request.url.includes('/door/close'));
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toBeNull();
            req.flush({ success: true });
        });

        it('should handle successful response', () => {
            const mockResponse = { success: true, message: 'Door closed' };

            service.closeDoor(mockUser).subscribe(response => {
                expect(response).toEqual(mockResponse);
            });

            const req = httpMock.expectOne(request => request.url.includes('/door/close'));
            req.flush(mockResponse);
        });
    });

    describe('openDoor', () => {
        it('should send POST request to open door', () => {
            service.openDoor(mockUser).subscribe(response => {
                expect(response).toBeDefined();
            });

            const req = httpMock.expectOne(request => request.url.includes('/door/open'));
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toBeNull();
            req.flush({ success: true });
        });


        it('should handle successful response', () => {
            const mockResponse = { success: true, message: 'Door opened' };

            service.openDoor(mockUser).subscribe(response => {
                expect(response).toEqual(mockResponse);
            });

            const req = httpMock.expectOne(request => request.url.includes('/door/open'));
            req.flush(mockResponse);
        });
    });
});
