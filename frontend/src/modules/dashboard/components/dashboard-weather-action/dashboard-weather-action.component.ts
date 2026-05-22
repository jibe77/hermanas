import { Component } from '@angular/core';
import { DashboardWidgetsComponent } from '@modules/dashboard/components/dashboard-widgets/dashboard-widgets.component';

@Component({
    selector: 'sb-dashboard-weather-action',
    templateUrl: './dashboard-weather-action.component.html',
    styleUrls: ['dashboard-weather-action.component.scss'],
})
export class DashboardWeatherActionComponent {
    constructor(private _dashboardWidgetsComponent: DashboardWidgetsComponent) {}

    public refreshWeather() {
        this._dashboardWidgetsComponent.createSubscriptionToMeteoInfo();
    }
}
