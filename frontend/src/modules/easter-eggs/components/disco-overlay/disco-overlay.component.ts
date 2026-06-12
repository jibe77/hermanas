import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { EasterEggsService } from '../../services';

// Full-viewport overlay that pulses colored gradients when disco mode is
// active. Tied to the disco signal of the EasterEggsService; the timing
// of the disco lifecycle is owned by the service.
@Component({
    selector: 'sb-disco-overlay',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './disco-overlay.component.html',
    styleUrls: ['disco-overlay.component.scss'],
    imports: [NgIf],
})
export class DiscoOverlayComponent {
    private ee = inject(EasterEggsService);
    readonly active = computed(() => this.ee.disco());
}
