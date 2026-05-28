import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

@Component({
    selector: 'sb-sort-icon',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './sort-icon.component.html',
    styleUrls: ['sort-icon.component.scss'],
    imports: [FaIconComponent],
})
export class SortIconComponent {
    @Input() direction!: string;

    constructor() {}
}
