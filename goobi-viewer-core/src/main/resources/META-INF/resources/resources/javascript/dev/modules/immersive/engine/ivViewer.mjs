import { computeSpread, residentPages } from './ivImageWindow.mjs';
import { crossfade, heightBand, snapshotOverlay, fadeOverlay } from './ivTransitions.mjs';
import { createNavLock } from './ivNavLock.mjs';
import { IvOverlays } from './ivOverlays.mjs';
import { createAccessOverlay } from './ivAccessOverlay.mjs';
import { env } from '../shared/ivEnv.mjs';

/**
 * OSD viewport margins that keep the fitted page clear of the floating chrome.
 * Desktop reserves generous side margins; small viewports shrink them so the
 * page actually uses the screen (bars stay clear via the top/bottom values:
 * title pill 36px + inset, bottom bar 36-40px + inset). Pure + tested.
 *
 * @param {number} viewportWidth  window.innerWidth at viewer construction
 * @returns {{top:number, bottom:number, left:number, right:number}}
 */
export function stageMargins(viewportWidth) {
    if (viewportWidth <= 768) {
        return { top: 56, bottom: 60, left: 16, right: 16 };
    }
    return { top: 64, bottom: 72, left: 64, right: 64 };
}

/** Minimal dependency-free event emitter (rxjs-compatible `subscribe` shape). */
export class Emitter {
    constructor() {
        this._subs = new Set();
    }
    subscribe(fn) {
        this._subs.add(fn);
        return () => this._subs.delete(fn);
    }
    emit(value) {
        this._subs.forEach((fn) => fn(value));
    }
    clear() {
        this._subs.clear();
    }
}

/** Maps a IIIF image-service base id to an OSD tile source (its info.json URL). */
function toTileSource(serviceId) {
    return serviceId.endsWith('/info.json') ? serviceId : `${serviceId}/info.json`;
}

/**
 * Default single-image sequence config (mirrors zoomableImage.mjs); _arrangeImageSequence
 * reads it on every open(). Copied per instance because _open() mutates `columns`.
 */
const SEQUENCE_DEFAULTS = { columns: 1, useWindowing: true, windowSize: 100, windowExpandThreshold: 10, windowExpandSize: 50 };
const PREFETCH_RADIUS = 1;
const ZOOM_STEP = 1.5;
const FADE_MS = 160;

/**
 * Last-resort release for the navigation lock. Double-page navigation re-opens the library
 * (a fresh OpenSeadragon per spread) and only settles on its async 'open' event; if rapid
 * reopens lose that event the promise never settles. Freeing the lock after this window
 * keeps the viewer from staying frozen. Long enough not to abort a genuinely slow load.
 */
const NAV_WATCHDOG_MS = 8000;

/**
 * Immersive image viewer engine around a single live ImageView.Image (OSD).
 * Single pages are swapped flicker-free by fading in a preloaded neighbour; double
 * pages are composed by the library and transitioned with a snapshot crossfade.
 * Consumers subscribe to `onPageChange` / `onLoaded`.
 */
