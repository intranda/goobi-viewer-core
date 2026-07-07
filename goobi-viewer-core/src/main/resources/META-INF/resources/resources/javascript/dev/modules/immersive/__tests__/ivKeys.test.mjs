/**
 * Unit tests for the immersive key dispatcher: one document-level keydown
 * listener, handlers consulted in priority order, first consumer wins.
 */
import { jest } from '@jest/globals';
import { createKeyDispatcher } from '../ivKeys.mjs';

describe('createKeyDispatcher', () => {
    test('consults handlers in priority order and stops at the first consumer', () => {
        const keys = createKeyDispatcher();
        const calls = [];
        keys.register(20, (e) => {
            calls.push('grid');
            return e.key === 'g';
        });
        keys.register(10, (e) => {
            calls.push('modal');
            return e.key === 'm';
        });

        expect(keys.handleEvent({ key: 'm' })).toBe(true);
        expect(calls).toEqual(['modal']);

        calls.length = 0;
        expect(keys.handleEvent({ key: 'g' })).toBe(true);
        expect(calls).toEqual(['modal', 'grid']);

        calls.length = 0;
        expect(keys.handleEvent({ key: 'x' })).toBe(false);
        expect(calls).toEqual(['modal', 'grid']);
    });

    test('handlers registered late still slot in by priority (async setups)', () => {
        const keys = createKeyDispatcher();
        const calls = [];
        keys.register(40, () => {
            calls.push('panels');
            return true;
        });
        keys.register(30, () => {
            calls.push('dropdown');
            return true;
        });
        keys.handleEvent({ key: 'Escape' });
        expect(calls).toEqual(['dropdown']);
    });

    test('attach binds a single document keydown listener that runs the dispatch', () => {
        const keys = createKeyDispatcher();
        const seen = jest.fn(() => true);
        keys.register(10, seen);
        keys.attach(document);
        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'a' }));
        expect(seen).toHaveBeenCalledTimes(1);
    });
});
