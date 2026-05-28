import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { Error401Component } from './error-401.component';

@Component({
    template: `
        <sb-error-401 [someInput]="someInput" (someFunction)="someFunction($event)"></sb-error-401>
    `,
    standalone: false,
})
class TestHostComponent {
    // someInput = 1;
    // someFunction(event: Event) {}
}

describe('Error401Component', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let _hostComponent: TestHostComponent;
    let hostComponentDE: DebugElement;
    let hostComponentNE: Element;

    let _component: Error401Component;
    let componentDE: DebugElement;
    let _componentNE: Element;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, Error401Component],
            imports: [NoopAnimationsModule],
            providers: [],
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
        expect(hostComponentNE.querySelector('sb-error-401')).toEqual(jasmine.anything());
    });
});
