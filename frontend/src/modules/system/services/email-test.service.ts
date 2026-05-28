import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface EmailTestResponse {
    message: string;
}

@Injectable({ providedIn: 'root' })
export class EmailTestService extends AbstractService {
    private http = inject(HttpClient);

    public sendTestEmail(): Observable<EmailTestResponse> {
        return this.http.post<EmailTestResponse>(
            this.domainBase + '/email/test',
            {},
            { headers: this.getHeaders() }
        );
    }
}
