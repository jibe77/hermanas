import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LayoutErrorComponent } from '../../../navigation/layouts/layout-error/layout-error.component';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

@Component({
    selector: 'sb-error-500',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './error-500.component.html',
    styleUrls: ['error-500.component.scss'],
    imports: [
        LayoutErrorComponent,
        RouterLink,
        FaIconComponent,
    ],
})
export class Error500Component {
    constructor() {}
}
