import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface HermanasUser {
    login: string;
    email: string | null;
    role: string;
    notificationsEnabled: boolean;
}

export interface UserUpdate {
    email?: string | null;
    notificationsEnabled?: boolean;
    password?: string;
    role?: string;
}

export interface UserCreate {
    login: string;
    password: string;
    email?: string | null;
    role?: string;
    notificationsEnabled?: boolean;
}

@Injectable()
export class ChartsService extends AbstractService {
    private http = inject(HttpClient);

    me(): Observable<HermanasUser> {
        return this.http.get<HermanasUser>(`${this.domainBase}/users/me`, {
            headers: this.getHeaders(),
        });
    }

    updateMe(payload: UserUpdate): Observable<HermanasUser> {
        return this.http.put<HermanasUser>(`${this.domainBase}/users/me`, payload, {
            headers: this.getHeaders(),
        });
    }

    list(): Observable<HermanasUser[]> {
        return this.http.get<HermanasUser[]>(`${this.domainBase}/users`, {
            headers: this.getHeaders(),
        });
    }

    create(payload: UserCreate): Observable<HermanasUser> {
        return this.http.post<HermanasUser>(`${this.domainBase}/users`, payload, {
            headers: this.getHeaders(),
        });
    }

    update(login: string, payload: UserUpdate): Observable<HermanasUser> {
        return this.http.put<HermanasUser>(
            `${this.domainBase}/users/${encodeURIComponent(login)}`,
            payload,
            { headers: this.getHeaders() }
        );
    }

    delete(login: string): Observable<void> {
        return this.http.delete<void>(`${this.domainBase}/users/${encodeURIComponent(login)}`, {
            headers: this.getHeaders(),
        });
    }
}
