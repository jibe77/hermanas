import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LoginService, UserService } from '@modules/auth/services';
import { AuthState } from '@modules/auth/models';
import { NavigationService } from '@modules/navigation/services';
import { Subscription } from 'rxjs';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { AsyncPipe } from '@angular/common';

@Component({
    selector: 'sb-top-nav-user',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav-user.component.html',
    styleUrls: ['top-nav-user.component.scss'],
    imports: [
        NgbDropdown,
        NgbDropdownToggle,
        FaIconComponent,
        NgbDropdownMenu,
        RouterLink,
        AsyncPipe,
    ],
})
export class TopNavUserComponent implements OnInit, OnDestroy {
    navigationService = inject(NavigationService);
    userService = inject(UserService);
    private loginService = inject(LoginService);
    private changeDetectorRef = inject(ChangeDetectorRef);
    private router = inject(Router);

    authState: AuthState = AuthState.SignedOut;
    subscription: Subscription = new Subscription();

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
