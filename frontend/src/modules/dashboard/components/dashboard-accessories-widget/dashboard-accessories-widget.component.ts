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
import {
    FanService,
    FanStatus,
    LightService,
    LightStatus,
    MusicService,
    MusicStatus,
} from '@modules/dashboard/services';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-dashboard-accessories-widget',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard-accessories-widget.component.html',
    styleUrls: ['./dashboard-accessories-widget.component.scss'],
    standalone: false,
})
export class DashboardAccessoriesWidgetComponent implements OnInit, OnDestroy {
    private lightService = inject(LightService);
    private fanService = inject(FanService);
    private musicService = inject(MusicService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    @Input() retryEvents: Observable<void>;
    @Output() componentError = new EventEmitter<any>();

    public lightStatus?: boolean;
    public lightStatusOnError = false;
    public musicStatus?: boolean;
    public musicStatusOnError = false;
    public fanStatus?: boolean;
    public fanStatusOnError = false;

    private destroy$ = new Subject<void>();

    ngOnInit() {
        this.loadLightStatus();
        this.loadFanStatus();
        this.loadMusicStatus();

        // Subscribe to retry events if provided
        if (this.retryEvents) {
            this.retryEvents.pipe(takeUntil(this.destroy$)).subscribe(() => {
                if (this.lightStatusOnError) {
                    this.loadLightStatus();
                }
                if (this.fanStatusOnError) {
                    this.loadFanStatus();
                }
                if (this.musicStatusOnError) {
                    this.loadMusicStatus();
                }
            });
        }
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    public loadLightStatus() {
        this.lightService
            .getStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (data: LightStatus) => {
                    this.lightStatusOnError = false;
                    this.lightStatus = data.statusEnum === 'ON';
                    this.changeDetectorRef.markForCheck();
                },
                error: err => {
                    this.lightStatusOnError = true;
                    this.lightStatus = undefined;
                    this.componentError.emit(err);
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    public loadFanStatus() {
        this.fanService
            .getStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (data: FanStatus) => {
                    this.fanStatusOnError = false;
                    this.fanStatus = data.statusEnum === 'ON';
                    this.changeDetectorRef.markForCheck();
                },
                error: err => {
                    this.fanStatusOnError = true;
                    this.fanStatus = undefined;
                    this.componentError.emit(err);
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    public loadMusicStatus() {
        this.musicService
            .getStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (data: MusicStatus) => {
                    this.musicStatusOnError = false;
                    this.musicStatus = data.statusEnum === 'ON';
                    this.changeDetectorRef.markForCheck();
                },
                error: err => {
                    this.musicStatusOnError = true;
                    this.musicStatus = undefined;
                    this.componentError.emit(err);
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    public updateLightStatus(status: boolean) {
        this.lightStatus = status;
        this.changeDetectorRef.markForCheck();
    }

    public updateFanStatus(status: boolean) {
        this.fanStatus = status;
        this.changeDetectorRef.markForCheck();
    }

    public updateMusicStatus(status: boolean) {
        this.musicStatus = status;
        this.changeDetectorRef.markForCheck();
    }
}
