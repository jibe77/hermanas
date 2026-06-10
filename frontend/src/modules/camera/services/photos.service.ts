import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, interval, throwError, timer } from 'rxjs';
import { filter, map, retry, switchMap, takeWhile } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { UserService } from '@modules/auth/services';

export interface SnapshotAnalysis {
    status: string;
    lang: string;
    message: string;
}

export type CaptureStatusName = 'CAPTURING' | 'ANALYZING' | 'DONE' | 'ERROR';

export interface CaptureState {
    status: CaptureStatusName;
    lang: string;
    message?: string;
    errorCode?: string;
    errorMessage?: string;
    imageAvailable: boolean;
}

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
    private http = inject(HttpClient);
    private userService = inject(UserService);

    private readonly base = `${environment.apiUrl}/camera/photos`;

    list(path: string = ''): Observable<PhotoListing> {
        let params = new HttpParams();
        if (path) {
            params = params.set('path', path);
        }
        return this.http.get<PhotoListing>(this.base, { params, withCredentials: true });
    }

    /** Returns the URL to use as <img src=""> for the given image relative path. */
    fileUrl(relativePath: string): string {
        // In demo mode there is no real backend file — return an inline SVG so
        // the <img> tag renders something plausible. Angular HTTP interceptors
        // are bypassed by <img src>, so we have to handle the substitution here.
        if (this.userService.isDemoMode()) {
            return buildPlaceholderDataUrl(/* live */ false, relativePath);
        }
        return `${this.base}/file?path=${encodeURIComponent(relativePath)}`;
    }

    /**
     * URL of a freshly captured snapshot. The `date` query param is a cache buster
     * on the browser side so the <img> tag refetches whenever the URL is rebuilt.
     *
     * @param highQuality when true, asks the backend for the 960×540 / quality 80
     *                    capture (~600 ko) instead of the default 480×270 / quality 70
     *                    (~150 ko). Used by the dedicated Webcam page where bandwidth
     *                    matters less than detail.
     * @param force when true, asks the backend to bypass its 30-second picture cache
     *              and take a fresh capture. Used by the "refresh" button on the
     *              Webcam page so the operator can demand a new shot. Default is
     *              false: page loads, snapshots and AI analysis all share the same
     *              capture if they happen within the cache window.
     */
    snapshotUrl(highQuality = false, force = false): string {
        if (this.userService.isDemoMode()) {
            return buildPlaceholderDataUrl(/* live */ true);
        }
        const parts = [`date=${Date.now()}`];
        if (highQuality) parts.push('highQuality=true');
        if (force) parts.push('force=true');
        return `${environment.apiUrl}/camera/takePicture?${parts.join('&')}`;
    }

    /**
     * WIP: triggers the (not yet implemented) AI analysis of the latest snapshot.
     * The backend currently returns HTTP 501 with a localized "under development"
     * message; the response shape is the final one so the UI does not need to
     * change once the LLM integration lands.
     */
    analyzeSnapshot(lang: 'fr' | 'en' | 'ro'): Observable<SnapshotAnalysis> {
        const params = new HttpParams().set('lang', lang);
        return this.http.get<SnapshotAnalysis>(`${environment.apiUrl}/camera/analyze`, {
            params,
            withCredentials: true,
        });
    }

    /**
     * Kicks an async capture + analysis pipeline on the backend. Returns the
     * opaque capture id used to drive {@link captureImageUrl} and
     * {@link pollCaptureStatus}.
     */
    startCapture(lang: 'fr' | 'en' | 'ro'): Observable<string> {
        const params = new HttpParams().set('lang', lang);
        return this.http
            .post<{ captureId: string }>(`${environment.apiUrl}/captures`, null, {
                params,
                withCredentials: true,
            })
            .pipe(map(r => r.captureId));
    }

    /**
     * Fetches the captured JPEG as a blob, retrying on 404 (the image is not
     * ready yet) with a 500 ms backoff up to ~30 s. Resolves to a blob: URL the
     * template can hand to <img [src]>.
     */
    captureImageUrl(captureId: string): Observable<string> {
        return this.http
            .get(`${environment.apiUrl}/captures/${captureId}/image`, {
                responseType: 'blob',
                withCredentials: true,
            })
            .pipe(
                retry({
                    count: 60,
                    delay: (err: HttpErrorResponse) =>
                        err.status === 404 ? timer(500) : throwError(() => err),
                }),
                map(blob => URL.createObjectURL(blob))
            );
    }

    /**
     * Polls the capture status every second and emits every state change.
     * Completes naturally once the backend reports {@code DONE} or {@code ERROR}.
     */
    pollCaptureStatus(captureId: string): Observable<CaptureState> {
        return interval(1000).pipe(
            switchMap(() =>
                this.http.get<CaptureState>(
                    `${environment.apiUrl}/captures/${captureId}/status`,
                    { withCredentials: true }
                )
            ),
            takeWhile(s => s.status === 'CAPTURING' || s.status === 'ANALYZING', true),
            filter(s => s.status === 'DONE' || s.status === 'ERROR')
        );
    }
}

/**
 * Returns an inline SVG data-URL used as a placeholder image in demo mode. Two
 * palettes — green/yellow for the "Live" snapshot and brown/cream for archive
 * photos — so the two panels feel distinct at a glance. The optional `label`
 * argument is rendered in the bottom-right so an interviewer can verify the
 * archive grid wires through to per-file URLs (the file name appears on each
 * placeholder).
 */
function buildPlaceholderDataUrl(live: boolean, label?: string): string {
    const bg = live ? '#3b6b3a' : '#7a6b58';
    const accent = live ? '#d4e85b' : '#f0d27a';
    const title = live ? 'LIVE DEMO' : 'DEMO ARCHIVE';
    const sublabel = label ? label.split('/').pop() ?? '' : 'Hermanas — chicken coop';
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">
  <rect width="100%" height="100%" fill="${bg}"/>
  <rect x="40" y="40" width="560" height="280" fill="none" stroke="${accent}" stroke-width="4"/>
  <text x="320" y="180" font-family="sans-serif" font-size="38" font-weight="bold"
        text-anchor="middle" fill="${accent}">${title}</text>
  <text x="320" y="220" font-family="sans-serif" font-size="18"
        text-anchor="middle" fill="#ffffff">${sublabel}</text>
</svg>`;
    return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg);
}
