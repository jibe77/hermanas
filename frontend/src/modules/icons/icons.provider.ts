import { provideAppInitializer, inject } from '@angular/core';
import { FaIconLibrary } from '@fortawesome/angular-fontawesome';

import { fontAwesomeBrandsIcons } from './icons.font-awesome-brands';
import { fontAwesomeRegularIcons } from './icons.font-awesome-regular';
import { fontAwesomeSolidIcons } from './icons.font-awesome-solid';

/**
 * Standalone-era replacement for IconsModule. The old NgModule registered the
 * three FontAwesome icon packs from its constructor; here we use
 * provideAppInitializer (Angular 19+) so the same `library.addIconPacks(...)`
 * call runs once during bootstrap. Templates that need icons keep importing
 * `FaIconComponent` directly from `@fortawesome/angular-fontawesome`.
 */
export function provideHermanasIcons() {
    return provideAppInitializer(() => {
        const library = inject(FaIconLibrary);
        library.addIconPacks(
            fontAwesomeSolidIcons,
            fontAwesomeRegularIcons,
            fontAwesomeBrandsIcons
        );
    });
}
