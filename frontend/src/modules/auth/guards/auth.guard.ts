import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';

import { User, AuthState } from '../models';
import { UserService } from '../services';

export const AuthGuard: CanActivateFn = () => {
    const userService = inject(UserService);
    const router = inject(Router);
    return userService.user$.pipe(
        take(1),
        map((user: User) => {
            if (user && user.authState === AuthState.SignedIn) {
                return true;
            }
            return router.createUrlTree(['/auth/login']);
        })
    );
};
