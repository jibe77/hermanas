import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { LoginModalService } from '@modules/auth/services';
import { NavigationService } from '@modules/navigation/services';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
    AdventOverlayComponent,
    ConfettiComponent,
    DiscoOverlayComponent,
    EasterOverlayComponent,
} from '@modules/easter-eggs/components';
import { EasterEggsService } from '@modules/easter-eggs/services';
import { ChickensStripComponent } from '../../components/chickens-strip/chickens-strip.component';
import { TopNavLangComponent } from '../../components/top-nav-lang/top-nav-lang.component';
import { TopNavUserComponent } from '../../components/top-nav-user/top-nav-user.component';

@Component({
    selector: 'sb-top-nav',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav.component.html',
    styleUrls: ['top-nav.component.scss'],
    imports: [
        RouterLink,
        FaIconComponent,
        ChickensStripComponent,
        TopNavLangComponent,
        TopNavUserComponent,
        ConfettiComponent,
        DiscoOverlayComponent,
        AdventOverlayComponent,
        EasterOverlayComponent,
    ],
})
export class TopNavComponent {
    private navigationService = inject(NavigationService);
    private loginModal = inject(LoginModalService);
    private ee = inject(EasterEggsService);

    readonly advent = computed(() => this.ee.advent());
    readonly easter = computed(() => this.ee.easter());

    toggleSideNav() {
        if (this.loginModal.open()) {
            return;
        }
        this.navigationService.toggleSideNav();
    }

    onBrandClick(): void {
        this.ee.onBrandClick();
    }
}
