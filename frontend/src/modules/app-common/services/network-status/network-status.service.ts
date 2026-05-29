import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Tracks whether the browser thinks it has network connectivity. Note that
 * `navigator.onLine === true` only means "the OS has *some* network interface
 * up" — it does NOT prove the chicken coop server is reachable. Use this for
 * coarse UX cues (show/hide the "you are offline" banner) and let the actual
 * HTTP retry layer + service-worker cache handle the real failure paths.
 */
@Injectable({ providedIn: 'root' })
export class NetworkStatusService {
    private onlineSubject: BehaviorSubject<boolean>;

    public online$: Observable<boolean>;

    constructor() {
        const initial = typeof navigator === 'undefined' ? true : navigator.onLine;
        this.onlineSubject = new BehaviorSubject<boolean>(initial);
        this.online$ = this.onlineSubject.asObservable();

        if (typeof window !== 'undefined') {
            window.addEventListener('online', () => this.onlineSubject.next(true));
            window.addEventListener('offline', () => this.onlineSubject.next(false));
        }
    }

    public isOnline(): boolean {
        return this.onlineSubject.value;
    }
}
