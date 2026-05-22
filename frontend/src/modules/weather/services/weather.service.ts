import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { MeteoInfo } from '@modules/dashboard/services';
import { Observable } from 'rxjs';

@Injectable()
export class WeatherService extends AbstractService {
    constructor(private _httpClient: HttpClient) {
        super();
    }

    public getInfoUsingDateRange(from: string, to: string): Observable<MeteoInfo[]> {
        return this._httpClient.get<MeteoInfo[]>(
            this.domainBase + '/sensor/history/' + from + '/' + to,
            { headers: this.getHeaders() }
        );
    }
}
