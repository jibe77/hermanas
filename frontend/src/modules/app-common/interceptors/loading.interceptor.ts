import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '../services/loading/loading.service';

/**
 * Loading interceptor that automatically tracks HTTP requests
 * and updates the global loading state.
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
    const loadingService = inject(LoadingService);

    // Start loading
    loadingService.start();

    return next(req).pipe(
        finalize(() => {
            // Stop loading when request completes (success or error)
            loadingService.stop();
        })
    );
};
