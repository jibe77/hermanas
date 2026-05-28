import { Routes } from '@angular/router';

import * as utilityContainers from './containers';

export const ROUTES: Routes = [
    {
        path: '',
        component: utilityContainers.VersionComponent,
    },
];
