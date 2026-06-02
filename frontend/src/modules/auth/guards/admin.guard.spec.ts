import { Router, UrlTree } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, isObservable, of } from 'rxjs';

import { AdminGuard } from './admin.guard';
import { AuthState, User } from '../models';
import { LoginModalService, UserService } from '../services';

/**
 * The guard is a `CanActivateFn`, so it runs inside an injection context. The
 * helper uses `runInInjectionContext` to keep the call lightweight.
 */
function runGuard(): Promise<boolean | UrlTree> {
    const guardResult = TestBed.runInInjectionContext(() =>
        AdminGuard(undefined as any, undefined as any)
    );
    // CanActivateFn can return boolean | UrlTree | Promise | Observable. Normalise to Promise.
    if (isObservable(guardResult)) {
        return firstValueFrom(guardResult);
    }
    return Promise.resolve(guardResult as boolean | UrlTree);
}

describe('AdminGuard', () => {
    const fakeUrlTree = { __urlTree: true } as unknown as UrlTree;
    let mockRouter: { createUrlTree: ReturnType<typeof vi.fn> };
    let user$: { next: (u: User) => void };
    let userServiceStub: { user$: any };
    let modalShow: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        // Sentinel UrlTree so we can assert the redirect target.
        mockRouter = { createUrlTree: vi.fn().mockReturnValue(fakeUrlTree) };

        // user$ is replaced per-test below; the stub is reused across tests.
        userServiceStub = { user$: undefined };
        user$ = {
            next: (u: User) => {
                userServiceStub.user$ = of(u);
            },
        };

        modalShow = vi.fn();

        TestBed.configureTestingModule({
            providers: [
                { provide: Router, useValue: mockRouter },
                { provide: UserService, useValue: userServiceStub },
                { provide: LoginModalService, useValue: { show: modalShow, hide: vi.fn() } },
            ],
        });
    });

    it('allows authenticated admins through', async () => {
        user$.next({
            id: 'marguerite',
            login: 'marguerite',
            email: 'marguerite@coop.local',
            authState: AuthState.SignedIn,
            roles: ['ADMIN'],
        });

        await expect(runGuard()).resolves.toBe(true);
        expect(mockRouter.createUrlTree).not.toHaveBeenCalled();
    });

    it('accepts the legacy Spring spelling ROLE_ADMIN too', async () => {
        user$.next({
            id: 'marguerite',
            login: 'marguerite',
            email: 'marguerite@coop.local',
            authState: AuthState.SignedIn,
            roles: ['ROLE_ADMIN'],
        });

        await expect(runGuard()).resolves.toBe(true);
    });

    it('redirects signed-in USERs to /dashboard without opening the modal', async () => {
        user$.next({
            id: 'alice',
            login: 'alice',
            email: 'alice@coop.local',
            authState: AuthState.SignedIn,
            roles: ['USER'],
        });

        await expect(runGuard()).resolves.toBe(fakeUrlTree);
        expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
        expect(modalShow).not.toHaveBeenCalled();
    });

    it('redirects signed-out visitors to /dashboard and opens the login modal', async () => {
        user$.next({
            id: undefined,
            login: 'guest',
            email: 'guest',
            authState: AuthState.SignedOut,
            roles: [],
        });

        await expect(runGuard()).resolves.toBe(fakeUrlTree);
        expect(modalShow).toHaveBeenCalledTimes(1);
    });

    it('redirects when roles is undefined', async () => {
        user$.next({
            id: 'alice',
            login: 'alice',
            email: 'alice@coop.local',
            authState: AuthState.SignedIn,
        });

        await expect(runGuard()).resolves.toBe(fakeUrlTree);
    });
});
