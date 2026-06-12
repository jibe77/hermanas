import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { EasterEggsService } from '../../services';

interface FloatingEgg {
    left: string;
    delay: string;
    duration: string;
    sway: string;
    body: string;
    pattern: string;
    rotation: string;
    size: number;
}

// Holy Week → Easter Monday overlay: pastel eggs drift upward from below
// the viewport in a slow sway, like balloons in a soft spring breeze.
// pointer-events: none so the page stays interactive.
@Component({
    selector: 'sb-easter-overlay',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './easter-overlay.component.html',
    styleUrls: ['easter-overlay.component.scss'],
    imports: [NgIf, NgFor],
})
export class EasterOverlayComponent {
    private ee = inject(EasterEggsService);
    readonly active = computed(() => this.ee.easter());

    readonly eggs: FloatingEgg[] = Array.from({ length: 18 }).map(() => {
        const palette = PASTEL_PALETTE[Math.floor(Math.random() * PASTEL_PALETTE.length)];
        return {
            left: `${Math.random() * 100}%`,
            delay: `${(Math.random() * 12).toFixed(2)}s`,
            duration: `${(14 + Math.random() * 10).toFixed(2)}s`,
            sway: `${(Math.random() * 60 - 30).toFixed(0)}px`,
            body: palette.body,
            pattern: palette.pattern,
            rotation: `${Math.floor(Math.random() * 30) - 15}deg`,
            size: 22 + Math.floor(Math.random() * 14),
        };
    });
}

const PASTEL_PALETTE: { body: string; pattern: string }[] = [
    { body: '#ffd1dc', pattern: '#f47ca5' }, // pink
    { body: '#c1e7ff', pattern: '#5aa9d6' }, // blue
    { body: '#d8f5c1', pattern: '#6cb84a' }, // green
    { body: '#fff3b0', pattern: '#e0b34c' }, // yellow
    { body: '#e5d4ff', pattern: '#9c7ed1' }, // lavender
    { body: '#ffe1c1', pattern: '#e0944c' }, // peach
];
