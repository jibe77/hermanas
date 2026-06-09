import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export type GpioKind = 'button' | 'light' | 'fan' | 'servo' | 'sensor';

export interface GpioPin {
    key: string;
    /** English label (Door upper end-stop button, …). */
    label: string;
    /** French label (Bouton de fin de course haut de la porte, …). */
    labelFr: string;
    direction: string;
    /** Logical category — drives how the live status is computed in the UI. */
    kind: GpioKind;
    /** BCM (GPIO) number — what code addresses. */
    pin: number;
    /** Physical pin number on the 40-pin header. May be "?" if unknown. */
    boardPin: string;
}

@Injectable({ providedIn: 'root' })
export class ElectronicsService extends AbstractService {
    private http = inject(HttpClient);

    listGpioPins(): Observable<GpioPin[]> {
        return this.http.get<GpioPin[]>(`${this.domainBase}/electronics/gpio`);
    }
}
