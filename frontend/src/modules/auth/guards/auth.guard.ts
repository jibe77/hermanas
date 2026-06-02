import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs/operators';

import { User, AuthState } from '../models';
import { UserService, LoginModalService } from '../services';

export const AuthGuard: CanActivateFn = () => {
    const userService = inject(UserService);
    const router = inject(Router);
    const modal = inject(LoginModalService);
    return userService.user$.pipe(
        take(1),
        map((user: User) => {
            if (user && user.authState === AuthState.SignedIn) {
                return true;
            }
            modal.show();
            return router.createUrlTree(['/dashboard']);
        })
    );
};
