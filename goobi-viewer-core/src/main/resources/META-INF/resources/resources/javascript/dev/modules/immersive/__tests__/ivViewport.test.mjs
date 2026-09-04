/** Relative view maths: keeping zoom and centre across a reload, and the cases that must not be kept. */
import { captureView, restoredView } from '../engine/ivTransitions.mjs';

const row = (x, y, width, height) => ({ x, y, width, height });

describe('captureView', () => {
    test('expresses zoom per row width and the centre as a fraction of the row', () => {
        expect(captureView(row(0, 0, 1, 0.5), { x: 0.25, y: 0.125 }, 4, 2)).toEqual({ zoom: 4, x: 0.25, y: 0.25, pages: 2 });
    });

    test('a degenerate row yields nothing to restore', () => {
        expect(captureView(null, { x: 0, y: 0 }, 1, 1)).toBeNull();
        expect(captureView(row(0, 0, 0, 1), { x: 0, y: 0 }, 1, 1)).toBeNull();
        expect(captureView(row(0, 0, 1, 0), { x: 0, y: 0 }, 1, 1)).toBeNull();
        expect(captureView(row(0, 0, 1, 1), null, 1, 1)).toBeNull();
        expect(captureView(row(0, 0, 1, 1), { x: 0, y: 0 }, 0, 1)).toBeNull();
    });
});

describe('restoredView', () => {
    test('re-applies zoom and centre to a row of the same shape', () => {
        const view = captureView(row(0, 0, 1, 0.5), { x: 0.25, y: 0.125 }, 4, 2);
        expect(restoredView(view, row(2, 1, 1, 0.5), 2)).toEqual({ zoom: 4, center: { x: 2.25, y: 1.125 } });
    });

    test('keeps the magnification per page when the row is scaled', () => {
        const view = captureView(row(0, 0, 1, 0.5), { x: 0.5, y: 0.25 }, 4, 2);
        // half as wide a row means twice the zoom for the same magnification
        expect(restoredView(view, row(0, 0, 0.5, 0.25), 2)).toEqual({ zoom: 8, center: { x: 0.25, y: 0.125 } });
    });

    test('a row of a different page count is left to the library', () => {
        const view = captureView(row(0, 0, 1, 0.5), { x: 0.5, y: 0.25 }, 4, 2);
        expect(restoredView(view, row(0, 0, 0.5, 0.5), 1)).toBeNull();
    });

    test('nothing to restore, or nothing to restore onto', () => {
        const view = captureView(row(0, 0, 1, 0.5), { x: 0.5, y: 0.25 }, 4, 2);
        expect(restoredView(null, row(0, 0, 1, 0.5), 2)).toBeNull();
        expect(restoredView(view, null, 2)).toBeNull();
        expect(restoredView(view, row(0, 0, 0, 0.5), 2)).toBeNull();
    });
});
