import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { EasterEggsService } from '../../services';

interface FloatingPumpkin {
    left: string;
    delay: string;
    duration: string;
    sway: string;
    size: number;
    rotation: string;
}

interface FlyingBat {
    top: string;
    delay: string;
    duration: string;
    direction: 'ltr' | 'rtl';
}

// Halloween season overlay (Oct 24 → Nov 1 inclusive). Pumpkins drift up
// from below with a horizontal sway (same family as the Easter eggs but
// orange-and-black palette), bats glide across the screen, and a small
// spider web is pinned to the top-right corner. pointer-events: none so
// the page stays interactive.
@Component({
    selector: 'sb-halloween-overlay',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './halloween-overlay.component.html',
    styleUrls: ['halloween-overlay.component.scss'],
    imports: [NgIf, NgFor],
})
export class HalloweenOverlayComponent {
    private ee = inject(EasterEggsService);
    readonly active = computed(() => this.ee.halloween());

    readonly pumpkins: FloatingPumpkin[] = Array.from({ length: 14 }).map(() => ({
        left: `${Math.random() * 100}%`,
        delay: `${(Math.random() * 12).toFixed(2)}s`,
        duration: `${(16 + Math.random() * 10).toFixed(2)}s`,
        sway: `${(Math.random() * 60 - 30).toFixed(0)}px`,
        size: 28 + Math.floor(Math.random() * 14),
        rotation: `${Math.floor(Math.random() * 30) - 15}deg`,
    }));

    readonly bats: FlyingBat[] = Array.from({ length: 6 }).map((_, i) => ({
        top: `${10 + Math.random() * 60}%`,
        delay: `${(i * 2.4).toFixed(1)}s`,
        duration: `${(10 + Math.random() * 8).toFixed(2)}s`,
        direction: i % 2 === 0 ? 'ltr' : 'rtl',
    }));
}
