import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DashboardWidgetsComponent } from './dashboard-widgets.component';
import { ProgressWebsocketService } from '@modules/dashboard/services/progresswebsocket.service';

@Component({
    template: ` <sb-dashboard-widgets></sb-dashboard-widgets> `,
    standalone: false,
})
class TestHostComponent {}

describe('DashboardWidgetsComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let _hostComponent: TestHostComponent;
    let hostComponentDE: DebugElement;
    let hostComponentNE: Element;

    let _component: DashboardWidgetsComponent;
    let componentDE: DebugElement;
    let _componentNE: Element;
    let mockWebsocketService: jasmine.SpyObj<ProgressWebsocketService>;

    beforeEach(() => {
        mockWebsocketService = jasmine.createSpyObj('ProgressWebsocketService', ['getObservable']);
        mockWebsocketService.getObservable.and.returnValue(of());

        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DashboardWidgetsComponent],
            imports: [NoopAnimationsModule],
            providers: [{ provide: ProgressWebsocketService, useValue: mockWebsocketService }],
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
        expect(hostComponentNE.querySelector('sb-dashboard-widgets')).toBeTruthy();
    });

    it('should create the component', () => {
        expect(component).toBeTruthy();
    });
});
