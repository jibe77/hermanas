import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';

import { AuthState, User } from '../models';
import { UserService, LoginModalService } from '../services';

/**
 * Allows navigation only to authenticated administrators. Anonymous or USER
 * sessions are bounced to /dashboard with the login modal opened. The check
 * accepts both the canonical `ADMIN` role and Spring's legacy `ROLE_ADMIN`.
 */
export const AdminGuard: CanActivateFn = () => {
    const userService = inject(UserService);
    const router = inject(Router);
    const modal = inject(LoginModalService);
    return userService.user$.pipe(
        take(1),
        map((user: User) => {
            const isSignedIn = !!user && user.authState === AuthState.SignedIn;
            const roles = (user?.roles ?? []) as string[];
            const isAdmin = roles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN');
            if (isSignedIn && isAdmin) {
                return true;
            }
            if (!isSignedIn) {
                modal.show();
            }
            return router.createUrlTree(['/dashboard']);
        })
    );
};
