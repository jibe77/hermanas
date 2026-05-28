import { OnInit, ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'sb-card',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './card.component.html',
    styleUrls: ['card.component.scss'],
    standalone: false,
})
export class CardComponent implements OnInit {
    @Input() background!: string;
    @Input() color!: string;

    customClasses: string[] = [];

    constructor() {}
    ngOnInit() {
        if (this.background) {
            this.customClasses.push(this.background);
        }
        if (this.color) {
            this.customClasses.push(this.color);
        }
    }
}
