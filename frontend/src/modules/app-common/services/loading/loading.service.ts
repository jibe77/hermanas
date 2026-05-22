import { Injectable, signal, WritableSignal, computed, Signal } from '@angular/core';

export interface LoadingState {
    active: number; // Count of active requests
    message?: string;
}

@Injectable({
    providedIn: 'root',
})
export class LoadingService {
    private readonly _state: WritableSignal<LoadingState> = signal({ active: 0 });

    /**
     * Get the current loading state as a signal (readonly)
     */
    get state(): Signal<LoadingState> {
        return this._state.asReadonly();
    }

    /**
     * Computed signal that returns true if any requests are active
     */
    readonly isLoading: Signal<boolean> = computed(() => this._state().active > 0);

    /**
     * Start loading - increments the active request counter
     */
    public start(message?: string): void {
        this._state.update(state => ({
            active: state.active + 1,
            message: message || state.message,
        }));
    }

    /**
     * Stop loading - decrements the active request counter
     */
    public stop(): void {
        this._state.update(state => ({
            active: Math.max(0, state.active - 1),
            message: state.active <= 1 ? undefined : state.message,
        }));
    }

    /**
     * Reset loading state to initial
     */
    public reset(): void {
        this._state.set({ active: 0 });
    }

    /**
     * Get current active request count
     */
    public getActiveCount(): number {
        return this._state().active;
    }
}
