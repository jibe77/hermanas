import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface Toast {
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    message: string;
    title?: string;
    duration?: number;
    timestamp: Date;
}

@Injectable({
    providedIn: 'root',
})
export class ToastService {
    private readonly DEFAULT_DURATION = 5000; // 5 seconds
    private toastSubject = new Subject<Toast>();
    private removeToastSubject = new Subject<string>();

    public toasts$ = this.toastSubject.asObservable();
    public removeToast$ = this.removeToastSubject.asObservable();

    private toastQueue: Toast[] = [];

    /**
     * Show a success toast notification
     */
    public success(message: string, title?: string, duration?: number): void {
        this.show('success', message, title, duration);
    }

    /**
     * Show an error toast notification
     */
    public error(message: string, title?: string, duration?: number): void {
        this.show('error', message, title, duration);
    }

    /**
     * Show a warning toast notification
     */
    public warning(message: string, title?: string, duration?: number): void {
        this.show('warning', message, title, duration);
    }

    /**
     * Show an info toast notification
     */
    public info(message: string, title?: string, duration?: number): void {
        this.show('info', message, title, duration);
    }

    /**
     * Show a toast notification
     */
    private show(type: Toast['type'], message: string, title?: string, duration?: number): void {
        const toast: Toast = {
            id: this.generateId(),
            type,
            message,
            title,
            duration: duration ?? this.DEFAULT_DURATION,
            timestamp: new Date(),
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
