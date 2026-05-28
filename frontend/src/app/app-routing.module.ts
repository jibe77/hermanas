import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

/**
 * Lazy child routes export a `ROUTES` const since the standalone migration —
 * the wrapping *-routing.module.ts NgModules are now thin shells over that
 * const and exist only to keep the historic import sites working.
 */
const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: '/dashboard',
    },
    {
        path: 'auth',
        loadChildren: () => import('modules/auth/auth-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'camera',
        loadChildren: () => import('modules/camera/camera-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'dashboard',
        loadChildren: () =>
            import('modules/dashboard/dashboard-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'energy',
        loadChildren: () => import('modules/energy/energy-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'error',
        loadChildren: () => import('modules/error/error-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'logs',
        loadChildren: () => import('modules/logs/logs-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'music',
        loadChildren: () => import('modules/music/music-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'notification',
        loadChildren: () =>
            import('modules/notification/notification-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'system',
        loadChildren: () => import('modules/system/system-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'utility',
        loadChildren: () => import('modules/utility/utility-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'version',
        loadChildren: () => import('modules/utility/utility-routing.module').then(m => m.ROUTES),
    },
    {
        path: 'weather',
        loadChildren: () => import('modules/weather/weather-routing.module').then(m => m.ROUTES),
    },
    {
        path: '**',
        pathMatch: 'full',
        loadChildren: () => import('modules/error/error-routing.module').then(m => m.ROUTES),
    },
];

@NgModule({
    imports: [RouterModule.forRoot(routes, {})],
    exports: [RouterModule],
})
export class AppRoutingModule {}
