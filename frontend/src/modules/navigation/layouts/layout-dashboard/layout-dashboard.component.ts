import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostBinding,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { NavigationService } from '@modules/navigation/services';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-layout-dashboard',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './layout-dashboard.component.html',
    styleUrls: ['layout-dashboard.component.scss'],
    standalone: false
})
export class LayoutDashboardComponent implements OnInit, OnDestroy {
    @HostBinding('class.sb-sidenav-toggled') sideNavHidden = false;
    private destroy$ = new Subject<void>();

    constructor(
        public navigationService: NavigationService,
        private changeDetectorRef: ChangeDetectorRef
    ) {}

    ngOnInit() {
        this.navigationService
            .sideNavVisible$()
            .pipe(takeUntil(this.destroy$))
            .subscribe(isVisible => {
                this.sideNavHidden = !isVisible;
                this.changeDetectorRef.markForCheck();
            });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
