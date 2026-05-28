import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SystemPowerService } from './system-power.service';

describe('SystemPowerService', () => {
    let service: SystemPowerService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [SystemPowerService, provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(SystemPowerService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('shutdown() POSTs to /system/shutdown with no body', async () => {
        const result = new Promise<void>(resolve => service.shutdown().subscribe(() => resolve()));

        const req = httpMock.expectOne(r => r.url.endsWith('/system/shutdown'));
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBeNull();
        req.flush(null);

        await result;
    });

    it('reboot() POSTs to /system/reboot with no body', async () => {
        const result = new Promise<void>(resolve => service.reboot().subscribe(() => resolve()));

        const req = httpMock.expectOne(r => r.url.endsWith('/system/reboot'));
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBeNull();
        req.flush(null);

        await result;
    });
});
