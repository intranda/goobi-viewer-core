/**
 * Unit tests for ivViewer.mjs: the pure `Emitter` export and IvViewer's navigation lock.
 *
 * IvViewer depends on the global `ImageView` object (only present in a real browser). The
 * navigation-lock tests stand up a minimal `ImageView` mock and stub the async `_open`, so
 * they exercise the serialisation/queue/watchdog logic without a real OpenSeadragon.
 *
 * Note: Jest's `jest` global is not auto-injected in native ESM projects
 * (transform: {}). We import it explicitly from @jest/globals.
 */
import { jest } from '@jest/globals';
import IvViewer, { Emitter } from '../ivViewer.mjs';
import { computeSpread } from '../iv_imageWindow.mjs';

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
                this.config = { sequence: { columns: 1 } };
                this.openseadragon = {
                    addHandler() {},
                    addTiledImage() {},
                    world: { getItemAt: () => null, removeItem() {}, getItemCount: () => 0 },
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
});
