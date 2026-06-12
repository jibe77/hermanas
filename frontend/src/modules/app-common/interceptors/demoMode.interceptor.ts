import {
    HttpErrorResponse,
    HttpEvent,
    HttpInterceptorFn,
    HttpResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { UserService } from '@modules/auth/services';
import {
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
 *    short-circuits the request, surfaces a yellow "Demo mode — action
 *    disabled" toast through {@link ToastService.demoBlocked}, and rejects
 *    the call with a synthetic 0-status error so the component error path
 *    runs and any in-flight spinner clears.
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

    // ── Mutations: yellow toast + synthetic failure ────────────────────────
    // We treat the canonical mutating verbs *and* a small allowlist of
    // state-changing GETs the same way. The latter exists because some legacy
    // endpoints (music/switch, music/cocorico, fan/switch) are still GET on
    // the backend but really do flip hardware state — without this they would
    // bypass the demo block and hit the real backend.
    if (isMutating(req.method) || isStateChangingGet(req.method, req.url)) {
        const toast = inject(ToastService);
        toast.demoBlocked(
            $localize`:@@demoActionBlockedMessage:This action is disabled in demo mode. Sign in to change the chicken coop configuration.`,
            $localize`:@@demoActionBlockedTitle:Demo mode`
        );
        return throwError(
            () =>
                new HttpErrorResponse({
                    status: 0,
                    statusText: 'Blocked (demo)',
                    url: req.url,
                    error: { error: 'DEMO_MODE_BLOCKED' },
                })
        );
    }

    // ── Reads: let the request through, replace failures with a fixture ────
    // We accept any 4xx/5xx as the substitution trigger (not just 401/403)
    // because some backend endpoints currently throw a 500 NPE when the
    // session is missing instead of returning a clean 401. The fixture
    // lookup is the safety net: if no fixture matches the URL, the original
    // error is re-thrown so the component error path still runs.
    const fixtures = inject(DemoFixtureService);
    return next(req).pipe(
        catchError((err: HttpErrorResponse) => {
            if (!isServerOrAuthError(err)) {
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
 * demo-mode interceptor needs to recognise them so the demo block still
 * fires and the request never reaches the live service.
 */
function isStateChangingGet(method: string, url: string): boolean {
    if (method.toUpperCase() !== 'GET') {
        return false;
    }
    const tail = url.split('?')[0].replace(/^https?:\/\/[^/]+/, '');
    return (
        tail.endsWith('/music/switch') ||
        tail.endsWith('/music/cocorico') ||
        tail.endsWith('/fan/switch') ||
        // Electronics page "open/close a bit" buttons hit these GET endpoints
        // — they really move the servomotor, so they must be gated by the
        // demo block just like POST mutations.
        tail.endsWith('/door/turnClockwise') ||
        tail.endsWith('/door/turnCounterClockwise') ||
        tail.endsWith('/door/turnServo')
    );
}

function isAuthEndpoint(url: string): boolean {
    return (
        url.includes('/auth/login') ||
        url.includes('/auth/logout') ||
        url.includes('/auth/register')
    );
}

function isServerOrAuthError(err: HttpErrorResponse): boolean {
    // 0 is the synthetic "blocked (demo)" status we emit ourselves for
    // mutations — never substitute a fixture for those. Otherwise any 4xx
    // or 5xx that comes back from the live backend is fair game.
    return err.status >= 400 && err.status < 600;
}
