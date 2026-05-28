import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';
import { LoadingService } from '../../services/loading/loading.service';

@Component({
    selector: 'sb-loading-spinner',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './loading-spinner.component.html',
    styleUrls: ['./loading-spinner.component.scss'],
    animations: [
        trigger('fadeAnimation', [
            transition(':enter', [
                style({ opacity: 0 }),
                animate('200ms ease-in', style({ opacity: 1 })),
            ]),
            transition(':leave', [animate('200ms ease-out', style({ opacity: 0 }))]),
        ]),
    ],
    standalone: false,
})
export class LoadingSpinnerComponent {
    loadingService = inject(LoadingService);
}
