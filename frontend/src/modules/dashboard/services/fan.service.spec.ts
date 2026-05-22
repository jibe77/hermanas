import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FanService } from '@modules/dashboard/services/fan.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('FanService', () => {
    let service: FanService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                FanService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        service = TestBed.inject(FanService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
