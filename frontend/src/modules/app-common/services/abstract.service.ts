import { HttpHeaders } from '@angular/common/http';
import { User } from '@modules/auth/models';
import { environment } from '../../../environments/environment';

export class AbstractService {
    public domainBase = environment.apiUrl;

    public getHeaders() {
        return new HttpHeaders();
    }

    /**
     * Kept for API compatibility with existing services. Authentication is now
     * handled by the session cookie (forwarded by authInterceptor), so the
     * `user` argument is no longer used.
     */
    protected getHeadersWithAuth(_user?: User) {
        return this.getHeaders();
    }
}
