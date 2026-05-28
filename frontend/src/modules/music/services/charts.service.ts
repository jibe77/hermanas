import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface SelectedPlaylist {
    playlist: string;
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
}
