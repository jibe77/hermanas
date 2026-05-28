import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { LoggerService } from '@common/services';

import { ApplianceMessage, SocketResponse } from '../models';
import { RxStompService } from './rx-stomp.service';
import { ProgressWebsocketService } from './progresswebsocket.service';

describe('ProgressWebsocketService', () => {
    let service: ProgressWebsocketService;
    let mockRxStompService: jasmine.SpyObj<RxStompService>;
    let mockLoggerService: jasmine.SpyObj<LoggerService>;
    let mockStompClient: any;
    let messageSubject: Subject<any>;
    let errorSubject: Subject<any>;

    beforeEach(() => {
        messageSubject = new Subject();
        errorSubject = new Subject();

        // Create mock STOMP client with RxStomp methods
        mockStompClient = {
            configure: jasmine.createSpy('configure'),
            activate: jasmine.createSpy('activate'),
            watch: jasmine.createSpy('watch').and.returnValue(messageSubject.asObservable()),
            stompErrors$: errorSubject.asObservable(),
        };

        // Create mock RxStompService
        mockRxStompService = jasmine.createSpyObj('RxStompService', [], {
            stompClient: mockStompClient,
        });

        // Create mock LoggerService
        mockLoggerService = jasmine.createSpyObj('LoggerService', [
            'debug',
            'info',
            'warn',
            'error',
        ]);

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
        it('should configure with progressStompConfig', () => {
            expect(mockStompClient.configure).toHaveBeenCalled();
            const configArg = mockStompClient.configure.calls.mostRecent().args[0];
            expect(configArg.webSocketFactory).toBeDefined();
        });

        it('should subscribe to /topic/progress endpoint', () => {
            expect(mockStompClient.watch).toHaveBeenCalledWith('/topic/progress');
        });

        it('should activate the STOMP client on construction', () => {
            expect(mockStompClient.activate).toHaveBeenCalled();
        });
    });

    describe('Inherited functionality from WebSocketService', () => {
        it('should inherit getObservable() method', () => {
            const observable = service.getObservable();
            expect(observable).toBeDefined();
            expect(typeof observable.subscribe).toBe('function');
        });

        it('should parse progress messages correctly', done => {
            const progressMessage: ApplianceMessage = {
                appliance: 'DOOR',
                state: 'OPENING',
            };

            const testFrame = {
                body: JSON.stringify(progressMessage),
            };

            service
                .getObservable()
                .pipe(take(1))
                .subscribe((response: SocketResponse) => {
                    expect(response.type).toBe('SUCCESS');
                    expect(response.message).toEqual(progressMessage);
                    done();
                });

            messageSubject.next(testFrame);
        });

        it('should handle progress errors correctly', done => {
            const errorMessage = 'Progress update failed';
            const errorFrame = {
                headers: { message: errorMessage },
            };

            service
                .getObservable()
                .pipe(take(1))
                .subscribe((response: SocketResponse) => {
                    expect(response.type).toBe('ERROR');
                    expect(response.message).toBe(errorMessage);
                    done();
                });

            errorSubject.next(errorFrame);
        });

        it('should handle multiple progress updates', done => {
            const progressStates = ['STARTING', 'IN_PROGRESS', 'COMPLETING', 'COMPLETED'];
            const results: SocketResponse[] = [];

            service.getObservable().subscribe(response => {
                results.push(response);
                if (results.length === progressStates.length) {
                    results.forEach((result, index) => {
                        expect(result.type).toBe('SUCCESS');
                        const message = result.message as ApplianceMessage;
                        expect(message.state).toBe(progressStates[index]);
                    });
                    done();
                }
            });

            progressStates.forEach(state => {
                messageSubject.next({
                    body: JSON.stringify({ appliance: 'DOOR', state }),
                });
            });
        });

        it('should merge progress messages and errors', done => {
            const responses: SocketResponse[] = [];
            const expectedCount = 3;

            service.getObservable().subscribe(response => {
                responses.push(response);
                if (responses.length === expectedCount) {
                    expect(responses[0].type).toBe('SUCCESS');
                    expect(responses[1].type).toBe('ERROR');
                    expect(responses[2].type).toBe('SUCCESS');
                    done();
                }
            });

            messageSubject.next({ body: JSON.stringify({ appliance: 'DOOR', state: 'STARTING' }) });
            errorSubject.next({ headers: { message: 'Temporary error' } });
            messageSubject.next({
                body: JSON.stringify({ appliance: 'DOOR', state: 'COMPLETED' }),
            });
        });
    });
});
