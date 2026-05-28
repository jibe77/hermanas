import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

/**
 * Functional interceptor (Angular 14+) replacing the class-based
 * HttpErrorInterceptor of the NgModule era. Wires the same console-log +
 * rethrow behaviour. Registered in main.ts via
 * provideHttpClient(withInterceptors([…, httpErrorInterceptor])).
 */
export const httpErrorInterceptor: HttpInterceptorFn = (request, next) => {
    return next(request).pipe(
        catchError((error: HttpErrorResponse) => {
            let errorMessage: string;
            if (error.error instanceof ErrorEvent) {
                // client-side error
                errorMessage = `Error: ${error.error.message}`;
            } else {
                // server-side error
                errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
            }
            // eslint-disable-next-line no-console
            console.log(errorMessage);
            return throwError(() => errorMessage);
        })
    );
};
