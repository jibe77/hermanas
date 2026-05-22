export * from './dashboard.model';

export interface ApplianceMessage {
    appliance: 'LIGHT' | 'FAN' | 'DOOR' | 'MUSIC';
    state: string;
}

export interface SocketResponse {
    type: 'SUCCESS' | 'ERROR';
    message: ApplianceMessage | string;
}

export interface SwitchResponse {
    success: boolean;
    message?: string;
}

export class WebSocketOptions {
    constructor(public brokerEndpoint: string) {}
}
