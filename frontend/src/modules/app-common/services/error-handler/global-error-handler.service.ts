import { ErrorHandler, Injectable, Injector } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '../toast/toast.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
    constructor(private injector: Injector) {}

    handleError(error: Error | HttpErrorResponse): void {
        // Use injector to avoid circular dependency issues
        const toastService = this.injector.get(ToastService);

        let errorMessage = 'An unexpected error occurred';
        let errorTitle = 'Error';

        if (error instanceof HttpErrorResponse) {
            // Server or connection error
            if (!navigator.onLine) {
                errorMessage = 'No internet connection';
                errorTitle = 'Connection Error';
            } else {
                errorMessage = this.getServerErrorMessage(error);
                errorTitle = `HTTP Error ${error.status}`;
            }

            // Log to console for debugging
            console.error('HTTP Error:', {
                status: error.status,
                statusText: error.statusText,
                url: error.url,
                message: error.message,
                error: error.error,
            });
        } else {
            // Client-side error
            errorMessage = this.getClientErrorMessage(error);
            errorTitle = 'Application Error';

            // Log to console for debugging
            console.error('Client Error:', {
                name: error.name,
                message: error.message,
                stack: error.stack,
            });
        }

        // Show toast notification
        toastService.error(errorMessage, errorTitle, 8000);

        // In production, you might want to send errors to a logging service
        // this.logErrorToService(error);
    }

    /**
     * Extract user-friendly message from server errors
     */
    private getServerErrorMessage(error: HttpErrorResponse): string {
        if (error.status === 0) {
            return 'Cannot connect to server. Please check your connection.';
        }

        if (error.status === 401) {
            return 'Authentication required. Please log in.';
        }

        if (error.status === 403) {
            return 'You do not have permission to perform this action.';
        }

        if (error.status === 404) {
            return 'The requested resource was not found.';
        }

        if (error.status === 500) {
            return 'Server error. Please try again later.';
        }

        if (error.status === 503) {
            return 'Service temporarily unavailable. Please try again later.';
        }

        // Try to extract message from error body
        if (error.error?.message) {
            return error.error.message;
        }

        if (typeof error.error === 'string') {
            return error.error;
        }

        return `Server error: ${error.statusText || 'Unknown error'}`;
    }

    /**
     * Extract user-friendly message from client errors
     */
    private getClientErrorMessage(error: Error): string {
        if (!error) {
            return 'An unknown error occurred';
        }

        // Check for specific error types
        if (error.name === 'ChunkLoadError') {
            return 'Failed to load application resources. Please refresh the page.';
        }

        if (error.message) {
            // Remove technical details for user-facing message
            const message = error.message;
            if (message.includes('Cannot read property')) {
                return 'A component failed to load properly.';
            }
            if (message.includes('undefined') || message.includes('null')) {
                return 'Missing required data. Please try again.';
            }
            return message;
        }

        return 'An unexpected error occurred';
    }

    /**
     * Send error to logging service (implement based on your needs)
     */
    // private logErrorToService(error: Error | HttpErrorResponse): void {
    //     // Example: Send to Sentry, LogRocket, or custom logging endpoint
    //     // this.loggingService.logError(error);
    // }
}
