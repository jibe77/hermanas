/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

/* Modules */
import { AppCommonModule } from '@common/app-common.module';
import { NavigationModule } from '@modules/navigation/navigation.module';

/* Components */
import * as chartsComponents from './components';

/* Containers */
import * as chartsContainers from './containers';

/* Guards */
import * as chartsGuards from './guards';

/* Services */
import * as weatherServices from './services';

/* Directives */
import * as tablesDirectives from './directives';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        ReactiveFormsModule,
        FormsModule,
        AppCommonModule,
        NavigationModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
    ],
    providers: [
        ...weatherServices.services,
        ...chartsGuards.guards,
        ...tablesDirectives.directives,
    ],
    declarations: [...chartsContainers.containers, ...chartsComponents.components],
    exports: [...chartsContainers.containers, ...chartsComponents.components],
})
export class WeatherModule {}
