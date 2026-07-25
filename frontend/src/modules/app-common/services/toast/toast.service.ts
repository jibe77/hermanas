import { Injectable, Injector, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { UserService } from '@modules/auth/services/user.service';

export interface ToastAction {
    label: string;
    handler: () => void;
}

export interface Toast {
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    message: string;
    title?: string;
    duration?: number;
    timestamp: Date;
    // Optional CTA rendered as a hyperlink inside the toast body. Used by
    // SwUpdateService to offer a "Reload now" link on the update-available toast.
    action?: ToastAction;
}

@Injectable({
    providedIn: 'root',
})
export class ToastService {
    private readonly DEFAULT_DURATION = 5000; // 5 seconds
    private toastSubject = new Subject<Toast>();
    private removeToastSubject = new Subject<string>();

    // Lazy injector so the UserService import doesn't create a circular
    // dependency at boot. We only read isDemoMode() to suppress error
    // toasts after a REST call short-circuited by demoModeInterceptor —
    // the user already saw the dedicated demo modal, the toast that
    // would otherwise surface "HTTP 0" or a generic "Cannot load …"
    // message just adds noise.
    private injector = inject(Injector);
    private demoModeCheck: (() => boolean) | null = null;

    public toasts$ = this.toastSubject.asObservable();
    public removeToast$ = this.removeToastSubject.asObservable();

    private toastQueue: Toast[] = [];

    /**
     * Show a success toast notification
     */
    public success(message: string, title?: string, duration?: number, action?: ToastAction): void {
        this.show('success', message, title, duration, action);
    }

    /**
     * Show an error toast notification
     */
    public error(message: string, title?: string, duration?: number, action?: ToastAction): void {
        this.show('error', message, title, duration, action);
    }

    /**
     * Show a warning toast notification
     */
    public warning(message: string, title?: string, duration?: number, action?: ToastAction): void {
        this.show('warning', message, title, duration, action);
    }

    /**
     * Show an info toast notification
     */
    public info(message: string, title?: string, duration?: number, action?: ToastAction): void {
        this.show('info', message, title, duration, action);
    }

    /**
     * Yellow toast emitted by {@code demoModeInterceptor} when a mutation
     * is blocked in demo mode. Bypasses the demo-mode warning/error filter
     * so the visitor gets *exactly one* explanation that the action did not
     * run. Components using regular {@code warning()} / {@code error()} stay
     * silenced under demo mode — only this dedicated path surfaces.
     */
    public demoBlocked(message: string, title?: string, duration?: number): void {
        const toast: Toast = {
            id: this.generateId(),
            type: 'warning',
            message,
            title,
            duration: duration ?? this.DEFAULT_DURATION,
            timestamp: new Date(),
        };
        this.toastQueue.push(toast);
        this.toastSubject.next(toast);
        if (toast.duration > 0) {
            setTimeout(() => this.remove(toast.id), toast.duration);
        }
    }

    /**
     * In demo mode, swallow error and warning toasts. They almost always
     * trace back to a REST call that demoModeInterceptor cancelled with a
     * synthetic 0-status response — the visitor has already seen the
     * dedicated demo modal explaining what happened, so a follow-up red
     * toast saying "HTTP 0 — Cancelled (demo)" only adds confusion.
     * Success/info toasts still go through because they reflect the
     * intercepted "fake success" path that we *want* the user to see.
     *
     * UserService is resolved lazily through the injector so the
     * dependency exists structurally only when first consulted — protects
     * against circular DI if a future change makes auth depend on Toast.
     */
    private isSuppressedByDemoMode(type: Toast['type']): boolean {
        if (type !== 'error' && type !== 'warning') {
            return false;
        }
        if (!this.demoModeCheck) {
            try {
                const svc = this.injector.get(UserService, null, { optional: true });
                this.demoModeCheck = svc ? () => svc.isDemoMode() : () => false;
            } catch {
                // UserService construction fails when its own deps are missing
                // (typical in lightweight unit tests). Treat as "never in demo".
                this.demoModeCheck = () => false;
            }
        }
        return this.demoModeCheck();
    }

    /**
     * Show a toast notification
     */
    private show(
        type: Toast['type'],
        message: string,
        title?: string,
        duration?: number,
        action?: ToastAction
    ): void {
        if (this.isSuppressedByDemoMode(type)) {
            return;
        }
        const toast: Toast = {
            id: this.generateId(),
            type,
            message,
            title,
            duration: duration ?? this.DEFAULT_DURATION,
            timestamp: new Date(),
            action,
        };

        this.toastQueue.push(toast);
        this.toastSubject.next(toast);

        // Auto-remove after duration
        if (toast.duration > 0) {
            setTimeout(() => {
                this.remove(toast.id);
            }, toast.duration);
        }
    }

    /**
     * Remove a toast by ID
     */
    public remove(id: string): void {
        const index = this.toastQueue.findIndex(t => t.id === id);
        if (index > -1) {
            this.toastQueue.splice(index, 1);
            this.removeToastSubject.next(id);
        }
    }

    /**
     * Clear all toasts
     */
    public clear(): void {
        this.toastQueue.forEach(toast => this.removeToastSubject.next(toast.id));
        this.toastQueue = [];
    }

    /**
     * Get all active toasts
     */
    public getToasts(): Toast[] {
        return [...this.toastQueue];
    }

    /**
     * Generate a unique ID for toasts
     */
    private generateId(): string {
        return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }
}
