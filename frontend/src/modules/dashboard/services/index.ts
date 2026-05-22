import { ProgressWebsocketService } from '@modules/dashboard/services/progresswebsocket.service';
import { WebSocketService } from '@modules/dashboard/services/websocket.service';
import { VersionService } from '@modules/system/services/version.service';

import { DoorService } from './door.service';
import { FanService } from './fan.service';
import { LightService } from './light.service';
import { MeteoService } from './meteo.service';
import { MusicService } from './music.service';
import { RxStompService } from './rx-stomp.service';
import { SchedulerService } from './scheduler.service';

export const services = [
    DoorService,
    SchedulerService,
    MeteoService,
    MusicService,
    FanService,
    LightService,
    VersionService,
    RxStompService,
    WebSocketService,
    ProgressWebsocketService,
];

export * from './door.service';
export * from './scheduler.service';
export * from './meteo.service';
export * from './music.service';
export * from './fan.service';
export * from './light.service';
export * from './rx-stomp.service';
export * from './websocket.service';
export * from './progresswebsocket.service';
