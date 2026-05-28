import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { trigger, style, transition, animate } from '@angular/animations';
import { Toast, ToastService } from '../../services/toast/toast.service';
import { Subject, takeUntil } from 'rxjs';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { TitleCasePipe } from '@angular/common';

@Component({
    selector: 'sb-toast-container',
    templateUrl: './toast-container.component.html',
    styleUrls: ['./toast-container.component.scss'],
    animations: [
        trigger('toastAnimation', [
            transition(':enter', [
                style({ transform: 'translateX(100%)', opacity: 0 }),
                animate('300ms ease-out', style({ transform: 'translateX(0)', opacity: 1 })),
            ]),
            transition(':leave', [
                animate('200ms ease-in', style({ transform: 'translateX(100%)', opacity: 0 })),
            ]),
        ]),
    ],
    imports: [FaIconComponent, TitleCasePipe],
})
export class ToastContainerComponent implements OnInit, OnDestroy {
    private toastService = inject(ToastService);

    public toasts: Toast[] = [];
    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        // Subscribe to new toasts
        this.toastService.toasts$.pipe(takeUntil(this.destroy$)).subscribe(toast => {
            this.toasts.push(toast);
        });

        // Subscribe to toast removals
        this.toastService.removeToast$.pipe(takeUntil(this.destroy$)).subscribe(id => {
            this.toasts = this.toasts.filter(t => t.id !== id);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * Remove a toast
     */
    public removeToast(id: string): void {
        this.toastService.remove(id);
    }

    /**
     * Get icon class for toast type
     */
    public getIconClass(type: Toast['type']): string {
        switch (type) {
            case 'success':
                return 'fa-check-circle';
            case 'error':
                return 'fa-exclamation-circle';
            case 'warning':
                return 'fa-exclamation-triangle';
            case 'info':
                return 'fa-info-circle';
            default:
                return 'fa-info-circle';
        }
    }

    /**
     * Get CSS class for toast type
     */
    public getToastClass(type: Toast['type']): string {
        return `toast-${type}`;
    }
}
