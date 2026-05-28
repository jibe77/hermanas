import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { ChartsComponent } from './charts.component';
import { ChartsService } from '@modules/notification/services/charts.service';
import { UserService } from '@modules/auth/services';
import { ToastService } from '@common/services';

@Component({
    template: `
        <sb-charts [someInput]="someInput" (someFunction)="someFunction($event)"></sb-charts>
    `,
})
class TestHostComponent {
    // someInput = 1;
    // someFunction(event: Event) {}
}

describe('ChartsComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let _hostComponent: TestHostComponent;
    let hostComponentDE: DebugElement;
    let hostComponentNE: Element;

    let _component: ChartsComponent;
    let componentDE: DebugElement;
    let _componentNE: Element;

    const chartsServiceStub: Partial<ChartsService> = {
        me: () => of({ login: 'guest', email: null, role: 'USER', notificationsEnabled: false }),
        updateMe: () =>
            of({ login: 'guest', email: null, role: 'USER', notificationsEnabled: false }),
        list: () => of([]),
        create: () => of({ login: 'x', email: null, role: 'USER', notificationsEnabled: false }),
        update: () => of({ login: 'x', email: null, role: 'USER', notificationsEnabled: false }),
        delete: () => of(undefined),
    };
    const userServiceStub: Partial<UserService> = {
        getCurrentUser: () => ({
            id: 'guest',
            login: 'guest',
            email: 'guest',
            authState: 'signedOut',
            roles: [],
        }),
    };
    const toastServiceStub: Partial<ToastService> = {
        success: () => {},
        error: () => {},
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, ChartsComponent],
            imports: [NoopAnimationsModule, HttpClientTestingModule],
            providers: [
                { provide: ChartsService, useValue: chartsServiceStub },
                { provide: UserService, useValue: userServiceStub },
                { provide: ToastService, useValue: toastServiceStub },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        _hostComponent = fixture.componentInstance;
        hostComponentDE = fixture.debugElement;
        hostComponentNE = hostComponentDE.nativeElement;

        componentDE = hostComponentDE.children[0];
        _component = componentDE.componentInstance;
        _componentNE = componentDE.nativeElement;

        fixture.detectChanges();
    });

    it('should display the component', () => {
        expect(hostComponentNE.querySelector('sb-charts')).toEqual(jasmine.anything());
    });
});
