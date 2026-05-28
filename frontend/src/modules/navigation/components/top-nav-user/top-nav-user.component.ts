import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { Router } from '@angular/router';
import { LoginService, UserService } from '@modules/auth/services';
import { AuthState } from '@modules/auth/models';
import { NavigationService } from '@modules/navigation/services';
import { Subscription } from 'rxjs';

@Component({
    selector: 'sb-top-nav-user',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav-user.component.html',
    styleUrls: ['top-nav-user.component.scss'],
    standalone: false,
})
export class TopNavUserComponent implements OnInit, OnDestroy {
    authState: AuthState = AuthState.SignedOut;
    subscription: Subscription = new Subscription();

    constructor(
        public navigationService: NavigationService,
        public userService: UserService,
        private loginService: LoginService,
        private changeDetectorRef: ChangeDetectorRef,
        private router: Router
    ) {}

    ngOnInit() {
        this.userService.checkAuthState();
        this.subscription.add(
            this.userService.user$.subscribe(u => {
                this.authState = (u.authState as AuthState) || AuthState.SignedOut;
                this.changeDetectorRef.markForCheck();
            })
        );
    }

    async logout(): Promise<void> {
        await this.loginService.logout();
        this.router.navigate(['/dashboard']);
        this.changeDetectorRef.markForCheck();
    }

    navigateTo(url: string) {
        this.router.navigate([url]);
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}
