/**
 * Unit tests for viewerImmersive.mjs — pageUrlPath and pickActiveTocPageNo (pure)
 * and attachUrlSync (DOM-bound).
 *
 * Note: Jest's `jest` global is not auto-injected in native ESM projects
 * (transform: {}). We import it explicitly from @jest/globals.
 */
import { jest } from '@jest/globals';
import { pageUrlPath, pickActiveTocPageNo, attachUrlSync } from '../viewerImmersive.mjs';

// ---------------------------------------------------------------------------
// pageUrlPath
// ---------------------------------------------------------------------------

describe('pageUrlPath', function () {
    test('replaces existing page-number segment in URL', function () {
        expect(pageUrlPath('/viewer/immersive/PPN123/4/', 'PPN123', 7)).toBe('/viewer/immersive/PPN123/7/');
    });

    test('appends page number when no numeric segment exists', function () {
        expect(pageUrlPath('/viewer/immersive/PPN123/', 'PPN123', 3)).toBe('/viewer/immersive/PPN123/3/');
    });

    test('does not mistake a numeric PI for the page-number segment', function () {
        expect(pageUrlPath('/viewer/immersive/15920242/', '15920242', 5)).toBe('/viewer/immersive/15920242/5/');
    });
});

// ---------------------------------------------------------------------------
// pickActiveTocPageNo
// ---------------------------------------------------------------------------

describe('pickActiveTocPageNo', function () {
    test('keeps the active section while any visible page is in its range', function () {
        expect(pickActiveTocPageNo([1, 5, 9], [5, 6], 5)).toBe(5);
        // double-page spread: the left page belongs to the previous section, the
        // right page still falls in the active one -> keep it
        expect(pickActiveTocPageNo([1, 5, 9], [4, 5], 5)).toBe(5);
    });

    test('a section starting on the right page of a spread wins when none is active', function () {
        expect(pickActiveTocPageNo([1, 6], [5, 6])).toBe(6);
    });

    test('the next section takes over once the active one leaves the visible range', function () {
        expect(pickActiveTocPageNo([1, 6], [6, 7], 1)).toBe(6);
    });

    test('without an active section the owner of the last visible page wins', function () {
        expect(pickActiveTocPageNo([1, 5, 9], [7])).toBe(5);
        expect(pickActiveTocPageNo([1, 5, 9], [9, 10])).toBe(9);
    });

    test('returns null when no section starts at or before the visible pages', function () {
        expect(pickActiveTocPageNo([10], [2])).toBeNull();
    });

    test('the last section owns all pages to the end', function () {
        expect(pickActiveTocPageNo([1], [40], 1)).toBe(1);
    });

    test('empty entries or pages leave the active section untouched', function () {
        expect(pickActiveTocPageNo([], [5], 3)).toBe(3);
        expect(pickActiveTocPageNo([1, 5], [], 5)).toBe(5);
    });
});

// ---------------------------------------------------------------------------
// attachUrlSync
// ---------------------------------------------------------------------------

class FakeEmitter {
    constructor() {
        this._subscribers = [];
    }
    subscribe(fn) {
        this._subscribers.push(fn);
    }
    emit(v) {
        this._subscribers.forEach((fn) => fn(v));
    }
}

describe('attachUrlSync', function () {
    test('page-change pushes history entry with order+1 in pathname', function () {
        window.history.replaceState({}, '', '/viewer/immersive/PPN123/1/');
        const viewer = { onPageChange: new FakeEmitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        viewer.onPageChange.emit(2);
        expect(window.location.pathname).toBe('/viewer/immersive/PPN123/3/');
    });

    test('popstate with state.order calls viewer.goToPage', function () {
        const viewer = { onPageChange: new FakeEmitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        window.dispatchEvent(new PopStateEvent('popstate', { state: { order: 4 } }));
        expect(viewer.goToPage).toHaveBeenCalledWith(4);
    });

    test('popstate with null state does not call viewer.goToPage', function () {
        const viewer = { onPageChange: new FakeEmitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        window.dispatchEvent(new PopStateEvent('popstate', { state: null }));
        expect(viewer.goToPage).not.toHaveBeenCalled();
    });
});
