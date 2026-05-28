import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';
import { AuthGuard } from '@modules/auth/guards';

import * as chartsContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        canActivate: [AuthGuard],
        component: chartsContainers.ChartsComponent,
        data: {
            title: 'Camera - Hermanas',
            breadcrumbs: [
                {
                    text: 'Dashboard',
                    link: '/dashboard',
                },
                {
                    text: 'Camera',
                    active: true,
                },
            ],
        } as SBRouteData,
    },
];
