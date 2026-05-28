import { OnInit, ChangeDetectorRef, Component, Input, OnDestroy } from '@angular/core';
import { User } from '@modules/auth/models';
import { UserService } from '@modules/auth/services';
import { DashboardWidgetsComponent } from '@modules/dashboard/components/dashboard-widgets/dashboard-widgets.component';
import { DoorService } from '@modules/dashboard/services';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
    selector: 'sb-dashboard-door-action',
    templateUrl: './dashboard-door-action.component.html',
    styleUrls: ['dashboard-door-action.component.scss'],
    standalone: false,
})
export class DashboardDoorActionComponent implements OnInit, OnDestroy {
    @Input() public doorStatus;
    user: User;
    subscription: Subscription = new Subscription();

    constructor(
        public _doorService: DoorService,
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

    public openDoor() {
        this.dashboardWidgetsComponent.displayWebcam();
        this._doorService
            .openDoor(this.user)
            .pipe(take(1))
            .subscribe(() => {
                this.dashboardWidgetsComponent.refreshPicture();
            });
    }

    public closeDoor() {
        this.dashboardWidgetsComponent.displayWebcam();
        this._doorService
            .closeDoor(this.user)
            .pipe(take(1))
            .subscribe(() => {
                this.dashboardWidgetsComponent.refreshPicture();
            });
    }
}
