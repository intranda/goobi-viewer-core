/** Unit tests for the navigation lock: serialisation, release hooks, watchdog and teardown. */
import { jest } from '@jest/globals';
import { createNavLock } from '../engine/ivNavLock.mjs';

const flush = () => new Promise((r) => setTimeout(r, 0));

test('busy while the task runs, released afterwards with the hooks called once', async () => {
    const lock = createNavLock(1000);
    const hook = jest.fn();
    lock.onRelease(hook);
    let finish;
    lock.run(() => new Promise((res) => (finish = res)));
    expect(lock.busy).toBe(true);
    finish();
    await flush();
    expect(lock.busy).toBe(false);
    expect(hook).toHaveBeenCalledTimes(1);
});

test('a rejected or throwing task still releases', async () => {
    const lock = createNavLock(1000);
    lock.run(() => Promise.reject(new Error('x')));
    await flush();
    expect(lock.busy).toBe(false);
    lock.run(() => {
        throw new Error('sync');
    });
    expect(lock.busy).toBe(false);
});

test('the watchdog releases a task that never settles and runs onStuck; destroy clears it', () => {
    jest.useFakeTimers();
    try {
        const lock = createNavLock(500);
        const stuck = jest.fn();
        lock.run(() => new Promise(() => {}), stuck);
        jest.advanceTimersByTime(499);
        expect(lock.busy).toBe(true);
        jest.advanceTimersByTime(1);
        expect(stuck).toHaveBeenCalledTimes(1);
        expect(lock.busy).toBe(false);
        lock.run(() => new Promise(() => {}));
        lock.destroy();
        expect(jest.getTimerCount()).toBe(0);
    } finally {
        jest.useRealTimers();
    }
});
