import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
    ApplicationConfig,
    ErrorHandler,
    isDevMode,
    provideAppInitializer,
    provideZoneChangeDetection,
    inject,
} from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';

import { GlobalErrorHandler } from '@common/services';
import {
    authInterceptor,
    loadingInterceptor,
    loggingInterceptor,
    retryInterceptor,
} from '@common/interceptors';
import { UserService } from '@modules/auth/services/user.service';
import { httpErrorInterceptor } from '@modules/dashboard/interceptors';
import { provideHermanasIcons } from '@modules/icons/icons.provider';

import { APP_ROUTES } from './app.routes';

/**
 * Single source of truth for the application's runtime configuration. Replaces
 * the imports + providers of the old AppModule.
 *
 * The `provideAppInitializer` block keeps the cold-start auth check from the
 * previous APP_INITIALIZER token: Angular bootstrap waits for /auth/me to
 * resolve, so guards never have to race against an "unknown" session state.
 */
export const appConfig: ApplicationConfig = {
    providers: [
        provideZoneChangeDetection({ eventCoalescing: true }),
        provideRouter(APP_ROUTES, withComponentInputBinding()),
        provideAnimations(),
        provideHttpClient(
            withInterceptors([
                loadingInterceptor,
                loggingInterceptor,
                authInterceptor,
                retryInterceptor,
                httpErrorInterceptor,
            ])
        ),
        { provide: ErrorHandler, useClass: GlobalErrorHandler },
        provideHermanasIcons(),
        provideAppInitializer(() => inject(UserService).initialAuthCheck()),
        provideServiceWorker('ngsw-worker.js', {
            enabled: !isDevMode(),
            // Register the ServiceWorker as soon as the application is stable
            // or after 30 seconds (whichever comes first).
            registrationStrategy: 'registerWhenStable:30000',
        }),
    ],
};
