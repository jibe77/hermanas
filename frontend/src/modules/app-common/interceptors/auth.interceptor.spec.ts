import { HttpRequest } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';

import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
    it('sets withCredentials on the forwarded request', async () => {
        const request = new HttpRequest('GET', '/api/v1/sensor/info');
        const forwarded: { req?: HttpRequest<unknown> } = {};
        const next = (r: HttpRequest<unknown>) => {
            forwarded.req = r;
            return of(undefined as never);
        };

        await firstValueFrom(authInterceptor(request, next));

        expect(forwarded.req?.withCredentials).toBe(true);
    });

    it('does not mutate the original request', async () => {
        const request = new HttpRequest('GET', '/api/v1/sensor/info');
        expect(request.withCredentials).toBe(false);

        await firstValueFrom(authInterceptor(request, () => of(undefined as never)));

        expect(request.withCredentials).toBe(false);
    });
});
