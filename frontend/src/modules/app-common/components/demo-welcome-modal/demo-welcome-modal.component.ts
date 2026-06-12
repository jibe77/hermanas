import {
    ChangeDetectionStrategy,
    Component,
    HostListener,
    inject,
} from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

import { DemoWelcomeService } from '@common/services';

/**
 * Shown once when the visitor enables demo mode from the user menu. Reads
 * its open/closed state from {@link DemoWelcomeService} and dismisses on
 * Escape, backdrop click, or the "Got it" button. Mounted at the root of
 * the SPA so it overlays every route including the top-nav and side-nav.
 */
@Component({
    selector: 'sb-demo-welcome-modal',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './demo-welcome-modal.component.html',
    styleUrls: ['./demo-welcome-modal.component.scss'],
    imports: [FaIconComponent],
})
export class DemoWelcomeModalComponent {
    private service = inject(DemoWelcomeService);

    readonly open = this.service.open;

    @HostListener('document:keydown.escape')
    onEscape(): void {
        if (this.open()) {
            this.close();
        }
    }

    close(): void {
        this.service.close();
    }
}
