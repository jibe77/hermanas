/* eslint-disable no-console */
import {
    HttpErrorResponse,
    HttpEvent,
    HttpHandler,
    HttpRequest,
    HttpResponse,
} from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, lastValueFrom, Observable, of, throwError } from 'rxjs';

import { HttpErrorInterceptor } from './http-error.interceptor';

/**
 * Helper that wraps the interceptor's Observable<HttpEvent> into a Promise that
 * resolves with the first emitted value or rejects with the error string. This
 * replaces the Jasmine-era `done()` callbacks Vitest 3 has deprecated.
 */
function runIntercept(
    interceptor: HttpErrorInterceptor,
    req: HttpRequest<unknown>,
    handler: HttpHandler
): Promise<HttpEvent<unknown>> {
    return firstValueFrom(interceptor.intercept(req, handler));
}

describe('HttpErrorInterceptor', () => {
    let interceptor: HttpErrorInterceptor;
    let mockHandler: { handle: ReturnType<typeof vi.fn> };
    let asHandler: () => HttpHandler;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [HttpErrorInterceptor],
        });
        interceptor = TestBed.inject(HttpErrorInterceptor);
        mockHandler = { handle: vi.fn() };
        asHandler = () => mockHandler as unknown as HttpHandler;
    });

    it('should be created', () => {
        expect(interceptor).toBeTruthy();
    });

    describe('Successful requests', () => {
        it('passes successful responses through unchanged', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const response = new HttpResponse({ status: 200, body: { data: 'test' } });
            mockHandler.handle.mockReturnValue(of(response));

            await expect(runIntercept(interceptor, request, asHandler())).resolves.toEqual(
                response
            );
        });

        it('does not modify the original request', async () => {
            const request = new HttpRequest('POST', '/api/test', { test: 'data' });
            const response = new HttpResponse({ status: 201 });
            mockHandler.handle.mockReturnValue(of(response));

            await runIntercept(interceptor, request, asHandler());

            expect(mockHandler.handle).toHaveBeenCalledWith(request);
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
            mockHandler.handle.mockReturnValue(throwError(() => errorResponse));
            const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined);

            await expect(lastValueFrom(interceptor.intercept(request, asHandler()))).rejects.toBe(
                'Error: Connection refused'
            );
            expect(logSpy).toHaveBeenCalledWith('Error: Connection refused');
        });

        it('handles empty-message ErrorEvent', async () => {
            const request = new HttpRequest('GET', '/api/test');
            const errorEvent = new ErrorEvent('Network error', { message: '' });
            const errorResponse = new HttpErrorResponse({ error: errorEvent, status: 0 });
            mockHandler.handle.mockReturnValue(throwError(() => errorResponse));
            vi.spyOn(console, 'log').mockImplementation(() => undefined);

            await expect(lastValueFrom(interceptor.intercept(request, asHandler()))).rejects.toBe(
                'Error: '
            );
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
                mockHandler.handle.mockReturnValue(throwError(() => errorResponse));
                vi.spyOn(console, 'log').mockImplementation(() => undefined);

                await expect(
                    lastValueFrom(interceptor.intercept(request, asHandler()))
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
            mockHandler.handle.mockReturnValue(throwError(() => errorResponse));

            try {
                await lastValueFrom(interceptor.intercept(request, asHandler()));
                throw new Error('Expected the interceptor to error.');
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
            mockHandler.handle.mockReturnValue(throwError(() => errorResponse));
            const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined);

            await expect(
                lastValueFrom(interceptor.intercept(request, asHandler()))
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
            mockHandler.handle.mockReturnValue(throwError(() => errorResponse));

            const result = interceptor.intercept(request, asHandler());
            expect(result).toBeInstanceOf(Observable);

            await expect(lastValueFrom(result)).rejects.toBeDefined();
        });
    });
});
