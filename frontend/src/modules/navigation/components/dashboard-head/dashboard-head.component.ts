import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BreadcrumbsComponent } from '../breadcrumbs/breadcrumbs.component';

@Component({
    selector: 'sb-dashboard-head',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard-head.component.html',
    styleUrls: ['dashboard-head.component.scss'],
    imports: [BreadcrumbsComponent],
})
export class DashboardHeadComponent {
    @Input() title!: string;
    @Input() hideBreadcrumbs = false;

    constructor() {}
}
