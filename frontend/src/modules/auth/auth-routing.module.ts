import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as authContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'login',
    },
    {
        path: 'login',
        component: authContainers.LoginComponent,
        data: {
            title: 'Pages Login - Hermanas',
        } as SBRouteData,
    },
    {
        path: 'register',
        component: authContainers.RegisterComponent,
        data: {
            title: 'Inscription - Hermanas',
        } as SBRouteData,
    },
];
