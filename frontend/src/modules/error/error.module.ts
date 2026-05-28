/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

/* Modules */
import { AppCommonModule } from '@common/app-common.module';
import { NavigationModule } from '@modules/navigation/navigation.module';

/* Components */
import * as errorComponents from './components';

/* Containers */
import * as errorContainers from './containers';

/* Guards */
import * as errorGuards from './guards';

/* Services */
import * as errorServices from './services';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        ReactiveFormsModule,
        FormsModule,
        AppCommonModule,
        NavigationModule,
        ...errorContainers.containers,
        ...errorComponents.components,
    ],
    providers: [...errorServices.services, ...errorGuards.guards],
    exports: [...errorContainers.containers, ...errorComponents.components],
})
export class ErrorModule {}
