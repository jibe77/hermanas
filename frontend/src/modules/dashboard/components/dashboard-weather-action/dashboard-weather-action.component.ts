import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { DashboardWidgetsComponent } from '@modules/dashboard/components/dashboard-widgets/dashboard-widgets.component';
import { NgbDropdown } from '@ng-bootstrap/ng-bootstrap';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

@Component({
    selector: 'sb-dashboard-weather-action',
    templateUrl: './dashboard-weather-action.component.html',
    styleUrls: ['dashboard-weather-action.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [NgbDropdown, RouterLink, FaIconComponent],
})
export class DashboardWeatherActionComponent {
    private _dashboardWidgetsComponent = inject(DashboardWidgetsComponent);

    public refreshWeather() {
        this._dashboardWidgetsComponent.createSubscriptionToMeteoInfo();
    }
}
