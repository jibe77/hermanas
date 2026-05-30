import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    Input,
    Signal,
    inject,
} from '@angular/core';
import { UserService } from '@modules/auth/services';
import { AuthState } from '@modules/auth/models';
import { LoggerService } from '@common/services';
import { SideNavItems, SideNavSection } from '@modules/navigation/models';
import { NavigationService } from '@modules/navigation/services';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

@Component({
    selector: 'sb-side-nav',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './side-nav.component.html',
    styleUrls: ['side-nav.component.scss'],
    imports: [RouterLink, RouterLinkActive, FaIconComponent],
})
export class SideNavComponent {
    navigationService = inject(NavigationService);
    userService = inject(UserService);
    private logger = inject(LoggerService);

    @Input() sideNavItems!: SideNavItems;
    @Input() sideNavSections!: SideNavSection[];

    // Derived signals: Angular re-evaluates only the *ngIfs that read them when the user
    // signal changes — no need to wrap the whole template in a *ngIf, which would otherwise
    // destroy and recreate every menu link on every state change.
    readonly isSignedIn: Signal<boolean>;
    readonly isAdmin: Signal<boolean>;
    readonly currentLogin: Signal<string>;

    constructor() {
        this.isSignedIn = computed(() => this.userService.user().authState === AuthState.SignedIn);
        this.isAdmin = computed(() => {
            const u = this.userService.user();
            if (u.authState !== AuthState.SignedIn) {
                return false;
            }
            const roles = u.roles ?? [];
            return roles.some(r => r === 'ADMIN' || r === 'ROLE_ADMIN');
        });
        this.currentLogin = computed(() => this.userService.user().login);

        // Debug helper for the "authenticated-user menu entries don't show up"
        // bug: logs every time the auth signal changes. Inspect the browser
        // console after login / reload to confirm whether the signal flips to
        // SignedIn at all. Remove once the bug is firmly closed.
        effect(() => {
            const u = this.userService.user();
            this.logger.info(
                'SideNav auth state',
                {
                    login: u.login,
                    authState: u.authState,
                    roles: u.roles,
                    isSignedIn: u.authState === AuthState.SignedIn,
                },
                'SideNavComponent'
            );
        });
    }
}
