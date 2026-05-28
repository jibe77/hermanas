/* eslint-disable no-console -- this spec exercises the central console wrapper. */
import { TestBed } from '@angular/core/testing';

import { LoggerService, LogLevel } from './logger.service';

describe('LoggerService', () => {
    let service: LoggerService;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [LoggerService] });
        service = TestBed.inject(LoggerService);

        // Silence the actual console output during the spec; we still want to
        // assert the calls were made.
        vi.spyOn(console, 'debug').mockImplementation(() => undefined);
        vi.spyOn(console, 'info').mockImplementation(() => undefined);
        vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
    });

    describe('level filtering', () => {
        it('respects the configured threshold', () => {
            service.setLogLevel(LogLevel.Warn);

            service.debug('debug me');
            service.info('info me');
            service.warn('warn me');
            service.error('error me');

            expect(console.debug).not.toHaveBeenCalled();
            expect(console.info).not.toHaveBeenCalled();
            expect(console.warn).toHaveBeenCalledTimes(1);
            expect(console.error).toHaveBeenCalledTimes(1);
        });

        it('routes each level to the matching console method', () => {
            service.setLogLevel(LogLevel.Debug);

            service.debug('d');
            service.info('i');
            service.warn('w');
            service.error('e');

            expect(console.debug).toHaveBeenCalledTimes(1);
            expect(console.info).toHaveBeenCalledTimes(1);
            expect(console.warn).toHaveBeenCalledTimes(1);
            expect(console.error).toHaveBeenCalledTimes(1);
        });

        it('LogLevel.None silences everything', () => {
            service.setLogLevel(LogLevel.None);

            service.debug('d');
            service.info('i');
            service.warn('w');
            service.error('e');

            expect(console.debug).not.toHaveBeenCalled();
            expect(console.info).not.toHaveBeenCalled();
            expect(console.warn).not.toHaveBeenCalled();
            expect(console.error).not.toHaveBeenCalled();
        });
    });

    describe('formatting', () => {
        it('prefixes the level name and the source tag', () => {
            service.setLogLevel(LogLevel.Debug);
            service.info('hello', { request: 'GET /foo' }, 'AuthService');

            const call = (console.info as ReturnType<typeof vi.fn>).mock.calls.at(-1);
            expect(String(call?.[0])).toContain('[INFO]');
            expect(String(call?.[0])).toContain('[AuthService]');
            expect(String(call?.[0])).toContain('hello');
            expect(call?.[1]).toEqual({ request: 'GET /foo' });
        });

        it('omits the source bracket when none is given', () => {
            service.setLogLevel(LogLevel.Debug);
            service.debug('plain');

            const call = (console.debug as ReturnType<typeof vi.fn>).mock.calls.at(-1);
            expect(String(call?.[0])).toMatch(/\[DEBUG\]/);
            // No source segment between the ISO timestamp and the message
            expect(String(call?.[0])).not.toMatch(/\[\w+\]\s\[/);
        });
    });

    describe('history', () => {
        it('captures emitted entries and clearHistory() resets them', () => {
            service.setLogLevel(LogLevel.Debug);
            service.info('one');
            service.warn('two');

            const history = service.getHistory();
            expect(history).toHaveLength(2);
            expect(history[0].message).toBe('one');
            expect(history[1].level).toBe(LogLevel.Warn);

            service.clearHistory();
            expect(service.getHistory()).toHaveLength(0);
        });

        it('keeps history bounded (MAX_HISTORY = 100 — oldest evicted first)', () => {
            service.setLogLevel(LogLevel.Debug);
            for (let i = 0; i < 120; i++) {
                service.info(`entry ${i}`);
            }

            const history = service.getHistory();
            expect(history).toHaveLength(100);
            expect(history[0].message).toBe('entry 20'); // oldest 20 evicted
            expect(history.at(-1)?.message).toBe('entry 119');
        });

        it('does not record entries filtered out by the level threshold', () => {
            service.setLogLevel(LogLevel.Error);
            service.info('ignored');
            service.warn('also ignored');
            service.error('kept');

            const history = service.getHistory();
            expect(history).toHaveLength(1);
            expect(history[0].message).toBe('kept');
        });
    });
});
