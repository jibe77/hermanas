import { Router, UrlTree } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, isObservable, of } from 'rxjs';

import { AuthGuard } from './auth.guard';
import { AuthState, User } from '../models';
import { LoginModalService, UserService } from '../services';

function runGuard(): Promise<boolean | UrlTree> {
    const guardResult = TestBed.runInInjectionContext(() =>
        AuthGuard(undefined as any, undefined as any)
    );
    if (isObservable(guardResult)) {
        return firstValueFrom(guardResult);
    }
    return Promise.resolve(guardResult as boolean | UrlTree);
}

describe('AuthGuard', () => {
    const fakeUrlTree = { __urlTree: true } as unknown as UrlTree;
    let modalShow: ReturnType<typeof vi.fn>;

    function configureWith(user: User) {
        modalShow = vi.fn();
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: Router,
                    useValue: { createUrlTree: vi.fn().mockReturnValue(fakeUrlTree) },
                },
                { provide: UserService, useValue: { user$: of(user) } },
                { provide: LoginModalService, useValue: { show: modalShow, hide: vi.fn() } },
            ],
        });
    }

    it('allows any signed-in user through', async () => {
        configureWith({
            id: 'alice',
            login: 'alice',
            email: 'alice@coop.local',
            authState: AuthState.SignedIn,
        });

        await expect(runGuard()).resolves.toBe(true);
        expect(modalShow).not.toHaveBeenCalled();
    });

    it('redirects signed-out visitors to /dashboard and opens the login modal', async () => {
        configureWith({
            id: undefined,
            login: 'guest',
            email: 'guest',
            authState: AuthState.SignedOut,
        });

        await expect(runGuard()).resolves.toBe(fakeUrlTree);
        expect(modalShow).toHaveBeenCalledTimes(1);
    });
});
