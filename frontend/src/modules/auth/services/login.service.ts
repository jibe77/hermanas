import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserService } from './user.service';

export type LoginOutcome = 'ok' | 'invalid' | 'pending-validation';

@Injectable({ providedIn: 'root' })
export class LoginService {
    private http = inject(HttpClient);
    private userService = inject(UserService);

    async login(
        username: string,
        password: string,
        rememberMe: boolean = false
    ): Promise<LoginOutcome> {
        let body = new HttpParams().set('username', username).set('password', password);
        if (rememberMe) {
            // Spring Security's PersistentTokenBasedRememberMeServices reads this exact param
            // name (see SecurityConfig#rememberMeServices) and only activates when the value
            // matches one of: "true", "on", "yes", "1". We send "true" so behaviour is explicit.
            body = body.set('remember-me', 'true');
        }
        try {
            await firstValueFrom(
                this.http.post(`${environment.apiUrl}/auth/login`, body.toString(), {
                    headers: new HttpHeaders({
                        'Content-Type': 'application/x-www-form-urlencoded',
                    }),
                    withCredentials: true,
                })
            );
            await this.userService.checkAuthState();
            return 'ok';
        } catch (e: unknown) {
            this.userService.setSignedOutUser();
            // Backend returns 401 with body {"error":"ACCOUNT_PENDING_VALIDATION"} for accounts
            // awaiting admin approval, so we can show a tailored message instead of a generic one.
            if (
                e instanceof HttpErrorResponse &&
                e.error &&
                typeof e.error === 'object' &&
                (e.error as { error?: string }).error === 'ACCOUNT_PENDING_VALIDATION'
            ) {
                return 'pending-validation';
            }
            return 'invalid';
        }
    }

    async logout(): Promise<void> {
        try {
            await firstValueFrom(
                this.http.post(`${environment.apiUrl}/auth/logout`, null, {
                    withCredentials: true,
                })
            );
        } finally {
            this.userService.setSignedOutUser();
        }
    }
}
