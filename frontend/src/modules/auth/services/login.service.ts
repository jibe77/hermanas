import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserService } from './user.service';

/**
 * Result of a login attempt. Deliberately only two failure-free outcomes: the backend
 * returns one uniform 401 whatever the reason, so the SPA must not pretend to know more
 * than it is told (AUTH-VULN-02).
 */
export type LoginOutcome = 'ok' | 'invalid';

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
            // AUTH-VULN-02: the backend answers one uniform 401 for every failure reason,
            // including "account awaiting validation" — a distinguishable body there was a
            // pre-authentication account oracle. A real applicant learns the same thing
            // from the registration response and the confirmation mail, so the SPA now has
            // exactly one failure branch.
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
