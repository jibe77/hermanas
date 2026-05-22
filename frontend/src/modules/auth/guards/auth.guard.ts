import { Injectable } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { User, AuthState } from '../models';
import { UserService } from '../services';

@Injectable()
export class AuthGuard {
    constructor(private userService: UserService, private router: Router) {}

    canActivate(): Observable<boolean | UrlTree> {
        return this.userService.user$.pipe(
            take(1),
            map((user: User) => {
                if (user && user.authState === AuthState.SignedIn) {
                    return true;
                }
                // Redirect to login page if not authenticated
                return this.router.createUrlTree(['/auth/login']);
            })
        );
    }
}
