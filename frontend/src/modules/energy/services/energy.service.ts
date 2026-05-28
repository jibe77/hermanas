import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export type EnergyModeEnum = 'ECO' | 'SUNNY' | 'REGULAR';

export interface EnergyMode {
    currentMode: EnergyModeEnum;
    forced: boolean;
    /** Map<month 1-12, mode>. */
    monthlyMapping: Record<number, EnergyModeEnum>;
}

export interface EnergyModeConfig {
    energyMode: EnergyModeEnum;
    /** Hidden in the UI but still part of the payload; the backend ignores it. */
    wifiDisabled: boolean;
    durationOfFanInMilliseconds: number;
    durationOfLightInMilliseconds: number;
    durationOfMusicInMilliseconds: number;
}

@Injectable({ providedIn: 'root' })
export class EnergyService extends AbstractService {
    private http = inject(HttpClient);

    getCurrentMode(): Observable<EnergyMode> {
        return this.http.get<EnergyMode>(`${this.domainBase}/energy/currentMode`);
    }

    getConfig(mode: EnergyModeEnum): Observable<EnergyModeConfig> {
        const params = new HttpParams().set('energyMode', mode);
        return this.http.get<EnergyModeConfig>(`${this.domainBase}/energy/configMode`, { params });
    }

    updateConfig(config: EnergyModeConfig): Observable<void> {
        return this.http.put<void>(`${this.domainBase}/energy/updateMode`, config);
    }

    updateMonthlyMapping(mapping: Record<number, EnergyModeEnum>): Observable<void> {
        return this.http.put<void>(`${this.domainBase}/energy/monthlyMapping`, mapping);
    }

    setEcoForced(forced: boolean): Observable<void> {
        const params = new HttpParams().set('forced', String(forced));
        return this.http.put<void>(`${this.domainBase}/energy/forceEco`, null, { params });
    }
}
