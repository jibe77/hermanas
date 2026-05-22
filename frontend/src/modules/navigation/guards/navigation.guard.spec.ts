import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthState } from '@modules/auth/models';
import { UserService } from '@modules/auth/services';

import { NavigationGuard } from './navigation.guard';

describe('Navigation Guards', () => {
    let navigationGuard: NavigationGuard;
    let mockUserService: jasmine.SpyObj<UserService>;
    let mockRouter: jasmine.SpyObj<Router>;

    beforeEach(() => {
        mockUserService = jasmine.createSpyObj('UserService', [], {
            user$: of({
                authState: AuthState.SignedIn,
                backEndUser: 'test',
                backEndPassword: 'test',
            }),
        });
        mockRouter = jasmine.createSpyObj('Router', ['createUrlTree']);

        TestBed.configureTestingModule({
            imports: [],
            providers: [
                NavigationGuard,
                { provide: UserService, useValue: mockUserService },
                { provide: Router, useValue: mockRouter },
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
