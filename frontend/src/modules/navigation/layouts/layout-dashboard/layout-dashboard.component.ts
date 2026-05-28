import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostBinding,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { NavigationService } from '@modules/navigation/services';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TopNavComponent } from '../../containers/top-nav/top-nav.component';
import { SideNavComponent } from '../../containers/side-nav/side-nav.component';
import { FooterComponent } from '../../containers/footer/footer.component';

@Component({
    selector: 'sb-layout-dashboard',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './layout-dashboard.component.html',
    styleUrls: ['layout-dashboard.component.scss'],
    imports: [TopNavComponent, SideNavComponent, FooterComponent],
})
export class LayoutDashboardComponent implements OnInit, OnDestroy {
    navigationService = inject(NavigationService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    @HostBinding('class.sb-sidenav-toggled') sideNavHidden = false;
    private destroy$ = new Subject<void>();

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
