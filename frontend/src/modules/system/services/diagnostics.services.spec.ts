import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DiskUsage, DiskUsageService } from './disk-usage.service';
import { EmailTestResponse, EmailTestService } from './email-test.service';
import { VersionInfo, VersionService } from './version.service';

describe('Diagnostics services (Version / Email / DiskUsage)', () => {
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    describe('VersionService', () => {
        it('GETs /info and forwards the JSON payload as-is', async () => {
            const payload: VersionInfo = {
                time: '2026-05-28T12:00:00Z',
                version: '0.8.1',
                artifact: 'hermanas',
                group: 'org.jibe77',
                name: 'hermanas',
            };
            const service = TestBed.inject(VersionService);
            const result = new Promise<VersionInfo>(resolve =>
                service.getVersionInfo().subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/info'));
            expect(req.request.method).toBe('GET');
            req.flush(payload);

            await expect(result).resolves.toEqual(payload);
        });
    });

    describe('EmailTestService', () => {
        it('POSTs /email/test with an empty body', async () => {
            const service = TestBed.inject(EmailTestService);
            const result = new Promise<EmailTestResponse>(resolve =>
                service.sendTestEmail().subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/email/test'));
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual({});
            req.flush({ message: 'Test email sent.' });

            await expect(result).resolves.toEqual({ message: 'Test email sent.' });
        });
    });

    describe('DiskUsageService', () => {
        it('GETs /system/disk-usage and forwards the payload', async () => {
            const payload: DiskUsage = {
                path: '/home/pi',
                totalBytes: 16_000_000_000,
                usedBytes: 4_000_000_000,
                freeBytes: 12_000_000_000,
                usedPercent: 25,
            };
            const service = TestBed.inject(DiskUsageService);
            const result = new Promise<DiskUsage>(resolve =>
                service.getDiskUsage().subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/system/disk-usage'));
            expect(req.request.method).toBe('GET');
            req.flush(payload);

            await expect(result).resolves.toEqual(payload);
        });
    });
});
