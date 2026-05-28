import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as tablesContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        component: tablesContainers.SystemComponent,
        data: {
            title: 'Tables - Hermanas',
            breadcrumbs: [
                { text: 'Dashboard', link: '/dashboard' },
                { text: 'Tables', active: true },
            ],
        } as SBRouteData,
    },
];
