import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MeteoService } from '@modules/dashboard/services';

import { VersionService } from './version.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('VersionService', () => {
    let service: VersionService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                VersionService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        service = TestBed.inject(VersionService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
