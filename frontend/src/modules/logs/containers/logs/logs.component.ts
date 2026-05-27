import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import {
    LogFileInfo,
    LogLevel,
    LogsService,
} from '@modules/logs/services/logs.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-logs',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './logs.component.html',
    styleUrls: ['logs.component.scss'],
})
export class LogsComponent implements OnInit, OnDestroy {
    files: LogFileInfo[] = [];
    selectedFile?: string;

    lineOptions = [200, 500, 1000, 2000, 5000];
    levelOptions: LogLevel[] = ['ALL', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];

    selectedLines = 500;
    selectedLevel: LogLevel = 'ALL';
    searchText = '';

    lines: string[] = [];
    loading = false;
    listError = false;
    contentError?: string;

    private destroy$ = new Subject<void>();

    constructor(
        private _logsService: LogsService,
        private _toastService: ToastService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.refreshFiles();
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    refreshFiles(): void {
        this.listError = false;
        this._logsService
            .listFiles()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: files => {
                    this.files = files;
                    if (!this.selectedFile && files.length > 0) {
                        this.selectedFile = files[0].name;
                        this.refreshContent();
                    }
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.listError = true;
                    this.files = [];
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot list log files',
                        `Logs — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    onFileChange(filename: string): void {
        this.selectedFile = filename;
        this.refreshContent();
    }

    refreshContent(): void {
        if (!this.selectedFile) {
            return;
        }
        this.loading = true;
        this.contentError = undefined;
        this.cdr.detectChanges();
        this._logsService
            .tail(this.selectedFile, {
                lines: this.selectedLines,
                level: this.selectedLevel,
                search: this.searchText,
            })
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: lines => {
                    this.lines = lines;
                    this.loading = false;
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.loading = false;
                    this.contentError = err.error?.message || err.message || 'Cannot read log file';
                    this.lines = [];
                    this._toastService.error(this.contentError, `Logs — HTTP ${err.status}`);
                    this.cdr.detectChanges();
                },
            });
    }

    cssClassForLine(line: string): string {
        if (/\bERROR\b/.test(line)) return 'log-error';
        if (/\bWARN\b/.test(line)) return 'log-warn';
        if (/\bINFO\b/.test(line)) return 'log-info';
        if (/\bDEBUG\b/.test(line)) return 'log-debug';
        if (/\bTRACE\b/.test(line)) return 'log-trace';
        return 'log-other';
    }

    formatSize(bytes: number): string {
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }
}
