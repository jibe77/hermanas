import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { LogFileInfo, LogsService } from './logs.service';

describe('LogsService', () => {
    let service: LogsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(LogsService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    describe('listFiles', () => {
        it('GETs /logs', async () => {
            const files: LogFileInfo[] = [
                { name: 'hermanas.log', size: 1024, lastModified: 1700000000000 },
            ];
            const result = new Promise<LogFileInfo[]>(resolve =>
                service.listFiles().subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/logs'));
            expect(req.request.method).toBe('GET');
            req.flush(files);

            await expect(result).resolves.toEqual(files);
        });
    });

    describe('tail', () => {
        it('GETs /logs/<filename> without query params when options are empty', async () => {
            new Promise<string[]>(resolve => service.tail('hermanas.log').subscribe(resolve));

            const req = httpMock.expectOne(r => r.url.endsWith('/logs/hermanas.log'));
            expect(req.request.method).toBe('GET');
            expect(req.request.params.keys()).toHaveLength(0);
            req.flush([]);
        });

        it('forwards the lines count', async () => {
            new Promise<string[]>(resolve =>
                service.tail('hermanas.log', { lines: 500 }).subscribe(resolve)
            );

            const req = httpMock.expectOne(
                r => r.url.endsWith('/logs/hermanas.log') && r.params.get('lines') === '500'
            );
            req.flush([]);
        });

        it('forwards a non-ALL level and skips ALL', async () => {
            new Promise<string[]>(resolve =>
                service.tail('hermanas.log', { level: 'WARN' }).subscribe(resolve)
            );
            const req = httpMock.expectOne(
                r => r.url.endsWith('/logs/hermanas.log') && r.params.get('level') === 'WARN'
            );
            req.flush([]);

            new Promise<string[]>(resolve =>
                service.tail('hermanas.log', { level: 'ALL' }).subscribe(resolve)
            );
            const reqAll = httpMock.expectOne(r => r.url.endsWith('/logs/hermanas.log'));
            expect(reqAll.request.params.has('level')).toBe(false);
            reqAll.flush([]);
        });

        it('trims the search string and skips blank input', async () => {
            new Promise<string[]>(resolve =>
                service.tail('hermanas.log', { search: '  door open  ' }).subscribe(resolve)
            );
            const req = httpMock.expectOne(
                r => r.url.endsWith('/logs/hermanas.log') && r.params.get('search') === 'door open'
            );
            req.flush([]);

            new Promise<string[]>(resolve =>
                service.tail('hermanas.log', { search: '   ' }).subscribe(resolve)
            );
            const reqBlank = httpMock.expectOne(r => r.url.endsWith('/logs/hermanas.log'));
            expect(reqBlank.request.params.has('search')).toBe(false);
            reqBlank.flush([]);
        });

        it('URL-encodes the filename', async () => {
            new Promise<string[]>(resolve =>
                service.tail('archive 2026/05.log').subscribe(resolve)
            );

            const req = httpMock.expectOne(r => r.url.endsWith('/logs/archive%202026%2F05.log'));
            req.flush([]);
        });
    });
});
