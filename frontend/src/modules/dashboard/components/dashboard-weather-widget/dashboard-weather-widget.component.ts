import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { MeteoInfo, MeteoService } from '@modules/dashboard/services';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-dashboard-weather-widget',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard-weather-widget.component.html',
    styleUrls: ['./dashboard-weather-widget.component.scss'],
})
export class DashboardWeatherWidgetComponent implements OnInit, OnDestroy {
    @Input() retryEvents: Observable<void>;
    @Output() componentError = new EventEmitter<any>();

    public temperature?: string;
    public temperatureExternal?: number;
    public humidity?: number;
    public humidityExternal?: number;
    public meteoOnError = false;

    private destroy$ = new Subject<void>();

    constructor(private meteoService: MeteoService, private changeDetectorRef: ChangeDetectorRef) {}

    ngOnInit() {
        this.loadMeteoInfo();

        // Subscribe to retry events if provided
        if (this.retryEvents) {
            this.retryEvents.pipe(takeUntil(this.destroy$)).subscribe(() => {
                if (this.meteoOnError) {
                    this.loadMeteoInfo();
                }
            });
        }
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    public loadMeteoInfo() {
        this.meteoService
            .getMeteoInfo()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (data: MeteoInfo) => {
                    this.meteoOnError = false;
                    this.temperature = data.temperature;
                    this.humidity = data.humidity;
                    this.temperatureExternal = data.externalTemperature;
                    this.humidityExternal = data.externalHumidity;
                    this.changeDetectorRef.markForCheck();
                },
                error: err => {
                    this.meteoOnError = true;
                    this.temperature = undefined;
                    this.humidity = undefined;
                    this.temperatureExternal = undefined;
                    this.humidityExternal = undefined;
                    this.componentError.emit(err);
                    this.changeDetectorRef.markForCheck();
                },
            });
    }
}
