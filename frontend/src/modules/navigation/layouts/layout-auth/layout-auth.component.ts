import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'sb-layout-auth',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './layout-auth.component.html',
    styleUrls: ['layout-auth.component.scss'],
    standalone: false,
})
export class LayoutAuthComponent {
    constructor() {}
}
