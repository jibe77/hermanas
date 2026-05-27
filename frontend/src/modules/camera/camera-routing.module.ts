/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

/* Module */
import { CameraModule } from './camera.module';

/* Containers */
import * as chartsContainers from './containers';

/* Guards */
import { AuthGuard } from '@modules/auth/guards';

/* Routes */
export const ROUTES: Routes = [
    {
        path: '',
        canActivate: [AuthGuard],
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

@NgModule({
    imports: [CameraModule, RouterModule.forChild(ROUTES)],
    exports: [RouterModule],
})
export class CameraRoutingModule {}
