import {
    OnInit,
    ChangeDetectorRef,
    Component,
    Input,
    OnDestroy,
    inject,
    ChangeDetectionStrategy,
} from '@angular/core';
import { User } from '@modules/auth/models';
import { UserService } from '@modules/auth/services';
import { DashboardWidgetsComponent } from '@modules/dashboard/components/dashboard-widgets/dashboard-widgets.component';
import { DoorService } from '@modules/dashboard/services';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { RouterLink } from '@angular/router';
import { AsyncPipe } from '@angular/common';

@Component({
    selector: 'sb-dashboard-door-action',
    templateUrl: './dashboard-door-action.component.html',
    styleUrls: ['dashboard-door-action.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        NgbDropdown,
        NgbDropdownToggle,
        FaIconComponent,
        NgbDropdownMenu,
        RouterLink,
        AsyncPipe,
    ],
})
export class DashboardDoorActionComponent implements OnInit, OnDestroy {
    _doorService = inject(DoorService);
    _userService = inject(UserService);
    private changeDetectorRef = inject(ChangeDetectorRef);
    private dashboardWidgetsComponent = inject(DashboardWidgetsComponent);

    @Input() public doorStatus;
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

    public openDoor() {
        // Switch the webcam panel to the live MJPEG stream so the operator can
        // watch the door move. We deliberately do NOT refresh to a still image
        // afterwards: that would tear down the stream right when it's most
        // useful, and mjpg_streamer can't coexist with a takePicture call.
        this.dashboardWidgetsComponent.displayWebcam();
        this._doorService.openDoor(this.user).pipe(take(1)).subscribe();
    }

    public closeDoor() {
        this.dashboardWidgetsComponent.displayWebcam();
        this._doorService.closeDoor(this.user).pipe(take(1)).subscribe();
    }
}
