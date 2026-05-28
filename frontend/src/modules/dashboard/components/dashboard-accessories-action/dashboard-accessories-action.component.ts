import { OnInit, ChangeDetectorRef, Component, Input, OnDestroy } from '@angular/core';
import { User } from '@modules/auth/models';
import { UserService } from '@modules/auth/services';
import { DashboardWidgetsComponent } from '@modules/dashboard/components/dashboard-widgets/dashboard-widgets.component';
import { FanService, LightService, MusicService } from '@modules/dashboard/services';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
    selector: 'sb-dashboard-accessories-action',
    templateUrl: './dashboard-accessories-action.component.html',
    styleUrls: ['dashboard-accessories-action.component.scss'],
})
export class DashboardAccessoriesActionComponent implements OnInit, OnDestroy {
    @Input() public musicStatus;
    @Input() public fanStatus;
    @Input() public lightStatus;

    user: User;
    subscription: Subscription = new Subscription();

    constructor(
        public _lightService: LightService,
        public _fanService: FanService,
        public _musicService: MusicService,
        public _userService: UserService,
        private changeDetectorRef: ChangeDetectorRef,
        private dashboardWidgetsComponent: DashboardWidgetsComponent
    ) {}

    ngOnInit(): void {
        this.subscription = this._userService.user$.subscribe((user: User) => {
            this.refresh(user);
        });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    refresh(user: User) {
        this.user = user;
    }

    public switchLight(param: boolean) {
        this._lightService
            .switch(param, this.user)
            .pipe(take(1))
            .subscribe(() => {
                this.dashboardWidgetsComponent.createSubscriptionToLightNotifications();
            });
    }

    public switchMusic(param: boolean) {
        this._musicService
            .switch(param, this.user)
            .pipe(take(1))
            .subscribe(() => {
                this.dashboardWidgetsComponent.createSubscriptionToMusicNotifications();
            });
    }

    public switchFan(param: boolean) {
        this._fanService
            .switch(param, this.user)
            .pipe(take(1))
            .subscribe(() => {
                this.dashboardWidgetsComponent.createSubscriptionToFanNotifications();
            });
    }
}
