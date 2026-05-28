import { OnInit, ChangeDetectorRef, Component, Input, OnDestroy, inject } from '@angular/core';
import { User } from '@modules/auth/models';
import { UserService } from '@modules/auth/services';
import { DashboardWidgetsComponent } from '@modules/dashboard/components/dashboard-widgets/dashboard-widgets.component';
import { FanService, LightService, MusicService } from '@modules/dashboard/services';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { RouterLink } from '@angular/router';
import { AsyncPipe } from '@angular/common';

@Component({
    selector: 'sb-dashboard-accessories-action',
    templateUrl: './dashboard-accessories-action.component.html',
    styleUrls: ['dashboard-accessories-action.component.scss'],
    imports: [
        NgbDropdown,
        NgbDropdownToggle,
        FaIconComponent,
        NgbDropdownMenu,
        RouterLink,
        AsyncPipe,
    ],
})
export class DashboardAccessoriesActionComponent implements OnInit, OnDestroy {
    _lightService = inject(LightService);
    _fanService = inject(FanService);
    _musicService = inject(MusicService);
    _userService = inject(UserService);
    private changeDetectorRef = inject(ChangeDetectorRef);
    private dashboardWidgetsComponent = inject(DashboardWidgetsComponent);

    @Input() public musicStatus;
    @Input() public fanStatus;
    @Input() public lightStatus;

    user: User;
    subscription: Subscription = new Subscription();

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
