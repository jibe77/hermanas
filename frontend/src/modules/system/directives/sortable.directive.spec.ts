import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SBSortableHeaderDirective, SortDirection, SortEvent } from './sortable.directive';

// `sortDirection` is exposed as a real @Input() on TestComponent so the spec
// can flip it through `fixture.componentRef.setInput()`. Angular 22 dropped
// the implicit "re-read every template binding on the next detectChanges()"
// behavior the previous spec relied on; setInput() is the supported API to
// push a fresh value into a binding from a test, and it integrates with the
// scheduler so the change propagates to the child directive immediately.
@Component({
    template: `
        <table>
            <thead>
                <tr>
                    <th sbSortable="name" [direction]="sortDirection" (sort)="onSort($event)">
                        Name
                    </th>
                </tr>
            </thead>
        </table>
    `,
    imports: [SBSortableHeaderDirective],
})
class TestComponent {
    @Input() sortDirection: SortDirection = '';
    lastSortEvent?: SortEvent;

    onSort(event: SortEvent) {
        this.lastSortEvent = event;
        this.sortDirection = event.direction;
    }
}

describe('SBSortableHeaderDirective (System)', () => {
    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;
    let directiveElement: DebugElement;
    let directive: SBSortableHeaderDirective;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestComponent],
        });

        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
        directiveElement = fixture.debugElement.query(By.directive(SBSortableHeaderDirective));
        directive = directiveElement.injector.get(SBSortableHeaderDirective);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(directive).toBeTruthy();
    });

    // Helper: push a fresh value into TestComponent.sortDirection through the
    // supported Angular 22 input API. setInput() updates the binding, marks
    // the host view dirty and schedules a CD cycle; the following
    // detectChanges() then propagates the new value to the child directive's
    // @Input. The previous "assign + detectChanges" pattern silently broke
    // when Angular 22 stopped re-reading template bindings on every CD pass.
    function setDirection(value: SortDirection): void {
        fixture.componentRef.setInput('sortDirection', value);
        fixture.detectChanges();
    }

    describe('Input properties', () => {
        it('should have sbSortable column name', () => {
            expect(directive.sbSortable).toBe('name');
        });

        it('should default direction to empty string', () => {
            const newFixture = TestBed.createComponent(TestComponent);
            const newDirectiveElement = newFixture.debugElement.query(
                By.directive(SBSortableHeaderDirective)
            );
            const newDirective = newDirectiveElement.injector.get(SBSortableHeaderDirective);

            expect(newDirective.direction).toBe('');
        });

        it('should accept direction input', () => {
            setDirection('asc');

            expect(directive.direction).toBe('asc');
        });
    });

    describe('CSS class bindings', () => {
        it('should not have asc or desc class when direction is empty', () => {
            setDirection('');

            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(false);
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(false);
        });

        it('should apply asc class when direction is ascending', () => {
            setDirection('asc');

            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(true);
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(false);
        });

        it('should apply desc class when direction is descending', () => {
            setDirection('desc');

            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(true);
            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(false);
        });

        it('should update classes when direction changes', () => {
            setDirection('asc');
            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(true);

            setDirection('desc');
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(true);
            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(false);

            setDirection('');
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(false);
            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(false);
        });
    });

    describe('Click behavior and sort rotation', () => {
        it('should rotate from empty to asc on first click', () => {
            directive.direction = '';
            directiveElement.nativeElement.click();
            fixture.detectChanges();

            expect(directive.direction).toBe('asc');
        });

        it('should rotate from asc to desc on second click', () => {
            directive.direction = 'asc';
            directiveElement.nativeElement.click();
            fixture.detectChanges();

            expect(directive.direction).toBe('desc');
        });

        it('should rotate from desc to empty on third click', () => {
            directive.direction = 'desc';
            directiveElement.nativeElement.click();
            fixture.detectChanges();

            expect(directive.direction).toBe('');
        });

        it('should complete full rotation cycle', () => {
            directive.direction = '';

            // First click: '' -> 'asc'
            directiveElement.nativeElement.click();
            expect(directive.direction).toBe('asc');

            // Second click: 'asc' -> 'desc'
            directiveElement.nativeElement.click();
            expect(directive.direction).toBe('desc');

            // Third click: 'desc' -> ''
            directiveElement.nativeElement.click();
            expect(directive.direction).toBe('');

            // Fourth click: '' -> 'asc' (cycle repeats)
            directiveElement.nativeElement.click();
            expect(directive.direction).toBe('asc');
        });
    });

    describe('Sort event emission', () => {
        it('should emit sort event on click', () => {
            vi.spyOn(directive.sort, 'emit');

            directiveElement.nativeElement.click();

            expect(directive.sort.emit).toHaveBeenCalled();
        });

        it('should emit sort event with column and direction', () => {
            directive.direction = '';
            directiveElement.nativeElement.click();

            expect(component.lastSortEvent).toEqual({
                column: 'name',
                direction: 'asc',
            });
        });

        it('should emit correct direction in sort event', () => {
            directive.direction = 'asc';
            directiveElement.nativeElement.click();

            expect(component.lastSortEvent?.direction).toBe('desc');
        });

        it('should emit multiple sort events on multiple clicks', () => {
            const sortEvents: SortEvent[] = [];

            directive.sort.subscribe((event: SortEvent) => {
                sortEvents.push(event);
            });

            directive.direction = '';

            // Click three times
            directiveElement.nativeElement.click();
            directiveElement.nativeElement.click();
            directiveElement.nativeElement.click();

            expect(sortEvents.length).toBe(3);
            expect(sortEvents[0]).toEqual({ column: 'name', direction: 'asc' });
            expect(sortEvents[1]).toEqual({ column: 'name', direction: 'desc' });
            expect(sortEvents[2]).toEqual({ column: 'name', direction: '' });
        });
    });

    describe('Host bindings', () => {
        it('isAscending should return true only when direction is asc', () => {
            directive.direction = 'asc';
            expect(directive.isAscending).toBe(true);

            directive.direction = 'desc';
            expect(directive.isAscending).toBe(false);

            directive.direction = '';
            expect(directive.isAscending).toBe(false);
        });

        it('isDescending should return true only when direction is desc', () => {
            directive.direction = 'desc';
            expect(directive.isDescending).toBe(true);

            directive.direction = 'asc';
            expect(directive.isDescending).toBe(false);

            directive.direction = '';
            expect(directive.isDescending).toBe(false);
        });
    });

    describe('Integration with component', () => {
        it('should update component state when clicked', () => {
            expect(component.sortDirection).toBe('');

            directiveElement.nativeElement.click();
            expect(component.sortDirection).toBe('asc');

            directiveElement.nativeElement.click();
            expect(component.sortDirection).toBe('desc');

            directiveElement.nativeElement.click();
            expect(component.sortDirection).toBe('');
        });

        it('should reflect component direction changes', () => {
            setDirection('desc');

            expect(directive.direction).toBe('desc');
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(true);
        });
    });
});
