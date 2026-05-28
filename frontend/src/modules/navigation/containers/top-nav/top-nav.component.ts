import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NavigationService } from '@modules/navigation/services';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { TopNavLangComponent } from '../../components/top-nav-lang/top-nav-lang.component';
import { TopNavUserComponent } from '../../components/top-nav-user/top-nav-user.component';

@Component({
    selector: 'sb-top-nav',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav.component.html',
    styleUrls: ['top-nav.component.scss'],
    imports: [RouterLink, FaIconComponent, TopNavLangComponent, TopNavUserComponent],
})
export class TopNavComponent {
    private navigationService = inject(NavigationService);

    toggleSideNav() {
        this.navigationService.toggleSideNav();
    }
}
