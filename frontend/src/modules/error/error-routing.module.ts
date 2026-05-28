import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as errorContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: '404',
    },
    {
        path: '401',
        component: errorContainers.Error401Component,
        data: { title: 'Error 401 - Hermanas' } as SBRouteData,
    },
    {
        path: '404',
        component: errorContainers.Error404Component,
        data: { title: 'Error 404 - Hermanas' } as SBRouteData,
    },
    {
        path: '500',
        component: errorContainers.Error500Component,
        data: { title: 'Error 500 - Hermanas' } as SBRouteData,
    },
    {
        path: '**',
        pathMatch: 'full',
        component: errorContainers.Error404Component,
    },
];
