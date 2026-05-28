import { Component, inject } from '@angular/core';
import { DashboardWidgetsComponent } from '@modules/dashboard/components/dashboard-widgets/dashboard-widgets.component';

@Component({
    selector: 'sb-dashboard-weather-action',
    templateUrl: './dashboard-weather-action.component.html',
    styleUrls: ['dashboard-weather-action.component.scss'],
    standalone: false,
})
export class DashboardWeatherActionComponent {
    private _dashboardWidgetsComponent = inject(DashboardWidgetsComponent);

    public refreshWeather() {
        this._dashboardWidgetsComponent.createSubscriptionToMeteoInfo();
    }
}
