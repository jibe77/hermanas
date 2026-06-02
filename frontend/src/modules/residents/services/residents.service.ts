import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface Resident {
    id: number;
    name: string;
    breed: string | null;
    birthDate: string | null;
    arrivalDate: string | null;
    deathDate: string | null;
    comments: string | null;
    photoUrl: string | null;
}

export interface ResidentRequest {
    name: string;
    breed: string | null;
    birthDate: string | null;
    arrivalDate: string | null;
    deathDate: string | null;
    comments: string | null;
}

@Injectable({ providedIn: 'root' })
export class ResidentsService extends AbstractService {
    private http = inject(HttpClient);

    list(): Observable<Resident[]> {
        return this.http.get<Resident[]>(`${this.domainBase}/residents`);
    }

    create(request: ResidentRequest): Observable<Resident> {
        return this.http.post<Resident>(`${this.domainBase}/residents`, request);
    }

    update(id: number, request: ResidentRequest): Observable<Resident> {
        return this.http.put<Resident>(`${this.domainBase}/residents/${id}`, request);
    }

    remove(id: number): Observable<void> {
        return this.http.delete<void>(`${this.domainBase}/residents/${id}`);
    }

    uploadPhoto(id: number, file: File): Observable<Resident> {
        const form = new FormData();
        form.append('file', file);
        return this.http.post<Resident>(`${this.domainBase}/residents/${id}/photo`, form);
    }

    deletePhoto(id: number): Observable<void> {
        return this.http.delete<void>(`${this.domainBase}/residents/${id}/photo`);
    }
}
