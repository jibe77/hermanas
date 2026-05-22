import { signal, WritableSignal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { AuthState } from '@modules/auth/models/auth-state';
import { UserService } from '@modules/auth/services';
import { MockUser, User } from '@testing/mocks';
import { Observable } from 'rxjs';

const mockUser = new MockUser();

// @ts-ignore
export class UserServiceStub implements UserService {
    private _user: WritableSignal<User> = signal(mockUser);
    private _user$: Observable<User> = toObservable(this._user);

    get user(): WritableSignal<User> {
        return this._user;
    }

    set user(user: User) {
        this._user.set(user);
    }

    get user$(): Observable<User> {
        return this._user$;
    }

    getCurrentUser(): User {
        return this._user();
    }

    setSignedInUser(username: string, roles: string[] = []): void {
        this._user.set({
            id: username,
            login: username,
            email: username,
            authState: AuthState.SignedIn,
            roles,
        });
    }

    setSignedOutUser(): void {
        this._user.set({
            id: undefined,
            email: 'guest',
            login: 'guest',
            authState: AuthState.SignedOut,
            roles: [],
        });
    }

    async checkAuthState(): Promise<void> {
        // no-op in tests
    }
}
