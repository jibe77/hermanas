import { TestBed } from '@angular/core/testing';
import { firstValueFrom, take, toArray } from 'rxjs';

import { Toast, ToastService } from './toast.service';

describe('ToastService', () => {
    let service: ToastService;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [ToastService] });
        service = TestBed.inject(ToastService);
    });

    describe('emission helpers', () => {
        it('success() pushes a toast typed "success"', async () => {
            const next = firstValueFrom(service.toasts$.pipe(take(1)));
            service.success('Saved', 'Energy');
            const toast: Toast = await next;

            expect(toast.type).toBe('success');
            expect(toast.message).toBe('Saved');
            expect(toast.title).toBe('Energy');
            expect(toast.duration).toBe(5000);
            expect(toast.timestamp).toBeInstanceOf(Date);
            expect(toast.id).toMatch(/^toast-/);
        });

        it('error(), warning() and info() carry their type', async () => {
            const collected = firstValueFrom(service.toasts$.pipe(take(3), toArray()));
            service.error('Boom');
            service.warning('Careful');
            service.info('FYI');

            const [a, b, c] = await collected;
            expect(a.type).toBe('error');
            expect(b.type).toBe('warning');
            expect(c.type).toBe('info');
        });

        it('honours an explicit duration override', async () => {
            const next = firstValueFrom(service.toasts$.pipe(take(1)));
            service.info('Quick', undefined, 1000);
            const toast = await next;
            expect(toast.duration).toBe(1000);
        });
    });

    describe('queue management', () => {
        it('getToasts() reflects active toasts and clear() empties them', () => {
            service.info('one', undefined, 0); // duration 0 disables auto-remove
            service.info('two', undefined, 0);
            expect(service.getToasts()).toHaveLength(2);

            service.clear();
            expect(service.getToasts()).toHaveLength(0);
        });

        it('remove() drops a specific toast', () => {
            service.info('one', undefined, 0);
            service.info('two', undefined, 0);

            const [first] = service.getToasts();
            service.remove(first.id);

            const remaining = service.getToasts();
            expect(remaining).toHaveLength(1);
            expect(remaining[0].id).not.toBe(first.id);
        });

        it('emits the removal id on removeToast$', async () => {
            service.info('one', undefined, 0);
            const id = service.getToasts()[0].id;

            const removalEvent = firstValueFrom(service.removeToast$.pipe(take(1)));
            service.remove(id);
            await expect(removalEvent).resolves.toBe(id);
        });
    });

    describe('auto-removal timer', () => {
        beforeEach(() => vi.useFakeTimers());
        afterEach(() => vi.useRealTimers());

        it('removes the toast after its duration elapses', () => {
            service.info('Pop', undefined, 1000);
            expect(service.getToasts()).toHaveLength(1);

            vi.advanceTimersByTime(999);
            expect(service.getToasts()).toHaveLength(1);

            vi.advanceTimersByTime(1);
            expect(service.getToasts()).toHaveLength(0);
        });

        it('skips the timer when duration is 0 (sticky toast)', () => {
            service.info('Sticky', undefined, 0);
            vi.advanceTimersByTime(60_000);
            expect(service.getToasts()).toHaveLength(1);
        });
    });
});
