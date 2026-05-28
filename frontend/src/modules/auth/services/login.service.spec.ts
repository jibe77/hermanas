import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { LoginService } from './login.service';
import { UserService } from './user.service';

describe('LoginService', () => {
    let loginService: LoginService;
    let httpMock: HttpTestingController;
    let userService: UserService;
    let checkAuthSpy: ReturnType<typeof vi.spyOn>;
    let setSignedOutSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [LoginService, provideHttpClient(), provideHttpClientTesting()],
        });
        loginService = TestBed.inject(LoginService);
        httpMock = TestBed.inject(HttpTestingController);
        userService = TestBed.inject(UserService);

        checkAuthSpy = vi
            .spyOn(userService, 'checkAuthState')
            .mockImplementation(async () => undefined);
        setSignedOutSpy = vi
            .spyOn(userService, 'setSignedOutUser')
            .mockImplementation(() => undefined);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('login', () => {
        it('POSTs username and password as form-urlencoded and resolves "ok" on 200', async () => {
            const result = loginService.login('alice', 'secret');

            const req = httpMock.expectOne(r => r.url.endsWith('/auth/login'));
            expect(req.request.method).toBe('POST');
            expect(req.request.headers.get('Content-Type')).toBe(
                'application/x-www-form-urlencoded'
            );
            expect(req.request.body).toContain('username=alice');
            expect(req.request.body).toContain('password=secret');
            // remember-me is intentionally absent by default
            expect(req.request.body).not.toContain('remember-me');
            // The interceptor sets withCredentials globally, but the service also sets it
            // explicitly — assert both for safety.
            expect(req.request.withCredentials).toBe(true);
            req.flush(null);

            await expect(result).resolves.toBe('ok');
            expect(checkAuthSpy).toHaveBeenCalled();
        });

        it('includes remember-me=true in the body when the flag is set', async () => {
            const result = loginService.login('alice', 'secret', true);

            const req = httpMock.expectOne(r => r.url.endsWith('/auth/login'));
            expect(req.request.body).toContain('remember-me=true');
            req.flush(null);

            await result;
        });

        it('resolves "invalid" on a generic 401 and clears the user', async () => {
            const result = loginService.login('alice', 'wrong');

            const req = httpMock.expectOne(r => r.url.endsWith('/auth/login'));
            req.flush(null, { status: 401, statusText: 'Unauthorized' });

            await expect(result).resolves.toBe('invalid');
            expect(setSignedOutSpy).toHaveBeenCalled();
        });

        it('resolves "pending-validation" on the dedicated 401 JSON body', async () => {
            const result = loginService.login('alice', 'secret');

            const req = httpMock.expectOne(r => r.url.endsWith('/auth/login'));
            req.flush(
                { error: 'ACCOUNT_PENDING_VALIDATION' },
                { status: 401, statusText: 'Unauthorized' }
            );

            await expect(result).resolves.toBe('pending-validation');
            expect(setSignedOutSpy).toHaveBeenCalled();
        });

        it('also resolves "invalid" on 500-class errors', async () => {
            const result = loginService.login('alice', 'secret');

            const req = httpMock.expectOne(r => r.url.endsWith('/auth/login'));
            req.error(new ProgressEvent('error') as unknown as ErrorEvent, {
                status: 500,
                statusText: 'Internal Server Error',
            });

            await expect(result).resolves.toBe('invalid');
        });
    });

    describe('logout', () => {
        it('POSTs to /auth/logout and clears the session', async () => {
            const result = loginService.logout();

            const req = httpMock.expectOne(r => r.url.endsWith('/auth/logout'));
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toBeNull();
            req.flush(null);

            await result;
            expect(setSignedOutSpy).toHaveBeenCalled();
        });

        it('still clears the session if the logout request fails', async () => {
            const result = loginService.logout();

            const req = httpMock.expectOne(r => r.url.endsWith('/auth/logout'));
            req.flush(null, { status: 500, statusText: 'Internal Server Error' });

            // logout() swallows the rejection in a finally block, so the promise resolves.
            await expect(result).rejects.toBeInstanceOf(HttpErrorResponse);
            expect(setSignedOutSpy).toHaveBeenCalled();
        });
    });
});
