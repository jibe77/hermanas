import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { trigger, style, transition, animate } from '@angular/animations';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NetworkStatusService } from '../../services/network-status/network-status.service';
import { map } from 'rxjs/operators';

@Component({
    selector: 'sb-offline-banner',
    templateUrl: './offline-banner.component.html',
    styleUrls: ['./offline-banner.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('slideDown', [
            transition(':enter', [
                style({ transform: 'translateY(-100%)' }),
                animate('200ms ease-out', style({ transform: 'translateY(0)' })),
            ]),
            transition(':leave', [
                animate('150ms ease-in', style({ transform: 'translateY(-100%)' })),
            ]),
        ]),
    ],
    imports: [AsyncPipe, FaIconComponent],
})
export class OfflineBannerComponent {
    private network = inject(NetworkStatusService);
    public offline$ = this.network.online$.pipe(map(online => !online));
}
