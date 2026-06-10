import { OnInit, ChangeDetectorRef, Component, OnDestroy, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ChildActivationEnd, Router, RouterOutlet } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { PwaInstallService, PushService, SwUpdateService } from '@common/services';
import {
    OfflineBannerComponent,
    PwaInstallBannerComponent,
    ToastContainerComponent,
} from '@common/components';
import { LoginModalComponent } from '@modules/auth/components';
import { DemoConfirmModalComponent } from '@common/components/demo-confirm-modal/demo-confirm-modal.component';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    imports: [
        RouterOutlet,
        ToastContainerComponent,
        PwaInstallBannerComponent,
        OfflineBannerComponent,
        LoginModalComponent,
        DemoConfirmModalComponent,
    ],
})
export class AppComponent implements OnInit, OnDestroy {
    router = inject(Router);
    private titleService = inject(Title);
    private ref = inject(ChangeDetectorRef);
    private pwaInstall = inject(PwaInstallService);
    private swUpdate = inject(SwUpdateService);
    private pushService = inject(PushService);

    title = 'hermanas-client';
    private destroy$ = new Subject<void>();

    ngOnInit() {
        this.pwaInstall.initialize();
        this.swUpdate.initialize();
        this.pushService.initialize();
        this.router.events
            .pipe(
                filter(event => event instanceof ChildActivationEnd),
                takeUntil(this.destroy$)
            )
            .subscribe(event => {
                let snapshot = (event as ChildActivationEnd).snapshot;
                while (snapshot.firstChild !== null) {
                    snapshot = snapshot.firstChild;
                }
                this.titleService.setTitle(snapshot.data.title || 'Hermanas');
            });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
