import { Injectable, signal } from '@angular/core';

/**
 * One-shot welcome modal shown when the visitor enables demo mode. Explains
 * what demo mode does and what the next clicks will and won't do, so the
 * "this is just a preview" message lands once instead of being surfaced
 * piecemeal through toasts on every protected action.
 *
 * State-wise the service mirrors {@link DemoConfirmService}: an {@code open}
 * signal feeds the modal component, and a {@code close} method handles the
 * single "OK, got it" button.
 */
@Injectable({ providedIn: 'root' })
export class DemoWelcomeService {
    readonly open = signal(false);

    show(): void {
        this.open.set(true);
    }

    close(): void {
        this.open.set(false);
    }
}
