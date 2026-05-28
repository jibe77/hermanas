import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
    inject,
} from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, MatSortable } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MeteoInfo } from '@modules/dashboard/services';
import { WeatherService } from '@modules/weather/services';
import { Observable, Subscription } from 'rxjs';

@Component({
    selector: 'hermanas-weather-table-area',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './weather-table-area.component.html',
    styleUrls: ['weather-table-area.component.scss'],
    standalone: false,
})
export class WeatherTableAreaComponent implements OnInit, OnDestroy {
    weatherService = inject(WeatherService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    @Output() serviceCommunicationError = new EventEmitter();
    @Input() retryLauncherEvents: Observable<void>;

    displayedColumns = [
        'dateTime',
        'temperature',
        'externalTemperature',
        'humidity',
        'externalHumidity',
    ];
    dataSource = new MatTableDataSource<MeteoInfo>();
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild(MatSort) sort: MatSort;
    public tableIsLoading = false;

    infoSubscription: Subscription;
    eventsSubscription: Subscription;
    private infoSubscriptionOnError: boolean;

    ngOnInit() {
        this.tableIsLoading = true;
        this.changeDetectorRef.detectChanges();

        this.eventsSubscription = this.retryLauncherEvents.subscribe(() => {
            this.createSubscriptionToWeatherService();
        });

        this.createSubscriptionToWeatherService();
    }

    ngOnDestroy(): void {
        if (this.infoSubscription) {
            this.infoSubscription.unsubscribe();
        }
        if (this.eventsSubscription) {
            this.eventsSubscription.unsubscribe();
        }
    }

    createSubscriptionToWeatherService() {
        if (this.infoSubscription !== undefined) {
            this.infoSubscription.unsubscribe();
            this.infoSubscriptionOnError = false;
            this.changeDetectorRef.detectChanges();
        }

        const today = new Date();
        const to = this.formatDate(today);

        const sevenDaysAgo = new Date(today.getTime() - 7 * 1000 * 60 * 60 * 24);
        const from = this.formatDate(sevenDaysAgo);

        this.infoSubscription = this.weatherService.getInfoUsingDateRange(from, to).subscribe(
            data => {
                this.dataSource.data = data;
                this.tableIsLoading = false;
                this.changeDetectorRef.detectChanges();
                this.sort.sort({ id: 'dateTime', start: 'desc' } as MatSortable);
                this.dataSource.paginator = this.paginator;
                this.dataSource.sort = this.sort;
            },
            error => {
                this.serviceCommunicationError.emit(error);
                this.changeDetectorRef.detectChanges();
            }
        );
    }

    private formatDate(today: Date) {
        return (
            today.getFullYear() +
            '-' +
            (today.getMonth() + 1 + '').padStart(2, '0') +
            '-' +
            (today.getUTCDate() + '').padStart(2, '0') +
            '-' +
            (today.getHours() + '').padStart(2, '0') +
            '-' +
            (today.getMinutes() + '').padStart(2, '0')
        );
    }
}
