import { TestBed } from '@angular/core/testing';
import { firstValueFrom, Subject } from 'rxjs';
import { take, toArray } from 'rxjs/operators';
import { LoggerService } from '@common/services';

import { ApplianceMessage, SocketResponse } from '../models';
import { RxStompService } from './rx-stomp.service';
import { ProgressWebsocketService } from './progresswebsocket.service';

describe('ProgressWebsocketService', () => {
    let service: ProgressWebsocketService;
    let mockRxStompService: Partial<RxStompService>;
    let mockLoggerService: Partial<LoggerService>;
    let mockStompClient: {
        configure: ReturnType<typeof vi.fn>;
        activate: ReturnType<typeof vi.fn>;
        watch: ReturnType<typeof vi.fn>;
        stompErrors$: ReturnType<Subject<unknown>['asObservable']>;
    };
    let messageSubject: Subject<{ body: string }>;
    let errorSubject: Subject<{ headers: Record<string, string> }>;

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

        mockLoggerService = {
            debug: vi.fn(),
            info: vi.fn(),
            warn: vi.fn(),
            error: vi.fn(),
        };

        TestBed.configureTestingModule({
            providers: [
                { provide: RxStompService, useValue: mockRxStompService },
                { provide: LoggerService, useValue: mockLoggerService },
                ProgressWebsocketService,
            ],
        });

        service = TestBed.inject(ProgressWebsocketService);
    });

    afterEach(() => {
        messageSubject.complete();
        errorSubject.complete();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('Configuration', () => {
        it('configures with the progress STOMP config', () => {
            expect(mockStompClient.configure).toHaveBeenCalled();
            const configArg = mockStompClient.configure.mock.calls.at(-1)?.[0];
            expect(configArg.webSocketFactory).toBeDefined();
        });

        it('subscribes to /topic/progress', () => {
            expect(mockStompClient.watch).toHaveBeenCalledWith('/topic/progress');
        });

        it('activates the STOMP client on construction', () => {
            expect(mockStompClient.activate).toHaveBeenCalled();
        });
    });

    describe('Inherited functionality from WebSocketService', () => {
        it('inherits getObservable()', () => {
            const observable = service.getObservable();
            expect(observable).toBeDefined();
            expect(typeof observable.subscribe).toBe('function');
        });

        it('parses progress messages correctly', async () => {
            const progressMessage: ApplianceMessage = { appliance: 'DOOR', state: 'OPENING' };
            const next = firstValueFrom(service.getObservable().pipe(take(1)));
            messageSubject.next({ body: JSON.stringify(progressMessage) });

            const response: SocketResponse = await next;
            expect(response.type).toBe('SUCCESS');
            expect(response.message).toEqual(progressMessage);
        });

        it('handles progress errors correctly', async () => {
            const errorMessage = 'Progress update failed';
            const next = firstValueFrom(service.getObservable().pipe(take(1)));
            errorSubject.next({ headers: { message: errorMessage } });

            const response: SocketResponse = await next;
            expect(response.type).toBe('ERROR');
            expect(response.message).toBe(errorMessage);
        });

        it('streams every progress update in order', async () => {
            const progressStates = ['STARTING', 'IN_PROGRESS', 'COMPLETING', 'COMPLETED'];
            const collected = firstValueFrom(
                service.getObservable().pipe(take(progressStates.length), toArray())
            );
            for (const state of progressStates) {
                messageSubject.next({ body: JSON.stringify({ appliance: 'DOOR', state }) });
            }

            const results = await collected;
            results.forEach((result, index) => {
                expect(result.type).toBe('SUCCESS');
                expect((result.message as ApplianceMessage).state).toBe(progressStates[index]);
            });
        });

        it('merges progress messages and errors', async () => {
            const collected = firstValueFrom(service.getObservable().pipe(take(3), toArray()));

            messageSubject.next({ body: JSON.stringify({ appliance: 'DOOR', state: 'STARTING' }) });
            errorSubject.next({ headers: { message: 'Temporary error' } });
            messageSubject.next({
                body: JSON.stringify({ appliance: 'DOOR', state: 'COMPLETED' }),
            });

            const responses = await collected;
            expect(responses[0].type).toBe('SUCCESS');
            expect(responses[1].type).toBe('ERROR');
            expect(responses[2].type).toBe('SUCCESS');
        });
    });
});
