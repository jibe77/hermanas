/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

/* Modules */
import { AppCommonModule } from '@common/app-common.module';

/* Components */
import * as navigationComponents from './components';

/* Containers */
import * as navigationContainers from './containers';

/* Layouts */
import * as appCommonLayouts from './layouts';

/* Guards */
import * as navigationGuards from './guards';

/* Services */
import * as navigationServices from './services';

@NgModule({
    imports: [CommonModule, RouterModule, AppCommonModule, ...navigationContainers.containers,
        ...navigationComponents.components,
        ...appCommonLayouts.layouts],
    providers: [...navigationServices.services, ...navigationGuards.guards],
    exports: [
        ...navigationContainers.containers,
        ...navigationComponents.components,
        ...appCommonLayouts.layouts,
    ],
})
export class NavigationModule {}
