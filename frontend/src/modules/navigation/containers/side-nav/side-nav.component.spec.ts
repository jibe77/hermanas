import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { UserService } from '@modules/auth/services';
import { NavigationService } from '@modules/navigation/services';
import { NavigationServiceStub, UserServiceStub } from '@testing/stubs';

import { SideNavComponent } from './side-nav.component';

@Component({
    template: `
        <sb-side-nav [someInput]="someInput" (someFunction)="someFunction($event)"></sb-side-nav>
    `,
    standalone: false,
})
class TestHostComponent {
    // someInput = 1;
    // someFunction(event: Event) {}
}

describe('SideNavComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let _hostComponent: TestHostComponent;
    let hostComponentDE: DebugElement;
    let hostComponentNE: Element;

    let _component: SideNavComponent;
    let componentDE: DebugElement;
    let _componentNE: Element;

    let _navigationService: NavigationService;
    let _userService: UserService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, SideNavComponent],
            imports: [NoopAnimationsModule],
            providers: [
                { provide: NavigationService, useValue: NavigationServiceStub },
                { provide: UserService, useValue: UserServiceStub },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        hostComponent = fixture.componentInstance;
        hostComponentDE = fixture.debugElement;
        hostComponentNE = hostComponentDE.nativeElement;

        componentDE = hostComponentDE.children[0];
        component = componentDE.componentInstance;
        componentNE = componentDE.nativeElement;

        _navigationService = TestBed.inject(NavigationService);
        _userService = TestBed.inject(UserService);

        fixture.detectChanges();
    });

    it('should display the component', () => {
        expect(hostComponentNE.querySelector('sb-side-nav')).toEqual(jasmine.anything());
    });
});
