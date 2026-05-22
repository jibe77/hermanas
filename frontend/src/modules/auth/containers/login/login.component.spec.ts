import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { UserServiceStub } from '@testing/stubs';

import { LoginComponent } from './login.component';
import { UserService } from '@modules/auth/services';
import { NavigationService } from '@modules/navigation/services';

@Component({
    template: ` <sb-login></sb-login> `,
})
class TestHostComponent {}

describe('LoginComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let _hostComponent: TestHostComponent;
    let hostComponentDE: DebugElement;
    let hostComponentNE: Element;

    let _component: LoginComponent;
    let componentDE: DebugElement;
    let _componentNE: Element;

    beforeEach(() => {
        const mockRouter = jasmine.createSpyObj('Router', ['navigate']);
        const mockNavigationService = jasmine.createSpyObj('NavigationService', ['sideNavVisible']);

        TestBed.configureTestingModule({
            declarations: [TestHostComponent, LoginComponent],
            imports: [NoopAnimationsModule],
            providers: [
                { provide: UserService, useClass: UserServiceStub },
                { provide: Router, useValue: mockRouter },
                { provide: NavigationService, useValue: mockNavigationService },
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

        fixture.detectChanges();
    });

    it('should display the component', () => {
        expect(hostComponentNE.querySelector('sb-login')).toBeTruthy();
    });

    it('should create the component', () => {
        expect(component).toBeTruthy();
    });
});
