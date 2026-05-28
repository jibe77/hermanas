import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'sb-dashboard-head',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard-head.component.html',
    styleUrls: ['dashboard-head.component.scss'],
    standalone: false
})
export class DashboardHeadComponent {
    @Input() title!: string;
    @Input() hideBreadcrumbs = false;

    constructor() {}
}
