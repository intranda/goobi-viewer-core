/**
 * Unit tests for the pure, side-effect-free exports of ivViewer.mjs.
 *
 * IvViewer (the default export) depends on the global `ImageView` object that
 * is only present in a real browser context — do NOT import or instantiate it
 * here. Only `Emitter` is tested.
 *
 * Note: Jest's `jest` global is not auto-injected in native ESM projects
 * (transform: {}). We import it explicitly from @jest/globals.
 */
import { jest } from '@jest/globals';
import { Emitter } from '../ivViewer.mjs';

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
