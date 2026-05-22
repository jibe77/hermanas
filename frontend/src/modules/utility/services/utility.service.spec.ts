import { HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { take } from 'rxjs/operators';

import { UtilityService } from './utility.service';

describe('UtilityService', () => {
    let utilityService: UtilityService;

    let _httpClient: HttpClient;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                UtilityService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });
        utilityService = TestBed.inject(UtilityService);

        httpClient = TestBed.inject(HttpClient);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    describe('getUtility$', () => {
        it('should return Observable<Utility>', () => {
            utilityService.version$.pipe(take(1)).subscribe(response => {
                expect(response).toEqual('a.b.c');
            });

            const req = httpTestingController.expectOne('/assets/version');
            expect(req.request.method).toEqual('GET');

            req.flush('a.b.c');
            httpTestingController.verify();
        });
    });
});
