import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import { ChartsService } from '@modules/music/services/charts.service';
import { ConfigService } from '@modules/energy/services/config.service';
import { UserService } from '@modules/auth/services';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CardComponent,
        FaIconComponent,
        ReactiveFormsModule,
        FormsModule,
    ],
})
export class ChartsComponent implements OnInit, OnDestroy {
    private _chartsService = inject(ChartsService);
    private _configService = inject(ConfigService);
    private _userService = inject(UserService);
    private _toastService = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    isAdmin = false;
    cocoricoEnabled = false;
    songAtSunsetEnabled = false;

    playlists: string[] = [];
    selectedPlaylist = '';
    persistedPlaylist = '';
    songs: string[] = [];

    loadingPlaylists = false;
    loadingSongs = false;
    saving = false;

    volumePercent = 78;
    private persistedVolume = 78;
    volumeSaving = false;

    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        this.isAdmin = this._userService.isAdmin();
        this.loadPlaylists();
        this.loadConfig();
    }

    private loadConfig(): void {
        this._configService
            .getAll()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: cfg => {
                    this.volumePercent = cfg.music_settings.volume_regular_percent;
                    this.persistedVolume = this.volumePercent;
                    this.cocoricoEnabled = cfg.audio_toggles.cocorico_at_sunrise;
                    this.songAtSunsetEnabled = cfg.audio_toggles.song_at_sunset;
                    this.cdr.detectChanges();
                },
                error: () => {
                    // Silent: keep the default the input was rendered with.
                },
            });
    }

    onCocoricoToggle(): void {
        this._configService
            .setCocoricoAtSunrise(this.cocoricoEnabled)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () =>
                    this._toastService.success(
                        this.cocoricoEnabled ? 'Cocorico activé' : 'Cocorico désactivé',
                        'Music'
                    ),
                error: (err: HttpErrorResponse) =>
                    this._toastService.error(
                        err.error?.message || err.message || 'Save failed',
                        `Music — HTTP ${err.status}`
                    ),
            });
    }

    onSongAtSunsetToggle(): void {
        this._configService
            .setSongAtSunset(this.songAtSunsetEnabled)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () =>
                    this._toastService.success(
                        this.songAtSunsetEnabled
                            ? 'Chanson du soir activée'
                            : 'Chanson du soir désactivée',
                        'Music'
                    ),
                error: (err: HttpErrorResponse) =>
                    this._toastService.error(
                        err.error?.message || err.message || 'Save failed',
                        `Music — HTTP ${err.status}`
                    ),
            });
    }

    onVolumeChange(): void {
        // Just a hook to make the template binding feel responsive; the actual
        // dirty check is the getter below.
    }

    get volumeDirty(): boolean {
        return this.volumePercent !== this.persistedVolume;
    }

    saveVolume(): void {
        if (!this.volumeDirty || this.volumeSaving) {
            return;
        }
        this.volumeSaving = true;
        this._configService
            .setMusicVolume(this.volumePercent)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => {
                    this.persistedVolume = this.volumePercent;
                    this.volumeSaving = false;
                    this._toastService.success(`Volume réglé à ${this.volumePercent}%`, 'Music');
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.volumeSaving = false;
                    this._toastService.error(
                        err.error?.message || err.message || 'Cannot save volume',
                        `Music — HTTP ${err.status}`
                    );
                    this.cdr.detectChanges();
                },
            });
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
                        err.error?.error ||
                            err.error?.message ||
                            err.message ||
                            'Cannot save selection',
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
