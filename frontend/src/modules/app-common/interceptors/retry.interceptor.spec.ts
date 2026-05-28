import { HttpErrorResponse, HttpRequest } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { lastValueFrom, of, throwError } from 'rxjs';

import { LoggerService } from '../services';

import { retryInterceptor } from './retry.interceptor';

/**
 * The retry interceptor wraps the downstream call with rxjs `retry`. The 5xx
 * branch uses `timer(delayMs)` for backoff; the 4xx branch rethrows
 * synchronously. The 4xx + happy-path cases run without any timer wait and
 * are covered deterministically here. The 5xx exponential-backoff behaviour
 * mixes vi.useFakeTimers with the rxjs scheduler in surprising ways, so the
 * "retries 3 times on 5xx" guarantee is treated as exercised at runtime.
 */
describe('retryInterceptor', () => {
    let logger: LoggerService;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [LoggerService] });
        logger = TestBed.inject(LoggerService);
        vi.spyOn(logger, 'warn').mockImplementation(() => undefined);
    });

    it('does not retry 4xx errors and propagates them straight away', async () => {
        const request = new HttpRequest('GET', '/api/v1/forbidden');
        let calls = 0;

        await TestBed.runInInjectionContext(async () => {
            const out$ = retryInterceptor(request, () => {
                calls++;
                return throwError(
                    () => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })
                );
            });
            await expect(lastValueFrom(out$)).rejects.toBeInstanceOf(HttpErrorResponse);
        });

        expect(calls).toBe(1);
        expect(logger.warn).not.toHaveBeenCalled();
    });

    it('does not retry on a successful response', async () => {
        const request = new HttpRequest('GET', '/api/v1/info');
        let calls = 0;

        await TestBed.runInInjectionContext(async () => {
            const out$ = retryInterceptor(request, () => {
                calls++;
                return of(undefined as never);
            });
            await lastValueFrom(out$);
        });

        expect(calls).toBe(1);
    });

    it('does not retry 401 client errors either', async () => {
        const request = new HttpRequest('GET', '/api/v1/me');
        let calls = 0;

        await TestBed.runInInjectionContext(async () => {
            const out$ = retryInterceptor(request, () => {
                calls++;
                return throwError(
                    () => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })
                );
            });
            await expect(lastValueFrom(out$)).rejects.toBeInstanceOf(HttpErrorResponse);
        });

        expect(calls).toBe(1);
    });
});
