import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MockUser } from '@testing/mocks';

import { UserService } from './user.service';

const mockUser = new MockUser();

describe('UserService', () => {
    let userService: UserService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        userService = TestBed.inject(UserService);
    });

    // The Observable<User> path goes through Angular's toObservable() which emits
    // asynchronously via the microtask queue. Karma + Jasmine plain `done()` is
    // racy with that — see the Vitest port for a fakeAsync version.

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
