import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs/operators';
import { LoggerService } from '../services';

/**
 * Logging interceptor that logs HTTP requests and responses for debugging.
 * Only logs in development mode to avoid console noise in production.
 */
export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
    const logger = inject(LoggerService);
    const started = Date.now();

    return next(req).pipe(
        tap({
            next: event => {
                // Only log final HTTP response
                if (event instanceof HttpResponse) {
                    const elapsed = Date.now() - started;
                    logger.debug(
                        `${req.method} ${req.urlWithParams} - ${event.status}`,
                        {
                            elapsed: `${elapsed}ms`,
                            status: event.status,
                            method: req.method,
                            url: req.urlWithParams,
                        },
                        'LoggingInterceptor'
                    );
                }
            },
            error: error => {
                const elapsed = Date.now() - started;
                logger.error(
                    `${req.method} ${req.urlWithParams} - ${error.status} ${error.statusText}`,
                    {
                        elapsed: `${elapsed}ms`,
                        status: error.status,
                        method: req.method,
                        url: req.urlWithParams,
                        error,
                    },
                    'LoggingInterceptor'
                );
            },
        })
    );
};
