import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { trigger, style, transition, animate } from '@angular/animations';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { PwaInstallService } from '../../services/pwa-install/pwa-install.service';

@Component({
    selector: 'sb-pwa-install-banner',
    templateUrl: './pwa-install-banner.component.html',
    styleUrls: ['./pwa-install-banner.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('slideUp', [
            transition(':enter', [
                style({ transform: 'translateY(100%)' }),
                animate('250ms ease-out', style({ transform: 'translateY(0)' })),
            ]),
            transition(':leave', [
                animate('200ms ease-in', style({ transform: 'translateY(100%)' })),
            ]),
        ]),
    ],
    imports: [AsyncPipe, FaIconComponent],
})
export class PwaInstallBannerComponent {
    private pwa = inject(PwaInstallService);
    public canInstall$ = this.pwa.canInstall$;

    install(): void {
        void this.pwa.promptInstall();
    }

    dismiss(): void {
        this.pwa.dismiss();
    }
}
