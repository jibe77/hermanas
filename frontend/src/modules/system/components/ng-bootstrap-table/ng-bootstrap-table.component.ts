import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
    QueryList,
    ViewChildren,
    inject,
} from '@angular/core';
import { SBSortableHeaderDirective, SortEvent } from '@modules/system/directives';
import { Country } from '@modules/system/models';
import { CountryService } from '@modules/system/services';
import { Observable } from 'rxjs';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SBSortableHeaderDirective as SBSortableHeaderDirective_1 } from '../../directives/sortable.directive';
import { SortIconComponent } from '../sort-icon/sort-icon.component';
import { NgbHighlight, NgbPagination } from '@ng-bootstrap/ng-bootstrap';
import { AsyncPipe, DecimalPipe } from '@angular/common';

@Component({
    selector: 'sb-ng-bootstrap-table',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './ng-bootstrap-table.component.html',
    styleUrls: ['ng-bootstrap-table.component.scss'],
    imports: [
        ReactiveFormsModule,
        FormsModule,
        SBSortableHeaderDirective_1,
        SortIconComponent,
        NgbHighlight,
        NgbPagination,
        AsyncPipe,
        DecimalPipe,
    ],
})
export class NgBootstrapTableComponent implements OnInit {
    countryService = inject(CountryService);
    private changeDetectorRef = inject(ChangeDetectorRef);

    @Input() pageSize = 4;

    countries$!: Observable<Country[]>;
    total$!: Observable<number>;
    sortedColumn!: string;
    sortedDirection!: string;

    @ViewChildren(SBSortableHeaderDirective) headers!: QueryList<SBSortableHeaderDirective>;

    ngOnInit() {
        this.countryService.pageSize = this.pageSize;
        this.countries$ = this.countryService.countries$;
        this.total$ = this.countryService.total$;
    }

    onSort({ column, direction }: SortEvent) {
        this.sortedColumn = column;
        this.sortedDirection = direction;
        this.countryService.sortColumn = column;
        this.countryService.sortDirection = direction;
        this.changeDetectorRef.detectChanges();
    }
}
