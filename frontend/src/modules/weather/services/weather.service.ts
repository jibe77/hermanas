import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { MeteoInfo } from '@modules/dashboard/services';
import { Observable } from 'rxjs';

@Injectable()
export class WeatherService extends AbstractService {
    private _httpClient = inject(HttpClient);

    public getInfoUsingDateRange(from: string, to: string): Observable<MeteoInfo[]> {
        return this._httpClient.get<MeteoInfo[]>(
            this.domainBase + '/sensor/history/' + from + '/' + to,
            { headers: this.getHeaders() }
        );
    }
}
