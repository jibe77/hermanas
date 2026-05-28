import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy, inject } from '@angular/core';
import { AbstractService, LoggerService } from '@common/services';
import { RxStompService } from '@modules/dashboard/services/rx-stomp.service';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export type ButtonName = 'UP' | 'BOTTOM' | 'BIRDHOUSE';

export interface ButtonStatus {
    button: ButtonName;
    pressed: boolean;
    timestamp: number;
}

@Injectable()
export class ButtonStatusService extends AbstractService implements OnDestroy {
    private httpClient = inject(HttpClient);
    private stompService = inject(RxStompService);
    private logger = inject(LoggerService);

    private readonly destroy$ = new Subject<void>();

    constructor() {
        super();
        this.ensureStompActivated();
    }

    public getInitialStatus(): Observable<ButtonStatus[]> {
        return this.httpClient.get<ButtonStatus[]>(this.domainBase + '/buttons/status', {
            headers: this.getHeaders(),
        });
    }

    public observeUpdates(): Observable<ButtonStatus> {
        return this.stompService.stompClient
            .watch('/topic/buttons')
            .pipe(map(frame => JSON.parse(frame.body) as ButtonStatus));
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
            debug: str => this.logger.debug(str, undefined, 'ButtonStatusService'),
        });
        client.activate();
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
