import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'sb-layout-error',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './layout-error.component.html',
    styleUrls: ['layout-error.component.scss'],
    standalone: false
})
export class LayoutErrorComponent {
    constructor() {}
}
