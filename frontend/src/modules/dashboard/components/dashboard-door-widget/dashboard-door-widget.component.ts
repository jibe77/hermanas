import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    inject,
} from '@angular/core';
import { NextEvents, SchedulerService } from '@modules/dashboard/services';
import { DoorService, DoorStatus } from '@modules/dashboard/services/door.service';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { DashboardDoorActionComponent } from '../dashboard-door-action/dashboard-door-action.component';
import { DashboardWebcamActionComponent } from '../dashboard-webcam-action/dashboard-webcam-action.component';

@Component({
    selector: 'sb-dashboard-door-widget',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard-door-widget.component.html',
    styleUrls: ['./dashboard-door-widget.component.scss'],
    imports: [
        CardComponent,
        FaIconComponent,
        DashboardDoorActionComponent,
        DashboardWebcamActionComponent,
    ],
})
export class DashboardDoorWidgetComponent implements OnInit, OnDestroy {
    doorService = inject(DoorService);
    private schedulerService = inject(SchedulerService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    @Input() retryEvents: Observable<void>;
    @Input() domainBase: string;
    @Output() componentError = new EventEmitter<any>();

    public doorStatus?: string;
    public doorStatusOnError = false;
    public nextOpeningTime?: string;
    public nextClosingTime?: string;
    public nextEventsOnError = false;
    public pictureInitialised = false;
    public pictureNotInitialised = false;
    public picturePath = 'favicon.ico';

    private destroy$ = new Subject<void>();

    ngOnInit() {
        this.loadDoorStatus();
        this.loadNextEvents();
        this.refreshPicture();

        // Subscribe to retry events if provided
        if (this.retryEvents) {
            this.retryEvents.pipe(takeUntil(this.destroy$)).subscribe(() => {
                if (this.doorStatusOnError) {
                    this.loadDoorStatus();
                }
                if (this.nextEventsOnError) {
                    this.loadNextEvents();
                }
                this.refreshPicture();
            });
        }
    }

    ngOnDestroy() {
        this.pictureInitialised = false;
        // Reset src before teardown so the browser closes the MJPEG socket;
        // otherwise mjpg_streamer keeps pushing frames the user can't see.
        this.picturePath = 'favicon.ico';
        this.destroy$.next();
        this.destroy$.complete();
    }

    private loadDoorStatus() {
        this.doorService
            .getDoorStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (data: DoorStatus) => {
                    this.doorStatusOnError = false;
                    this.doorStatus = data.status;
                    this.changeDetectorRef.markForCheck();
                },
                error: err => {
                    this.doorStatusOnError = true;
                    this.doorStatus = undefined;
                    this.componentError.emit(err);
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    private loadNextEvents() {
        this.schedulerService
            .getNextEvents()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (data: NextEvents) => {
                    this.nextEventsOnError = false;
                    this.nextOpeningTime = data.nextDoorOpeningTime.substr(11, 5);
                    this.nextClosingTime = data.nextDoorClosingTime.substr(11, 5);
                    this.changeDetectorRef.markForCheck();
                },
                error: err => {
                    this.nextEventsOnError = true;
                    this.nextOpeningTime = undefined;
                    this.nextClosingTime = undefined;
                    this.componentError.emit(err);
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    public updateDoorStatus(status: string) {
        this.doorStatus = status;
        // Only force a new still picture when we're already showing one. While
        // the live MJPEG stream is on (the user is actively watching the door
        // move), refreshing to /takePicture would kill mjpg_streamer mid-cycle
        // — every websocket transition (OPENING, OPENED, …) would otherwise
        // tear the stream down and the user would see a flash of a still photo
        // instead of the door moving.
        if (!this.isStreamingWebcam()) {
            this.refreshPicture();
        }
        this.loadNextEvents();
        this.changeDetectorRef.markForCheck();
    }

    private isStreamingWebcam(): boolean {
        return this.picturePath.indexOf('/camera/stream') !== -1;
    }

    public refreshPicture() {
        this.pictureInitialised = false;
        this.pictureNotInitialised = false;
        // force=true bypasses the backend's 30 s picture cache so the dashboard
        // never serves a stale shot. date= is a cache-buster on the browser side
        // — changing the URL is enough to make the browser drop any in-flight
        // request for the previous src (no need for an intermediate reset).
        this.picturePath =
            this.domainBase + '/camera/takePicture?force=true&date=' + new Date().getTime();
        this.changeDetectorRef.markForCheck();
    }

    public displayWebcam() {
        // The URL change alone makes the browser close the previous <img>
        // request before opening the stream. Assigning two values to the same
        // [src] within one change-detection tick triggers spurious setProperty
        // calls and was tripping up Angular bindings.
        this.picturePath = this.domainBase + '/camera/stream';
        this.changeDetectorRef.markForCheck();
    }

    public pictureIsInitialised() {
        if (this.picturePath !== 'favicon.ico') {
            this.pictureInitialised = true;
            this.changeDetectorRef.markForCheck();
        }
    }

    public pictureIsNotInitialised() {
        this.pictureNotInitialised = true;
        this.changeDetectorRef.markForCheck();
    }

    // Helper methods to simplify template logic
    public isDoorStatus(status: string): boolean {
        return this.doorStatus !== undefined && this.doorStatus === status;
    }

    public get isLoading(): boolean {
        return this.doorStatus === undefined && this.doorStatusOnError === false;
    }

    public get hasError(): boolean {
        return this.doorStatusOnError === true;
    }

    public get isPictureLoading(): boolean {
        return this.pictureInitialised === false && this.pictureNotInitialised === false;
    }
}
