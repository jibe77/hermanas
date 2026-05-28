import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthState } from '../models';
import { UserService } from './user.service';

describe('UserService', () => {
    let userService: UserService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        userService = TestBed.inject(UserService);
    });

    describe('default state', () => {
        it('starts as a signed-out guest', () => {
            const user = userService.getCurrentUser();
            expect(user.authState).toBe(AuthState.SignedOut);
            expect(user.login).toBe('guest');
            expect(user.roles).toEqual([]);
        });

        it('reports isAdmin() = false when signed out', () => {
            expect(userService.isAdmin()).toBe(false);
        });
    });

    describe('setSignedInUser', () => {
        it('updates the user signal with the given roles', () => {
            userService.setSignedInUser('marguerite', ['ADMIN']);
            const user = userService.getCurrentUser();
            expect(user.login).toBe('marguerite');
            expect(user.authState).toBe(AuthState.SignedIn);
            expect(user.roles).toEqual(['ADMIN']);
        });

        it('isAdmin() returns true for ADMIN role', () => {
            userService.setSignedInUser('marguerite', ['ADMIN']);
            expect(userService.isAdmin()).toBe(true);
        });

        it('isAdmin() also accepts legacy ROLE_ADMIN spelling', () => {
            userService.setSignedInUser('marguerite', ['ROLE_ADMIN']);
            expect(userService.isAdmin()).toBe(true);
        });

        it('isAdmin() returns false for a USER role', () => {
            userService.setSignedInUser('alice', ['USER']);
            expect(userService.isAdmin()).toBe(false);
        });

        it('isAdmin() returns false when roles are empty', () => {
            userService.setSignedInUser('alice', []);
            expect(userService.isAdmin()).toBe(false);
        });
    });

    describe('setSignedOutUser', () => {
        it('resets to the guest default', () => {
            userService.setSignedInUser('marguerite', ['ADMIN']);
            userService.setSignedOutUser();

            const user = userService.getCurrentUser();
            expect(user.authState).toBe(AuthState.SignedOut);
            expect(user.login).toBe('guest');
            expect(user.roles).toEqual([]);
            expect(userService.isAdmin()).toBe(false);
        });
    });
});
