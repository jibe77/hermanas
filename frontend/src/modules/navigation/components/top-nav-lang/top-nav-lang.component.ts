import { OnInit, ChangeDetectionStrategy, Component } from '@angular/core';
import { NavigationService } from '@modules/navigation/services';

@Component({
    selector: 'sb-top-nav-lang',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './top-nav-lang.component.html',
    styleUrls: ['top-nav-lang.component.scss'],
    standalone: false
})
export class TopNavLangComponent implements OnInit {
    siteLanguage = 'English';
    siteLocale: string;
    languageList = [
        { code: 'en-US', label: 'English' },
        { code: 'fr-FR', label: 'Français' },
    ];

    constructor(public navigationService: NavigationService) {}

    ngOnInit(): void {
        this.siteLocale = window.location.pathname.split('/')[1];
        const language = this.languageList.find(f => f.code === this.siteLocale);
        if (language) {
            this.siteLanguage = language.label;
            // Note: I18n API removed in Amplify v6 - translations now configured via Amplify UI components
        } else {
            this.siteLanguage = this.languageList[1].label;
            // Note: I18n API removed in Amplify v6 - translations now configured via Amplify UI components
        }
    }
}
