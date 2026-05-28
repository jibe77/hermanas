import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Subject } from 'rxjs';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CommonCardsComponent } from '../../../app-common/components/common-cards/common-cards.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { WeatherTableAreaComponent } from '../../components/weather-table-area/weather-table-area.component';

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CommonCardsComponent,
        CardComponent,
        FaIconComponent,
        WeatherTableAreaComponent,
    ],
})
export class ChartsComponent {
    constructor() {}

    notificationSubject: Subject<void> = new Subject<void>();
    retrySubject: Subject<void> = new Subject<void>();

    onServiceCommunicationError(_event: any) {
        this.notificationSubject.next();
    }

    onServiceRetry(_event: any) {
        this.retrySubject.next();
    }
}
