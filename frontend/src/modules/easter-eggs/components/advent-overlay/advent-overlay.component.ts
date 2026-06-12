import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { EasterEggsService } from '../../services';

interface Snowflake {
    left: string;
    size: string;
    delay: string;
    duration: string;
    drift: string;
    opacity: string;
}

// Full-viewport overlay shown during Advent (1st Sunday of Advent → Dec 25).
// Renders falling snow plus a small blinking Christmas tree pinned to the
// bottom-left corner. The tree is anchored at the LEFT so it doesn't clash
// with the right-side floating action button on the residents page.
@Component({
    selector: 'sb-advent-overlay',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './advent-overlay.component.html',
    styleUrls: ['advent-overlay.component.scss'],
    imports: [NgIf, NgFor],
})
export class AdventOverlayComponent {
    private ee = inject(EasterEggsService);
    readonly active = computed(() => this.ee.advent());

    readonly flakes: Snowflake[] = Array.from({ length: 80 }).map(() => ({
        left: `${Math.random() * 100}%`,
        size: `${(4 + Math.random() * 6).toFixed(1)}px`,
        delay: `${(Math.random() * 10).toFixed(2)}s`,
        duration: `${(8 + Math.random() * 10).toFixed(2)}s`,
        drift: `${(Math.random() * 80 - 40).toFixed(0)}px`,
        opacity: (0.4 + Math.random() * 0.6).toFixed(2),
    }));
}
