import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { timer } from 'rxjs';
import { retry } from 'rxjs/operators';
import { LoggerService } from '../services';

/**
 * Retry interceptor that retries failed HTTP requests with exponential backoff.
 * Retries up to 3 times with delays of 1s, 2s, 4s between attempts.
 */
export const retryInterceptor: HttpInterceptorFn = (req, next) => {
    const logger = inject(LoggerService);

    return next(req).pipe(
        retry({
            count: 3,
            delay: (error, retryCount) => {
                // Only retry on server errors (5xx) or network errors
                if (error.status >= 500 || error.status === 0) {
                    const delayMs = Math.pow(2, retryCount - 1) * 1000; // Exponential backoff: 1s, 2s, 4s
                    logger.warn(
                        `Retrying HTTP request (attempt ${retryCount}/3) after ${delayMs}ms`,
                        { url: req.url, status: error.status, method: req.method },
                        'RetryInterceptor'
                    );
                    return timer(delayMs);
                }
                // Don't retry client errors (4xx)
                throw error;
            },
        })
    );
};
