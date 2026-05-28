import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CommonCardsComponent } from '../../../app-common/components/common-cards/common-cards.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { DashboardWidgetsComponent } from '../../components/dashboard-widgets/dashboard-widgets.component';

@Component({
    selector: 'sb-dashboard',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard.component.html',
    styleUrls: ['dashboard.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CommonCardsComponent,
        CardComponent,
        DashboardWidgetsComponent,
    ],
})
export class DashboardComponent {
    notificationSubject: Subject<void> = new Subject<void>();
    retrySubject: Subject<void> = new Subject<void>();
    domainBase = environment.apiUrl;

    constructor() {}

    onServiceCommunicationError(_event: any) {
        this.notificationSubject.next();
    }

    onServiceRetry(_event: any) {
        this.retrySubject.next();
    }
}
