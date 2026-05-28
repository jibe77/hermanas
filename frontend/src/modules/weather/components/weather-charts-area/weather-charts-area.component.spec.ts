import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { WeatherChartsAreaComponent } from './weather-charts-area.component';

@Component({
    template: `
        <sb-charts-area
            [someInput]="someInput"
            (someFunction)="someFunction($event)"
        ></sb-charts-area>
    `,
    standalone: false,
})
class TestHostComponent {
    // someInput = 1;
    // someFunction(event: Event) {}
}

describe('WeatherChartsAreaComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let _hostComponent: TestHostComponent;
    let hostComponentDE: DebugElement;
    let hostComponentNE: Element;

    let _component: WeatherChartsAreaComponent;
    let componentDE: DebugElement;
    let _componentNE: Element;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, WeatherChartsAreaComponent],
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
        expect(hostComponentNE.querySelector('hermanas-weather-charts-area')).toEqual(
            jasmine.anything()
        );
    });
});
