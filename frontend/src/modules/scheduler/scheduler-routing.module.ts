/**
 * Standalone-era routing config for /scheduler. Same shape as every other
 * feature — a single ROUTES const consumed lazily from app.routes.ts.
 */
import { Routes } from '@angular/router';
import { AdminGuard } from '@modules/auth/guards';
import { SBRouteData } from '@modules/navigation/models';

import * as containers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        canActivate: [AdminGuard],
        component: containers.SchedulerComponent,
        data: {
            title: 'Scheduler - Hermanas',
            breadcrumbs: [
                {
                    text: 'Dashboard',
                    link: '/dashboard',
                },
                {
                    text: 'Scheduler',
                    active: true,
                },
            ],
        } as SBRouteData,
    },
];
