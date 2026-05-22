import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SBSortableHeaderDirective, SortEvent } from './sortable.directive';

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
})
class TestComponent {
    sortDirection: 'asc' | 'desc' | '' = '';
    lastSortEvent?: SortEvent;

    onSort(event: SortEvent) {
        this.lastSortEvent = event;
        this.sortDirection = event.direction;
    }
}

describe('SBSortableHeaderDirective', () => {
    let _component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;
    let directiveElement: DebugElement;
    let directive: SBSortableHeaderDirective;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [SBSortableHeaderDirective, TestComponent],
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
            component.sortDirection = 'asc';
            fixture.detectChanges();

            expect(directive.direction).toBe('asc');
        });
    });

    describe('CSS class bindings', () => {
        it('should not have asc or desc class when direction is empty', () => {
            directive.direction = '';
            fixture.detectChanges();

            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(false);
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(false);
        });

        it('should apply asc class when direction is ascending', () => {
            directive.direction = 'asc';
            fixture.detectChanges();

            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(true);
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(false);
        });

        it('should apply desc class when direction is descending', () => {
            directive.direction = 'desc';
            fixture.detectChanges();

            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(true);
            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(false);
        });

        it('should update classes when direction changes', () => {
            directive.direction = 'asc';
            fixture.detectChanges();
            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(true);

            directive.direction = 'desc';
            fixture.detectChanges();
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(true);
            expect(directiveElement.nativeElement.classList.contains('asc')).toBe(false);

            directive.direction = '';
            fixture.detectChanges();
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
            spyOn(directive.sort, 'emit');

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
            component.sortDirection = 'desc';
            fixture.detectChanges();

            expect(directive.direction).toBe('desc');
            expect(directiveElement.nativeElement.classList.contains('desc')).toBe(true);
        });
    });
});
