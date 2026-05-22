import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Forwards the session cookie (JSESSIONID) on every request to the backend.
 * Authentication is handled by Spring Security via the session — no manual
 * Authorization header is needed.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    return next(req.clone({ withCredentials: true }));
};