export default class IvViewer {
    /**
     * @param {object} opts
     * @param {HTMLElement} opts.element   OSD mount element
     * @param {string[]} opts.services     ordered IIIF image-service URLs (all pages)
     * @param {number} [opts.startOrder=0] initial 0-based page index
     * @param {number} [opts.maxZoom]
     * @param {HTMLElement} [opts.deniedContent]  server-rendered placeholder content for image-restricted pages
     */
    constructor(opts) {
        this.services = opts.services;
        this.total = opts.services.length;
        this.current = Math.max(0, Math.min(opts.startOrder ?? 0, this.total - 1));
        this.allowZoom = opts.allowZoom !== false;
        this.restricted = Array.isArray(opts.restricted) ? opts.restricted : [];
        this.deniedText = opts.deniedText || '';
        this.double = false;
        this.currentItem = null;
        this._anchor = null;
        this._preloaded = new Map();
        this._queuedNav = null;
        this._queuedReopen = false;
        this._lock = createNavLock(opts.navWatchdogMs ?? NAV_WATCHDOG_MS);
        this._lock.onRelease(() => this._runQueued());
        this._fadeMs = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : FADE_MS;
        this.onPageChange = new Emitter();
        this.onLoaded = new Emitter();
        this.onOpen = new Emitter();
        this.onReset = new Emitter();

        this.viewer = new env.ImageView.Image({
            element: opts.element,
            fittingMode: 'fixed',
            margins: stageMargins(window.innerWidth),
            zoom: { enabled: true, max: opts.maxZoom },
            sequence: { ...SEQUENCE_DEFAULTS },
            navigator: { enabled: false },
        });
        this.viewer.openseadragon.crossOriginPolicy = 'Anonymous';
        this.zoom = new env.ImageView.Controls.Zoom(this.viewer);
        this.rotation = new env.ImageView.Controls.Rotation(this.viewer);
        this._overlays = new IvOverlays({
            osd: () => this.viewer.openseadragon,
            itemFor: (order) => this._itemForOrder(order),
            currentItem: () => this.currentItem || this.viewer.openseadragon.world.getItemAt(0),
        });
        this._access = createAccessOverlay({ host: this.viewer.element, template: opts.deniedContent || null, text: this.deniedText });

        if (!this.allowZoom) {
            const osd = this.viewer.openseadragon;
            osd.zoomPerScroll = 1;
            osd.zoomPerClick = 1;
            ['gestureSettingsMouse', 'gestureSettingsTouch', 'gestureSettingsPen', 'gestureSettingsUnknown'].forEach((k) => {
                const g = osd[k];
                if (g) {
                    g.scrollToZoom = false;
                    g.clickToZoom = false;
                    g.dblClickToZoom = false;
                    g.pinchToZoom = false;
                }
            });
        }

        this.viewer.openseadragon.addHandler('canvas-key', (e) => {
            const key = e.originalEvent.key;
            if (key === 'ArrowRight') this.next();
            else if (key === 'ArrowLeft') this.prev();
            else if (key === '0') {
                this.resetView();
                e.preventDefaultAction = true;
                e.originalEvent.preventDefault();
            }
            if (['ArrowLeft', 'ArrowRight'].includes(key)) e.preventDefaultAction = true;
            if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(key)) e.originalEvent.preventDefault();
            if (!this.allowZoom && ['-', '_', '+', '='].includes(key)) e.preventDefaultAction = true;
        });

