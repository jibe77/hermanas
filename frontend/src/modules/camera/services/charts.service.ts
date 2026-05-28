import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChartsService {
    constructor() {}

    getCharts$(): Observable<{}> {
        return of({});
    }
}
