import { ChangeDetectionStrategy, Component} from '@angular/core';
import { Subject } from 'rxjs';

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
})
export class ChartsComponent {
    constructor() {}

    notificationSubject: Subject<void> = new Subject<void>();
    retrySubject: Subject<void> = new Subject<void>();

    onServiceCommunicationError(event: any) {
        this.notificationSubject.next();
    }

    onServiceRetry(event: any) {
        this.retrySubject.next();
    }
}
