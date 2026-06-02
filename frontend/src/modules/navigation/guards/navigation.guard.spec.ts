import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthState, User } from '@modules/auth/models';
import { LoginModalService, UserService } from '@modules/auth/services';

import { NavigationGuard } from './navigation.guard';

describe('Navigation Guards', () => {
    let navigationGuard: NavigationGuard;
    let mockUserService: { user$: ReturnType<typeof of<User>> };
    let mockRouter: Partial<Router>;

    beforeEach(() => {
        mockUserService = {
            user$: of({
                id: 'test',
                login: 'test',
                email: 'test',
                authState: AuthState.SignedIn,
            } as User),
        };
        mockRouter = { createUrlTree: vi.fn() };

        TestBed.configureTestingModule({
            imports: [],
            providers: [
                NavigationGuard,
                { provide: UserService, useValue: mockUserService },
                { provide: Router, useValue: mockRouter },
                { provide: LoginModalService, useValue: { show: vi.fn(), hide: vi.fn() } },
            ],
        });
        navigationGuard = TestBed.inject(NavigationGuard);
    });

    describe('canActivate', () => {
        it('should return an Observable<boolean>', () => {
            navigationGuard.canActivate().subscribe(response => {
                expect(response).toEqual(true);
            });
        });
    });
});
