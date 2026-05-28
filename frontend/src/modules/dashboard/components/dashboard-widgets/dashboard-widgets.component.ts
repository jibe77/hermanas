import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { ApplianceMessage } from '@modules/dashboard/models';
import { ProgressWebsocketService } from '@modules/dashboard/services/progresswebsocket.service';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DashboardAccessoriesWidgetComponent } from '../dashboard-accessories-widget/dashboard-accessories-widget.component';
import { DashboardDoorWidgetComponent } from '../dashboard-door-widget/dashboard-door-widget.component';
import { DashboardWeatherWidgetComponent } from '../dashboard-weather-widget/dashboard-weather-widget.component';

@Component({
    selector: 'sb-dashboard-widgets',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dashboard-widgets.component.html',
    styleUrls: ['dashboard-widgets.component.scss'],
    standalone: false
})
export class DashboardWidgetsComponent implements OnInit, OnDestroy {
    @Output() serviceCommunicationError = new EventEmitter<any>();
    @Input() retryLauncherEvents: Observable<void>;
    @Input() domainBase: string;

    @ViewChild(DashboardDoorWidgetComponent) doorWidget?: DashboardDoorWidgetComponent;
    @ViewChild(DashboardAccessoriesWidgetComponent)
    accessoriesWidget?: DashboardAccessoriesWidgetComponent;
    @ViewChild(DashboardWeatherWidgetComponent) weatherWidget?: DashboardWeatherWidgetComponent;

    private destroy$ = new Subject<void>();

    constructor(private websocketService: ProgressWebsocketService) {}

    ngOnInit() {
        this.initWebSocket();
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    private initWebSocket() {
        this.websocketService
            .getObservable()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: data => {
                    // Type guard: check if message is ApplianceMessage
                    if (typeof data.message !== 'string' && 'appliance' in data.message) {
                        const message = data.message as ApplianceMessage;
                        this.handleWebSocketMessage(message);
                    }
                },
                error: msg => {
                    console.error('Error Getting WebSocket message: ', msg);
                },
            });
    }

    private handleWebSocketMessage(message: ApplianceMessage) {
        switch (message.appliance) {
            case 'LIGHT':
                this.accessoriesWidget?.updateLightStatus(message.state === 'ON');
                break;
            case 'FAN':
                this.accessoriesWidget?.updateFanStatus(message.state === 'ON');
                break;
            case 'MUSIC':
                this.accessoriesWidget?.updateMusicStatus(message.state === 'ON');
                break;
            case 'DOOR':
                this.doorWidget?.updateDoorStatus(message.state);
                break;
        }
    }

    public onChildError(error: any) {
        this.serviceCommunicationError.emit(error);
    }

    // Delegation methods for action components
    public refreshPicture() {
        this.doorWidget?.refreshPicture();
    }

    public displayWebcam() {
        this.doorWidget?.displayWebcam();
    }

    public createSubscriptionToLightNotifications() {
        this.accessoriesWidget?.loadLightStatus();
    }

    public createSubscriptionToFanNotifications() {
        this.accessoriesWidget?.loadFanStatus();
    }

    public createSubscriptionToMusicNotifications() {
        this.accessoriesWidget?.loadMusicStatus();
    }

    public createSubscriptionToMeteoInfo() {
        this.weatherWidget?.loadMeteoInfo();
    }
}
