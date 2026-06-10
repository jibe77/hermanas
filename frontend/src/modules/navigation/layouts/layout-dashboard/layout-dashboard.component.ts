import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostBinding,
    OnDestroy,
    OnInit,
    effect,
    inject,
} from '@angular/core';
import { LoginModalService } from '@modules/auth/services';
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
    private loginModal = inject(LoginModalService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    @HostBinding('class.sb-sidenav-toggled') sideNavHidden = false;
    private destroy$ = new Subject<void>();
    private sideNavStateBeforeModal: boolean | null = null;

    constructor() {
        // The auth modal is a centered overlay covering the whole viewport with
        // a dark backdrop. The side nav only *physically* overlaps the modal on
        // mobile, when the user has slid the off-canvas menu open — on desktop
        // the menu sits in its own column and the backdrop already darkens it.
        //
        // So: only hide the menu when there is a real overlap, and restore the
        // user's previous menu state once the modal closes. TopNavComponent
        // still ignores the burger toggle while the modal is open so the user
        // can't slide the overlay back over the dialog.
        //
        // Reminder on the inverted signal semantics — `sideNavVisible` is the
        // "default state" flag: on desktop `true` means the menu is shown,
        // on mobile `true` means the off-canvas overlay is closed. So a mobile
        // user has the menu *open over the content* when the signal is `false`.
        effect(() => {
            const modalOpen = this.loginModal.open();
            const isDesktop = typeof window !== 'undefined' && window.innerWidth >= 992;

            if (modalOpen) {
                if (this.sideNavStateBeforeModal !== null) {
                    return;
                }
                const current = this.navigationService.sideNavVisible();
                const menuOverlapsModal = !isDesktop && current === false;
                if (menuOverlapsModal) {
                    this.sideNavStateBeforeModal = current;
                    this.navigationService.toggleSideNav(true);
                }
            } else if (this.sideNavStateBeforeModal !== null) {
                this.navigationService.toggleSideNav(this.sideNavStateBeforeModal);
                this.sideNavStateBeforeModal = null;
            }
        });
    }

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
