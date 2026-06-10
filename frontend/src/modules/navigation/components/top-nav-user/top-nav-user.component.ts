import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { Router } from '@angular/router';
import { LoginModalService, LoginService, UserService } from '@modules/auth/services';
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
    imports: [NgbDropdown, NgbDropdownToggle, FaIconComponent, NgbDropdownMenu, AsyncPipe],
})
export class TopNavUserComponent implements OnInit, OnDestroy {
    navigationService = inject(NavigationService);
    userService = inject(UserService);
    private loginService = inject(LoginService);
    private loginModal = inject(LoginModalService);
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

    openLogin(): void {
        this.loginModal.show();
    }

    openRegister(): void {
        this.loginModal.show('register');
    }

    /**
     * Front-only demo mode for showcasing the application during job interviews.
     * Flips the synthetic user to ADMIN so every protected panel and menu entry
     * unfolds without a real backend login. Mutating calls are blocked by
     * demoModeInterceptor with a warning toast.
     */
    enableDemo(): void {
        this.userService.enableDemoMode();
        const currentUrl = this.router.url || '/dashboard';
        this.router.navigateByUrl(currentUrl);
        this.changeDetectorRef.markForCheck();
    }

    disableDemo(): void {
        this.userService.disableDemoMode();
        const currentUrl = this.router.url || '/dashboard';
        this.router.navigateByUrl(currentUrl);
        this.changeDetectorRef.markForCheck();
    }

    isDemoMode(): boolean {
        return this.userService.isDemoMode();
    }

    async logout(): Promise<void> {
        await this.loginService.logout();
        // Re-trigger the current route (or fall back to /dashboard for protected
        // pages — the auth guard will redirect signed-out users away from admin
        // screens). The router is configured with onSameUrlNavigation: 'reload'
        // so this rebuilds the component tree and clears any admin-only UI that
        // is still mounted.
        const currentUrl = this.router.url || '/dashboard';
        this.router.navigateByUrl(currentUrl);
        this.changeDetectorRef.markForCheck();
    }

    navigateTo(url: string) {
        this.router.navigate([url]);
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}
