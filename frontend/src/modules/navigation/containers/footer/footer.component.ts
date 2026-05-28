import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'sb-footer',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './footer.component.html',
    styleUrls: ['footer.component.scss'],
    standalone: false
})
export class FooterComponent {
    constructor() {}
}
