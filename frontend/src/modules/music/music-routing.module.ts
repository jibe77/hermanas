import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as chartsContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        component: chartsContainers.ChartsComponent,
        data: {
            title: 'Charts - Hermanas',
            breadcrumbs: [
                { text: 'Dashboard', link: '/dashboard' },
                { text: 'Charts', active: true },
            ],
        } as SBRouteData,
    },
];
