import { OnInit, ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { Breadcrumb } from '@modules/navigation/models';
import { NavigationService } from '@modules/navigation/services';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-breadcrumbs',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './breadcrumbs.component.html',
    styleUrls: ['breadcrumbs.component.scss'],
    standalone: false
})
export class BreadcrumbsComponent implements OnInit, OnDestroy {
    breadcrumbs!: Breadcrumb[];
    private destroy$ = new Subject<void>();

    constructor(public navigationService: NavigationService) {}

    ngOnInit() {
        this.navigationService
            .routeData$()
            .pipe(takeUntil(this.destroy$))
            .subscribe(routeData => {
                this.breadcrumbs = routeData.breadcrumbs;
            });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
