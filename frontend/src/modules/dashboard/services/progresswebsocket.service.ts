import { Injectable } from '@angular/core';
import { RxStompConfig } from '@stomp/rx-stomp';
import { environment } from '../../../environments/environment';
import { LoggerService } from '@common/services';

import { WebSocketOptions } from '../models';

import { RxStompService } from './rx-stomp.service';
import { WebSocketService } from './websocket.service';

// Create factory function to use LoggerService instance
export function createProgressStompConfig(logger: LoggerService): RxStompConfig {
    const brokerURL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}${environment.wsUrl}`;
    return {
        webSocketFactory: () => {
            logger.info(
                'Connecting to progress STOMP broker',
                { wsUrl: brokerURL },
                'ProgressWebsocketService'
            );
            return new WebSocket(brokerURL);
        },
    };
}

@Injectable()
export class ProgressWebsocketService extends WebSocketService {
    constructor(stompService: RxStompService, logger: LoggerService) {
        super(
            stompService,
            createProgressStompConfig(logger),
            new WebSocketOptions('/topic/progress'),
            logger
        );
    }
}
