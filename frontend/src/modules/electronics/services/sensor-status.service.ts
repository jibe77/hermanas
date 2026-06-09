import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy, inject } from '@angular/core';
import { AbstractService, LoggerService } from '@common/services';
import { RxStompService } from '@modules/dashboard/services/rx-stomp.service';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/**
 * Live DHT22 reading pushed by the backend on /topic/sensor every time
 * SensorService.refreshData() succeeds (periodic job + manual GET on the
 * sensor REST endpoint). Used by the Electronics page to display fresh
 * temperature/humidity values without polling.
 */
export interface SensorReading {
    temperature: number | null;
    humidity: number | null;
    /** ISO LocalDateTime when the sample was taken (no zone). */
    sampledAt: string | null;
    /** Server-side epoch millis — easier to format than sampledAt. */
    timestamp: number;
}

@Injectable({ providedIn: 'root' })
export class SensorStatusService extends AbstractService implements OnDestroy {
    private httpClient = inject(HttpClient);
    private stompService = inject(RxStompService);
    private logger = inject(LoggerService);

    private readonly destroy$ = new Subject<void>();

    constructor() {
        super();
        this.ensureStompActivated();
    }

    /**
     * Seed value retrieved over HTTP so the UI is not blank until the first
     * scheduled refresh. Hits the existing /sensor/info endpoint.
     */
    public getInitialStatus(): Observable<SensorReading> {
        return this.httpClient
            .get<{ temperature: number | null; humidity: number | null; dateTime: string | null }>(
                this.domainBase + '/sensor/info',
                { headers: this.getHeaders() }
            )
            .pipe(
                map(info => ({
                    temperature: info.temperature,
                    humidity: info.humidity,
                    sampledAt: info.dateTime,
                    timestamp: Date.now(),
                }))
            );
    }

    public observeUpdates(): Observable<SensorReading> {
        return this.stompService.stompClient
            .watch('/topic/sensor')
            .pipe(map(frame => JSON.parse(frame.body) as SensorReading));
    }

    private ensureStompActivated(): void {
        const client = this.stompService.stompClient;
        if (client.active) {
            return;
        }
        const brokerURL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}${environment.wsUrl}`;
        client.configure({
            brokerURL,
            heartbeatIncoming: 0,
            heartbeatOutgoing: 20000,
            reconnectDelay: 10000,
            webSocketFactory: () => new WebSocket(brokerURL),
            debug: str => this.logger.debug(str, undefined, 'SensorStatusService'),
        });
        client.activate();
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
