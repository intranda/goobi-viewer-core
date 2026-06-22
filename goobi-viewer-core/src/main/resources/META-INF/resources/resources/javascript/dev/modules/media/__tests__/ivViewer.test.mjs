/**
 * Unit tests for the pure, side-effect-free exports of ivViewer.mjs.
 *
 * IvViewer (the default export) depends on the global `ImageView` object that
 * is only present in a real browser context — do NOT import or instantiate it
 * here. Only `resolveNavigation` and `Emitter` are tested.
 *
 * Note: Jest's `jest` global is not auto-injected in native ESM projects
 * (transform: {}). We import it explicitly from @jest/globals.
 */
import { jest } from '@jest/globals';
import { resolveNavigation, Emitter } from '../ivViewer.mjs';

// ---------------------------------------------------------------------------
// resolveNavigation
// ---------------------------------------------------------------------------

describe('resolveNavigation', function () {
    test('target inside current window returns reload:false and correct indexInWindow', function () {
        const result = resolveNavigation(3, { start: 1, end: 6 }, 7, 5);
        expect(result).toEqual({ reload: false, indexInWindow: 2 });
    });

    test('target outside window returns reload:true with new window and correct indexInWindow', function () {
        const result = resolveNavigation(6, { start: 0, end: 5 }, 7, 5);
        expect(result).toEqual({ reload: true, window: { start: 2, end: 7 }, indexInWindow: 4 });
    });

    test('out-of-range target is clamped to a finite non-negative indexInWindow', function () {
        const result = resolveNavigation(99, { start: 0, end: 5 }, 7, 5);
        expect(result.reload).toBe(true);
        expect(Number.isFinite(result.indexInWindow)).toBe(true);
        expect(result.indexInWindow).toBeGreaterThanOrEqual(0);
    });
});

// ---------------------------------------------------------------------------
// Emitter
// ---------------------------------------------------------------------------

describe('Emitter', function () {
    test('subscriber receives the emitted value', function () {
        const emitter = new Emitter();
        const mock = jest.fn();
        emitter.subscribe(mock);
        emitter.emit(42);
        expect(mock).toHaveBeenCalledWith(42);
    });

    test('multiple subscribers all receive the emitted value', function () {
        const emitter = new Emitter();
        const mockA = jest.fn();
        const mockB = jest.fn();
        emitter.subscribe(mockA);
        emitter.subscribe(mockB);
        emitter.emit('hello');
        expect(mockA).toHaveBeenCalledWith('hello');
        expect(mockB).toHaveBeenCalledWith('hello');
    });

    test('unsubscribe function stops further notifications', function () {
        const emitter = new Emitter();
        const mock = jest.fn();
        const unsubscribe = emitter.subscribe(mock);
        unsubscribe();
        emitter.emit(99);
        expect(mock).not.toHaveBeenCalled();
    });
});
