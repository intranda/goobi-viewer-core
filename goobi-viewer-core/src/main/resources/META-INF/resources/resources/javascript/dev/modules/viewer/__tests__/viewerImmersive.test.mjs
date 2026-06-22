/**
 * Unit tests for viewerImmersive.mjs — pageUrlPath (pure) and attachUrlSync (DOM-bound).
 *
 * Note: Jest's `jest` global is not auto-injected in native ESM projects
 * (transform: {}). We import it explicitly from @jest/globals.
 */
import { jest } from '@jest/globals';
import { pageUrlPath, attachUrlSync } from '../viewerImmersive.mjs';

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
