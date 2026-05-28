import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
} from '@angular/core';

@Component({
    selector: 'sb-card-view-details',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './card-view-details.component.html',
    styleUrls: ['card-view-details.component.scss'],
    standalone: false,
})
export class CardViewDetailsComponent implements OnInit {
    @Input() background!: string;
    @Input() color!: string;
    @Input() link = '';
    @Output() events = new EventEmitter();

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

    retry() {
        this.events.emit(this.link);
    }
}
