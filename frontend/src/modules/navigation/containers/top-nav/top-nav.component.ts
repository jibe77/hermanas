import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NavigationService } from '@modules/navigation/services';

@Component({
    selector: 'sb-top-nav',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav.component.html',
    styleUrls: ['top-nav.component.scss'],
    standalone: false,
})
export class TopNavComponent {
    private navigationService = inject(NavigationService);

    toggleSideNav() {
        this.navigationService.toggleSideNav();
    }
}
