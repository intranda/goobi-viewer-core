/**
 * Unit tests for ivViewer.mjs: the pure `Emitter` export and IvViewer's navigation,
 * overlay and preload logic.
 *
 * IvViewer depends on the global `ImageView` object (only present in a real browser).
 * The tests stand up a minimal `ImageView` mock and stub the async `_open`, so they
 * exercise the serialisation/queue/watchdog logic without a real OpenSeadragon.
 *
 * Note: Jest's `jest` global is not auto-injected in native ESM projects
 * (transform: {}). We import it explicitly from @jest/globals.
 */
import { jest } from '@jest/globals';
import IvViewer, { Emitter } from '../ivViewer.mjs';
import { computeSpread } from '../ivImageWindow.mjs';

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

/** Flush pending micro- and macro-tasks. */
const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

/** Minimal ImageView global so IvViewer can be constructed without a real OpenSeadragon. */
function mockImageView() {
    global.ImageView = {
        Image: class {
            constructor(opts) {
                this.element = opts.element;
                // Keep the sequence object passed by IvViewer so shared-state bugs are visible.
                this.config = { sequence: opts.sequence || { columns: 1 } };
                const handlers = {};
                this.openseadragon = {
                    _handlers: handlers,
                    addHandler(name, fn) {
                        handlers[name] = fn;
                    },
                    removeHandler() {},
                    addTiledImage: jest.fn(),
                    addOverlay: jest.fn(),
                    removeOverlay: jest.fn(),
                    world: { getItemAt: () => null, removeItem: jest.fn(), getItemCount: () => 0 },
                    viewport: { goHome() {} },
                    drawer: { canvas: null }, // -> _snapshotOverlay() returns null (no DOM needed)
                };
            }
            load() {
                return Promise.resolve();
            }
            getCanvasContext() {
                return null;
            }
        },
        Controls: {
            Zoom: class {
                zoomBy() {}
                goHome() {}
            },
            Rotation: class {
                rotateLeft() {}
                rotateRight() {}
                rotateTo() {}
            },
        },
    };
}

const services = (n) => Array.from({ length: n }, (_, i) => `https://example.test/img${i}`);

