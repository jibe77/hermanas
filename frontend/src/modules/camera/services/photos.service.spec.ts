import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PhotoListing, PhotosService } from './photos.service';

describe('PhotosService', () => {
    let service: PhotosService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(PhotosService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    describe('list', () => {
        it('GETs /camera/photos without a path param for the root', async () => {
            const payload: PhotoListing = { path: '', directories: [], files: [] };
            const result = new Promise<PhotoListing>(resolve => service.list().subscribe(resolve));

            const req = httpMock.expectOne(r => r.url.endsWith('/camera/photos'));
            expect(req.request.method).toBe('GET');
            // empty path → no query param at all (back-end falls back to root)
            expect(req.request.params.has('path')).toBe(false);
            req.flush(payload);

            await expect(result).resolves.toEqual(payload);
        });

        it('passes the path as ?path= when set', async () => {
            const result = new Promise<PhotoListing>(resolve =>
                service.list('2026/05').subscribe(resolve)
            );

            const req = httpMock.expectOne(
                r => r.url.endsWith('/camera/photos') && r.params.get('path') === '2026/05'
            );
            req.flush({ path: '2026/05', directories: [], files: [] });

            await result;
        });
    });

    describe('fileUrl', () => {
        it('encodes special characters in the path', () => {
            const url = service.fileUrl('2026/05/28 morning.jpg');
            // Spaces become %20 — anything else than alnum / -_.~ should be percent-encoded.
            expect(url).toContain('%20morning.jpg');
            expect(url).toContain('/camera/photos/file?path=');
        });

        it('does not double-encode plain ASCII', () => {
            const url = service.fileUrl('2026/05/28/foo.jpg');
            expect(url).toBe('/api/v1/camera/photos/file?path=2026%2F05%2F28%2Ffoo.jpg');
        });
    });
});
