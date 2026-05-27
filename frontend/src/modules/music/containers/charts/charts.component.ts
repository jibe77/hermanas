import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import { ChartsService } from '@modules/music/services/charts.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
})
export class ChartsComponent implements OnInit, OnDestroy {
    playlists: string[] = [];
    selectedPlaylist = '';
    persistedPlaylist = '';
    songs: string[] = [];

    loadingPlaylists = false;
    loadingSongs = false;
    saving = false;

    private destroy$ = new Subject<void>();

    constructor(
        private _chartsService: ChartsService,
        private _toastService: ToastService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.loadPlaylists();
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    private loadPlaylists(): void {
        this.loadingPlaylists = true;
        this._chartsService
            .listPlaylists()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: list => {
                    this.playlists = list;
                    this.loadingPlaylists = false;
                    this.loadPersistedSelection();
                },
                error: (err: HttpErrorResponse) => {
                    this.loadingPlaylists = false;
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot list playlists',
                        `Music — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    private loadPersistedSelection(): void {
        this._chartsService
            .getSelectedPlaylist()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: res => {
                    this.persistedPlaylist = res.playlist || '';
                    // Default to persisted choice, otherwise first available playlist.
                    if (this.persistedPlaylist && this.playlists.includes(this.persistedPlaylist)) {
                        this.selectedPlaylist = this.persistedPlaylist;
                    } else if (this.playlists.length > 0) {
                        this.selectedPlaylist = this.playlists[0];
                    }
                    if (this.selectedPlaylist) {
                        this.loadSongs();
                    }
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot read selected playlist',
                        `Music — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    onPlaylistChange(playlist: string): void {
        this.selectedPlaylist = playlist;
        this.loadSongs();
    }

    private loadSongs(): void {
        if (!this.selectedPlaylist) {
            this.songs = [];
            return;
        }
        this.loadingSongs = true;
        this._chartsService
            .listSongs(this.selectedPlaylist)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: list => {
                    this.songs = list;
                    this.loadingSongs = false;
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.loadingSongs = false;
                    this.songs = [];
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot list songs',
                        `Music — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    validate(): void {
        if (!this.selectedPlaylist || this.saving) {
            return;
        }
        this.saving = true;
        this._chartsService
            .setSelectedPlaylist(this.selectedPlaylist)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: res => {
                    this.persistedPlaylist = res.playlist || '';
                    this.saving = false;
                    this._toastService.success(
                        `Playlist "${this.persistedPlaylist}" sauvegardée`,
                        'Music'
                    );
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.saving = false;
                    this._toastService.error(
                        err.error?.error || err.error?.message || err.message || 'Cannot save selection',
                        `Music — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
    }

    get isDirty(): boolean {
        return this.selectedPlaylist !== this.persistedPlaylist;
    }
}
