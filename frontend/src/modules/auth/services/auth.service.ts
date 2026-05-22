import { Injectable } from '@angular/core';
/* Import the Amplify Auth API */
import { Observable, of } from 'rxjs';

@Injectable()
export class AuthService {
    constructor() {}

    getAuth$(): Observable<{}> {
        return of({});
    }
}
