import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export class MeteoInfo {
    temperature: string;
    externalTemperature: number;
    humidity: number;
    externalHumidity: number;
    dateTime: string;
}

@Injectable()
export class MeteoService extends AbstractService {
    private _httpClient = inject(HttpClient);

    public getMeteoInfo(): Observable<MeteoInfo> {
        return this._httpClient
            .get(this.domainBase + '/sensor/info', { headers: this.getHeaders() })
            .pipe(
                map((data: MeteoInfo) => {
                    return data;
                })
            );
    }
}
