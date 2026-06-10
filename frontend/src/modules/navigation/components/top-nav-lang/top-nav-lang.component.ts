import { OnInit, ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NavigationService } from '@modules/navigation/services';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu } from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'sb-top-nav-lang',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav-lang.component.html',
    styleUrls: ['top-nav-lang.component.scss'],
    imports: [NgbDropdown, NgbDropdownToggle, NgbDropdownMenu],
})
export class TopNavLangComponent implements OnInit {
    navigationService = inject(NavigationService);

    siteLanguage = 'English';
    siteLocale: string;
    languageList = [
        { code: 'en-US', label: 'English' },
        { code: 'fr-FR', label: 'Français' },
        { code: 'ro-RO', label: 'Română' },
    ];

    ngOnInit(): void {
        this.siteLocale = window.location.pathname.split('/')[1];
        const language = this.languageList.find(f => f.code === this.siteLocale);
        // Default to English when the URL prefix is unknown, since "/" serves the EN bundle.
        this.siteLanguage = (language ?? this.languageList[0]).label;
    }
}
