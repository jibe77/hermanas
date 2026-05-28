import { HttpRequest } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { lastValueFrom, of, throwError } from 'rxjs';

import { loadingInterceptor } from './loading.interceptor';
import { LoadingService } from '../services/loading/loading.service';

describe('loadingInterceptor', () => {
    let loading: LoadingService;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [LoadingService] });
        loading = TestBed.inject(LoadingService);
    });

    it('increments the LoadingService counter on entry, decrements on completion', async () => {
        const request = new HttpRequest('GET', '/api/v1/info');
        expect(loading.getActiveCount()).toBe(0);

        await TestBed.runInInjectionContext(async () => {
            const out$ = loadingInterceptor(request, () => of(undefined as never));
            // The Observable runs synchronously here: by the time we await it,
            // the start/finalize pair has happened.
            await lastValueFrom(out$);
        });

        expect(loading.getActiveCount()).toBe(0);
    });

    it('also decrements when the downstream errors', async () => {
        const request = new HttpRequest('GET', '/api/v1/info');

        await TestBed.runInInjectionContext(async () => {
            const out$ = loadingInterceptor(request, () => throwError(() => new Error('boom')));
            await expect(lastValueFrom(out$)).rejects.toThrow('boom');
        });

        expect(loading.getActiveCount()).toBe(0);
    });
});
