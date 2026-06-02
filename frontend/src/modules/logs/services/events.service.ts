import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface EventEntry {
    id: number;
    eventType: string;
    dateTime: string;
    details: string | null;
}

export interface EventQuery {
    from?: string;
    to?: string;
    limit?: number;
}

@Injectable({ providedIn: 'root' })
export class EventsService extends AbstractService {
    private http = inject(HttpClient);

    listBusiness(query: EventQuery = {}): Observable<EventEntry[]> {
        return this.http.get<EventEntry[]>(`${this.domainBase}/events/business`, {
            params: this.buildParams(query),
            headers: this.getHeaders(),
        });
    }

    listAuth(query: EventQuery = {}): Observable<EventEntry[]> {
        return this.http.get<EventEntry[]>(`${this.domainBase}/events/auth`, {
            params: this.buildParams(query),
            headers: this.getHeaders(),
        });
    }

    private buildParams(q: EventQuery): HttpParams {
        let params = new HttpParams();
        if (q.from) params = params.set('from', q.from);
        if (q.to) params = params.set('to', q.to);
        if (q.limit !== undefined) params = params.set('limit', String(q.limit));
        return params;
    }
}
