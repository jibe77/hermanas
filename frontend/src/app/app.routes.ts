import { Routes } from '@angular/router';

/**
 * Top-level routes. Every feature is lazy-loaded via its `ROUTES` const, which
 * lives in `<feature>-routing.module.ts` (filename kept from the NgModule era
 * for stable lazy-import paths, but the file no longer declares a NgModule).
 */
export const APP_ROUTES: Routes = [
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
        path: 'electronics',
        loadChildren: () =>
            import('modules/electronics/electronics-routing.module').then(m => m.ROUTES),
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
        path: 'residents',
        loadChildren: () =>
            import('modules/residents/residents-routing.module').then(m => m.ROUTES),
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
