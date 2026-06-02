import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as containers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        component: containers.ResidentsComponent,
        data: {
            title: 'Pensionnaires - Hermanas',
            breadcrumbs: [
                { text: 'Dashboard', link: '/dashboard' },
                { text: 'Pensionnaires', active: true },
            ],
        } as SBRouteData,
    },
];
