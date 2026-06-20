import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import {
    ApplicationConfig,
    ErrorHandler,
    isDevMode,
    provideAppInitializer,
    provideZoneChangeDetection,
    inject,
} from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withComponentInputBinding, withRouterConfig } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';

import { GlobalErrorHandler } from '@common/services';
import {
    authInterceptor,
    demoModeInterceptor,
    loadingInterceptor,
    loggingInterceptor,
    retryInterceptor,
} from '@common/interceptors';
import { UserService } from '@modules/auth/services/user.service';
import { httpErrorInterceptor } from '@modules/dashboard/interceptors';
import { EasterEggsService } from '@modules/easter-eggs/services';
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
        // `onSameUrlNavigation: 'reload'` lets us re-trigger the current route after
        // login/logout so components that read `isAdmin` / `isAuthenticated` once in
        // `ngOnInit` get re-created with the new session state instead of staying
        // stale until the user manually reloads the page.
        provideRouter(
            APP_ROUTES,
            withComponentInputBinding(),
            withRouterConfig({ onSameUrlNavigation: 'reload' })
        ),
        provideAnimations(),
        provideHttpClient(
            withXhr(),
            withInterceptors([
                // Demo-mode safeguard must run first so blocked mutations never
                // reach the loading spinner / retry / logging stack.
                demoModeInterceptor,
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
        // Easter eggs: install global listeners (konami, window.cocorico…)
        // and check today for a pensioner's birthday. Runs after the auth
        // check so the /residents call has a chance at being authenticated.
        provideAppInitializer(() => inject(EasterEggsService).install()),
        provideServiceWorker('ngsw-worker.js', {
            enabled: !isDevMode(),
            // Register the ServiceWorker as soon as the application is stable
            // or after 30 seconds (whichever comes first).
            registrationStrategy: 'registerWhenStable:30000',
        }),
    ],
};
