import {
    HttpErrorResponse,
    HttpEvent,
    HttpInterceptorFn,
    HttpResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { from, Observable, of, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { UserService } from '@modules/auth/services';
import {
    DemoConfirmService,
    DemoFixtureService,
    ToastService,
} from '@common/services';

/**
 * Front-only safeguard + dataset shim for the demo mode.
 *
 * When the visitor pretends to be an admin (UserService.enableDemoMode), this
 * interceptor:
 *
 * 1. Before every mutating call (POST/PUT/DELETE/PATCH except /auth/*) it
 *    asks {@link DemoConfirmService} to show a confirmation modal explaining
 *    that the action will not actually run. A successful confirmation
 *    short-circuits the request with a 200 synthetic response (so the UI
 *    behaves as if the save worked) and shows a localized warning toast. A
 *    cancellation rejects the request with a synthetic 0 status so the
 *    component error path runs and any in-flight UI spinner is cleared.
 *
 * 2. Intercepts protected GETs: if the real backend would return 401/403
 *    because the visitor is not actually logged in, the interceptor catches
 *    the error and replaces it with a fixture from {@link DemoFixtureService}.
 *
 * Auth endpoints stay reachable so a real login is still possible.
 */
export const demoModeInterceptor: HttpInterceptorFn = (req, next) => {
    const userService = inject(UserService);
    if (!userService.isDemoMode()) {
        return next(req);
    }
    if (isAuthEndpoint(req.url)) {
        return next(req);
    }

    // ── Mutations: ask, block, toast, fake success ─────────────────────────
    // We treat the canonical mutating verbs *and* a small allowlist of
    // state-changing GETs the same way. The latter exists because some legacy
    // endpoints (music/switch, music/cocorico, fan/switch) are still GET on
    // the backend but really do flip hardware state — without this they would
    // bypass the demo confirmation modal and hit the real backend.
    if (isMutating(req.method) || isStateChangingGet(req.method, req.url)) {
        const fixtures = inject(DemoFixtureService);
        const toast = inject(ToastService);
        const confirm = inject(DemoConfirmService);
        const description = describeMutation(req.method, req.url);

        return from(confirm.confirm(description)).pipe(
            switchMap(accepted => {
                if (!accepted) {
                    // User cancelled — surface a 0-status error so the caller's
                    // .subscribe({ error }) clears its loading flag. The status
                    // code 0 mirrors what a network-aborted request looks like.
                    return throwError(
                        () =>
                            new HttpErrorResponse({
                                status: 0,
                                statusText: 'Cancelled (demo)',
                                url: req.url,
                                error: { error: 'DEMO_MODE_CANCELLED' },
                            })
                    );
                }
                toast.warning(
                    $localize`:@@demoActionBlockedMessage:Cette action est désactivée en mode démo. Connectez-vous pour modifier la configuration du poulailler.`,
                    $localize`:@@demoActionBlockedTitle:Mode démo`
                );
                const body = fixtures.matchMutation(req.url, req.body);
                return of(
                    new HttpResponse({
                        status: 200,
                        statusText: 'OK (demo)',
                        url: req.url,
                        body: body ?? '',
                    }) as HttpEvent<unknown>
                );
            })
        );
    }

    // ── Reads: let the request through, replace 401/403 with a fixture ─────
    const fixtures = inject(DemoFixtureService);
    return next(req).pipe(
        catchError((err: HttpErrorResponse) => {
            if (!isAuthError(err)) {
                return throwError(() => err);
            }
            const fixture = fixtures.matchGet(req.url);
            if (fixture === undefined) {
                return throwError(() => err);
            }
            return of(
                new HttpResponse({
                    status: 200,
                    statusText: 'OK (demo)',
                    url: req.url,
                    body: fixture,
                }) as HttpEvent<unknown>
            );
        })
    );
};

function isMutating(method: string): boolean {
    const m = method.toUpperCase();
    return m === 'POST' || m === 'PUT' || m === 'DELETE' || m === 'PATCH';
}

/**
 * Allowlist of GET endpoints that actually mutate hardware state on the
 * backend. They predate the HTTP/REST cleanup that moved /light/switch,
 * /door/open and /door/close to POST. Until the backend follows suit, the
 * demo-mode interceptor needs to recognise them so the confirmation modal
 * still fires and the request never reaches the live service.
 */
function isStateChangingGet(method: string, url: string): boolean {
    if (method.toUpperCase() !== 'GET') {
        return false;
    }
    const tail = url.split('?')[0].replace(/^https?:\/\/[^/]+/, '');
    return (
        tail.endsWith('/music/switch') ||
        tail.endsWith('/music/cocorico') ||
        tail.endsWith('/fan/switch')
    );
}

function isAuthEndpoint(url: string): boolean {
    return (
        url.includes('/auth/login') ||
        url.includes('/auth/logout') ||
        url.includes('/auth/register')
    );
}

function isAuthError(err: HttpErrorResponse): boolean {
    return err.status === 401 || err.status === 403;
}

/**
 * Returns a short, human-readable string describing what the mutation is about
 * — surfaced in the modal so the visitor knows what they're "simulating".
 * Heuristic mapping from common URL patterns; falls back to "METHOD path" so
 * unknown endpoints still produce something readable.
 */
function describeMutation(method: string, url: string): string {
    const path = url.split('?')[0];
    // Strip the protocol/host if any so the heuristics work on relative URLs
    // and fully-qualified ones the same way.
    const tail = path.replace(/^https?:\/\/[^/]+/, '');

    // High-traffic endpoints that benefit from a localized, friendly label.
    if (tail.endsWith('/system/reboot')) {
        return $localize`:@@demoMutReboot:Reboot the Raspberry Pi`;
    }
    if (tail.endsWith('/system/shutdown')) {
        return $localize`:@@demoMutShutdown:Shut down the Raspberry Pi`;
    }
    if (tail.endsWith('/door/open')) {
        return $localize`:@@demoMutDoorOpen:Open the chicken coop door`;
    }
    if (tail.endsWith('/door/close')) {
        return $localize`:@@demoMutDoorClose:Close the chicken coop door`;
    }
    if (tail.endsWith('/light/switch')) {
        return $localize`:@@demoMutLightSwitch:Switch the light`;
    }
    if (tail.endsWith('/fan/switch')) {
        return $localize`:@@demoMutFanSwitch:Switch the fan`;
    }
    if (tail.endsWith('/music/switch')) {
        return $localize`:@@demoMutMusicSwitch:Switch the music`;
    }
    if (tail.endsWith('/music/cocorico')) {
        return $localize`:@@demoMutMusicCocorico:Play a cock crow over the speakers`;
    }
    if (tail.endsWith('/music/selected-playlist')) {
        return $localize`:@@demoMutMusicSelectedPlaylist:Change the selected playlist`;
    }
    if (tail.endsWith('/config/refresh')) {
        return $localize`:@@demoMutConfigRefresh:Refresh the runtime configuration`;
    }
    if (tail.includes('/config/')) {
        return $localize`:@@demoMutConfigSave:Save a configuration change`;
    }
    if (tail.includes('/users/') || tail.endsWith('/users')) {
        if (method.toUpperCase() === 'DELETE') {
            return $localize`:@@demoMutUserDelete:Delete a user account`;
        }
        if (method.toUpperCase() === 'POST') {
            return $localize`:@@demoMutUserCreate:Create a user account`;
        }
        return $localize`:@@demoMutUserUpdate:Update a user account`;
    }
    if (tail.includes('/residents/')) {
        if (method.toUpperCase() === 'DELETE') {
            return $localize`:@@demoMutResidentDelete:Delete a resident`;
        }
        return $localize`:@@demoMutResidentSave:Save a resident change`;
    }
    if (tail.includes('/email/test')) {
        return $localize`:@@demoMutEmailTest:Send a test email`;
    }
    return `${method.toUpperCase()} ${tail}`;
}
