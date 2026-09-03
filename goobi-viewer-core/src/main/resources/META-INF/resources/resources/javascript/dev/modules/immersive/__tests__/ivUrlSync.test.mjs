/** Unit tests for the page URL sync: pure path rewriting and the DOM-bound attachUrlSync. */
import { jest } from '@jest/globals';
import { pageUrlPath, attachUrlSync } from '../ui/ivUrlSync.mjs';

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

    test('destroy stops the history sync and the popstate handling', function () {
        window.history.replaceState({}, '', '/viewer/immersive/PPN123/1/');
        const subs = new Set();
        const viewer = {
            onPageChange: {
                subscribe: (fn) => {
                    subs.add(fn);
                    return () => subs.delete(fn);
                },
            },
            goToPage: jest.fn(),
        };
        const sync = attachUrlSync(viewer, 'PPN123');
        sync.destroy();
        subs.forEach((fn) => fn(2));
        expect(window.location.pathname).toBe('/viewer/immersive/PPN123/1/');
        window.dispatchEvent(new PopStateEvent('popstate', { state: { order: 4 } }));
        expect(viewer.goToPage).not.toHaveBeenCalled();
    });
});

describe('attachUrlSync history hygiene', function () {
    const emitter = () => {
        const subs = new Set();
        return { subscribe: (fn) => (subs.add(fn), () => subs.delete(fn)), emit: (v) => subs.forEach((fn) => fn(v)) };
    };

    test('an emit for the page already in the URL replaces the entry instead of pushing a duplicate', function () {
        window.history.replaceState(null, '', '/viewer/immersive/PPN123/1/');
        const length = window.history.length;
        const viewer = { onPageChange: emitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        viewer.onPageChange.emit(0);
        expect(window.location.pathname).toBe('/viewer/immersive/PPN123/1/');
        expect(window.history.length).toBe(length);
        expect(window.history.state).toEqual({ order: 0 });
        viewer.onPageChange.emit(4);
        expect(window.location.pathname).toBe('/viewer/immersive/PPN123/5/');
        expect(window.history.length).toBe(length + 1);
    });

    test('a popstate-driven page change does not push (the browser already restored the URL)', function () {
        window.history.replaceState({ order: 2 }, '', '/viewer/immersive/PPN123/3/');
        const length = window.history.length;
        const viewer = { onPageChange: emitter(), goToPage: jest.fn() };
        attachUrlSync(viewer, 'PPN123');
        window.dispatchEvent(new PopStateEvent('popstate', { state: { order: 2 } }));
        expect(viewer.goToPage).toHaveBeenCalledWith(2);
        viewer.onPageChange.emit(2);
        expect(window.location.pathname).toBe('/viewer/immersive/PPN123/3/');
        expect(window.history.length).toBe(length);
    });
});
