import { Injectable } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { User, AuthState } from '@modules/auth/models';
import { UserService } from '@modules/auth/services';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

@Injectable()
export class NavigationGuard {
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
