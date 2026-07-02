/**
 * Unit tests for ivImageWindow.mjs
 *
 * Tests the pure page/spread window math: computeSpread, framePages and residentPages.
 */
import { computeSpread, framePages, residentPages } from '../ivImageWindow.mjs';

// ---------------------------------------------------------------------------
// computeSpread — book-layout page pairing
// ---------------------------------------------------------------------------

describe('computeSpread (book layout, cover alone)', () => {
    test('cover page stands alone', () => {
        expect(computeSpread(0, 40)).toEqual([0]);
    });
    test('pairs after the cover: (1,2),(3,4)', () => {
        expect(computeSpread(1, 40)).toEqual([1, 2]);
        expect(computeSpread(2, 40)).toEqual([1, 2]);
        expect(computeSpread(3, 40)).toEqual([3, 4]);
        expect(computeSpread(4, 40)).toEqual([3, 4]);
    });
    test('odd final page stands alone', () => {
        // pages 0..3 -> [0],[1,2],[3]
        expect(computeSpread(3, 4)).toEqual([3]);
    });
    test('even final page is paired', () => {
        // pages 0..4 -> [0],[1,2],[3,4]
        expect(computeSpread(4, 5)).toEqual([3, 4]);
    });
    test('clamps out-of-range order', () => {
        expect(computeSpread(99, 40)).toEqual([39]);
        expect(computeSpread(-3, 40)).toEqual([0]);
    });
    test('coverAlone:false pairs from the start', () => {
        expect(computeSpread(0, 40, { coverAlone: false })).toEqual([0, 1]);
        expect(computeSpread(2, 40, { coverAlone: false })).toEqual([2, 3]);
    });
    test('coverAlone:false: odd final page stands alone', () => {
        // pages 0..4 -> [0,1],[2,3],[4]
        expect(computeSpread(4, 5, { coverAlone: false })).toEqual([4]);
    });
});

// ---------------------------------------------------------------------------
// framePages — frame containing a given order
// ---------------------------------------------------------------------------

describe('framePages', () => {
    test('single mode: just the page', () => {
        expect(framePages(5, 40)).toEqual([5]);
        expect(framePages(5, 40, { double: false })).toEqual([5]);
    });
    test('double mode: the spread', () => {
        expect(framePages(0, 40, { double: true })).toEqual([0]); // cover alone
        expect(framePages(2, 40, { double: true })).toEqual([1, 2]);
    });
    test('clamps out of range', () => {
        expect(framePages(99, 40)).toEqual([39]);
    });
});

// ---------------------------------------------------------------------------
// residentPages — current frame + adjacent frames
// ---------------------------------------------------------------------------

describe('residentPages (current frame + adjacent frames)', () => {
    test('single mode keeps prev/current/next', () => {
        expect(residentPages(5, 40)).toEqual([4, 5, 6]);
    });
    test('single mode at start: no prev', () => {
        expect(residentPages(0, 40)).toEqual([0, 1]);
    });
    test('single mode at end: no next', () => {
        expect(residentPages(39, 40)).toEqual([38, 39]);
    });
    test('double mode keeps adjacent spreads', () => {
        // frames: [0],[1,2],[3,4]; at order 2 -> here[1,2], prev[0], next[3,4]
        expect(residentPages(2, 40, { double: true })).toEqual([0, 1, 2, 3, 4]);
    });
});
