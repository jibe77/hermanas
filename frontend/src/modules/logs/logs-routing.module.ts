import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';
import { AuthGuard } from '@modules/auth/guards';

import * as logsContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        canActivate: [AuthGuard],
        component: logsContainers.LogsComponent,
        data: {
            title: 'Logs - Hermanas',
            breadcrumbs: [
                { text: 'Dashboard', link: '/dashboard' },
                { text: 'Logs', active: true },
            ],
        } as SBRouteData,
    },
];
