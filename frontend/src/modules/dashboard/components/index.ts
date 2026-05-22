import { DashboardAccessoriesActionComponent } from './dashboard-accessories-action/dashboard-accessories-action.component';
import { DashboardAccessoriesWidgetComponent } from './dashboard-accessories-widget/dashboard-accessories-widget.component';
import { DashboardDoorActionComponent } from './dashboard-door-action/dashboard-door-action.component';
import { DashboardDoorWidgetComponent } from './dashboard-door-widget/dashboard-door-widget.component';
import { DashboardWeatherActionComponent } from './dashboard-weather-action/dashboard-weather-action.component';
import { DashboardWeatherWidgetComponent } from './dashboard-weather-widget/dashboard-weather-widget.component';
import { DashboardWebcamActionComponent } from './dashboard-webcam-action/dashboard-webcam-action.component';
import { DashboardWidgetsComponent } from './dashboard-widgets/dashboard-widgets.component';

export const components = [
    DashboardDoorActionComponent,
    DashboardWebcamActionComponent,
    DashboardAccessoriesActionComponent,
    DashboardWidgetsComponent,
    DashboardWeatherActionComponent,
    DashboardWeatherWidgetComponent,
    DashboardDoorWidgetComponent,
    DashboardAccessoriesWidgetComponent,
];

export * from './dashboard-accessories-widget/dashboard-accessories-widget.component';
export * from './dashboard-door-widget/dashboard-door-widget.component';
export * from './dashboard-weather-widget/dashboard-weather-widget.component';
