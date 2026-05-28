import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
    selector: 'sb-sort-icon',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './sort-icon.component.html',
    styleUrls: ['sort-icon.component.scss'],
    standalone: false
})
export class SortIconComponent {
    @Input() direction!: string;

    constructor() {}
}
