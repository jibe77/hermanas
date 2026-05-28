import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface VersionInfo {
    time: string;
    version: string;
    artifact: string;
    group: string;
    name: string;
}

@Injectable()
export class VersionService extends AbstractService {
    private _httpClient = inject(HttpClient);

    public getVersionInfo(): Observable<VersionInfo> {
        return this._httpClient.get(this.domainBase + '/info', { headers: this.getHeaders() }).pipe(
            map((data: VersionInfo) => {
                return data;
            })
        );
    }
}
