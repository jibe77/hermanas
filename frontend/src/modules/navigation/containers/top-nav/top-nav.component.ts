import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NavigationService } from '@modules/navigation/services';

@Component({
    selector: 'sb-top-nav',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav.component.html',
    styleUrls: ['top-nav.component.scss'],
})
export class TopNavComponent {
    constructor(private navigationService: NavigationService) {}
    toggleSideNav() {
        this.navigationService.toggleSideNav();
    }
}
