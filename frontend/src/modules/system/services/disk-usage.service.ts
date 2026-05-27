import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface DiskUsage {
    path: string;
    totalBytes: number;
    usedBytes: number;
    freeBytes: number;
    usedPercent: number;
}

@Injectable()
export class DiskUsageService extends AbstractService {
    constructor(private http: HttpClient) {
        super();
    }

    public getDiskUsage(): Observable<DiskUsage> {
        return this.http.get<DiskUsage>(this.domainBase + '/system/disk-usage', {
            headers: this.getHeaders(),
        });
    }
}
