import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';

import { AuthState, User } from '../models';
import { UserService } from '../services';

/**
 * Allows navigation only to authenticated administrators. Anonymous or USER
 * sessions get redirected to /auth/login. The check accepts both the canonical
 * `ADMIN` role and Spring's legacy `ROLE_ADMIN` spelling.
 */
export const AdminGuard: CanActivateFn = () => {
    const userService = inject(UserService);
    const router = inject(Router);
    return userService.user$.pipe(
        take(1),
        map((user: User) => {
            const isSignedIn = !!user && user.authState === AuthState.SignedIn;
            const roles = (user?.roles ?? []) as string[];
            const isAdmin = roles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN');
            if (isSignedIn && isAdmin) {
                return true;
            }
            return router.createUrlTree(['/auth/login']);
        })
    );
};
