import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';

export enum LogLevel {
    Debug = 0,
    Info = 1,
    Warn = 2,
    Error = 3,
    None = 4,
}

export interface LogEntry {
    level: LogLevel;
    message: string;
    timestamp: Date;
    data?: any;
    source?: string;
}

@Injectable({
    providedIn: 'root',
})
export class LoggerService {
    private currentLogLevel: LogLevel = environment.production ? LogLevel.Warn : LogLevel.Debug;
    private logHistory: LogEntry[] = [];
    private readonly MAX_HISTORY = 100;

    /**
     * Log a debug message
     */
    public debug(message: string, data?: any, source?: string): void {
        this.log(LogLevel.Debug, message, data, source);
    }

    /**
     * Log an info message
     */
    public info(message: string, data?: any, source?: string): void {
        this.log(LogLevel.Info, message, data, source);
    }

    /**
     * Log a warning message
     */
    public warn(message: string, data?: any, source?: string): void {
        this.log(LogLevel.Warn, message, data, source);
    }

    /**
     * Log an error message
     */
    public error(message: string, data?: any, source?: string): void {
        this.log(LogLevel.Error, message, data, source);
    }

    /**
     * Set the current log level
     */
    public setLogLevel(level: LogLevel): void {
        this.currentLogLevel = level;
    }

    /**
     * Get the current log level
     */
    public getLogLevel(): LogLevel {
        return this.currentLogLevel;
    }

    /**
     * Get log history
     */
    public getHistory(): LogEntry[] {
        return [...this.logHistory];
    }

    /**
     * Clear log history
     */
    public clearHistory(): void {
        this.logHistory = [];
    }

    /**
     * Main logging method
     */
    private log(level: LogLevel, message: string, data?: any, source?: string): void {
        if (level < this.currentLogLevel) {
            return;
        }

        const entry: LogEntry = {
            level,
            message,
            timestamp: new Date(),
            data,
            source,
        };

        // Add to history
        this.logHistory.push(entry);
        if (this.logHistory.length > this.MAX_HISTORY) {
            this.logHistory.shift();
        }

        // Log to console based on level
        this.logToConsole(entry);

        // In production, you might want to send logs to a service
        if (environment.production && level >= LogLevel.Error) {
            this.sendToLoggingService(entry);
        }
    }

    /**
     * Log to console with appropriate method and formatting
     */
    private logToConsole(entry: LogEntry): void {
        const prefix = `[${this.getLogLevelName(entry.level)}]`;
        const timestamp = entry.timestamp.toISOString();
        const source = entry.source ? `[${entry.source}]` : '';
        const message = `${prefix} ${timestamp} ${source} ${entry.message}`;

        switch (entry.level) {
            case LogLevel.Debug:
                console.debug(message, entry.data || '');
                break;
            case LogLevel.Info:
                console.info(message, entry.data || '');
                break;
            case LogLevel.Warn:
                console.warn(message, entry.data || '');
                break;
            case LogLevel.Error:
                console.error(message, entry.data || '');
                break;
        }
    }

    /**
     * Send log to external logging service (implement based on your needs)
     */
    private sendToLoggingService(entry: LogEntry): void {
        // Example: Send to Sentry, LogRocket, or custom logging endpoint
        // try {
        //     this.http.post('/api/logs', entry).subscribe();
        // } catch (e) {
        //     // Fail silently to avoid infinite loops
        // }
    }

    /**
     * Get human-readable log level name
     */
    private getLogLevelName(level: LogLevel): string {
        switch (level) {
            case LogLevel.Debug:
                return 'DEBUG';
            case LogLevel.Info:
                return 'INFO';
            case LogLevel.Warn:
                return 'WARN';
            case LogLevel.Error:
                return 'ERROR';
            default:
                return 'UNKNOWN';
        }
    }
}
