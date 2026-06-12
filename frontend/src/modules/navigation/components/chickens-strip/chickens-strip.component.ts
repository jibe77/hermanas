import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { NgFor } from '@angular/common';
import { EasterEggsService } from '@modules/easter-eggs/services';

interface ChickenPalette {
    body: string;
    wing: string;
    tail: string;
    comb: string;
    beak: string;
    outline: string;
}

interface Chicken {
    palette: ChickenPalette;
    delay: string;
    duration: string;
    bobDelay: string;
}

@Component({
    selector: 'sb-chickens-strip',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './chickens-strip.component.html',
    styleUrls: ['chickens-strip.component.scss'],
    imports: [NgFor],
})
export class ChickensStripComponent {
    private ee = inject(EasterEggsService);

    readonly dancing = computed(() => this.ee.dancing());
    readonly aprilFools = computed(() => this.ee.aprilFools());
    readonly advent = computed(() => this.ee.advent());
    readonly easter = computed(() => this.ee.easter());

    // Pastel colours assigned per-hen so each lays a uniquely coloured egg
    // during Easter season. Indexed in template via the *ngFor index.
    readonly eggColors = ['#ffd1dc', '#c1e7ff', '#d8f5c1', '#fff3b0'];

    chickens: Chicken[] = [
        {
            palette: {
                body: '#f5d49b', wing: '#e0a96d', tail: '#c08552',
                comb: '#d93030', beak: '#f0a830', outline: '#6b4226',
            },
            delay: '0s', duration: '22s', bobDelay: '0s',
        },
        {
            palette: {
                body: '#fff5e0', wing: '#e8d8b5', tail: '#c9b48a',
                comb: '#e25555', beak: '#f0a830', outline: '#7a6a4a',
            },
            delay: '-6s', duration: '26s', bobDelay: '-0.4s',
        },
        {
            palette: {
                body: '#5a4233', wing: '#3e2c20', tail: '#2a1d14',
                comb: '#d93030', beak: '#f0a830', outline: '#1f1410',
            },
            delay: '-12s', duration: '24s', bobDelay: '-0.8s',
        },
        {
            palette: {
                body: '#b8b5ad', wing: '#8c8a85', tail: '#6e6c68',
                comb: '#d93030', beak: '#f0a830', outline: '#3a3936',
            },
            delay: '-18s', duration: '28s', bobDelay: '-1.2s',
        },
    ];

    onChickenClick(event: MouseEvent, index: number): void {
        event.stopPropagation();
        this.ee.playCluck();
        const target = (event.currentTarget as HTMLElement);
        // Toggle a one-shot jump animation by adding then removing a class.
        target.classList.remove('jumping');
        // Force a reflow so re-adding the class restarts the animation when
        // the user clicks the same hen twice in a row.
        void target.offsetWidth;
        target.classList.add('jumping');
        setTimeout(() => target.classList.remove('jumping'), 600);
    }
}
