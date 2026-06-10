import { Routes } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

import * as chartsContainers from './containers';

// The Webcam page is intentionally public — the live photo panel is what the
// chicken coop is meant to showcase. The archive panel inside the component
// is hidden for anonymous visitors (see the @if (isSignedIn) guard in the
// template) so private historical images stay private.
export const ROUTES: Routes = [
    {
        path: '',
        component: chartsContainers.ChartsComponent,
        data: {
            title: 'Camera - Hermanas',
            breadcrumbs: [
                {
                    text: 'Dashboard',
                    link: '/dashboard',
                },
                {
                    text: 'Camera',
                    active: true,
                },
            ],
        } as SBRouteData,
    },
];
