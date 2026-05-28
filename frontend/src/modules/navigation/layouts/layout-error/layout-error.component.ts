import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FooterComponent } from '../../containers/footer/footer.component';

@Component({
    selector: 'sb-layout-error',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './layout-error.component.html',
    styleUrls: ['layout-error.component.scss'],
    imports: [FooterComponent],
})
export class LayoutErrorComponent {
    constructor() {}
}
