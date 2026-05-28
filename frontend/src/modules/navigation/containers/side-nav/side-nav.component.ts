import { ChangeDetectionStrategy, Component, computed, Input, Signal } from '@angular/core';
import { UserService } from '@modules/auth/services';
import { AuthState } from '@modules/auth/models';
import { SideNavItems, SideNavSection } from '@modules/navigation/models';
import { NavigationService } from '@modules/navigation/services';

@Component({
    selector: 'sb-side-nav',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './side-nav.component.html',
    styleUrls: ['side-nav.component.scss'],
})
export class SideNavComponent {
    @Input() sideNavItems!: SideNavItems;
    @Input() sideNavSections!: SideNavSection[];

    // Derived signals: Angular re-evaluates only the *ngIfs that read them when the user
    // signal changes — no need to wrap the whole template in a *ngIf, which would otherwise
    // destroy and recreate every menu link on every state change.
    readonly isSignedIn: Signal<boolean>;
    readonly currentLogin: Signal<string>;

    constructor(
        public navigationService: NavigationService,
        public userService: UserService
    ) {
        this.isSignedIn = computed(() => this.userService.user().authState === AuthState.SignedIn);
        this.currentLogin = computed(() => this.userService.user().login);
    }
}
