/* eslint-disable no-console */
import {
    HttpErrorResponse,
    HttpEvent,
    HttpHandlerFn,
    HttpRequest,
    HttpResponse,
} from '@angular/common/http';
import { firstValueFrom, lastValueFrom, of, throwError } from 'rxjs';

import { httpErrorInterceptor } from './http-error.interceptor';

const failTest = (msg: string) => {
    throw new Error(msg);
};

/**
 * Runs the functional interceptor with a simple {@link HttpHandlerFn} stub.
 */
function intercept(
    req: HttpRequest<unknown>,
    next: HttpHandlerFn
): ReturnType<typeof httpErrorInterceptor> {
    return httpErrorInterceptor(req, next);
}

describe('httpErrorInterceptor', () => {
    describe('Successful requests', () => {
        it('passes successful responses through unchanged', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const response = new HttpResponse({ status: 200, body: { data: 'test' } });

            const result = firstValueFrom(intercept(request, () => of(response)));
            await expect(result).resolves.toEqual(response);
        });

        it('does not modify the original request', async () => {
            const request = new HttpRequest('POST', '/api/test', { test: 'data' });
            const response = new HttpResponse({ status: 201 });
            const next = vi.fn().mockReturnValue(of(response));

            await firstValueFrom(intercept(request, next));
            expect(next).toHaveBeenCalledWith(request);
        });
    });

    describe('Client-side errors', () => {
        it('formats ErrorEvent failures with the original message', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const errorEvent = new ErrorEvent('Network error', { message: 'Connection refused' });
            const errorResponse = new HttpErrorResponse({
                error: errorEvent,
                status: 0,
                statusText: 'Unknown Error',
            });
            const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined);

            await expect(
                lastValueFrom(intercept(request, () => throwError(() => errorResponse)))
            ).rejects.toBe('Error: Connection refused');
            expect(logSpy).toHaveBeenCalledWith('Error: Connection refused');
        });

        it('handles empty-message ErrorEvent', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const errorEvent = new ErrorEvent('Network error', { message: '' });
            const errorResponse = new HttpErrorResponse({ error: errorEvent, status: 0 });
            vi.spyOn(console, 'log').mockImplementation(() => undefined);

            await expect(
                lastValueFrom(intercept(request, () => throwError(() => errorResponse)))
            ).rejects.toBe('Error: ');
        });
    });

    describe('Server-side errors', () => {
        const cases = [
            { status: 404, statusText: 'Not Found', error: 'Not Found' },
            { status: 500, statusText: 'Internal Server Error', error: 'oops' },
            { status: 401, statusText: 'Unauthorized', error: 'Unauthorized' },
            { status: 403, statusText: 'Forbidden', error: 'Forbidden' },
            { status: 503, statusText: 'Service Unavailable', error: 'Service Unavailable' },
        ];

        for (const c of cases) {
            it(`formats HTTP ${c.status} with the status code`, async () => {
                const request = new HttpRequest('GET', '/api/test');
                const errorResponse = new HttpErrorResponse({ ...c });
                vi.spyOn(console, 'log').mockImplementation(() => undefined);

                await expect(
                    lastValueFrom(intercept(request, () => throwError(() => errorResponse)))
                ).rejects.toContain(`Error Code: ${c.status}`);
            });
        }
    });

    describe('Error message formatting', () => {
        it('includes a newline between the code and the message', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Not Found',
                status: 404,
                statusText: 'Not Found',
            });

            try {
                await lastValueFrom(intercept(request, () => throwError(() => errorResponse)));
                failTest('Expected the interceptor to error.');
            } catch (raw) {
                const formatted = raw as string;
                expect(formatted).toContain('\n');
                expect(formatted.split('\n').length).toBe(2);
            }
        });
    });

    describe('Logging', () => {
        it('logs the formatted error exactly once', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({ error: 'Test error', status: 500 });
            const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined);

            await expect(
                lastValueFrom(intercept(request, () => throwError(() => errorResponse)))
            ).rejects.toBeDefined();

            expect(consoleSpy).toHaveBeenCalledTimes(1);
            const loggedMessage = consoleSpy.mock.calls.at(-1)?.[0];
            expect(String(loggedMessage)).toContain('Error Code:');
        });
    });

    describe('Observable behavior', () => {
        it('returns an Observable that emits errors', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({ error: 'Test error', status: 500 });

            const result = intercept(request, () => throwError(() => errorResponse)) as ReturnType<
                typeof intercept
            >;
            expect(typeof (result as { subscribe: unknown }).subscribe).toBe('function');

            await expect(
                lastValueFrom(result as never as Promise<HttpEvent<unknown>>)
            ).rejects.toBeDefined();
        });
    });
});
