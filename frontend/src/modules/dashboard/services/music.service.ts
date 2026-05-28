import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { User } from '@modules/auth/models';
import { Observable } from 'rxjs';
import { SwitchResponse } from '../models';

export interface MusicStatus {
    statusEnum: string;
    timeOut: number;
}

@Injectable({ providedIn: 'root' })
export class MusicService extends AbstractService {
    private _httpClient = inject(HttpClient);

    public getStatus(): Observable<MusicStatus> {
        const musicStatusUrl = this.domainBase + '/music/status';
        return this._httpClient.get<MusicStatus>(musicStatusUrl, { headers: this.getHeaders() });
    }

    public switch(param: boolean, user: User): Observable<SwitchResponse> {
        const musicSwitchUrl = this.domainBase + '/music/switch';
        return this._httpClient.get<SwitchResponse>(musicSwitchUrl + '?param=' + param, {
            headers: this.getHeadersWithAuth(user),
        });
    }
}
