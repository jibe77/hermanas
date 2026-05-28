import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ErrorService {
    constructor() {}

    getError$(): Observable<{}> {
        return of({});
    }
}
