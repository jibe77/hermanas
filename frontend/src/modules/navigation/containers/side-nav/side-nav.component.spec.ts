import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UserService } from '@modules/auth/services';
import { NavigationService } from '@modules/navigation/services';

import { SideNavComponent } from './side-nav.component';

/**
 * SideNavComponent's signals (isSignedIn / isAdmin / currentLogin) are derived
 * from UserService.user(). These tests check the derivations, which decide
 * which entries of the side nav are visible to each persona.
 *
 * NavigationService is mocked because the real one pulls in Router + ActivatedRoute,
 * neither of which is needed to exercise the user-derived signals.
 */
describe('SideNavComponent', () => {
    let component: SideNavComponent;
    let userService: UserService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: NavigationService, useValue: {} },
                SideNavComponent,
            ],
        });

        component = TestBed.inject(SideNavComponent);
        userService = TestBed.inject(UserService);
    });

    it('reports a signed-out guest by default', () => {
        expect(component.isSignedIn()).toBe(false);
        expect(component.isAdmin()).toBe(false);
        expect(component.currentLogin()).toBe('guest');
    });

    it('flips isSignedIn() when the user signs in', () => {
        userService.setSignedInUser('alice', ['USER']);
        expect(component.isSignedIn()).toBe(true);
        expect(component.currentLogin()).toBe('alice');
    });

    it('isAdmin() is true only for ADMIN role', () => {
        userService.setSignedInUser('alice', ['USER']);
        expect(component.isAdmin()).toBe(false);

        userService.setSignedInUser('marguerite', ['ADMIN']);
        expect(component.isAdmin()).toBe(true);

        userService.setSignedInUser('legacy', ['ROLE_ADMIN']);
        expect(component.isAdmin()).toBe(true);
    });

    it('reverts to guest on sign-out', () => {
        userService.setSignedInUser('marguerite', ['ADMIN']);
        userService.setSignedOutUser();

        expect(component.isSignedIn()).toBe(false);
        expect(component.isAdmin()).toBe(false);
        expect(component.currentLogin()).toBe('guest');
    });
});
