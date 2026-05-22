import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { User } from '@modules/auth/models';
import { Observable } from 'rxjs';
import { SwitchResponse } from '../models';

export interface FanStatus {
    statusEnum: string;
    timeOut: number;
}

@Injectable()
export class FanService extends AbstractService {
    constructor(private _httpClient: HttpClient) {
        super();
    }

    public getStatus(): Observable<FanStatus> {
        const fanStatusUrl = this.domainBase + '/fan/status';
        return this._httpClient.get<FanStatus>(fanStatusUrl, { headers: this.getHeaders() });
    }

    public switch(param: boolean, user: User): Observable<SwitchResponse> {
        const fanSwitchUrl = this.domainBase + '/fan/switch';
        return this._httpClient.get<SwitchResponse>(fanSwitchUrl + '?param=' + param, {
            headers: this.getHeadersWithAuth(user),
        });
    }
}
