import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as containers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        component: containers.ElectronicsComponent,
        data: {
            title: 'Electronics - Hermanas',
            breadcrumbs: [
                { text: 'Dashboard', link: '/dashboard' },
                { text: 'Electronics', active: true },
            ],
        } as SBRouteData,
    },
];
