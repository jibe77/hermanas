/* tslint:disable: ordered-imports*/
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

/* Third Party */
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { IconsModule } from '@modules/icons/icons.module';

const thirdParty = [IconsModule, NgbModule];

/* Containers */
import * as appCommonContainers from './containers';

/* Components */
import * as appCommonComponents from './components';

/* Guards */
import * as appCommonGuards from './guards';

/* Services */
import * as appCommonServices from './services';

// AuthService / LoginService / UserService were previously listed here as
// providers; they are now providedIn: 'root' so the module no longer needs
// to declare them.

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        ...thirdParty,
        ...appCommonContainers.containers,
        ...appCommonComponents.components,
    ],
    providers: [...appCommonServices.services, ...appCommonGuards.guards],
    exports: [...appCommonContainers.containers, ...appCommonComponents.components, ...thirdParty],
})
export class AppCommonModule {}
