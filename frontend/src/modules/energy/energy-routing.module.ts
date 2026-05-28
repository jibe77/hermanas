/**
 * Standalone-era routing config for /energy. The historic *-routing.module.ts
 * wrapper is gone — app-routing.module.ts now imports `ROUTES` directly.
 * Kept under the old filename so existing import sites resolve unchanged.
 */
import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';
import { AdminGuard } from '@modules/auth/guards';

import * as chartsContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        canActivate: [AdminGuard],
        component: chartsContainers.ChartsComponent,
        data: {
            title: 'Energy - Hermanas',
            breadcrumbs: [
                {
                    text: 'Dashboard',
                    link: '/dashboard',
                },
                {
                    text: 'Energy',
                    active: true,
                },
            ],
        } as SBRouteData,
    },
];
