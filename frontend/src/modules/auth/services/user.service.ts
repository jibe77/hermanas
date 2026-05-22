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

@Injectable()
export class UserService {
    private readonly _user: WritableSignal<User>;
    private readonly _user$: Observable<User>;

    constructor(private http: HttpClient, private logger: LoggerService) {
        this._user = signal(this.createDefaultNewUser());
        this._user$ = toObservable(this._user);
        this.checkAuthState();
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

    async checkAuthState(): Promise<void> {
        try {
            const me = await firstValueFrom(
                this.http.get<AuthMeResponse>(`${environment.apiUrl}/api/v1/auth/me`, {
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
