import { TestBed } from '@angular/core/testing';

import { LoadingService } from './loading.service';

/**
 * LoadingService is a tiny counter wrapped in two signals. The tests below
 * exercise the counter arithmetic and the `isLoading` derivation — the rest
 * of the service is just signal-getter sugar.
 */
describe('LoadingService', () => {
    let service: LoadingService;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [LoadingService] });
        service = TestBed.inject(LoadingService);
    });

    it('starts inactive', () => {
        expect(service.getActiveCount()).toBe(0);
        expect(service.isLoading()).toBe(false);
        expect(service.state().message).toBeUndefined();
    });

    it('start() increments the counter and isLoading flips to true', () => {
        service.start('Loading dashboard…');
        expect(service.getActiveCount()).toBe(1);
        expect(service.isLoading()).toBe(true);
        expect(service.state().message).toBe('Loading dashboard…');
    });

    it('handles nested start/stop pairs without going negative', () => {
        service.start();
        service.start();
        service.start();
        expect(service.getActiveCount()).toBe(3);

        service.stop();
        service.stop();
        service.stop();
        expect(service.getActiveCount()).toBe(0);
        expect(service.isLoading()).toBe(false);

        // an extra stop() must clamp at 0 rather than go negative
        service.stop();
        expect(service.getActiveCount()).toBe(0);
    });

    it('clears the message only when the last consumer stops', () => {
        service.start('First');
        service.start('Second'); // a non-empty message overwrites the previous one
        expect(service.state().message).toBe('Second');

        service.stop();
        expect(service.state().message).toBe('Second'); // still 1 active, message kept

        service.stop();
        expect(service.state().message).toBeUndefined();
    });

    it('keeps the previous message when start() is called without arguments', () => {
        service.start('Keep me');
        service.start();
        expect(service.state().message).toBe('Keep me');
    });

    it('reset() jumps straight back to zero', () => {
        service.start('Heavy');
        service.start();
        service.start();
        service.reset();

        expect(service.getActiveCount()).toBe(0);
        expect(service.isLoading()).toBe(false);
        expect(service.state().message).toBeUndefined();
    });
});
