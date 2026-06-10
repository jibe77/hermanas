import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface SelectedPlaylist {
    playlist: string;
}

export interface SwitchStatus {
    statusEnum: string;
    timeOut: number;
}

@Injectable({ providedIn: 'root' })
export class ChartsService extends AbstractService {
    private http = inject(HttpClient);

    listPlaylists(): Observable<string[]> {
        return this.http.get<string[]>(`${this.domainBase}/music/playlists`, {
            headers: this.getHeaders(),
        });
    }

    listSongs(playlist: string): Observable<string[]> {
        return this.http.get<string[]>(
            `${this.domainBase}/music/playlists/${encodeURIComponent(playlist)}/songs`,
            { headers: this.getHeaders() }
        );
    }

    getSelectedPlaylist(): Observable<SelectedPlaylist> {
        return this.http.get<SelectedPlaylist>(`${this.domainBase}/music/selected-playlist`, {
            headers: this.getHeaders(),
        });
    }

    setSelectedPlaylist(playlist: string): Observable<SelectedPlaylist> {
        return this.http.put<SelectedPlaylist>(
            `${this.domainBase}/music/selected-playlist`,
            { playlist },
            { headers: this.getHeaders() }
        );
    }

    /**
     * Plays the given playlist immediately. Does not change the persisted
     * "selected playlist" — used to preview the current dropdown choice
     * without committing it.
     */
    playPlaylist(playlist: string): Observable<SwitchStatus> {
        const params = new HttpParams().set('param', 'true').set('playlist', playlist);
        return this.http.get<SwitchStatus>(`${this.domainBase}/music/switch`, {
            params,
            headers: this.getHeaders(),
        });
    }

    stop(): Observable<SwitchStatus> {
        const params = new HttpParams().set('param', 'false');
        return this.http.get<SwitchStatus>(`${this.domainBase}/music/switch`, {
            params,
            headers: this.getHeaders(),
        });
    }

    /**
     * Triggers a one-shot cock crow over the speakers (the same sound the
     * sunrise scheduler plays automatically). USER role required server-side.
     */
    cocorico(): Observable<boolean> {
        return this.http.get<boolean>(`${this.domainBase}/music/cocorico`, {
            headers: this.getHeaders(),
        });
    }

    /**
     * Returns the playback duration (ms) that will apply on the next play,
     * computed from the current energy mode (eco / regular / sunny). Lets the
     * UI advertise "Play for 15 minutes" instead of an opaque Play button.
     */
    getPlayDuration(): Observable<{ durationMs: number }> {
        return this.http.get<{ durationMs: number }>(
            `${this.domainBase}/music/play-duration`,
            { headers: this.getHeaders() }
        );
    }
}
