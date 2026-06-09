import { Injectable, signal } from '@angular/core';

/**
 * Mediates the "are you sure you want to simulate this action?" modal that
 * pops up before every mutating call while the SPA is in demo mode.
 *
 * Two pieces of UX glue live here:
 *
 * 1. **Debouncing per burst** — components frequently fire several mutations
 *    in parallel via {@code forkJoin([...])} (energy save, weather save, …).
 *    Without debouncing the visitor would see one modal per request. We keep
 *    a single "active decision" promise for {@code BURST_WINDOW_MS} after the
 *    first request opens the modal; any other mutation arriving within the
 *    window reuses the same answer. After the window closes, the next
 *    mutation opens a fresh modal.
 *
 * 2. **"Don't show again for this session"** — sessionStorage-backed flag.
 *    Demo mode resets on page refresh anyway, so a session-scoped opt-out
 *    matches the user's mental model of "this is a temporary mode".
 */
@Injectable({ providedIn: 'root' })
export class DemoConfirmService {
    private static readonly SUPPRESSION_KEY = 'hermanas.demoConfirm.suppressed';
    private static readonly BURST_WINDOW_MS = 500;

    /**
     * UI state exposed to {@link DemoConfirmModalComponent}. {@code open}
     * drives the visibility of the modal. {@code message} is the localized
     * description of the action being attempted (component-supplied).
     */
    readonly open = signal(false);
    readonly message = signal<string>('');

    /**
     * Promise that the current modal will settle when the user clicks one of
     * the buttons. Reused for every mutation arriving within {@link BURST_WINDOW_MS}
     * of the modal opening, so a `forkJoin` of 5 settings → 1 modal, 5 same answers.
     */
    private pendingDecision: Promise<boolean> | null = null;
    private resolvePending: ((accept: boolean) => void) | null = null;
    private burstTimer: ReturnType<typeof setTimeout> | null = null;

    /**
     * Returns true when the visitor confirmed they want the action simulated,
     * false when they cancelled. Resolves immediately to {@code true} when the
     * "don't show again" flag is set on the current sessionStorage entry.
     */
    confirm(actionDescription: string): Promise<boolean> {
        if (this.isSuppressed()) {
            return Promise.resolve(true);
        }
        if (this.pendingDecision) {
            // A burst is already in flight — reuse the same promise so the
            // visitor sees only one modal.
            return this.pendingDecision;
        }
        this.message.set(actionDescription);
        this.open.set(true);
        this.pendingDecision = new Promise<boolean>(resolve => {
            this.resolvePending = resolve;
        });
        return this.pendingDecision;
    }

    /** Called by the modal "Simulate" button. */
    accept(suppressFurther: boolean): void {
        if (suppressFurther) {
            this.setSuppressed(true);
        }
        this.settle(true);
    }

    /** Called by the modal Cancel button, backdrop click, or ESC key. */
    reject(): void {
        this.settle(false);
    }

    private settle(accept: boolean): void {
        const resolver = this.resolvePending;
        this.resolvePending = null;
        this.open.set(false);
        // Keep the burst window alive a bit so debounced mutations queued
        // within the same forkJoin reuse this decision rather than reopen.
        if (this.burstTimer) {
            clearTimeout(this.burstTimer);
        }
        this.burstTimer = setTimeout(() => {
            this.pendingDecision = null;
            this.burstTimer = null;
        }, DemoConfirmService.BURST_WINDOW_MS);
        if (resolver) {
            resolver(accept);
        }
    }

    private isSuppressed(): boolean {
        try {
            return sessionStorage.getItem(DemoConfirmService.SUPPRESSION_KEY) === '1';
        } catch {
            // Storage may be disabled (private mode, sandboxing) — never trust
            // it as the "don't show" signal in that case.
            return false;
        }
    }

    private setSuppressed(value: boolean): void {
        try {
            if (value) {
                sessionStorage.setItem(DemoConfirmService.SUPPRESSION_KEY, '1');
            } else {
                sessionStorage.removeItem(DemoConfirmService.SUPPRESSION_KEY);
            }
        } catch {
            /* ignore — best-effort persistence */
        }
    }
}
