import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface LogFileInfo {
    name: string;
    size: number;
    lastModified: number;
}

export type LogLevel = 'ALL' | 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

export interface LogTailOptions {
    lines?: number;
    level?: LogLevel;
    search?: string;
}

@Injectable()
export class LogsService extends AbstractService {
    private http = inject(HttpClient);

    listFiles(): Observable<LogFileInfo[]> {
        return this.http.get<LogFileInfo[]>(`${this.domainBase}/logs`, {
            headers: this.getHeaders(),
        });
    }

    tail(filename: string, options: LogTailOptions = {}): Observable<string[]> {
        let params = new HttpParams();
        if (options.lines !== undefined) {
            params = params.set('lines', String(options.lines));
        }
        if (options.level && options.level !== 'ALL') {
            params = params.set('level', options.level);
        }
        if (options.search && options.search.trim().length > 0) {
            params = params.set('search', options.search.trim());
        }
        return this.http.get<string[]>(`${this.domainBase}/logs/${encodeURIComponent(filename)}`, {
            params,
            headers: this.getHeaders(),
        });
    }
}
