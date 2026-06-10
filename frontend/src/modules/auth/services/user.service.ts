import { Injectable, signal, WritableSignal, inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';
import { LoggerService } from '@common/services';
import { environment } from '../../../environments/environment';

import { User, AuthState } from '../models';

interface AuthMeResponse {
    authenticated: boolean;
    username?: string;
    roles?: string[];
    language?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
    private http = inject(HttpClient);
    private logger = inject(LoggerService);

    private readonly _user: WritableSignal<User>;
    private readonly _user$: Observable<User>;
    private _initialCheck?: Promise<void>;

    /**
     * Front-only "demo admin" mode used to showcase the app during job
     * interviews. When ON, every read-side check (isSignedIn / isAdmin /
     * user.authState) behaves as if the visitor were an ADMIN — so the full UI
     * unfolds without touching the backend. Mutating HTTP calls are blocked
     * client-side by demoModeInterceptor with a warning toast. The state is
     * intentionally NOT persisted: a page refresh drops the demo so a real
     * Guest session starts clean.
     */
    private readonly _demoMode: WritableSignal<boolean> = signal(false);

    constructor() {
        this._user = signal(this.createDefaultNewUser());
        this._user$ = toObservable(this._user);
    }

    enableDemoMode(): void {
        this._demoMode.set(true);
        // Re-emit the user signal so components subscribed to user$ pick up the
        // new derived state (isSignedIn/isAdmin via the synthetic ADMIN user).
        this.user = this.createDemoUser();
    }

    disableDemoMode(): void {
        this._demoMode.set(false);
        this.user = this.createDefaultNewUser();
    }

    isDemoMode(): boolean {
        return this._demoMode();
    }

    get demoMode(): WritableSignal<boolean> {
        return this._demoMode;
    }

    /**
     * Runs the first /auth/me call and caches the resulting promise. Called from
     * APP_INITIALIZER so Angular bootstrap waits for the answer — guards and components
     * are guaranteed to see the resolved session state on their first read.
     * Subsequent calls return the same promise (idempotent).
     */
    initialAuthCheck(): Promise<void> {
        if (!this._initialCheck) {
            this._initialCheck = this.checkAuthState();
        }
        return this._initialCheck;
    }

    get user(): WritableSignal<User> {
        return this._user;
    }

    set user(user: User) {
        this._user.set(user);
    }

    get user$(): Observable<User> {
        return this._user$;
    }

    getCurrentUser(): User {
        return this._user();
    }

    /**
     * Returns true if the currently authenticated account holds the ADMIN role.
     * Accepts both the canonical `ADMIN` value and Spring's legacy `ROLE_ADMIN`
     * spelling, in case the backend ever surfaces one or the other.
     */
    isAdmin(): boolean {
        if (this._demoMode()) {
            return true;
        }
        const u = this._user();
        if (!u || u.authState !== AuthState.SignedIn) {
            return false;
        }
        const roles = u.roles ?? [];
        return roles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN');
    }

    async checkAuthState(): Promise<void> {
        // Demo mode is a front-only fiction — the backend would always answer
        // {authenticated:false} and wipe the synthetic ADMIN user from the
        // signal, which is exactly what we don't want when the page or top-nav
        // re-mounts during navigation.
        if (this._demoMode()) {
            return;
        }
        try {
            const me = await firstValueFrom(
                this.http.get<AuthMeResponse>(`${environment.apiUrl}/auth/me`, {
                    withCredentials: true,
                })
            );
            if (me.authenticated && me.username) {
                this.setSignedInUser(me.username, me.roles ?? [], me.language);
            } else {
                this.setSignedOutUser();
            }
        } catch (error) {
            this.logger.error('checkAuthState failed', error, 'UserService');
            this.setSignedOutUser();
        }
    }

    setSignedInUser(username: string, roles: string[], language?: string): void {
        // A real backend session takes precedence over the demo fiction. If the
        // operator logs in for real while the demo flag was on, drop the flag so
        // the synthetic admin user is replaced by the actual one.
        this._demoMode.set(false);
        this.user = {
            id: username,
            login: username,
            email: username,
            authState: AuthState.SignedIn,
            roles,
            language,
        };
    }

    setSignedOutUser(): void {
        // Make sure the demo flag is cleared too — otherwise isAdmin() would
        // still return true while the user object claims Guest, which is a
        // hard-to-reproduce inconsistency. Callers that want to keep the demo
        // up should not be calling this method.
        this._demoMode.set(false);
        this.user = this.createDefaultNewUser();
    }

    private createDefaultNewUser(): User {
        return {
            id: undefined,
            email: 'guest',
            login: 'guest',
            authState: AuthState.SignedOut,
            roles: [],
        };
    }

    /**
     * Synthetic ADMIN user surfaced while demo mode is on. The login string is
     * exposed in the side-nav footer so it is obvious to the operator that they
     * are NOT really authenticated.
     */
    private createDemoUser(): User {
        return {
            id: 'demo',
            email: 'demo',
            login: 'demo',
            authState: AuthState.SignedIn,
            roles: ['ADMIN'],
        };
    }

    /**
     * Reloads the SPA on the locale matching the user's preferred language.
     * Angular i18n compiles two separate bundles (`/` for English,
     * `/fr-FR/` for French) — there is no runtime switch, so a full page
     * reload is the only way to change locale. Returns true when the browser
     * is actually navigating away; false when no switch is needed.
     */
    syncLocaleWithPreference(): boolean {
        const language = this._user().language;
        if (!language) {
            return false;
        }
        const targetBaseHref = preferenceToBaseHref(language);
        if (targetBaseHref === null) {
            return false;
        }
        const currentBaseHref = currentLocaleBaseHref();
        if (currentBaseHref === targetBaseHref) {
            return false;
        }
        const rest = window.location.pathname.substring(currentBaseHref.length);
        window.location.assign(targetBaseHref + rest + window.location.search + window.location.hash);
        return true;
    }
}

/**
 * Maps a language preference ("fr"/"en") to the base href Angular i18n serves
 * the corresponding bundle from. Returns null for unknown codes so the caller
 * leaves the current locale untouched rather than navigating to a 404.
 */
function preferenceToBaseHref(language: string): string | null {
    const code = language.toLowerCase();
    if (code.startsWith('en')) {
        return '/en-US/';
    }
    if (code.startsWith('fr')) {
        return '/fr-FR/';
    }
    if (code.startsWith('ro')) {
        return '/ro-RO/';
    }
    return null;
}

/**
 * Detects which Angular-i18n bundle is currently being served. We look for
 * known locale prefixes in the URL path; anything else means the default
 * (English) bundle at the root, which is what the redirect.html shim serves
 * when no language preference cookie is set.
 */
function currentLocaleBaseHref(): string {
    if (window.location.pathname.startsWith('/fr-FR/')) {
        return '/fr-FR/';
    }
    if (window.location.pathname.startsWith('/ro-RO/')) {
        return '/ro-RO/';
    }
    if (window.location.pathname.startsWith('/en-US/')) {
        return '/en-US/';
    }
    return '/';
}
