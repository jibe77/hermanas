/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { SBRouteData } from '@modules/navigation/models';

/* Module */
import { LogsModule } from './logs.module';

/* Containers */
import * as logsContainers from './containers';

/* Guards */
import { AuthGuard } from '@modules/auth/guards';

/* Routes */
export const ROUTES: Routes = [
    {
        path: '',
        canActivate: [AuthGuard],
        component: logsContainers.LogsComponent,
        data: {
            title: 'Logs - Hermanas',
            breadcrumbs: [
                {
                    text: 'Dashboard',
                    link: '/dashboard',
                },
                {
                    text: 'Logs',
                    active: true,
                },
            ],
        } as SBRouteData,
    },
];

@NgModule({
    imports: [LogsModule, RouterModule.forChild(ROUTES)],
    exports: [RouterModule],
})
export class LogsRoutingModule {}