describe('IvViewer construction', () => {
    let opens;

    beforeEach(() => {
        mockImageView();
        opens = [];
        jest.spyOn(IvViewer.prototype, '_open').mockImplementation(function () {
            return new Promise((resolve) => opens.push(resolve));
        });
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    test('clamps startOrder into range and emits onLoaded once the initial open settles', async () => {
        const v = new IvViewer({ element: {}, services: services(5), startOrder: 99 });
        expect(v.current).toBe(4);
        const loaded = jest.fn();
        v.onLoaded.subscribe(loaded);
        opens.shift()();
        await flush();
        expect(loaded).toHaveBeenCalledWith(4);

        const w = new IvViewer({ element: {}, services: services(5), startOrder: -2 });
        expect(w.current).toBe(0);
    });

    test('each instance gets its own sequence config (no shared module state)', () => {
        const a = new IvViewer({ element: {}, services: services(3) });
        const b = new IvViewer({ element: {}, services: services(3) });
        a.viewer.config.sequence.columns = 2;
        expect(b.viewer.config.sequence.columns).toBe(1);
    });

    test('loads tiles with CORS so the canvas stays origin-clean for the image filters', () => {
        const v = new IvViewer({ element: {}, services: services(3) });
        expect(v.viewer.openseadragon.crossOriginPolicy).toBe('Anonymous');
    });

    test('arrow keys page instead of panning; other keys keep OSD handling', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.next = jest.fn();
        v.prev = jest.fn();
        const handler = v.viewer.openseadragon._handlers['canvas-key'];

        const right = { originalEvent: { key: 'ArrowRight', preventDefault: jest.fn() } };
        handler(right);
        expect(v.next).toHaveBeenCalledTimes(1);
        expect(right.preventDefaultAction).toBe(true);
        expect(right.originalEvent.preventDefault).toHaveBeenCalled();

        const left = { originalEvent: { key: 'ArrowLeft', preventDefault: jest.fn() } };
        handler(left);
        expect(v.prev).toHaveBeenCalledTimes(1);

        const other = { originalEvent: { key: 'a', preventDefault: jest.fn() } };
        handler(other);
        expect(v.next).toHaveBeenCalledTimes(1);
        expect(v.prev).toHaveBeenCalledTimes(1);
        expect(other.preventDefaultAction).toBeUndefined();
    });
});

describe('IvViewer page navigation (clamping, no-ops, next/prev)', () => {
    let opens;

    beforeEach(() => {
        mockImageView();
        opens = [];
        jest.spyOn(IvViewer.prototype, '_open').mockImplementation(function () {
            return new Promise((resolve) => opens.push(resolve));
        });
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_prewarmSpreads').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_crossfadeTo').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_navigateSpread').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    async function newViewer(total = 10, startOrder = 0) {
        const v = new IvViewer({ element: {}, services: services(total), startOrder });
        opens.shift()?.();
        await flush();
        v._open.mockClear();
        return v;
    }

    test('goToPage clamps to the valid range', async () => {
        const v = await newViewer(10);
        v.goToPage(-5); // clamps to 0 === current -> no-op
        expect(v._crossfadeTo).not.toHaveBeenCalled();
        v.goToPage(999); // clamps to the last page
        expect(v._crossfadeTo).toHaveBeenCalledWith(9);
    });

    test('goToPage is a no-op on the current page', async () => {
        const v = await newViewer(10, 3);
        v.goToPage(3);
        expect(v._crossfadeTo).not.toHaveBeenCalled();
    });

    test('prev at the first page is a no-op; next pages forward', async () => {
        const v = await newViewer(10);
        v.prev();
        expect(v._crossfadeTo).not.toHaveBeenCalled();
        v.next();
        expect(v._crossfadeTo).toHaveBeenCalledWith(1);
    });

    test('double mode: goToPage snaps to the spread leader and skips the current spread', async () => {
        const v = await newViewer(20, 1);
        v.double = true;
        v.goToPage(2); // same spread [1,2] -> no-op
        expect(v._navigateSpread).not.toHaveBeenCalled();
        v.goToPage(4); // spread [3,4] -> leader 3
        expect(v._navigateSpread).toHaveBeenCalledWith(3);
    });

    test('double mode: next advances past the trailing page of the spread', async () => {
        const v = await newViewer(20, 1);
        v.double = true;
        v.next(); // spread [1,2] -> goToPage(3) -> leader 3
        expect(v._navigateSpread).toHaveBeenCalledWith(3);
    });

    test('getCurrentPages returns [current] in single and the spread in double mode', async () => {
        const v = await newViewer(20, 2);
        expect(v.getCurrentPages()).toEqual([2]);
        v.double = true;
        expect(v.getCurrentPages()).toEqual([1, 2]);
    });

    test('toggling to double snaps current to the spread leader and returns the new state', async () => {
        const v = await newViewer(20, 4);
        expect(v.toggleDoublePage()).toBe(true);
        expect(v.current).toBe(computeSpread(4, 20)[0]); // 3
        expect(v._open).toHaveBeenCalledWith(3);
        opens.shift()?.();
        await flush();
        expect(v.toggleDoublePage()).toBe(false);
        expect(v.current).toBe(3);
    });
});

describe('IvViewer navigation lock (double-page hang regression)', () => {
    let opens; // pending resolvers for the stubbed _open, one per call

    beforeEach(() => {
        mockImageView();
        opens = [];
        // _open is the async boundary that hangs under rapid double-page reopens; control it.
        jest.spyOn(IvViewer.prototype, '_open').mockImplementation(function () {
            return new Promise((resolve) => opens.push(resolve));
        });
        // Isolate the lock logic from OSD-only side effects.
        jest.spyOn(IvViewer.prototype, '_prewarmSpreads').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    async function newDoublePageViewer(total = 20, opts = {}) {
        const v = new IvViewer({ element: {}, services: services(total), startOrder: 0, ...opts });
        opens.shift()?.(); // settle the constructor's initial _open
        await flush();
        v.double = true;
        v._open.mockClear();
        opens.length = 0;
        return v;
    }

    test('rapid TOC clicks do not run concurrent opens; the latest target wins', async () => {
        const v = await newDoublePageViewer(20);

        v.goToPage(4); // starts a spread navigation
        expect(v._open).toHaveBeenCalledTimes(1);

        v.goToPage(8); // busy -> queued
        v.goToPage(12); // busy -> overwrites 8 (latest wins)
        expect(v._open).toHaveBeenCalledTimes(1); // still no concurrent open

        opens.shift()(); // finish the first navigation
        await flush();

        expect(v._open).toHaveBeenCalledTimes(2); // exactly one follow-up, not two
        const expectedLeader = computeSpread(12, 20)[0];
        expect(v._open).toHaveBeenLastCalledWith(expectedLeader);
        expect(v.current).toBe(expectedLeader);
    });

    test('a spread load that never settles is released by the watchdog (no permanent freeze)', async () => {
        const v = await newDoublePageViewer(20, { navWatchdogMs: 20 });

        v.goToPage(4); // _open for this navigation never resolves
        expect(v._navigating).toBe(true);

        await new Promise((resolve) => setTimeout(resolve, 40)); // past the watchdog
        await flush();

        expect(v._navigating).toBe(false); // recovered instead of frozen
        v._open.mockClear();
        v.goToPage(6); // navigation works again
        expect(v._open).toHaveBeenCalledTimes(1);
    });

    test('toggling double-page while navigating defers the reopen (no concurrent open)', async () => {
        const v = await newDoublePageViewer(20);

        v.goToPage(4); // in-flight navigation
        expect(v._open).toHaveBeenCalledTimes(1);

        v.toggleDoublePage(); // must not open concurrently
        expect(v._open).toHaveBeenCalledTimes(1);
        expect(v._queuedReopen).toBe(true);

        opens.shift()(); // finish the navigation -> deferred reopen runs
        await flush();

        expect(v._open).toHaveBeenCalledTimes(2);
    });

    test('a queued mode reopen runs before the queued page target', async () => {
        const crossfade = jest.spyOn(IvViewer.prototype, '_crossfadeTo').mockImplementation(() => {});
        const v = await newDoublePageViewer(20);

        v.goToPage(4); // in-flight (_open for leader 3)
        v.goToPage(8); // queued page target
        v.toggleDoublePage(); // queued reopen (now single mode)
        expect(v._open).toHaveBeenCalledTimes(1);

        opens.shift()(); // finish the navigation -> the reopen must run first
        await flush();
        expect(v._open).toHaveBeenCalledTimes(2); // the reopen
        expect(v._queuedNav).toBe(8); // the page target survived the reopen

        opens.shift()(); // finish the reopen -> the queued target runs (single mode now)
        await flush();
        expect(crossfade).toHaveBeenCalledWith(8);
    });
});

describe('IvViewer overlays (search highlights + text regions)', () => {
    let opens;
    const fakeItem = () => ({ imageToViewportRectangle: (x, y, w, h) => ({ x, y, w, h }) });

    beforeEach(() => {
        mockImageView();
        opens = [];
        jest.spyOn(IvViewer.prototype, '_open').mockImplementation(function () {
            return new Promise((resolve) => opens.push(resolve));
        });
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    test('setHighlights draws one overlay per rect and marks the active hit', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.currentItem = fakeItem();
        const osd = v.viewer.openseadragon;

        v.setHighlights([
            { x: 1, y: 2, w: 3, h: 4 },
            { x: 5, y: 6, w: 7, h: 8, active: true },
        ]);

        expect(osd.addOverlay).toHaveBeenCalledTimes(2);
        const [first, second] = osd.addOverlay.mock.calls.map((c) => c[0]);
        expect(first.element.className).toBe('immersive__highlight');
        expect(second.element.className).toBe('immersive__highlight immersive__highlight--active');
        expect(first.location).toEqual({ x: 1, y: 2, w: 3, h: 4 });

        v.clearHighlights();
        expect(osd.removeOverlay).toHaveBeenCalledTimes(2);
    });

    test('setTextRegions returns a map of id-tagged overlays and skips rect-less regions', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.currentItem = fakeItem();
        const osd = v.viewer.openseadragon;

        const map = v.setTextRegions([
            { id: 'a', rect: { x: 1, y: 2, w: 3, h: 4 } },
            { id: 'b', rect: null },
        ]);

        expect(map.size).toBe(1);
        expect(map.get('a').dataset.ivRegionId).toBe('a');
        expect(map.get('a').className).toBe('immersive__text-region');
        expect(osd.addOverlay).toHaveBeenCalledTimes(1);

        v.clearTextRegions();
        expect(osd.removeOverlay).toHaveBeenCalledWith(map.get('a'));
    });

    test('without a current item nothing is drawn', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.currentItem = null; // world.getItemAt also returns null
        const osd = v.viewer.openseadragon;

        expect(v.setTextRegions([{ id: 'a', rect: { x: 1, y: 2, w: 3, h: 4 } }]).size).toBe(0);
        v.setHighlights([{ x: 1, y: 2, w: 3, h: 4 }]);
        expect(osd.addOverlay).not.toHaveBeenCalled();
    });
});

describe('IvViewer preloading', () => {
    let opens;

    beforeEach(() => {
        mockImageView();
        opens = [];
        jest.spyOn(IvViewer.prototype, '_open').mockImplementation(function () {
            return new Promise((resolve) => opens.push(resolve));
        });
        jest.spyOn(IvViewer.prototype, '_prewarmSpreads').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    test('_acquire caches the in-flight promise per order and builds info.json tile sources', () => {
        const list = [...services(5), 'https://example.test/img5/info.json'];
        const v = new IvViewer({ element: {}, services: list });
        const osd = v.viewer.openseadragon;
        osd.addTiledImage.mockClear();

        const p1 = v._acquire(3);
        const p2 = v._acquire(3);
        expect(p1).toBe(p2);
        expect(osd.addTiledImage).toHaveBeenCalledTimes(1);
        expect(osd.addTiledImage.mock.calls[0][0].tileSource).toBe('https://example.test/img3/info.json');

        v._acquire(5); // service id already ends in /info.json -> not suffixed twice
        expect(osd.addTiledImage.mock.calls[1][0].tileSource).toBe('https://example.test/img5/info.json');
    });

    test('_refreshPreload keeps the resident pages and evicts the rest', async () => {
        const v = new IvViewer({ element: {}, services: services(12), startOrder: 5 });
        const osd = v.viewer.openseadragon;
        const itemA = {};
        const itemB = {};
        v._preloaded.set(0, Promise.resolve(itemA));
        v._preloaded.set(9, Promise.resolve(itemB));

        v._refreshPreload();
        await flush();

        // resident around page 5 is {4,5,6}: 4 and 6 get acquired, 0 and 9 get evicted
        expect([...v._preloaded.keys()].sort((a, b) => a - b)).toEqual([4, 6]);
        expect(osd.world.removeItem).toHaveBeenCalledWith(itemA);
        expect(osd.world.removeItem).toHaveBeenCalledWith(itemB);
    });
});
