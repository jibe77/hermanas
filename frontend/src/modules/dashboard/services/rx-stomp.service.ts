import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';

/**
 * Custom Angular-compatible RxStompService wrapper.
 *
 * This service wraps @stomp/rx-stomp's RxStomp class to provide
 * an Angular-compatible injectable service that works with Angular 15+.
 *
 * Replaces the deprecated @stomp/ng2-stompjs package which is incompatible
 * with Angular 15+ Ivy compiler.
 */
@Injectable({
    providedIn: 'root',
})
export class RxStompService {
    /**
     * The underlying RxStomp client instance.
     * Provides access to the STOMP client for configuration and connection management.
     */
    public stompClient: RxStomp;

    constructor() {
        this.stompClient = new RxStomp();
    }
}
