import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AbstractService } from '@common/services';
import { Observable } from 'rxjs';

export interface SelectedPlaylist {
    playlist: string;
}

@Injectable()
export class ChartsService extends AbstractService {
    constructor(private http: HttpClient) {
        super();
    }

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
