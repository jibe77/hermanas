import { OnInit, ChangeDetectionStrategy, Component, OnDestroy, inject } from '@angular/core';
import { Breadcrumb } from '@modules/navigation/models';
import { NavigationService } from '@modules/navigation/services';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'sb-breadcrumbs',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './breadcrumbs.component.html',
    styleUrls: ['breadcrumbs.component.scss'],
    imports: [NgClass, RouterLink],
})
export class BreadcrumbsComponent implements OnInit, OnDestroy {
    navigationService = inject(NavigationService);

    breadcrumbs!: Breadcrumb[];
    private destroy$ = new Subject<void>();

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
