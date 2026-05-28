import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'sb-error-401',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './error-401.component.html',
    styleUrls: ['error-401.component.scss'],
    standalone: false,
})
export class Error401Component {
    constructor() {}
}
