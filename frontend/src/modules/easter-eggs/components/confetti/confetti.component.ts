import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { EasterEggsService } from '../../services';

interface ConfettiPiece {
    left: string;
    color: string;
    delay: string;
    duration: string;
    rotation: string;
}

// Renders a falling-confetti overlay anchored to the viewport top whenever
// the EasterEggsService flags a pensioner's birthday today. A small banner
// at the top names the celebrated hen so the operator notices it.
@Component({
    selector: 'sb-confetti',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './confetti.component.html',
    styleUrls: ['confetti.component.scss'],
    imports: [NgFor, NgIf],
})
export class ConfettiComponent {
    private ee = inject(EasterEggsService);

    readonly residentName = computed(() => this.ee.birthdayActive());

    readonly pieces: ConfettiPiece[] = Array.from({ length: 60 }).map(() => ({
        left: `${Math.random() * 100}%`,
        color: PALETTE[Math.floor(Math.random() * PALETTE.length)],
        delay: `${(Math.random() * 4).toFixed(2)}s`,
        duration: `${(3 + Math.random() * 3).toFixed(2)}s`,
        rotation: `${Math.floor(Math.random() * 360)}deg`,
    }));
}

const PALETTE = ['#f0a830', '#d93030', '#6b4226', '#fff5e0', '#e0a96d', '#5a4233', '#b8b5ad', '#e25555'];
