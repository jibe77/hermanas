import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Component({
    selector: 'sb-dashboard',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard.component.html',
    styleUrls: ['dashboard.component.scss'],
    standalone: false,
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
