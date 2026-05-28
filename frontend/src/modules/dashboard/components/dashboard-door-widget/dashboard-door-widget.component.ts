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

@Component({
    selector: 'sb-dashboard-door-widget',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard-door-widget.component.html',
    styleUrls: ['./dashboard-door-widget.component.scss'],
    standalone: false,
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
        this.refreshPicture();
        this.loadNextEvents();
        this.changeDetectorRef.markForCheck();
    }

    public refreshPicture() {
        this.pictureInitialised = false;
        this.pictureNotInitialised = false;
        // date param is functionally useless, but technically allows to force the web browser to refresh the picture
        this.picturePath = this.domainBase + '/camera/takePicture?date=' + new Date().getTime();
        this.changeDetectorRef.markForCheck();
    }

    public displayWebcam() {
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
