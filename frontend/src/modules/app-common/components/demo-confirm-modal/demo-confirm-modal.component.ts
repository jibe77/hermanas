import {
    ChangeDetectionStrategy,
    Component,
    HostListener,
    inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

import { DemoConfirmService } from '@common/services';

/**
 * Modal shown in demo mode every time the visitor triggers a mutating call.
 * Reads its open/closed state and message from {@link DemoConfirmService}, and
 * forwards the user's decision back to the service through accept/reject.
 *
 * Mounted at the root of the SPA (see app.component) so it overlays every
 * page including the side-nav and top-nav.
 */
@Component({
    selector: 'sb-demo-confirm-modal',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './demo-confirm-modal.component.html',
    styleUrls: ['./demo-confirm-modal.component.scss'],
    imports: [FormsModule, FaIconComponent],
})
export class DemoConfirmModalComponent {
    private service = inject(DemoConfirmService);

    readonly open = this.service.open;
    readonly message = this.service.message;

    /** Bound to the "Do not show again this session" checkbox. */
    suppressFurther = false;

    @HostListener('document:keydown.escape')
    onEscape(): void {
        if (this.open()) {
            this.cancel();
        }
    }

    accept(): void {
        this.service.accept(this.suppressFurther);
        this.suppressFurther = false;
    }

    cancel(): void {
        this.service.reject();
        this.suppressFurther = false;
    }
}
