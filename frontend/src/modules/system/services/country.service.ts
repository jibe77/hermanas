import { DecimalPipe } from '@angular/common';
import { Injectable, PipeTransform, signal, WritableSignal, Signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { COUNTRIES } from '@modules/system/data/countries';
import { SortDirection } from '@modules/system/directives';
import { Country } from '@modules/system/models';
import { Observable, of, Subject } from 'rxjs';
import { debounceTime, delay, switchMap, tap } from 'rxjs/operators';

interface SearchResult {
    countries: Country[];
    total: number;
}

interface State {
    page: number;
    pageSize: number;
    searchTerm: string;
    sortColumn: string;
    sortDirection: SortDirection;
}

function compare(v1: number | string, v2: number | string) {
    return v1 < v2 ? -1 : v1 > v2 ? 1 : 0;
}

function sort(countries: Country[], column: string, direction: string): Country[] {
    if (direction === '') {
        return countries;
    } else {
        return [...countries].sort((a, b) => {
            const res = compare(a[column], b[column]);
            return direction === 'asc' ? res : -res;
        });
    }
}

function matches(country: Country, term: string, pipe: PipeTransform) {
    return (
        country.name.toLowerCase().includes(term.toLowerCase()) ||
        pipe.transform(country.area).includes(term) ||
        pipe.transform(country.population).includes(term)
    );
}

@Injectable({ providedIn: 'root' })
export class CountryService {
    // Signals for reactive state management
    private _loading: WritableSignal<boolean> = signal(true);
    private _search$ = new Subject<void>();
    private _countries: WritableSignal<Country[]> = signal([]);
    private _total: WritableSignal<number> = signal(0);

    // Public readonly signals
    readonly loading: Signal<boolean> = this._loading.asReadonly();
    readonly countries: Signal<Country[]> = this._countries.asReadonly();
    readonly total: Signal<number> = this._total.asReadonly();

    private _state: State = {
        page: 1,
        pageSize: 4,
        searchTerm: '',
        sortColumn: '',
        sortDirection: '',
    };

    constructor(private pipe: DecimalPipe) {
        this._search$
            .pipe(
                tap(() => this._loading.set(true)),
                debounceTime(120),
                switchMap(() => this._search()),
                delay(120),
                tap(() => this._loading.set(false))
            )
            .subscribe(result => {
                this._countries.set(result.countries);
                this._total.set(result.total);
            });

        this._search$.next();
    }

    // Observable getters for backward compatibility
    get countries$(): Observable<Country[]> {
        return toObservable(this._countries);
    }
    get total$(): Observable<number> {
        return toObservable(this._total);
    }
    get loading$(): Observable<boolean> {
        return toObservable(this._loading);
    }
    get page() {
        return this._state.page;
    }
    set page(page: number) {
        this._set({ page });
    }
    get pageSize() {
        return this._state.pageSize;
    }
    set pageSize(pageSize: number) {
        this._set({ pageSize });
    }
    get searchTerm() {
        return this._state.searchTerm;
    }
    set searchTerm(searchTerm: string) {
        this._set({ searchTerm });
    }
    set sortColumn(sortColumn: string) {
        this._set({ sortColumn });
    }
    set sortDirection(sortDirection: SortDirection) {
        this._set({ sortDirection });
    }

    private _set(patch: Partial<State>) {
        Object.assign(this._state, patch);
        this._search$.next();
    }

    private _search(): Observable<SearchResult> {
        const { sortColumn, sortDirection, pageSize, page, searchTerm } = this._state;

        // 1. sort
        let countries = sort(COUNTRIES, sortColumn, sortDirection);

        // 2. filter
        countries = countries.filter(country => matches(country, searchTerm, this.pipe));
        const total = countries.length;

        // 3. paginate
        countries = countries.slice((page - 1) * pageSize, (page - 1) * pageSize + pageSize);
        return of({ countries, total });
    }
}
