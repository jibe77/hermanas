import { Injectable } from '@angular/core';
/* Import the Amplify Auth API */
import { Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
    constructor() {}

    getAuth$(): Observable<{}> {
        return of({});
    }
}
