/** Unit tests for the persisted preferences: tolerant storage round-trips. */
import { normalizeThumbSizeStep, readThumbSizeStep, writeThumbSizeStep, THUMB_SIZE_KEY, ANNOTATIONS_KEY, readAnnotationsEnabled, writeAnnotationsEnabled } from '../ivPrefs.mjs';

describe('normalizeThumbSizeStep', function () {
    test('accepts the valid steps 0, 1 and 2 (number or numeric string)', function () {
        expect(normalizeThumbSizeStep('0')).toBe(0);
        expect(normalizeThumbSizeStep('1')).toBe(1);
        expect(normalizeThumbSizeStep('2')).toBe(2);
        expect(normalizeThumbSizeStep(0)).toBe(0);
        expect(normalizeThumbSizeStep(2)).toBe(2);
    });

    test('accepts leading-numeric strings (parseInt semantics)', function () {
        expect(normalizeThumbSizeStep('2abc')).toBe(2);
        expect(normalizeThumbSizeStep('1.5')).toBe(1);
    });

    test('falls back to the default step (1) for invalid values', function () {
        expect(normalizeThumbSizeStep(null)).toBe(1);
        expect(normalizeThumbSizeStep(undefined)).toBe(1);
        expect(normalizeThumbSizeStep('')).toBe(1);
        expect(normalizeThumbSizeStep('abc')).toBe(1);
        expect(normalizeThumbSizeStep('5')).toBe(1);
        expect(normalizeThumbSizeStep(-1)).toBe(1);
    });
});

describe('readThumbSizeStep / writeThumbSizeStep', function () {
    function fakeStorage() {
        const store = {};
        return {
            getItem: (k) => (k in store ? store[k] : null),
            setItem: (k, v) => {
                store[k] = String(v);
            },
        };
    }

    test('round-trips a written step', function () {
        const storage = fakeStorage();
        writeThumbSizeStep(storage, 2);
        expect(readThumbSizeStep(storage)).toBe(2);
    });

    test('returns the default step for an empty storage', function () {
        expect(readThumbSizeStep(fakeStorage())).toBe(1);
    });

    test('returns the default step for an invalid stored value', function () {
        const storage = fakeStorage();
        storage.setItem(THUMB_SIZE_KEY, 'garbage');
        expect(readThumbSizeStep(storage)).toBe(1);
        storage.setItem(THUMB_SIZE_KEY, '7');
        expect(readThumbSizeStep(storage)).toBe(1);
    });

    test('normalizes invalid values when writing', function () {
        const storage = fakeStorage();
        writeThumbSizeStep(storage, 7);
        expect(storage.getItem(THUMB_SIZE_KEY)).toBe('1');
    });

    test('swallows storage errors (private mode)', function () {
        const throwing = {
            getItem: () => {
                throw new Error('denied');
            },
            setItem: () => {
                throw new Error('denied');
            },
        };
        expect(readThumbSizeStep(throwing)).toBe(1);
        expect(() => writeThumbSizeStep(throwing, 2)).not.toThrow();
    });
});

describe('readAnnotationsEnabled / writeAnnotationsEnabled', () => {
    test('reads only the literal "true" as enabled, default off', () => {
        const store = new Map();
        const storage = { getItem: (k) => (store.has(k) ? store.get(k) : null), setItem: (k, v) => store.set(k, v) };
        expect(readAnnotationsEnabled(storage)).toBe(false);
        writeAnnotationsEnabled(storage, true);
        expect(store.get(ANNOTATIONS_KEY)).toBe('true');
        expect(readAnnotationsEnabled(storage)).toBe(true);
        writeAnnotationsEnabled(storage, false);
        expect(readAnnotationsEnabled(storage)).toBe(false);
    });

    test('falls back to off and swallows errors when storage throws (private mode)', () => {
        const throwing = {
            getItem: () => {
                throw new Error('denied');
            },
            setItem: () => {
                throw new Error('denied');
            },
        };
        expect(readAnnotationsEnabled(throwing)).toBe(false);
        expect(() => writeAnnotationsEnabled(throwing, true)).not.toThrow();
    });
});
