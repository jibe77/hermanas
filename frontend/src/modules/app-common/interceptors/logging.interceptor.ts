import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs/operators';
import { LoggerService } from '../services';

/**
 * Logging interceptor for HTTP errors.
 *
 * <p>Successful responses are intentionally not logged: with the capture
 * polling loop firing one /status request per second the console used to
 * fill up faster than anyone could read it. The previous DEBUG line per
 * 2xx is gone — failures are the only case where the HTTP layer carries
 * non-redundant information for debugging.</p>
 */
export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
    const logger = inject(LoggerService);
    const started = Date.now();

    return next(req).pipe(
        tap({
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
