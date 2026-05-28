import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'sb-error-500',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './error-500.component.html',
    styleUrls: ['error-500.component.scss'],
    standalone: false
})
export class Error500Component {
    constructor() {}
}
