import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { ChartsComponent } from './charts.component';
import { ChartsService } from '@modules/music/services/charts.service';
import { ToastService } from '@common/services';

@Component({
    template: `
        <sb-charts [someInput]="someInput" (someFunction)="someFunction($event)"></sb-charts>
    `,
    standalone: false
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
        listPlaylists: () => of([]),
        listSongs: () => of([]),
        getSelectedPlaylist: () => of({ playlist: '' }),
        setSelectedPlaylist: () => of({ playlist: '' }),
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
                { provide: ToastService, useValue: toastServiceStub },
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
        expect(hostComponentNE.querySelector('sb-charts')).toEqual(jasmine.anything());
    });
});
