import {
    HttpErrorResponse,
    HttpEvent,
    HttpHandler,
    HttpRequest,
    HttpResponse,
} from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { HttpErrorInterceptor } from './http-error.interceptor';

describe('HttpErrorInterceptor', () => {
    let interceptor: HttpErrorInterceptor;
    let mockHandler: jasmine.SpyObj<HttpHandler>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [HttpErrorInterceptor],
        });
        interceptor = TestBed.inject(HttpErrorInterceptor);
        mockHandler = jasmine.createSpyObj('HttpHandler', ['handle']);
    });

    it('should be created', () => {
        expect(interceptor).toBeTruthy();
    });

    describe('Successful requests', () => {
        it('should pass through successful requests unchanged', done => {
            const request = new HttpRequest('GET', '/api/test');
            const response = new HttpResponse({ status: 200, body: { data: 'test' } });

            mockHandler.handle.and.returnValue(of(response));

            interceptor.intercept(request, mockHandler).subscribe({
                next: (event: HttpEvent<any>) => {
                    expect(event).toEqual(response);
                    done();
                },
                error: () => fail('should not error'),
            });
        });

        it('should not modify request headers or body', done => {
            const request = new HttpRequest('POST', '/api/test', { test: 'data' });
            const response = new HttpResponse({ status: 201 });

            mockHandler.handle.and.returnValue(of(response));

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => {
                    expect(mockHandler.handle).toHaveBeenCalledWith(request);
                    done();
                },
                error: () => fail('should not error'),
            });
        });
    });

    describe('Client-side errors', () => {
        it('should handle client-side ErrorEvent errors', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorEvent = new ErrorEvent('Network error', {
                message: 'Connection refused',
            });
            const errorResponse = new HttpErrorResponse({
                error: errorEvent,
                status: 0,
                statusText: 'Unknown Error',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toBe('Error: Connection refused');
                    expect(console.log).toHaveBeenCalledWith('Error: Connection refused');
                    done();
                },
            });
        });

        it('should handle client-side errors with empty message', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorEvent = new ErrorEvent('Network error', {
                message: '',
            });
            const errorResponse = new HttpErrorResponse({
                error: errorEvent,
                status: 0,
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toBe('Error: ');
                    expect(console.log).toHaveBeenCalledWith('Error: ');
                    done();
                },
            });
        });
    });

    describe('Server-side errors', () => {
        it('should handle 404 Not Found errors', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Not Found',
                status: 404,
                statusText: 'Not Found',
                url: '/api/test',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toContain('Error Code: 404');
                    expect(error).toContain('Message:');
                    expect(console.log).toHaveBeenCalled();
                    done();
                },
            });
        });

        it('should handle 500 Internal Server Error', done => {
            const request = new HttpRequest('POST', '/api/test', { data: 'test' });
            const errorResponse = new HttpErrorResponse({
                error: { message: 'Database connection failed' },
                status: 500,
                statusText: 'Internal Server Error',
                url: '/api/test',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toContain('Error Code: 500');
                    expect(console.log).toHaveBeenCalled();
                    done();
                },
            });
        });

        it('should handle 401 Unauthorized errors', done => {
            const request = new HttpRequest('GET', '/api/protected');
            const errorResponse = new HttpErrorResponse({
                error: 'Unauthorized',
                status: 401,
                statusText: 'Unauthorized',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toContain('Error Code: 401');
                    done();
                },
            });
        });

        it('should handle 403 Forbidden errors', done => {
            const request = new HttpRequest('DELETE', '/api/admin');
            const errorResponse = new HttpErrorResponse({
                error: 'Forbidden',
                status: 403,
                statusText: 'Forbidden',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toContain('Error Code: 403');
                    done();
                },
            });
        });

        it('should handle 503 Service Unavailable errors', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Service Unavailable',
                status: 503,
                statusText: 'Service Unavailable',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toContain('Error Code: 503');
                    done();
                },
            });
        });
    });

    describe('Error message formatting', () => {
        it('should format server error messages with status code', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Bad Request',
                status: 400,
                statusText: 'Bad Request',
                url: '/api/test',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toMatch(/Error Code: 400/);
                    expect(error).toMatch(/Message:/);
                    done();
                },
            });
        });

        it('should include newline between error code and message', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Not Found',
                status: 404,
                statusText: 'Not Found',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: (error: string) => {
                    expect(error).toContain('\n');
                    expect(error.split('\n').length).toBe(2);
                    done();
                },
            });
        });
    });

    describe('Logging', () => {
        it('should log all errors to console', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Test error',
                status: 500,
                statusText: 'Internal Server Error',
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            spyOn(console, 'log');

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should error'),
                error: () => {
                    expect(console.log).toHaveBeenCalledTimes(1);
                    const loggedMessage = (console.log as jasmine.Spy).calls.mostRecent().args[0];
                    expect(loggedMessage).toContain('Error Code:');
                    done();
                },
            });
        });
    });

    describe('Observable behavior', () => {
        it('should return an observable that emits errors', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Test error',
                status: 500,
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            const result = interceptor.intercept(request, mockHandler);

            expect(result).toBeInstanceOf(Observable);

            result.subscribe({
                error: error => {
                    expect(error).toBeDefined();
                    done();
                },
            });
        });

        it('should throw errors using throwError operator', done => {
            const request = new HttpRequest('GET', '/api/test');
            const errorResponse = new HttpErrorResponse({
                error: 'Test error',
                status: 500,
            });

            mockHandler.handle.and.returnValue(throwError(() => errorResponse));

            interceptor.intercept(request, mockHandler).subscribe({
                next: () => fail('should not emit next'),
                error: error => {
                    expect(error).toBeTruthy();
                    done();
                },
                complete: () => fail('should not complete'),
            });
        });
    });
});