        this._open(this.current)
            .then(() => {
                this._refreshPreload();
                this.onLoaded.emit(this.current);
            })
            .catch((e) => console.error('immersive viewer initial open failed', e));
    }

    /**
     * TiledImage rendering `order` in the current view, or null if that page is not
     * shown. In double mode the spread's pages map to world items 0/1 (see _open);
     * without an `order` the single current item is used.
     */
    _itemForOrder(order) {
        const osd = this.viewer.openseadragon;
        if (order == null) return this.currentItem || osd.world.getItemAt(0);
        const idx = this.getCurrentPages().indexOf(order);
        if (idx === -1) return null;
        return this.double ? osd.world.getItemAt(idx) : this.currentItem || osd.world.getItemAt(0);
    }

    /** Highlight rectangles for `owner` (search hits, layout-tag occurrences); see IvOverlays. */
    setHighlights(rects, owner = 'default') {
        this._overlays.setHighlights(rects, owner);
    }

    /** Removes the highlights of `owner`, or of every owner when omitted. */
    clearHighlights(owner) {
        this._overlays.clearHighlights(owner);
    }

    /** OCR boxes as hoverable, id-tagged overlays; returns a Map id → element. */
    setTextRegions(regions) {
        return this._overlays.setTextRegions(regions);
    }

    clearTextRegions() {
        this._overlays.clearTextRegions();
    }

    /** ALTO layout annotations (drawn lines, note areas) as overlays; returns a Map id → element. */
    setAnnotationRegions(regions) {
        return this._overlays.setAnnotationRegions(regions);
    }

    clearAnnotationRegions() {
        this._overlays.clearAnnotationRegions();
    }

    /** Current 0-based page order (the spread leader in double mode). */
    getCurrentOrder() {
        return this.current;
    }

    /** Total page count. */
    getPageCount() {
        return this.total;
    }

    /** Whether book-spread (double-page) mode is active. */
    isDoublePage() {
        return this.double;
    }

    /** 0-based page indices currently displayed (one page, or two in double mode). */
    getCurrentPages() {
        return this.double ? computeSpread(this.current, this.total) : [this.current];
    }

    /** Navigate to the page/spread containing `order` (snaps to the spread leader in double mode). */
    goToPage(order) {
        const target = Math.max(0, Math.min(order, this.total - 1));
        if (this._navigating) {
            this._queuedNav = target;
            return;
        }
        if (this.double) {
            const leader = computeSpread(target, this.total)[0];
            if (leader === this.current) return;
            this._navigateSpread(leader);
            return;
        }
        if (target === this.current) return;
        this._crossfadeTo(target);
    }

    /** Pages forward: the next page, or the page after the current spread in double mode. */
    next() {
        if (this.double) {
            const pages = computeSpread(this.current, this.total);
            this.goToPage(pages[pages.length - 1] + 1);
        } else {
            this.goToPage(this.current + 1);
        }
    }

    /** Pages one page back (snaps to the containing spread in double mode). */
    prev() {
        this.goToPage(this.current - 1);
    }

    /** Whether a navigation is in flight (queued targets wait for the release). */
    get _navigating() {
        return this._lock.busy;
    }

    /** Runs `run` under the navigation lock; `onStuck` cleans up if the watchdog fires (see ivNavLock). */
    _withNavLock(run, onStuck) {
        this._lock.run(run, onStuck);
    }

    /** After a release: a queued mode reopen first, else the latest queued page target. */
    _runQueued() {
        if (this._queuedReopen) {
            this._queuedReopen = false;
            this._reopen();
            return;
        }
        const next = this._queuedNav;
        this._queuedNav = null;
        if (next != null) this.goToPage(next);
    }

    /** Re-opens the current position in the current mode, serialised through the nav lock. */
    _reopen() {
        this._withNavLock(() => this._open(this.current));
    }

    zoomIn() {
        if (!this.allowZoom) return;
        this.zoom.zoomBy(ZOOM_STEP);
    }

    zoomOut() {
        if (!this.allowZoom) return;
        this.zoom.zoomBy(1 / ZOOM_STEP);
    }

    rotateLeft() {
        this.rotation.rotateLeft();
    }

    rotateRight() {
        this.rotation.rotateRight();
    }

    /**
     * Resets rotation and zoom to fit the whole page (whole spread in double mode).
     * Always goes through the library's zoom control: it fits the union of the
     * current row with margin compensation, while raw OSD viewport.goHome() FILLS
     * the viewport (homeFillsViewer) and would land on a zoomed-in spread.
     * Emits `onReset` so UI add-ons (image filters) can reset along with the view.
     */
    resetView() {
        this.rotation.rotateTo(0);
        this.zoom.goHome();
        this.onReset.emit();
    }

    /** Toggles book-spread mode and re-opens at the current position. Returns the new state. */
    toggleDoublePage() {
        this.double = !this.double;
        this.current = this.getCurrentPages()[0];
        if (this._navigating) {
            this._queuedReopen = true;
        } else {
            this._reopen();
        }
        return this.double;
    }

    /**
     * Single-page navigation: fade the (preloaded or freshly added) target page in
     * over the current one, then drop the old. No reload, no blank, no jump.
     */
    _crossfadeTo(target) {
        this._withNavLock(async () => {
            const previous = this.currentItem;
            const from = this.current;
            this.current = target;
            if (this.isPageRestricted(target)) {
                if (previous) this.viewer.openseadragon.world.removeItem(previous);
                this.currentItem = null;
                this._emit();
                this._refreshPreload();
                return;
            }
            let item;
            try {
                item = await this._acquire(target, this._anchor);
                this._preloaded.delete(target);
                await this._whenContent(item);
            } catch (e) {
                this.current = from;
                this._preloaded.delete(target);
                throw e;
            }
            await crossfade(item, previous, this._fadeMs);
            if (previous) this.viewer.openseadragon.world.removeItem(previous);
            this.currentItem = item;
            this._emit();
            this._refreshPreload();
        });
    }

    /**
     * Returns a cached promise for the TiledImage of `order`, adding it hidden and
     * preloaded (at `bounds`) if not already present/in-flight. Caching by order lets
     * an in-flight preload and an on-demand navigation share one image; it resolves as
     * soon as the image is added (not when fully loaded).
     */
    _acquire(order, bounds) {
        if (this._preloaded.has(order)) return this._preloaded.get(order);
        const osd = this.viewer.openseadragon;
        const anchor = bounds || (this.currentItem ? this.currentItem.getBounds() : undefined);
        const p = new Promise((resolve, reject) => {
            osd.addTiledImage({
                tileSource: toTileSource(this.services[order]),
                opacity: 0,
                preload: true,
                fitBounds: anchor,
                success: (e) => resolve(e.item),
                error: reject,
            });
        });
        this._preloaded.set(order, p);
        return p;
    }

    /** Resolves once `item` has painted its first (low-res) tile, or is already fully loaded. */
    _whenContent(item) {
        if (item.getFullyLoaded()) return Promise.resolve();
        const osd = this.viewer.openseadragon;
        return new Promise((resolve) => {
            let done = false;
            const finish = () => {
                if (done) return;
                done = true;
                osd.removeHandler('tile-loaded', onTile);
                item.removeHandler('fully-loaded-change', onFull);
                resolve();
            };
            const onTile = (e) => {
                if (e.tiledImage === item) finish();
            };
            const onFull = (ev) => {
                if (ev.fullyLoaded) finish();
            };
            osd.addHandler('tile-loaded', onTile);
            item.addHandler('fully-loaded-change', onFull);
        });
    }

    /** Keeps the adjacent pages (±1) preloaded and evicts the rest (single-page path). */
    _refreshPreload() {
        const keep = new Set(residentPages(this.current, this.total, { double: this.double }));
        for (const order of keep) {
            if (order === this.current) continue;
            if (this.isPageRestricted(order)) continue;
            if (!this._preloaded.has(order)) {
                this._acquire(order, this._anchor).catch(() => this._preloaded.delete(order));
            }
        }
        for (const [order, p] of this._preloaded) {
            if (!keep.has(order)) {
                p.then((item) => {
                    try {
                        this.viewer.openseadragon.world.removeItem(item);
                    } catch {}
                }).catch(() => {});
                this._preloaded.delete(order);
            }
        }
    }

    /**
     * Double-page navigation: freeze the current spread as a snapshot overlay, let the
     * library re-compose the new spread (columns:2) underneath, then fade the snapshot
     * out. Neighbour spreads are tile-prewarmed for near-instant sharpness.
     */
    _navigateSpread(leader) {
        const overlay = snapshotOverlay(this.viewer.openseadragon, this.viewer.element);
        this.current = leader;
        this._withNavLock(
            async () => {
                await this._open(leader);
                fadeOverlay(overlay, this._fadeMs);
                this._prewarmSpreads();
            },
            () => {
                if (overlay) overlay.remove();
            }
        );
    }

    /**
     * Warms OSD's tile cache for the neighbouring spreads (hidden preloaded images) so
     * the next spread renders almost immediately. The temp images are invisible and are
     * cleared by the next open(); their tiles stay cached.
     */
    _prewarmSpreads() {
        const here = computeSpread(this.current, this.total);
        const neighbours = [...computeSpread(here[0] - 1, this.total), ...computeSpread(here[here.length - 1] + 1, this.total)];
        const osd = this.viewer.openseadragon;
        for (const p of new Set(neighbours)) {
            if (p < 0 || p >= this.total || here.includes(p)) continue;
            if (this.isPageRestricted(p)) continue;
            osd.addTiledImage({ tileSource: toTileSource(this.services[p]), opacity: 0, preload: true });
        }
    }

    /**
     * Loads the page(s) for `order` via the library (a single page, or a columns:2
     * spread in double mode) and captures the single-page fit anchor. Used for the
     * initial open, mode toggles and every double-page spread change. Emits
     * `onOpen` once the library has settled, so consumers bound to the library's
     * per-load event pipeline (image filters) can re-attach.
     */
    _open(order) {
        const pages = this.double ? computeSpread(order, this.total) : [order];
        this.viewer.config.sequence.columns = pages.length;
        const sources = pages.map((p) => toTileSource(this.services[p]));
        const settle = () => {
            const world = this.viewer.openseadragon.world;
            this.currentItem = world.getItemCount() > 0 ? world.getItemAt(0) : null;
            if (!this.double && this.currentItem) {
                this._anchor = heightBand(this.currentItem.getBounds());
            }
            this._preloaded.clear();
            this._emit();
            this.onOpen.emit(this.current);
        };
        const loaded = this.viewer.load(sources, 0);
        this._prefetchAround(pages[pages.length - 1]);
        return loaded.then(settle, settle);
    }

    /** Warms neighbour info.json in the browser cache so the next load is faster. */
    _prefetchAround(order) {
        for (let d = 1; d <= PREFETCH_RADIUS; d++) {
            for (const o of [order - d, order + d]) {
                if (o >= 0 && o < this.total && !this.isPageRestricted(o)) {
                    fetch(toTileSource(this.services[o])).catch(() => {});
                }
            }
        }
    }

    _emit() {
        this._syncAccessOverlay();
        this.onPageChange.emit(this.current);
    }

    /** True when the page at `order` carries a server auth service (image not viewable). */
    isPageRestricted(order) {
        return this.restricted[order] === true;
    }

    _syncAccessOverlay() {
        this._access.sync(this.getCurrentPages().some((o) => this.isPageRestricted(o)));
    }

    /** Runs `fn` once when the first tile has been painted. */
    onceTileLoaded(fn) {
        this.viewer.openseadragon.addOnceHandler('tile-loaded', fn);
    }

    /** Runs `fn` on the next viewport resize; returns a cancel function. */
    onceResized(fn) {
        const osd = this.viewer.openseadragon;
        const handler = () => {
            osd.removeHandler('resize', handler);
            fn();
        };
        osd.addHandler('resize', handler);
        return () => osd.removeHandler('resize', handler);
    }

    /** Forces a redraw of the current frame. */
    redraw() {
        this.viewer.openseadragon.forceRedraw();
    }

    /** 2D context of the drawn canvas, null before the first paint. */
    getCanvasContext() {
        return typeof this.viewer.getCanvasContext === 'function' ? this.viewer.getCanvasContext() : null;
    }

    /**
     * Runs `fn` after every drawn frame. The library re-runs its own event wiring
     * on every load() against the same OpenSeadragon instance; attaching once per
     * instance keeps this handler from stacking, and a replaced instance is picked
     * up on onOpen. Returns a detach function.
     */
    onFrame(fn) {
        let bound = null;
        const attach = () => {
            const osd = this.viewer.openseadragon;
            if (!osd || bound === osd) return;
            bound = osd;
            osd.addHandler('update-viewport', fn);
        };
        attach();
        const unsubscribe = this.onOpen.subscribe(attach);
        return () => {
            unsubscribe();
            if (bound && typeof bound.removeHandler === 'function') bound.removeHandler('update-viewport', fn);
        };
    }

    /** Releases the OpenSeadragon instance, the access overlay and all subscriptions. */
    destroy() {
        this._lock.destroy();
        this._preloaded.clear();
        this._access.remove();
        this._overlays.destroy();
        if (typeof this.viewer.close === 'function') this.viewer.close();
        const osd = this.viewer.openseadragon;
        if (osd && typeof osd.destroy === 'function') osd.destroy();
        [this.onPageChange, this.onLoaded, this.onOpen, this.onReset].forEach((emitter) => emitter.clear());
    }
}
