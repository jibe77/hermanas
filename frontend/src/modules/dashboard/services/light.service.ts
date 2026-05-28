import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { User } from '@modules/auth/models';
import { Observable } from 'rxjs';
import { SwitchResponse } from '../models';

export interface LightStatus {
    statusEnum: string;
    timeOut: number;
}

@Injectable()
export class LightService extends AbstractService {
    private _httpClient = inject(HttpClient);

    public getStatus(): Observable<LightStatus> {
        const lightStatusUrl = this.domainBase + '/light/status';
        return this._httpClient.get<LightStatus>(lightStatusUrl, { headers: this.getHeaders() });
    }

    public switch(param: boolean, user: User): Observable<SwitchResponse> {
        const lightSwitchUrl = this.domainBase + '/light/switch';
        return this._httpClient.post<SwitchResponse>(lightSwitchUrl + '?param=' + param, null, {
            headers: this.getHeadersWithAuth(user),
        });
    }
}
