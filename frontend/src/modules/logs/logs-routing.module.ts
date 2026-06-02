import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as logsContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        component: logsContainers.LogsComponent,
        data: {
            title: 'Journalisation - Hermanas',
            breadcrumbs: [
                { text: 'Dashboard', link: '/dashboard' },
                { text: 'Journalisation', active: true },
            ],
        } as SBRouteData,
    },
];
