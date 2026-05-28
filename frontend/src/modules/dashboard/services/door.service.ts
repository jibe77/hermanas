import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService, LoggerService } from '@common/services';
import { User } from '@modules/auth/models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface DoorStatus {
    status: string;
    timeStatusHasChanged: string;
    timeStatusHasChangedAsDate: Date;
}

@Injectable({ providedIn: 'root' })
export class DoorService extends AbstractService {
    private _httpClient = inject(HttpClient);
    private logger = inject(LoggerService);

    public getDoorStatus(): Observable<DoorStatus> {
        const nextEventsUrl = this.domainBase + '/door/status';
        return this._httpClient
            .get(nextEventsUrl, {
                headers: this.getHeaders(),
            })
            .pipe(
                map((data: DoorStatus) => {
                    data.timeStatusHasChangedAsDate = new Date(data.timeStatusHasChanged);
                    return data;
                })
            );
    }

    public closeDoor(user: User) {
        const nextEventsUrl = this.domainBase + '/door/close';
        this.logger.info('Closing door', { user: user.login, url: nextEventsUrl }, 'DoorService');
        return this._httpClient.post(nextEventsUrl, null, {
            headers: this.getHeadersWithAuth(user),
        });
    }

    public openDoor(user: User) {
        const nextEventsUrl = this.domainBase + '/door/open';
        this.logger.info('Opening door', { user: user.login, url: nextEventsUrl }, 'DoorService');
        return this._httpClient.post(nextEventsUrl, null, {
            headers: this.getHeadersWithAuth(user),
        });
    }
}
