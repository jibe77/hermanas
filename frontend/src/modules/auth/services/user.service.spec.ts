import { TestBed } from '@angular/core/testing';
import { MockUser } from '@testing/mocks';
import { skip, take } from 'rxjs/operators';

import { UserService } from './user.service';

const mockUser = new MockUser();

describe('UserService', () => {
    let userService: UserService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [UserService],
        });
        userService = TestBed.inject(UserService);
    });

    describe('getUser$', () => {
        it('should return Observable<User>', done => {
            // Subscribe first, skip initial value, then set user to trigger emission
            userService.user$.pipe(skip(1), take(1)).subscribe(response => {
                expect(response).toEqual(mockUser);
                done();
            });
            // Set user after subscription to trigger emission
            userService.user = mockUser;
        });
    });

    describe('getCurrentUser', () => {
        it('should return current user synchronously', () => {
            userService.user = mockUser;
            const currentUser = userService.getCurrentUser();
            expect(currentUser).toEqual(mockUser);
        });
    });

    describe('user signal', () => {
        it('should update signal when user is set', () => {
            userService.user = mockUser;
            expect(userService.user()).toEqual(mockUser);
        });
    });
});
