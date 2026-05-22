import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface NextEvents {
    nextDoorOpeningTime: string;
    nextLightOnTime: string;
    nextDoorClosingTime: string;
    nextDoorOpeningTimeAsDate: Date;
    nextLightOnTimeAsDate: Date;
    nextDoorClosingTimeAsDate: Date;
}

@Injectable()
export class SchedulerService extends AbstractService {
    constructor(private _httpClient: HttpClient) {
        super();
    }

    public getNextEvents(): Observable<NextEvents> {
        const nextEventsUrl = this.domainBase + '/scheduler/nextEvents';
        return this._httpClient.get(nextEventsUrl, { headers: this.getHeaders() }).pipe(
            map((data: NextEvents) => {
                data.nextDoorOpeningTimeAsDate = new Date(data.nextDoorOpeningTime);
                data.nextLightOnTimeAsDate = new Date(data.nextLightOnTime);
                data.nextDoorClosingTimeAsDate = new Date(data.nextDoorClosingTime);
                return data;
            })
        );
    }
}
