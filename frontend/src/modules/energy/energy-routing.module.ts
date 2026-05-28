/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';
import { AdminGuard } from '@modules/auth/guards';

/* Module */
import { EnergyModule } from './energy.module';

/* Containers */
import * as chartsContainers from './containers';

/* Routes */
export const ROUTES: Routes = [
    {
        path: '',
        canActivate: [AdminGuard],
        component: chartsContainers.ChartsComponent,
        data: {
            title: 'Energy - Hermanas',
            breadcrumbs: [
                {
                    text: 'Dashboard',
                    link: '/dashboard',
                },
                {
                    text: 'Energy',
                    active: true,
                },
            ],
        } as SBRouteData,
    },
];

@NgModule({
    imports: [EnergyModule, RouterModule.forChild(ROUTES)],
    exports: [RouterModule],
})
export class EnergyRoutingModule {}
