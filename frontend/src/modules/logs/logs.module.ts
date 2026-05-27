/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

/* Modules */
import { AppCommonModule } from '@common/app-common.module';
import { NavigationModule } from '@modules/navigation/navigation.module';

/* Components */
import * as logsComponents from './components';

/* Containers */
import * as logsContainers from './containers';

/* Services */
import * as logsServices from './services';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        ReactiveFormsModule,
        FormsModule,
        AppCommonModule,
        NavigationModule,
    ],
    providers: [...logsServices.services],
    declarations: [...logsContainers.containers, ...logsComponents.components],
    exports: [...logsContainers.containers, ...logsComponents.components],
})
export class LogsModule {}
