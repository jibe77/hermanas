/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

/* Modules */
import { AppCommonModule } from '@common/app-common.module';
import { NavigationModule } from '@modules/navigation/navigation.module';

/* Components */
import * as systemComponents from './components';

/* Containers */
import * as systemContainers from './containers';

/* Directives */
import * as systemDirectives from './directives';

/* Guards */
import * as systemGuards from './guards';

/* Services */
import * as systemService from './services';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

@NgModule({
    exports: [...systemContainers.containers, ...systemComponents.components],
    imports: [
        CommonModule,
        RouterModule,
        ReactiveFormsModule,
        FormsModule,
        AppCommonModule,
        NavigationModule,
        ...systemContainers.containers,
        ...systemComponents.components,
        ...systemDirectives.directives,
    ],
    providers: [
        DecimalPipe,
        ...systemService.services,
        ...systemGuards.guards,
        ...systemDirectives.directives,
        provideHttpClient(withInterceptorsFromDi()),
    ],
})
export class SystemModule {}
