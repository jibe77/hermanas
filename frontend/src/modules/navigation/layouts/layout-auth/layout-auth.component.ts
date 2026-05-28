import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FooterComponent } from '../../containers/footer/footer.component';

@Component({
    selector: 'sb-layout-auth',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './layout-auth.component.html',
    styleUrls: ['layout-auth.component.scss'],
    imports: [FooterComponent],
})
export class LayoutAuthComponent {
    constructor() {}
}
