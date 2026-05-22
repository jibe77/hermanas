import { TestBed } from '@angular/core/testing';
import { RxStompConfig } from '@stomp/rx-stomp';
import { Subject } from 'rxjs';
import { take } from 'rxjs/operators';

import { ApplianceMessage, SocketResponse, WebSocketOptions } from '../models';
import { RxStompService } from './rx-stomp.service';
import { WebSocketService } from './websocket.service';

describe('WebSocketService', () => {
    let service: WebSocketService;
    let mockRxStompService: jasmine.SpyObj<RxStompService>;
    let mockStompClient: any;
    let messageSubject: Subject<any>;
    let errorSubject: Subject<any>;

    const testBrokerEndpoint = '/topic/test';
    const testStompConfig: RxStompConfig = {
        heartbeatIncoming: 1000,
        heartbeatOutgoing: 1000,
    };
    const testOptions = new WebSocketOptions(testBrokerEndpoint);

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
        it('should configure the STOMP client with merged config on construction', () => {
            expect(mockStompClient.configure).toHaveBeenCalled();
            const configArg = mockStompClient.configure.calls.mostRecent().args[0];
            expect(configArg.heartbeatIncoming).toBe(1000); // From testStompConfig
            expect(configArg.heartbeatOutgoing).toBe(1000); // From testStompConfig
            expect(configArg.reconnectDelay).toBe(10000); // From service default
            expect(configArg.brokerURL).toBeDefined();
        });

        it('should activate the STOMP client on construction', () => {
            expect(mockStompClient.activate).toHaveBeenCalled();
        });

        it('should subscribe to the specified broker endpoint', () => {
            expect(mockStompClient.watch).toHaveBeenCalledWith(testBrokerEndpoint);
        });
    });

    describe('Observable creation', () => {
        it('should return an observable from getObservable()', done => {
            const observable = service.getObservable();
            expect(observable).toBeDefined();

            // Verify it's a valid observable by subscribing
            const subscription = observable.pipe(take(1)).subscribe(response => {
                expect(response).toBeDefined();
                done();
            });

            // Emit a test message
            const testFrame = {
                body: JSON.stringify({ appliance: 'LIGHT', state: 'ON' }),
            };
            messageSubject.next(testFrame);

            subscription.unsubscribe();
        });
    });

    describe('Message handling', () => {
        it('should parse SUCCESS messages from STOMP frames', done => {
            const applianceMessage: ApplianceMessage = {
                appliance: 'LIGHT',
                state: 'ON',
            };

            const testFrame = {
                body: JSON.stringify(applianceMessage),
            };

            service
                .getObservable()
                .pipe(take(1))
                .subscribe((response: SocketResponse) => {
                    expect(response.type).toBe('SUCCESS');
                    expect(response.message).toEqual(applianceMessage);
                    done();
                });

            messageSubject.next(testFrame);
        });

        it('should handle multiple message types', done => {
            const messages: ApplianceMessage[] = [
                { appliance: 'LIGHT', state: 'ON' },
                { appliance: 'FAN', state: 'OFF' },
                { appliance: 'DOOR', state: 'OPEN' },
            ];

            const results: SocketResponse[] = [];

            service.getObservable().subscribe(response => {
                results.push(response);
                if (results.length === messages.length) {
                    results.forEach((result, index) => {
                        expect(result.type).toBe('SUCCESS');
                        expect(result.message).toEqual(messages[index]);
                    });
                    done();
                }
            });

            messages.forEach(msg => {
                messageSubject.next({ body: JSON.stringify(msg) });
            });
        });

        it('should parse complex JSON message bodies', done => {
            const complexMessage: ApplianceMessage = {
                appliance: 'DOOR',
                state: 'CLOSED',
            };

            service
                .getObservable()
                .pipe(take(1))
                .subscribe((response: SocketResponse) => {
                    expect(response.type).toBe('SUCCESS');
                    expect(response.message).toEqual(complexMessage);
                    done();
                });

            messageSubject.next({ body: JSON.stringify(complexMessage) });
        });
    });

    describe('Error handling', () => {
        it('should emit ERROR type for STOMP errors', done => {
            const errorMessage = 'Connection failed';
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

        it('should handle errors without message header', done => {
            const errorFrame = {
                headers: {},
            };

            service
                .getObservable()
                .pipe(take(1))
                .subscribe((response: SocketResponse) => {
                    expect(response.type).toBe('ERROR');
                    expect(response.message).toBe('Unknown STOMP error');
                    done();
                });

            errorSubject.next(errorFrame);
        });

        it('should handle multiple errors', done => {
            const errorMessages = ['Error 1', 'Error 2', 'Error 3'];
            const results: SocketResponse[] = [];

            service.getObservable().subscribe(response => {
                if (response.type === 'ERROR') {
                    results.push(response);
                    if (results.length === errorMessages.length) {
                        results.forEach((result, index) => {
                            expect(result.type).toBe('ERROR');
                            expect(result.message).toBe(errorMessages[index]);
                        });
                        done();
                    }
                }
            });

            errorMessages.forEach(msg => {
                errorSubject.next({ headers: { message: msg } });
            });
        });
    });

    describe('Message and error merging', () => {
        it('should merge both messages and errors into single observable', done => {
            const responses: SocketResponse[] = [];
            const expectedCount = 4;

            service.getObservable().subscribe(response => {
                responses.push(response);
                if (responses.length === expectedCount) {
                    expect(responses[0].type).toBe('SUCCESS');
                    expect(responses[1].type).toBe('ERROR');
                    expect(responses[2].type).toBe('SUCCESS');
                    expect(responses[3].type).toBe('ERROR');
                    done();
                }
            });

            // Emit in alternating pattern
            messageSubject.next({ body: JSON.stringify({ appliance: 'LIGHT', state: 'ON' }) });
            errorSubject.next({ headers: { message: 'Error 1' } });
            messageSubject.next({ body: JSON.stringify({ appliance: 'FAN', state: 'OFF' }) });
            errorSubject.next({ headers: { message: 'Error 2' } });
        });

        it('should maintain order of emissions within each stream', done => {
            const messages = ['msg1', 'msg2', 'msg3'];
            const errors = ['err1', 'err2'];
            const results: { type: string; value: any }[] = [];

            service.getObservable().subscribe(response => {
                results.push({ type: response.type, value: response.message });
                if (results.length === messages.length + errors.length) {
                    // Check messages maintain order
                    const successResults = results.filter(r => r.type === 'SUCCESS');
                    expect(successResults.length).toBe(messages.length);

                    // Check errors maintain order
                    const errorResults = results.filter(r => r.type === 'ERROR');
                    expect(errorResults.length).toBe(errors.length);
                    errorResults.forEach((result, index) => {
                        expect(result.value).toBe(errors[index]);
                    });

                    done();
                }
            });

            // Emit all messages first
            messages.forEach(msg => {
                messageSubject.next({ body: JSON.stringify(msg) });
            });

            // Then emit all errors
            errors.forEach(err => {
                errorSubject.next({ headers: { message: err } });
            });
        });
    });
});
