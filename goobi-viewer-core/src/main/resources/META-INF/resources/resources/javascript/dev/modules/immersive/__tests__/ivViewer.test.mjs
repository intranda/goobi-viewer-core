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
import IvViewer, { Emitter, stageMargins } from '../engine/ivViewer.mjs';
import { computeSpread } from '../engine/ivImageWindow.mjs';

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
                this.config = { sequence: opts.sequence || { columns: 1 } };
                const handlers = {};
                this.openseadragon = {
                    _handlers: handlers,
                    addHandler(name, fn) {
                        handlers[name] = fn;
                    },
                    removeHandler() {},
                    addOnceHandler: jest.fn(),
                    addTiledImage: jest.fn(),
                    addOverlay: jest.fn(),
                    removeOverlay: jest.fn(),
                    world: { getItemAt: () => null, removeItem: jest.fn(), getItemCount: () => 0 },
                    viewport: { goHome() {} },
                    drawer: { canvas: null },
                    zoomPerScroll: 1.2,
                    zoomPerClick: 2,
                    gestureSettingsMouse: { scrollToZoom: true, clickToZoom: true, dblClickToZoom: true, pinchToZoom: true },
                    gestureSettingsTouch: { scrollToZoom: true, clickToZoom: true, dblClickToZoom: true, pinchToZoom: true },
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

    test('clamps startOrder into range', async () => {
        const v = new IvViewer({ element: {}, services: services(5), startOrder: 99 });
        expect(v.current).toBe(4);
        opens.shift()();
        await flush();

        const w = new IvViewer({ element: {}, services: services(5), startOrder: -2 });
        expect(w.current).toBe(0);
    });

    test('destroy tears down OSD and silences the emitters', () => {
        const v = new IvViewer({ element: {}, services: services(3) });
        v.viewer.openseadragon.destroy = jest.fn();
        v.viewer.close = jest.fn();
        const seen = jest.fn();
        v.onPageChange.subscribe(seen);
        v.destroy();
        expect(v.viewer.close).toHaveBeenCalledTimes(1);
        expect(v.viewer.openseadragon.destroy).toHaveBeenCalledTimes(1);
        v.onPageChange.emit(1);
        expect(seen).not.toHaveBeenCalled();
    });

    test('a failed page load keeps the shown page, emits nothing and releases the lock', async () => {
        jest.spyOn(console, 'warn').mockImplementation(() => {});
        const v = new IvViewer({ element: {}, services: services(5), startOrder: 1 });
        opens.shift()();
        await flush();
        jest.spyOn(v, '_acquire').mockRejectedValue(new Error('tile source 404'));
        const seen = jest.fn();
        v.onPageChange.subscribe(seen);
        v.goToPage(3);
        await flush();
        await flush();
        expect(v.getCurrentOrder()).toBe(1);
        expect(seen).not.toHaveBeenCalled();
        expect(v._navigating).toBe(false);
    });

    test('a failed neighbour preload is evicted quietly instead of rejecting unhandled', async () => {
        const v = new IvViewer({ element: {}, services: services(5), startOrder: 1 });
        opens.shift()();
        await flush();
        v._refreshPreload.mockRestore();
        jest.spyOn(v, '_acquire').mockImplementation((order) => {
            const p = Promise.reject(new Error('tiles 403'));
            v._preloaded.set(order, p);
            return p;
        });
        const unhandled = jest.fn();
        process.on('unhandledRejection', unhandled);
        v._refreshPreload();
        await flush();
        await flush();
        process.off('unhandledRejection', unhandled);
        expect(unhandled).not.toHaveBeenCalled();
        expect(v._preloaded.size).toBe(0);
    });

    test('destroy clears a pending navigation watchdog', async () => {
        jest.useFakeTimers();
        try {
            const v = new IvViewer({ element: {}, services: services(5) });
            opens.shift()();
            await Promise.resolve();
            jest.spyOn(v, '_acquire').mockReturnValue(new Promise(() => {}));
            v.goToPage(2);
            expect(jest.getTimerCount()).toBe(1);
            v.destroy();
            expect(jest.getTimerCount()).toBe(0);
        } finally {
            jest.useRealTimers();
        }
    });

    test('onceTileLoaded and redraw wrap the OpenSeadragon instance', () => {
        const v = new IvViewer({ element: {}, services: services(3) });
        const osd = { addOnceHandler: jest.fn(), forceRedraw: jest.fn() };
        v.viewer.openseadragon = osd;
        const tile = jest.fn();
        v.onceTileLoaded(tile);
        expect(osd.addOnceHandler).toHaveBeenCalledWith('tile-loaded', tile);
        v.redraw();
        expect(osd.forceRedraw).toHaveBeenCalledTimes(1);
    });

    test('onFrame follows the library onto a new OpenSeadragon instance without stacking on the old one', () => {
        const v = new IvViewer({ element: {}, services: services(3) });
        const fakeOsd = () => {
            const handlers = {};
            return {
                handlers,
                addHandler: (n, fn) => (handlers[n] = handlers[n] || []).push(fn),
                removeHandler: (n, fn) => (handlers[n] = (handlers[n] || []).filter((h) => h !== fn)),
            };
        };
        const first = fakeOsd();
        v.viewer.openseadragon = first;
        const frame = jest.fn();
        const detach = v.onFrame(frame);
        v.onOpen.emit();
        v.onOpen.emit();
        expect(first.handlers['update-viewport']).toHaveLength(1);
        const second = fakeOsd();
        v.viewer.openseadragon = second;
        v.onOpen.emit();
        expect(second.handlers['update-viewport']).toHaveLength(1);
        detach();
        expect(second.handlers['update-viewport']).toHaveLength(0);
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

    test("'0' resets like the reset button (OSD's raw goHome would FILL the viewport instead of fitting)", () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.resetView = jest.fn();
        const handler = v.viewer.openseadragon._handlers['canvas-key'];

        const zero = { originalEvent: { key: '0', preventDefault: jest.fn() } };
        handler(zero);
        expect(v.resetView).toHaveBeenCalledTimes(1);
        expect(zero.preventDefaultAction).toBe(true);
        expect(zero.originalEvent.preventDefault).toHaveBeenCalled();
    });

    test('resetView emits onReset so UI add-ons (image filters) can reset with it', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        const reset = jest.fn();
        v.onReset.subscribe(reset);
        v.resetView();
        expect(reset).toHaveBeenCalledTimes(1);
    });

    test('allowZoom defaults to true and keeps zoomIn/zoomOut active', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        expect(v.allowZoom).toBe(true);
        const zoomBy = jest.spyOn(v.zoom, 'zoomBy');
        v.zoomIn();
        v.zoomOut();
        expect(zoomBy).toHaveBeenCalledTimes(2);
    });

    test('allowZoom:false makes zoomIn/zoomOut no-ops and disables OSD zoom gestures', () => {
        const v = new IvViewer({ element: {}, services: services(5), allowZoom: false });
        const zoomBy = jest.spyOn(v.zoom, 'zoomBy');
        v.zoomIn();
        v.zoomOut();
        expect(zoomBy).not.toHaveBeenCalled();

        const osd = v.viewer.openseadragon;
        expect(osd.zoomPerScroll).toBe(1);
        expect(osd.zoomPerClick).toBe(1);
        for (const g of [osd.gestureSettingsMouse, osd.gestureSettingsTouch]) {
            expect(g.scrollToZoom).toBe(false);
            expect(g.clickToZoom).toBe(false);
            expect(g.dblClickToZoom).toBe(false);
            expect(g.pinchToZoom).toBe(false);
        }
    });

    test('allowZoom:false blocks OSD keyboard zoom keys but leaves them alone when zoom is allowed', () => {
        const denied = new IvViewer({ element: {}, services: services(5), allowZoom: false });
        const deniedHandler = denied.viewer.openseadragon._handlers['canvas-key'];
        for (const key of ['-', '_', '+', '=']) {
            const e = { originalEvent: { key, preventDefault: jest.fn() } };
            deniedHandler(e);
            expect(e.preventDefaultAction).toBe(true);
        }

        const allowed = new IvViewer({ element: {}, services: services(5) });
        const allowedHandler = allowed.viewer.openseadragon._handlers['canvas-key'];
        const e = { originalEvent: { key: '-', preventDefault: jest.fn() } };
        allowedHandler(e);
        expect(e.preventDefaultAction).toBeUndefined();
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
        v.goToPage(-5);
        expect(v._crossfadeTo).not.toHaveBeenCalled();
        v.goToPage(999);
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
        v.goToPage(2);
        expect(v._navigateSpread).not.toHaveBeenCalled();
        v.goToPage(4);
        expect(v._navigateSpread).toHaveBeenCalledWith(3);
    });

    test('double mode: next advances past the trailing page of the spread', async () => {
        const v = await newViewer(20, 1);
        v.double = true;
        v.next();
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
        expect(v.current).toBe(computeSpread(4, 20)[0]);
        expect(v._open).toHaveBeenCalledWith(3, expect.any(Function));
        opens.shift()?.();
        await flush();
        expect(v.toggleDoublePage()).toBe(false);
        expect(v.current).toBe(3);
    });

    test('resetView resets rotation and fits via the library zoom control in single mode', async () => {
        const v = await newViewer(10);
        const rotate = jest.spyOn(v.rotation, 'rotateTo');
        const libraryHome = jest.spyOn(v.zoom, 'goHome');
        v.resetView();
        expect(rotate).toHaveBeenCalledWith(0);
        expect(libraryHome).toHaveBeenCalledTimes(1);
    });

    test('resetView fits the whole spread via the library zoom control in double mode', async () => {
        const v = await newViewer(20, 1);
        v.double = true;
        const libraryHome = jest.spyOn(v.zoom, 'goHome');
        const osdHome = jest.spyOn(v.viewer.openseadragon.viewport, 'goHome');
        v.resetView();
        expect(libraryHome).toHaveBeenCalledTimes(1);
        expect(osdHome).not.toHaveBeenCalled();
    });
});

