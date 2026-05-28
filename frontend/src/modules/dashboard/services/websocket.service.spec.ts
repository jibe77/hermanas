import { TestBed } from '@angular/core/testing';
import { RxStompConfig } from '@stomp/rx-stomp';
import { firstValueFrom, Subject } from 'rxjs';
import { take, toArray } from 'rxjs/operators';

import { ApplianceMessage, SocketResponse, WebSocketOptions } from '../models';
import { RxStompService } from './rx-stomp.service';
import { WebSocketService } from './websocket.service';

describe('WebSocketService', () => {
    let service: WebSocketService;
    let mockRxStompService: Partial<RxStompService>;
    let mockStompClient: {
        configure: ReturnType<typeof vi.fn>;
        activate: ReturnType<typeof vi.fn>;
        watch: ReturnType<typeof vi.fn>;
        stompErrors$: ReturnType<Subject<unknown>['asObservable']>;
    };
    let messageSubject: Subject<{ body: string }>;
    let errorSubject: Subject<{ headers: Record<string, string> }>;

    const testBrokerEndpoint = '/topic/test';
    const testStompConfig: RxStompConfig = {
        heartbeatIncoming: 1000,
        heartbeatOutgoing: 1000,
    };
    const testOptions = new WebSocketOptions(testBrokerEndpoint);

    beforeEach(() => {
        messageSubject = new Subject();
        errorSubject = new Subject();

        mockStompClient = {
            configure: vi.fn(),
            activate: vi.fn(),
            watch: vi.fn().mockReturnValue(messageSubject.asObservable()),
            stompErrors$: errorSubject.asObservable(),
        };

        mockRxStompService = {
            stompClient: mockStompClient as unknown as RxStompService['stompClient'],
        };

        TestBed.configureTestingModule({
            providers: [
                { provide: RxStompService, useValue: mockRxStompService },
                { provide: RxStompConfig, useValue: testStompConfig },
                { provide: WebSocketOptions, useValue: testOptions },
                WebSocketService,
            ],
        });

        service = TestBed.inject(WebSocketService);
    });

    afterEach(() => {
        messageSubject.complete();
        errorSubject.complete();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('Connection', () => {
        it('configures the STOMP client with the merged config', () => {
            expect(mockStompClient.configure).toHaveBeenCalled();
            const configArg = mockStompClient.configure.mock.calls.at(-1)?.[0];
            expect(configArg.heartbeatIncoming).toBe(1000);
            expect(configArg.heartbeatOutgoing).toBe(1000);
            expect(configArg.reconnectDelay).toBe(10000);
            expect(configArg.brokerURL).toBeDefined();
        });

        it('activates the STOMP client', () => {
            expect(mockStompClient.activate).toHaveBeenCalled();
        });

        it('subscribes to the configured broker endpoint', () => {
            expect(mockStompClient.watch).toHaveBeenCalledWith(testBrokerEndpoint);
        });
    });

    describe('Message handling', () => {
        it('parses SUCCESS messages from STOMP frames', async () => {
            const applianceMessage: ApplianceMessage = { appliance: 'LIGHT', state: 'ON' };
            const next = firstValueFrom(service.getObservable().pipe(take(1)));
            messageSubject.next({ body: JSON.stringify(applianceMessage) });

            const response = await next;
            expect(response.type).toBe('SUCCESS');
            expect(response.message).toEqual(applianceMessage);
        });

        it('parses every message in order', async () => {
            const messages: ApplianceMessage[] = [
                { appliance: 'LIGHT', state: 'ON' },
                { appliance: 'FAN', state: 'OFF' },
                { appliance: 'DOOR', state: 'OPEN' },
            ];
            const collected = firstValueFrom(
                service.getObservable().pipe(take(messages.length), toArray())
            );
            for (const msg of messages) {
                messageSubject.next({ body: JSON.stringify(msg) });
            }

            const results = await collected;
            results.forEach((result, index) => {
                expect(result.type).toBe('SUCCESS');
                expect(result.message).toEqual(messages[index]);
            });
        });
    });

    describe('Error handling', () => {
        it('emits ERROR type for STOMP errors', async () => {
            const errorMessage = 'Connection failed';
            const next = firstValueFrom(service.getObservable().pipe(take(1)));
            errorSubject.next({ headers: { message: errorMessage } });

            const response: SocketResponse = await next;
            expect(response.type).toBe('ERROR');
            expect(response.message).toBe(errorMessage);
        });

        it('falls back to a generic message when the header is missing', async () => {
            const next = firstValueFrom(service.getObservable().pipe(take(1)));
            errorSubject.next({ headers: {} });

            const response: SocketResponse = await next;
            expect(response.type).toBe('ERROR');
            expect(response.message).toBe('Unknown STOMP error');
        });

        it('streams every error in order', async () => {
            const errorMessages = ['Error 1', 'Error 2', 'Error 3'];
            const collected = firstValueFrom(
                service.getObservable().pipe(take(errorMessages.length), toArray())
            );
            for (const msg of errorMessages) {
                errorSubject.next({ headers: { message: msg } });
            }

            const results = await collected;
            results.forEach((result, index) => {
                expect(result.type).toBe('ERROR');
                expect(result.message).toBe(errorMessages[index]);
            });
        });
    });

    describe('Message and error merging', () => {
        it('merges both streams into a single observable', async () => {
            const collected = firstValueFrom(service.getObservable().pipe(take(4), toArray()));

            messageSubject.next({ body: JSON.stringify({ appliance: 'LIGHT', state: 'ON' }) });
            errorSubject.next({ headers: { message: 'Error 1' } });
            messageSubject.next({ body: JSON.stringify({ appliance: 'FAN', state: 'OFF' }) });
            errorSubject.next({ headers: { message: 'Error 2' } });

            const responses = await collected;
            expect(responses[0].type).toBe('SUCCESS');
            expect(responses[1].type).toBe('ERROR');
            expect(responses[2].type).toBe('SUCCESS');
            expect(responses[3].type).toBe('ERROR');
        });
    });
});
