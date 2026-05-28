import { Router, UrlTree } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, isObservable, of } from 'rxjs';

import { AuthGuard } from './auth.guard';
import { AuthState, User } from '../models';
import { UserService } from '../services';

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

    function configureWith(user: User) {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: Router,
                    useValue: { createUrlTree: vi.fn().mockReturnValue(fakeUrlTree) },
                },
                { provide: UserService, useValue: { user$: of(user) } },
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
    });

    it('redirects signed-out visitors to /auth/login', async () => {
        configureWith({
            id: undefined,
            login: 'guest',
            email: 'guest',
            authState: AuthState.SignedOut,
        });

        await expect(runGuard()).resolves.toBe(fakeUrlTree);
    });
});
