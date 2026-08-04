import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError, timer } from 'rxjs';
import { map, retry } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { UserService } from '@modules/auth/services';

export interface SnapshotAnalysis {
    status: string;
    lang: string;
    message: string;
}

export type CaptureStatusName = 'CAPTURING' | 'ANALYZING' | 'DONE' | 'ERROR';

/**
 * One bounding box returned by the vision model. Coordinates are normalized
 * (0..1) relative to the snapshot dimensions, which lets us overlay them on
 * the rendered <img> at any size without knowing the actual resolution.
 *
 * `type` is always the canonical English label (`chicken` / `egg`) regardless
 * of the analysis language, so the overlay color logic stays language-agnostic.
 */
export interface Detection {
    type: string;
    confidence: number;
    x: number;
    y: number;
    width: number;
    height: number;
}

export interface CaptureState {
    status: CaptureStatusName;
    lang: string;
    message?: string;
    errorCode?: string;
    errorMessage?: string;
    imageAvailable: boolean;
    detections?: Detection[];
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
        // In demo mode there is no real backend file — serve a real chicken-
        // coop snapshot bundled with the SPA. We swap the per-file URL for a
        // tag-line caption baked into the image itself, so every cell of the
        // demo gallery resolves to the same photo. Angular HTTP interceptors
        // are bypassed by <img src>, so we have to handle the substitution here.
        if (this.userService.isDemoMode()) {
            return DEMO_COOP_PHOTO_URL;
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
        // No demo-mode shortcut here: the live snapshot panel hits the real
        // backend even in demo, exactly like an unauthenticated visitor.
        // The blocked POST will surface the yellow "demo blocked" toast and
        // the panel stays empty — same behaviour as a logged-out session.
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
     * opaque capture id used to drive {@link captureImageUrl} and the STOMP
     * subscription on {@code /topic/captures/{id}}.
     *
     * @param analyze `false` to only take the picture. The page loads this way:
     *        one AI analysis per visit was flooding the inference server with
     *        work nobody had asked for. The analysis is now behind a button.
     */
    startCapture(lang: 'fr' | 'en' | 'ro', analyze = true): Observable<string> {
        const params = new HttpParams().set('lang', lang).set('analyze', String(analyze));
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
     * One-shot fetch of the current capture state. Used by the SPA right after
     * {@link startCapture} resolves to seed the UI before the STOMP stream
     * delivers the first frame — this covers the rare race where the backend
     * finishes the pipeline (e.g. served from the AI vision cache) before the
     * SPA subscribes to {@code /topic/captures/{id}}.
     */
    getCaptureStatus(captureId: string): Observable<CaptureState> {
        return this.http.get<CaptureState>(
            `${environment.apiUrl}/captures/${captureId}/status`,
            { withCredentials: true }
        );
    }
}

/**
 * Real chicken-coop snapshot bundled with the SPA. Used in demo mode by both
 * the live panel and the archive gallery so the visitor sees an actual hen
 * picture instead of the previous abstract SVG placeholder. Lives under
 * src/assets/demo/ so Angular's asset pipeline serves it from the same
 * origin as the SPA — no extra Spring static-resource wiring needed.
 */
const DEMO_COOP_PHOTO_URL = 'assets/demo/coop-01.jpeg';
