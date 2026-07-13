import { Injectable, inject } from '@angular/core';
import { RxStompConfig } from '@stomp/rx-stomp';
import { Observable, merge } from 'rxjs';
import { map } from 'rxjs/operators';
import { LoggerService } from '@common/services';
import { RxStompService } from '@modules/dashboard/services';
import { environment } from '../../../environments/environment';
import { CaptureState } from './photos.service';

/**
 * Replaces the 1-second poll on {@code GET /captures/{id}/status} with a single
 * STOMP subscription. The backend pushes a frame on {@code /topic/captures/{id}}
 * every time the capture moves through CAPTURING → ANALYZING → DONE/ERROR.
 *
 * <p>Why STOMP instead of HTTP polling: on flaky WiFi the long-running polls
 * occasionally hit the reverse-proxy timeout (the original 504 the operator
 * was seeing). The STOMP connection is already kept warm by other features
 * (sensor + appliance topics) and reconnects on its own with a 10 s backoff,
 * so there is no extra request to time out.</p>
 */
@Injectable({ providedIn: 'root' })
export class CaptureWebsocketService {
    private rxStomp = inject(RxStompService);
    private logger = inject(LoggerService);
    private connected = false;

    /**
     * Subscribes to {@code /topic/captures/{captureId}} and emits each frame as
     * a parsed {@link CaptureState}. Completes naturally once the backend
     * pushes a terminal state (DONE / ERROR). Errors out if the broker reports
     * a STOMP error while we are listening.
     */
    watch(captureId: string): Observable<CaptureState> {
        this.ensureConnected();
        const topic = `/topic/captures/${captureId}`;
        this.logger.info(
            'Subscribing to capture topic',
            { topic },
            'CaptureWebsocketService'
        );

        const messages$ = this.rxStomp.stompClient.watch(topic).pipe(
            map(frame => JSON.parse(frame.body) as CaptureState)
        );

        const errors$ = this.rxStomp.stompClient.stompErrors$.pipe(
            map(errorFrame => {
                const msg = errorFrame.headers['message'] || 'Unknown STOMP error';
                this.logger.error(
                    'Broker reported error while watching capture',
                    { topic, message: msg },
                    'CaptureWebsocketService'
                );
                throw new Error(msg);
            })
        );

        return merge(messages$, errors$).pipe(
            // Auto-complete on the first terminal frame so the caller never has
            // to .unsubscribe() by hand once the pipeline is done.
            takeUntilInclusive(s => s.status === 'DONE' || s.status === 'ERROR')
        );
    }

    /**
     * Activates the shared RxStomp client on first use. The Webcam page is
     * reachable without going through the dashboard, where
     * {@code ProgressWebsocketService} would otherwise have activated the
     * client. RxStomp.activate() is idempotent and a no-op when the client
     * is already connected, so this is safe to call on every {@link watch}.
     */
    private ensureConnected(): void {
        if (this.connected) {
            return;
        }
        this.connected = true;
        const brokerURL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}${environment.wsUrl}`;
        const config: RxStompConfig = {
            brokerURL,
            heartbeatIncoming: 0,
            heartbeatOutgoing: 20000,
            reconnectDelay: 10000,
            webSocketFactory: () => new WebSocket(brokerURL),
            debug: str => this.logger.debug(str, undefined, 'CaptureWebsocketService'),
        };
        this.logger.info(
            'Activating STOMP client for capture topics',
            { brokerURL },
            'CaptureWebsocketService'
        );
        this.rxStomp.stompClient.configure(config);
        this.rxStomp.stompClient.activate();
    }
}

/**
 * Mirrors {@code takeWhile(predicate, true)} but works on the inverse predicate
 * for readability at the call-site. Emits every value, including the first one
 * matching {@code stop}, then completes. Kept local to this file because
 * RxJS does not ship an inclusive {@code takeUntil(predicate)} out of the box.
 */
function takeUntilInclusive<T>(stop: (value: T) => boolean) {
    return (source: Observable<T>) =>
        new Observable<T>(subscriber => {
            return source.subscribe({
                next(value) {
                    subscriber.next(value);
                    if (stop(value)) {
                        subscriber.complete();
                    }
                },
                error(err) {
                    subscriber.error(err);
                },
                complete() {
                    subscriber.complete();
                },
            });
        });
}

