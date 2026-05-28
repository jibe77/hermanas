import { Injectable, signal, WritableSignal, Signal, inject } from '@angular/core';
import { ActivatedRoute, ChildActivationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';

import { SBRouteData } from '../models';

@Injectable()
export class NavigationService {
    route = inject(ActivatedRoute);
    router = inject(Router);

    // Signals for reactive state management
    private _sideNavVisible: WritableSignal<boolean> = signal(true);
    private _routeData: WritableSignal<SBRouteData> = signal({} as SBRouteData);
    private _currentURL: WritableSignal<string> = signal('');

    // Public readonly signals
    readonly sideNavVisible: Signal<boolean> = this._sideNavVisible.asReadonly();
    readonly routeData: Signal<SBRouteData> = this._routeData.asReadonly();
    readonly currentURL: Signal<string> = this._currentURL.asReadonly();

    private readonly _sideNavVisible$: Observable<boolean>;
    private readonly _routeData$: Observable<SBRouteData>;
    private readonly _currentURL$: Observable<string>;

    constructor() {
        const router = this.router;

        this._sideNavVisible$ = toObservable(this._sideNavVisible);
        this._routeData$ = toObservable(this._routeData);
        this._currentURL$ = toObservable(this._currentURL);

        this.router.events
            .pipe(filter(event => event instanceof ChildActivationEnd))
            .subscribe(event => {
                let snapshot = (event as ChildActivationEnd).snapshot;
                while (snapshot.firstChild !== null) {
                    snapshot = snapshot.firstChild;
                }
                this._routeData.set(snapshot.data as SBRouteData);
                this._currentURL.set(router.url);
            });
    }

    // Observable getters for backward compatibility
    sideNavVisible$(): Observable<boolean> {
        return this._sideNavVisible$;
    }

    routeData$(): Observable<SBRouteData> {
        return this._routeData$;
    }

    currentURL$(): Observable<string> {
        return this._currentURL$;
    }

    toggleSideNav(visibility?: boolean) {
        if (typeof visibility !== 'undefined') {
            this._sideNavVisible.set(visibility);
        } else {
            this._sideNavVisible.update(current => !current);
        }
    }
}
