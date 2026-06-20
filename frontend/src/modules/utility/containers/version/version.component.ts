import { OnInit, Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { UtilityService } from '@modules/utility/services';
import { take } from 'rxjs/operators';

@Component({
    selector: 'sb-version',
    templateUrl: './version.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['version.component.scss'],
})
export class VersionComponent implements OnInit {
    private utilityService = inject(UtilityService);

    version!: string;
    ngOnInit() {
        this.utilityService.version$.pipe(take(1)).subscribe(v => (this.version = v));
    }
}