describe('IvViewer navigation lock (double-page hang regression)', () => {
    let opens;

    beforeEach(() => {
        mockImageView();
        opens = [];
        jest.spyOn(IvViewer.prototype, '_open').mockImplementation(function () {
            return new Promise((resolve) => opens.push(resolve));
        });
        jest.spyOn(IvViewer.prototype, '_prewarmSpreads').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    async function newDoublePageViewer(total = 20, opts = {}) {
        const v = new IvViewer({ element: {}, services: services(total), startOrder: 0, ...opts });
        opens.shift()?.();
        await flush();
        v.double = true;
        v._open.mockClear();
        opens.length = 0;
        return v;
    }

    test('rapid TOC clicks do not run concurrent opens; the latest target wins', async () => {
        const v = await newDoublePageViewer(20);

        v.goToPage(4);
        expect(v._open).toHaveBeenCalledTimes(1);

        v.goToPage(8);
        v.goToPage(12);
        expect(v._open).toHaveBeenCalledTimes(1);

        opens.shift()();
        await flush();

        expect(v._open).toHaveBeenCalledTimes(2);
        const expectedLeader = computeSpread(12, 20)[0];
        expect(v._open).toHaveBeenLastCalledWith(expectedLeader, expect.any(Function));
        expect(v.current).toBe(expectedLeader);
    });

    test('a spread load that never settles is released by the watchdog (no permanent freeze)', async () => {
        const v = await newDoublePageViewer(20, { navWatchdogMs: 20 });

        v.goToPage(4);
        expect(v._navigating).toBe(true);

        await new Promise((resolve) => setTimeout(resolve, 40));
        await flush();

        expect(v._navigating).toBe(false);
        v._open.mockClear();
        v.goToPage(6);
        expect(v._open).toHaveBeenCalledTimes(1);
    });

    test('toggling double-page while navigating defers the reopen (no concurrent open)', async () => {
        const v = await newDoublePageViewer(20);

        v.goToPage(4);
        expect(v._open).toHaveBeenCalledTimes(1);

        v.toggleDoublePage();
        expect(v._open).toHaveBeenCalledTimes(1);
        expect(v._queuedReopen).toBe(true);

        opens.shift()();
        await flush();

        expect(v._open).toHaveBeenCalledTimes(2);
    });

    test('a failing spread open still fades the snapshot overlay out (stage not left covered)', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
        const getContext = HTMLCanvasElement.prototype.getContext;
        HTMLCanvasElement.prototype.getContext = () => ({ drawImage() {} });
        const host = document.createElement('div');
        document.body.appendChild(host);
        try {
            const v = await newDoublePageViewer(20, { element: host });
            v.viewer.openseadragon.drawer.canvas = document.createElement('canvas');
            v._fadeMs = 0;
            v._open.mockRejectedValueOnce(new Error('load failed'));
            v.goToPage(4);
            expect(host.querySelector('.immersive__xfade')).not.toBeNull();
            await new Promise((r) => setTimeout(r, 50));
            expect(host.querySelector('.immersive__xfade')).toBeNull();
            expect(v._navigating).toBe(false);
        } finally {
            HTMLCanvasElement.prototype.getContext = getContext;
            host.remove();
            warn.mockRestore();
        }
    });

    test('a queued mode reopen runs before the queued page target', async () => {
        const crossfade = jest.spyOn(IvViewer.prototype, '_crossfadeTo').mockImplementation(() => {});
        const v = await newDoublePageViewer(20);

        v.goToPage(4);
        v.goToPage(8);
        v.toggleDoublePage();
        expect(v._open).toHaveBeenCalledTimes(1);

        opens.shift()();
        await flush();
        expect(v._open).toHaveBeenCalledTimes(2);
        expect(v._queuedNav).toBe(8);

        opens.shift()();
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
        expect(second.element.className).toBe('immersive__highlight -active');
        expect(first.location).toEqual({ x: 1, y: 2, w: 3, h: 4 });

        v.clearHighlights();
        expect(osd.removeOverlay).toHaveBeenCalledTimes(2);
    });

    test('highlights are kept per owner: one panel never wipes another', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.currentItem = fakeItem();
        const osd = v.viewer.openseadragon;
        v.setHighlights([{ x: 1, y: 2, w: 3, h: 4 }], 'search');
        v.setHighlights(
            [
                { x: 5, y: 6, w: 7, h: 8 },
                { x: 9, y: 9, w: 1, h: 1 },
            ],
            'layout-tags'
        );
        expect(osd.addOverlay).toHaveBeenCalledTimes(3);
        v.clearHighlights('layout-tags');
        expect(osd.removeOverlay).toHaveBeenCalledTimes(2);
        v.setHighlights([{ x: 0, y: 0, w: 1, h: 1 }], 'search');
        expect(osd.removeOverlay).toHaveBeenCalledTimes(3);
        v.clearHighlights();
        expect(osd.removeOverlay).toHaveBeenCalledTimes(4);
    });

    test('setHighlights appends an optional per-rect class (layout-tag colour hook)', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.currentItem = fakeItem();
        const osd = v.viewer.openseadragon;

        v.setHighlights([{ x: 1, y: 2, w: 3, h: 4, className: 'alto-annotation--jacob-grimm' }]);

        expect(osd.addOverlay.mock.calls[0][0].element.className).toBe('immersive__highlight alto-annotation--jacob-grimm');
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

    test('double-page: each rect is drawn on the tiled image of its own page (misplaced-highlights regression)', () => {
        const v = new IvViewer({ element: {}, services: services(6) });
        v.double = true;
        v.current = 1;
        const left = { imageToViewportRectangle: jest.fn((x, y, w, h) => ({ page: 'left', x, y, w, h })) };
        const right = { imageToViewportRectangle: jest.fn((x, y, w, h) => ({ page: 'right', x, y, w, h })) };
        const osd = v.viewer.openseadragon;
        osd.world.getItemAt = (i) => [left, right][i] || null;
        v.currentItem = left;

        v.setHighlights([
            { x: 1, y: 1, w: 1, h: 1, order: 1 },
            { x: 2, y: 2, w: 2, h: 2, order: 2 },
            { x: 3, y: 3, w: 3, h: 3, order: 5 },
        ]);

        expect(left.imageToViewportRectangle).toHaveBeenCalledWith(1, 1, 1, 1);
        expect(right.imageToViewportRectangle).toHaveBeenCalledWith(2, 2, 2, 2);
        expect(osd.addOverlay).toHaveBeenCalledTimes(2);
        expect(osd.addOverlay.mock.calls[1][0].location).toEqual({ page: 'right', x: 2, y: 2, w: 2, h: 2 });
    });

    test('without a current item nothing is drawn', () => {
        const v = new IvViewer({ element: {}, services: services(5) });
        v.currentItem = null;
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

        v._acquire(5);
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

        expect([...v._preloaded.keys()].sort((a, b) => a - b)).toEqual([4, 6]);
        expect(osd.world.removeItem).toHaveBeenCalledWith(itemA);
        expect(osd.world.removeItem).toHaveBeenCalledWith(itemB);
    });
});

describe('IvViewer onOpen (library reload signal for filter re-sync)', () => {
    beforeEach(() => {
        mockImageView();
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_prefetchAround').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    test('emits after the initial open and after every double-page reopen', async () => {
        const v = new IvViewer({ element: {}, services: services(6), startOrder: 0, navWatchdogMs: 10 });
        const opened = jest.fn();
        v.onOpen.subscribe(opened);
        await flush();
        expect(opened).toHaveBeenCalledTimes(1);

        v.toggleDoublePage();
        await flush();
        expect(opened).toHaveBeenCalledTimes(2);

        v.toggleDoublePage();
        await flush();
        expect(opened).toHaveBeenCalledTimes(3);
    });

    test('does not emit for single-page crossfade navigation (no library reload)', async () => {
        const v = new IvViewer({ element: {}, services: services(6), startOrder: 0, navWatchdogMs: 10 });
        const opened = jest.fn();
        v.onOpen.subscribe(opened);
        await flush();
        expect(opened).toHaveBeenCalledTimes(1);

        v.goToPage(1);
        await new Promise((resolve) => setTimeout(resolve, 30));
        expect(opened).toHaveBeenCalledTimes(1);
    });
});

describe('IvViewer access overlay (mixed page rights)', () => {
    beforeEach(() => {
        mockImageView();
        jest.spyOn(IvViewer.prototype, '_open').mockImplementation(function () {
            return new Promise((resolve) => resolve());
        });
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    const restrictedViewer = (restricted, startOrder = 0, deniedContent = null) =>
        new IvViewer({ element: document.createElement('div'), services: services(restricted.length), startOrder, restricted, deniedText: 'Kein Zugriff', deniedContent });

    test('isPageRestricted reflects the per-page flags', () => {
        const v = restrictedViewer([false, true, false]);
        expect(v.isPageRestricted(0)).toBe(false);
        expect(v.isPageRestricted(1)).toBe(true);
        expect(v.isPageRestricted(2)).toBe(false);
    });

    test('shows the denied overlay when the current page is restricted, hides it otherwise', () => {
        const v = restrictedViewer([false, true, false], 1);
        v._emit();
        const overlay = v.viewer.element.querySelector('.immersive__page-denied');
        expect(overlay).not.toBeNull();
        expect(overlay.hidden).toBe(false);
        expect(overlay.querySelector('.immersive__page-denied-text').textContent).toBe('Kein Zugriff');

        v.current = 0;
        v._emit();
        expect(v.viewer.element.querySelector('.immersive__page-denied').hidden).toBe(true);
    });

    test('a free record never creates the overlay element', () => {
        const v = restrictedViewer([false, false, false], 0);
        v._emit();
        expect(v.viewer.element.querySelector('.immersive__page-denied')).toBeNull();
    });

    test('overlay clones the admin-configured denied template (image + text) when present', () => {
        const template = document.createElement('div');
        template.id = 'immersiveDeniedContent';
        template.innerHTML = '<img class="immersive__page-denied-img" src="/custom/denied.png" alt="x" /><div class="immersive__page-denied-text">Gesperrt (Lizenz)</div>';
        document.body.appendChild(template);
        try {
            const v = restrictedViewer([false, true], 1, template);
            v._emit();
            const overlay = v.viewer.element.querySelector('.immersive__page-denied');
            expect(overlay.querySelector('.immersive__page-denied-img').getAttribute('src')).toBe('/custom/denied.png');
            expect(overlay.querySelector('.immersive__page-denied-text').textContent).toBe('Gesperrt (Lizenz)');
        } finally {
            template.remove();
        }
    });

    test('double mode: the overlay shows when any page of the spread is restricted', () => {
        const v = restrictedViewer([false, false, true, false], 0);
        v.double = true;
        v.current = 2;
        v._emit();
        const overlay = v.viewer.element.querySelector('.immersive__page-denied');
        expect(overlay).not.toBeNull();
        expect(overlay.hidden).toBe(false);
    });

    test('missing restricted data defaults to unrestricted (no overlay)', () => {
        const v = new IvViewer({ element: document.createElement('div'), services: services(3) });
        v._emit();
        expect(v.viewer.element.querySelector('.immersive__page-denied')).toBeNull();
        expect(v.isPageRestricted(0)).toBe(false);
    });
});

describe('IvViewer restricted-page navigation (tile-load reject fix)', () => {
    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    test('_open still emits + shows the overlay when the tile load REJECTS (restricted entry)', async () => {
        mockImageView();
        global.ImageView.Image.prototype.load = () => Promise.reject(new Error('403'));
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_prefetchAround').mockImplementation(() => {});

        const v = new IvViewer({ element: document.createElement('div'), services: services(3), startOrder: 0, restricted: [true, false, false], deniedText: 'Kein Zugriff' });
        const opened = jest.fn();
        v.onOpen.subscribe(opened);
        await flush();
        await flush();

        expect(opened).toHaveBeenCalledWith(0);
        expect(v.currentItem).toBeNull();
        const overlay = v.viewer.element.querySelector('.immersive__page-denied');
        expect(overlay).not.toBeNull();
        expect(overlay.hidden).toBe(false);
    });

    test('_crossfadeTo short-circuits a restricted target: no tile acquire, overlay shown', async () => {
        mockImageView();
        jest.spyOn(IvViewer.prototype, '_open').mockResolvedValue();
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
        const acquire = jest.spyOn(IvViewer.prototype, '_acquire');

        const v = new IvViewer({ element: document.createElement('div'), services: services(3), startOrder: 0, restricted: [false, true, false], deniedText: 'Kein Zugriff' });
        await flush();

        v.goToPage(1);
        await flush();
        await flush();

        expect(v.current).toBe(1);
        expect(acquire).not.toHaveBeenCalled();
        const overlay = v.viewer.element.querySelector('.immersive__page-denied');
        expect(overlay).not.toBeNull();
        expect(overlay.hidden).toBe(false);
    });
});

describe('IvViewer crossfade with slow tiles', () => {
    let items;

    /** A TiledImage whose "first tile painted" event the test fires by hand. */
    const fakeItem = () => {
        const handlers = {};
        return {
            handlers,
            getFullyLoaded: () => false,
            setOpacity: jest.fn(),
            addHandler: (name, fn) => {
                handlers[name] = fn;
            },
            removeHandler: (name) => {
                delete handlers[name];
            },
            paint() {
                if (handlers['fully-loaded-change']) handlers['fully-loaded-change']({ fullyLoaded: true });
            },
        };
    };

    beforeEach(() => {
        mockImageView();
        items = [];
        jest.spyOn(IvViewer.prototype, '_open').mockResolvedValue();
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_prefetchAround').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    /** Viewer whose tile images are added instantly but only paint when the test says so. */
    async function slowTileViewer(opts) {
        const v = new IvViewer({ element: document.createElement('div'), services: services(5), startOrder: 0, ...opts });
        const osd = v.viewer.openseadragon;
        const osdHandlers = {};
        osd.addHandler = (name, fn) => {
            (osdHandlers[name] = osdHandlers[name] || []).push(fn);
        };
        osd.removeHandler = (name, fn) => {
            osdHandlers[name] = (osdHandlers[name] || []).filter((h) => h !== fn);
        };
        osd.addTiledImage = jest.fn(({ success }) => {
            const item = fakeItem();
            items.push(item);
            success({ item });
        });
        v._fadeMs = 0;
        await flush();
        return { v, osd, osdHandlers };
    }

    /** Long enough for the rAF-driven crossfade tween to finish. */
    const settle = () => new Promise((resolve) => setTimeout(resolve, 50));

    test('a page whose tiles never paint rolls the navigation back instead of freezing', async () => {
        jest.spyOn(console, 'warn').mockImplementation(() => {});
        const { v, osd, osdHandlers } = await slowTileViewer({ contentTimeoutMs: 20, navWatchdogMs: 5000 });
        const seen = jest.fn();
        v.onPageChange.subscribe(seen);
        const unhandled = jest.fn();
        process.on('unhandledRejection', unhandled);

        v.goToPage(1);
        await flush();
        expect(items).toHaveLength(1);
        expect(v._navigating).toBe(true);

        await settle();
        await flush();
        process.off('unhandledRejection', unhandled);

        expect(v.getCurrentOrder()).toBe(0);
        expect(seen).not.toHaveBeenCalled();
        expect(v._navigating).toBe(false);
        expect(unhandled).not.toHaveBeenCalled();
        expect(items[0].handlers['fully-loaded-change']).toBeUndefined();
        expect(osdHandlers['tile-loaded']).toEqual([]);
        expect(osd.world.removeItem).toHaveBeenCalledWith(items[0]);
    });

    test('destroy cancels a pending content wait: timers cleared, tile handlers detached, no late page change', async () => {
        const { v, osd, osdHandlers } = await slowTileViewer({ contentTimeoutMs: 5000, navWatchdogMs: 5000 });
        osd.destroy = jest.fn();
        const seen = jest.fn();
        v.onPageChange.subscribe(seen);
        jest.useFakeTimers();
        try {
            v.goToPage(1);
            for (let i = 0; i < 5; i++) await Promise.resolve();
            expect(osdHandlers['tile-loaded']).toHaveLength(1);
            expect(jest.getTimerCount()).toBe(2);
            v.destroy();
            expect(jest.getTimerCount()).toBe(0);
            expect(osdHandlers['tile-loaded']).toEqual([]);
            expect(items[0].handlers['fully-loaded-change']).toBeUndefined();
            items[0].paint();
            for (let i = 0; i < 5; i++) await Promise.resolve();
            expect(seen).not.toHaveBeenCalled();
        } finally {
            jest.useRealTimers();
        }
    });

    test('a load that outlives its watchdog release never overwrites the page a newer navigation shows', async () => {
        const { v, osd } = await slowTileViewer({ navWatchdogMs: 20, contentTimeoutMs: 5000 });

        v.goToPage(1);
        await flush();
        expect(items).toHaveLength(1);

        await settle();
        await flush();
        expect(v._navigating).toBe(false);

        v.goToPage(2);
        await flush();
        expect(items).toHaveLength(2);
        items[1].paint();
        await settle();
        expect(v.getCurrentOrder()).toBe(2);
        expect(v.currentItem).toBe(items[1]);

        const seen = jest.fn();
        v.onPageChange.subscribe(seen);
        items[0].paint();
        await settle();

        expect(v.getCurrentOrder()).toBe(2);
        expect(v.currentItem).toBe(items[1]);
        expect(seen).not.toHaveBeenCalled();
        expect(osd.world.removeItem).toHaveBeenCalledWith(items[0]);
    });
});

describe('IvViewer library open settling late or failing', () => {
    let loads;

    beforeEach(() => {
        mockImageView();
        loads = [];
        global.ImageView.Image.prototype.load = function () {
            return new Promise((res) => loads.push(res));
        };
        jest.spyOn(IvViewer.prototype, '_prefetchAround').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_prewarmSpreads').mockImplementation(() => {});
        jest.spyOn(IvViewer.prototype, '_refreshPreload').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
    });

    test('a spread open that settles after its watchdog release neither restores its view nor emits', async () => {
        const v = new IvViewer({ element: document.createElement('div'), services: services(20), startOrder: 0, navWatchdogMs: 20 });
        loads.shift()();
        await flush();
        v.double = true;
        v.goToPage(4);
        await new Promise((r) => setTimeout(r, 40));
        await flush();
        expect(v._navigating).toBe(false);
        v.goToPage(8);
        loads[1]();
        await flush();
        expect(v.getCurrentOrder()).toBe(7);

        const open = jest.fn();
        v.onOpen.subscribe(open);
        const change = jest.fn();
        v.onPageChange.subscribe(change);
        const restore = jest.spyOn(v, '_restoreView');
        loads[0]();
        await flush();
        expect(open).not.toHaveBeenCalled();
        expect(change).not.toHaveBeenCalled();
        expect(restore).not.toHaveBeenCalled();
        expect(v.getCurrentOrder()).toBe(7);
    });

    test('the initial open settling after destroy writes nothing and emits nothing', async () => {
        const v = new IvViewer({ element: document.createElement('div'), services: services(3), startOrder: 0, restricted: [true] });
        const emit = jest.spyOn(v, '_emit');
        const restore = jest.spyOn(v, '_restoreView');
        v.destroy();
        loads.shift()();
        await flush();
        expect(emit).not.toHaveBeenCalled();
        expect(restore).not.toHaveBeenCalled();
        expect(v.viewer.element.querySelector('.immersive__page-denied')).toBeNull();
    });

    test('a rejected library load of an unrestricted page is logged, not swallowed', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
        global.ImageView.Image.prototype.load = () => Promise.reject(new Error('503'));
        try {
            const v = new IvViewer({ element: document.createElement('div'), services: services(3), startOrder: 0 });
            await flush();
            expect(v.getCurrentOrder()).toBe(0);
            expect(warn).toHaveBeenCalledWith(expect.any(String), expect.any(Error));
        } finally {
            warn.mockRestore();
        }
    });
});

describe('stageMargins', function () {
    test('reserves the full desktop chrome above 768px', function () {
        expect(stageMargins(1280)).toEqual({ top: 64, bottom: 72, left: 64, right: 64 });
        expect(stageMargins(769)).toEqual({ top: 64, bottom: 72, left: 64, right: 64 });
    });

    test('shrinks the chrome margins on small viewports so the page uses the screen', function () {
        expect(stageMargins(768)).toEqual({ top: 56, bottom: 60, left: 16, right: 16 });
        expect(stageMargins(390)).toEqual({ top: 56, bottom: 60, left: 16, right: 16 });
    });
});

describe('IvViewer keeps the view across a reload', () => {
    beforeEach(() => {
        mockImageView();
        global.fetch = jest.fn(() => Promise.resolve());
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete global.ImageView;
        delete global.fetch;
    });

    const rect = (x, y, width, height) => ({
        x,
        y,
        width,
        height,
        clone: () => rect(x, y, width, height),
        union: (o) => rect(Math.min(x, o.x), Math.min(y, o.y), Math.max(x + width, o.x + o.width) - Math.min(x, o.x), Math.max(y + height, o.y + o.height) - Math.min(y, o.y)),
    });

    const stage = (v, bounds) => {
        const osd = v.viewer.openseadragon;
        osd.world = {
            getItemCount: () => bounds().length,
            getItemAt: (i) => ({ getBounds: () => bounds()[i].rect || bounds()[i], getOpacity: () => (bounds()[i].hidden ? 0 : 1) }),
            removeItem: jest.fn(),
        };
        osd.viewport = {
            goHome() {},
            getCenter: () => ({ x: 0.75, y: 0.25 }),
            getZoom: () => 4,
            getRotation: () => 0,
            zoomTo: jest.fn(),
            panTo: jest.fn(),
        };
        return osd;
    };

    test('the same two-page row gets the view back, a row that lost a page keeps the fresh fit', async () => {
        const v = new IvViewer({ element: {}, services: services(4), startOrder: 0 });
        await flush();
        let bounds = [rect(0, 0, 0.5, 0.5), rect(0.5, 0, 0.5, 0.5)];
        const osd = stage(v, () => bounds);
        v.double = true;

        await v._open(0);
        expect(osd.viewport.zoomTo).toHaveBeenCalledWith(4, null, true);
        expect(osd.viewport.panTo).toHaveBeenCalledWith({ x: 0.75, y: 0.25 }, true);

        osd.viewport.zoomTo.mockClear();
        osd.viewport.panTo.mockClear();
        v.double = false;
        // the spread is still on screen while the view is measured; the single page arrives with the load
        v.viewer.load = () => {
            bounds = [rect(0, 0, 0.5, 0.5)];
            return Promise.resolve();
        };
        await v._open(3);
        expect(osd.viewport.zoomTo).not.toHaveBeenCalled();
        expect(osd.viewport.panTo).not.toHaveBeenCalled();
    });

    test('pages preloaded into the same world do not count towards the row', async () => {
        const v = new IvViewer({ element: {}, services: services(6), startOrder: 2 });
        await flush();
        // two visible pages of the spread plus one neighbour preloaded hidden next to them
        let bounds = [rect(0, 0, 0.5, 0.5), rect(0.5, 0, 0.5, 0.5), { rect: rect(1, 0, 0.5, 0.5), hidden: true }];
        const osd = stage(v, () => bounds);
        v.double = true;
        v.viewer.load = () => {
            bounds = [rect(0, 0, 0.5, 0.5), rect(0.5, 0, 0.5, 0.5)];
            return Promise.resolve();
        };

        await v._open(2);
        // the row is the visible pair, so the view is put back instead of being dropped as a shape change
        expect(osd.viewport.zoomTo).toHaveBeenCalledWith(4, null, true);
        expect(osd.viewport.panTo).toHaveBeenCalledWith({ x: 0.75, y: 0.25 }, true);
    });

    test('a viewport that cannot be read leaves the navigation intact', async () => {
        const v = new IvViewer({ element: {}, services: services(3), startOrder: 0 });
        await flush();
        const osd = stage(v, () => [rect(0, 0, 0.5, 0.5)]);
        osd.viewport.getZoom = () => {
            throw new Error('viewport not ready');
        };

        await expect(v._open(1)).resolves.toBeUndefined();
        expect(osd.viewport.zoomTo).not.toHaveBeenCalled();
    });
});
