import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as dashboardContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        data: {
            title: 'Hermanas',
            breadcrumbs: [{ text: 'Dashboard', active: true }],
        } as SBRouteData,
        component: dashboardContainers.DashboardComponent,
    },
];
