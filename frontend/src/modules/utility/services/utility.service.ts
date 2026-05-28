import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UtilityService {
    private http = inject(HttpClient);

    get version$(): Observable<string> {
        return this.http.get('/assets/version', { responseType: 'text' });
    }
}
