import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PhotoEntry {
    name: string;
    type: 'DIRECTORY' | 'FILE';
    size: number;
    lastModified: number;
}

export interface PhotoListing {
    /** Relative path of the listed directory inside the photos root (forward slashes, '' = root). */
    path: string;
    directories: PhotoEntry[];
    files: PhotoEntry[];
}

@Injectable({ providedIn: 'root' })
export class PhotosService {
    private readonly base = `${environment.apiUrl}/camera/photos`;

    constructor(private http: HttpClient) {}

    list(path: string = ''): Observable<PhotoListing> {
        let params = new HttpParams();
        if (path) {
            params = params.set('path', path);
        }
        return this.http.get<PhotoListing>(this.base, { params, withCredentials: true });
    }

    /** Returns the URL to use as <img src=""> for the given image relative path. */
    fileUrl(relativePath: string): string {
        return `${this.base}/file?path=${encodeURIComponent(relativePath)}`;
    }
}
