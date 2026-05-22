import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserService } from './user.service';

@Injectable()
export class LoginService {
    constructor(private http: HttpClient, private userService: UserService) {}

    async login(username: string, password: string): Promise<boolean> {
        const body = new HttpParams()
            .set('username', username)
            .set('password', password);
        try {
            await firstValueFrom(
                this.http.post(`${environment.apiUrl}/api/v1/auth/login`, body.toString(), {
                    headers: new HttpHeaders({
                        'Content-Type': 'application/x-www-form-urlencoded',
                    }),
                    withCredentials: true,
                })
            );
            await this.userService.checkAuthState();
            return true;
        } catch {
            this.userService.setSignedOutUser();
            return false;
        }
    }

    async logout(): Promise<void> {
        try {
            await firstValueFrom(
                this.http.post(`${environment.apiUrl}/api/v1/auth/logout`, null, {
                    withCredentials: true,
                })
            );
        } finally {
            this.userService.setSignedOutUser();
        }
    }
}
