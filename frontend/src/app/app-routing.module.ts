import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: '/dashboard',
    },
    {
        path: 'auth',
        loadChildren: () =>
            import('modules/auth/auth-routing.module').then(m => m.AuthRoutingModule),
    },
    {
        path: 'camera',
        loadChildren: () =>
            import('modules/camera/camera-routing.module').then(m => m.CameraRoutingModule),
    },
    {
        path: 'dashboard',
        loadChildren: () =>
            import('modules/dashboard/dashboard-routing.module').then(
                m => m.DashboardRoutingModule
            ),
    },
    {
        path: 'energy',
        loadChildren: () =>
            import('modules/energy/energy-routing.module').then(m => m.EnergyRoutingModule),
    },
    {
        path: 'error',
        loadChildren: () =>
            import('modules/error/error-routing.module').then(m => m.ErrorRoutingModule),
    },
    {
        path: 'logs',
        loadChildren: () =>
            import('modules/logs/logs-routing.module').then(m => m.LogsRoutingModule),
    },
    {
        path: 'music',
        loadChildren: () =>
            import('modules/music/music-routing.module').then(m => m.MusicRoutingModule),
    },
    {
        path: 'notification',
        loadChildren: () =>
            import('modules/notification/notification-routing.module').then(
                m => m.NotificationRoutingModule
            ),
    },
    {
        path: 'system',
        loadChildren: () =>
            import('modules/system/system-routing.module').then(m => m.SystemRoutingModule),
    },
    {
        path: 'utility',
        loadChildren: () =>
            import('modules/utility/utility-routing.module').then(m => m.UtilityRoutingModule),
    },
    {
        path: 'version',
        loadChildren: () =>
            import('modules/utility/utility-routing.module').then(m => m.UtilityRoutingModule),
    },
    {
        path: 'weather',
        loadChildren: () =>
            import('modules/weather/weather-routing.module').then(m => m.WeatherRoutingModule),
    },
    {
        path: '**',
        pathMatch: 'full',
        loadChildren: () =>
            import('modules/error/error-routing.module').then(m => m.ErrorRoutingModule),
    },
];

@NgModule({
    imports: [RouterModule.forRoot(routes, {})],
    exports: [RouterModule],
})
export class AppRoutingModule {}
