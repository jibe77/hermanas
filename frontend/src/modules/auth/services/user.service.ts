import { Injectable, signal, WritableSignal } from '@angular/core';
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
}

@Injectable({ providedIn: 'root' })
export class UserService {
    private readonly _user: WritableSignal<User>;
    private readonly _user$: Observable<User>;
    private _initialCheck?: Promise<void>;

    constructor(private http: HttpClient, private logger: LoggerService) {
        this._user = signal(this.createDefaultNewUser());
        this._user$ = toObservable(this._user);
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
        const u = this._user();
        if (!u || u.authState !== AuthState.SignedIn) {
            return false;
        }
        const roles = u.roles ?? [];
        return roles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN');
    }

    async checkAuthState(): Promise<void> {
        try {
            const me = await firstValueFrom(
                this.http.get<AuthMeResponse>(`${environment.apiUrl}/auth/me`, {
                    withCredentials: true,
                })
            );
            if (me.authenticated && me.username) {
                this.setSignedInUser(me.username, me.roles ?? []);
            } else {
                this.setSignedOutUser();
            }
        } catch (error) {
            this.logger.error('checkAuthState failed', error, 'UserService');
            this.setSignedOutUser();
        }
    }

    setSignedInUser(username: string, roles: string[]): void {
        this.user = {
            id: username,
            login: username,
            email: username,
            authState: AuthState.SignedIn,
            roles,
        };
    }

    setSignedOutUser(): void {
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
}
