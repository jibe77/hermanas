import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy, inject } from '@angular/core';
import { AbstractService, LoggerService } from '@common/services';
import { RxStompService } from '@modules/dashboard/services/rx-stomp.service';
import { Observable, Subject } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/** Mirror of org.jibe77.hermanas.websocket.CoopStatus on the wire. */
interface CoopStatusFrame {
    appliance: 'LIGHT' | 'FAN' | 'MUSIC' | 'DOOR';
    state: string;
}

/** Mirror of org.jibe77.hermanas.service.abstract_model.StatusEnum (door subset). */
export type DoorState =
    | 'OPENING'
    | 'CLOSING'
    | 'CLOSED'
    | 'OPENED'
    | 'OPENED_INCORRECTLY'
    | 'CLOSED_INCORRECTLY';

export interface DoorReading {
    state: DoorState;
    /** Server-side epoch millis (client-assigned on receipt — backend frame has no timestamp). */
    timestamp: number;
}

/**
 * Live servomotor status pushed by the backend on /topic/progress whenever the
 * door is opened or closed (DoorService.open/close). Filtered to the DOOR
 * appliance so the Electronics page only reacts to the relevant frames.
 */
@Injectable({ providedIn: 'root' })
export class DoorStatusService extends AbstractService implements OnDestroy {
    private httpClient = inject(HttpClient);
    private stompService = inject(RxStompService);
    private logger = inject(LoggerService);

    private readonly destroy$ = new Subject<void>();

    constructor() {
        super();
        this.ensureStompActivated();
    }

    /** Seed the UI with the latest stored door state via /door/status. */
    public getInitialStatus(): Observable<DoorReading> {
        return this.httpClient
            .get<{ status: string; timeStatusHasChanged: string }>(
                this.domainBase + '/door/status',
                { headers: this.getHeaders() }
            )
            .pipe(
                map(info => ({
                    state: (info.status as DoorState) ?? 'OPENED',
                    timestamp: info.timeStatusHasChanged
                        ? new Date(info.timeStatusHasChanged).getTime()
                        : Date.now(),
                }))
            );
    }

    public observeUpdates(): Observable<DoorReading> {
        return this.stompService.stompClient.watch('/topic/progress').pipe(
            map(frame => JSON.parse(frame.body) as CoopStatusFrame),
            filter(coop => coop.appliance === 'DOOR'),
            map(coop => ({ state: coop.state as DoorState, timestamp: Date.now() }))
        );
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
            debug: str => this.logger.debug(str, undefined, 'DoorStatusService'),
        });
        client.activate();
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
