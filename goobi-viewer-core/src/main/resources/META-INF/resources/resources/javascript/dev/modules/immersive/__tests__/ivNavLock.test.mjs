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
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    const lock = createNavLock(1000);
    lock.run(() => Promise.reject(new Error('x')));
    await flush();
    expect(lock.busy).toBe(false);
    lock.run(() => {
        throw new Error('sync');
    });
    expect(lock.busy).toBe(false);
});

test('a second run while the lock is busy is refused, and the running task keeps its watchdog', () => {
    jest.useFakeTimers();
    try {
        const lock = createNavLock(500);
        const first = jest.fn(() => new Promise(() => {}));
        const second = jest.fn();
        const stuck = jest.fn();
        expect(lock.run(first, stuck)).toBe(true);
        expect(lock.run(second)).toBe(false);
        expect(second).not.toHaveBeenCalled();
        expect(lock.busy).toBe(true);
        jest.advanceTimersByTime(500);
        expect(stuck).toHaveBeenCalledTimes(1);
        expect(lock.busy).toBe(false);
    } finally {
        jest.useRealTimers();
    }
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

test('destroy drops the release hooks and refuses new runs, so a task settling late starts nothing', async () => {
    const lock = createNavLock(1000);
    const hook = jest.fn();
    lock.onRelease(hook);
    let finish;
    lock.run(() => new Promise((res) => (finish = res)));
    lock.destroy();
    expect(lock.run(() => Promise.resolve())).toBe(false);
    finish();
    await flush();
    expect(hook).not.toHaveBeenCalled();
    expect(lock.busy).toBe(false);
    expect(lock.run(() => Promise.resolve())).toBe(false);
});

test('a throwing release hook is logged and does not block the others', async () => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => {});
    try {
        const lock = createNavLock(1000);
        const second = jest.fn();
        lock.onRelease(() => {
            throw new Error('hook');
        });
        lock.onRelease(second);
        lock.run(() => Promise.resolve());
        await flush();
        expect(second).toHaveBeenCalledTimes(1);
        expect(error).toHaveBeenCalledTimes(1);
        expect(lock.busy).toBe(false);
    } finally {
        error.mockRestore();
    }
});

test('a rejected task is logged instead of swallowed', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    try {
        const lock = createNavLock(1000);
        lock.run(() => Promise.reject(new Error('x')));
        await flush();
        expect(warn).toHaveBeenCalledWith(expect.any(String), expect.any(Error));
    } finally {
        warn.mockRestore();
    }
});
