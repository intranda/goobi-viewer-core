import { computeSpread, residentPages } from './iv_imageWindow.mjs';

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
}

/** Maps a IIIF image-service base id to an OSD tile source (its info.json URL). */
function toTileSource(serviceId) {
    return serviceId.endsWith('/info.json') ? serviceId : `${serviceId}/info.json`;
}

/** Tweens one TiledImage's opacity 0→1 while fading another 1→0 (rAF). */
function _crossfade(incoming, outgoing, durationMs) {
    return new Promise((resolve) => {
        let start = null;
        const step = (ts) => {
            if (start === null) start = ts;
            const t = durationMs <= 0 ? 1 : Math.min(1, (ts - start) / durationMs);
            if (incoming) incoming.setOpacity(t);
            if (outgoing) outgoing.setOpacity(1 - t);
            if (t < 1) requestAnimationFrame(step);
            else resolve();
        };
        requestAnimationFrame(step);
    });
}

/**
 * Widens a bounds rect to a tall band (same y/height, 3x width, same centre).
 * As a fitBounds anchor this makes OSD fit by HEIGHT, so pages of differing scan
 * aspect render at a uniform height and paging never jumps vertically.
 */
function _heightBand(rect) {
    const band = rect.clone();
    band.x = rect.x - rect.width;
    band.width = rect.width * 3;
    return band;
}

/** Single-image sequence config (mirrors zoomableImage.mjs); _arrangeImageSequence reads it on every open(). */
const _sequence = { columns: 1, useWindowing: true, windowSize: 100, windowExpandThreshold: 10, windowExpandSize: 50 };
const PREFETCH_RADIUS = 1;

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
     */
    constructor(opts) {
        this.services = opts.services;
        this.total = opts.services.length;
        this.current = Math.max(0, Math.min(opts.startOrder ?? 0, this.total - 1));
        this.double = false;
        this.currentItem = null;
        this._anchor = null;
        this._preloaded = new Map();
        this._navigating = false;
        this._highlights = [];
        this._fadeMs = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 160;
        this.onPageChange = new Emitter();
        this.onLoaded = new Emitter();

        this.viewer = new ImageView.Image({
            element: opts.element,
            fittingMode: 'fixed',
            margins: { top: 64, bottom: 72, left: 64, right: 64 },
            zoom: { enabled: true, max: opts.maxZoom },
            sequence: _sequence,
            navigator: { enabled: false },
        });
        this.zoom = new ImageView.Controls.Zoom(this.viewer);
        this.rotation = new ImageView.Controls.Rotation(this.viewer);

        // Left/right arrows page the work instead of panning (preventDefaultAction skips OSD's
        // horizontal pan for that key); all other keys keep OSD's native handling.
        this.viewer.openseadragon.addHandler('canvas-key', (e) => {
            const key = e.originalEvent.key;
            if (key === 'ArrowRight') this.next();
            else if (key === 'ArrowLeft') this.prev();
            else return;
            e.preventDefaultAction = true;
            e.originalEvent.preventDefault();
        });

        this._open(this.current).then(() => {
            this._refreshPreload();
            this.onLoaded.emit(this.current);
        });
    }

    // --- search highlights ---

    /** Zeichnet Such-Treffer-Rechtecke (Bildpixel) als Overlays über das aktuelle Bild. */
    setHighlights(rects) {
        this.clearHighlights();
        const osd = this.viewer.openseadragon;
        const item = this.currentItem || osd.world.getItemAt(0);
        if (!item) return;
        this._highlights = (rects || []).map((r) => {
            const el = document.createElement('div');
            el.className = r.active ? 'immersive__hl immersive__hl--active' : 'immersive__hl';
            osd.addOverlay({ element: el, location: item.imageToViewportRectangle(r.x, r.y, r.w, r.h) });
            return el;
        });
    }

    /** Entfernt alle Treffer-Overlays. */
    clearHighlights() {
        const osd = this.viewer.openseadragon;
        (this._highlights || []).forEach((el) => osd.removeOverlay(el));
        this._highlights = [];
    }

    // --- text-region overlays (hover linking, separate from search highlights) ---

    /**
     * Draws OCR line boxes (image pixels) as hoverable, id-tagged overlays and
     * returns a Map id → overlay element. Regions without a `rect` are skipped.
     */
    setTextRegions(regions) {
        this.clearTextRegions();
        const osd = this.viewer.openseadragon;
        const item = this.currentItem || osd.world.getItemAt(0);
        const map = new Map();
        if (!item) return map;
        (regions || []).forEach((r) => {
            if (!r.rect) return;
            const el = document.createElement('div');
            el.className = 'immersive__text-region';
            el.dataset.ivRegionId = r.id;
            osd.addOverlay({ element: el, location: item.imageToViewportRectangle(r.rect.x, r.rect.y, r.rect.w, r.rect.h) });
            map.set(r.id, el);
        });
        this._textRegions = Array.from(map.values());
        return map;
    }

    /** Removes all text-region overlays (leaves search highlights untouched). */
    clearTextRegions() {
        const osd = this.viewer.openseadragon;
        (this._textRegions || []).forEach((el) => osd.removeOverlay(el));
        this._textRegions = [];
    }

    // --- state ---

    getCurrentOrder() {
        return this.current;
    }

    getPageCount() {
        return this.total;
    }

    isDoublePage() {
        return this.double;
    }

    /** 0-based page indices currently displayed (one page, or two in double mode). */
    getCurrentPages() {
        return this.double ? computeSpread(this.current, this.total) : [this.current];
    }

    // --- navigation ---

    /** Navigate to the page/spread containing `order` (snaps to the spread leader in double mode). */
    goToPage(order) {
        const target = Math.max(0, Math.min(order, this.total - 1));
        if (this.double) {
            const leader = computeSpread(target, this.total)[0];
            if (leader === this.current) return;
            this._navigateSpread(leader);
            return;
        }
        if (target === this.current) return;
        this._crossfadeTo(target);
    }

    next() {
        if (this.double) {
            const pages = computeSpread(this.current, this.total);
            this.goToPage(pages[pages.length - 1] + 1);
        } else {
            this.goToPage(this.current + 1);
        }
    }

    prev() {
        this.goToPage(this.current - 1);
    }

    // --- view controls ---

    zoomIn() {
        this.zoom.zoomBy(1.5);
    }

    zoomOut() {
        this.zoom.zoomBy(1 / 1.5);
    }

    rotateLeft() {
        this.rotation.rotateLeft();
    }

    rotateRight() {
        this.rotation.rotateRight();
    }

    /** Resets rotation and zoom to fit the whole page (whole spread in double mode). */
    resetView() {
        this.rotation.rotateTo(0);
        if (this.double) {
            this.viewer.openseadragon.viewport.goHome(true);
        } else {
            this.zoom.goHome();
        }
    }

    /** Toggles book-spread mode and re-opens at the current position. Returns the new state. */
    toggleDoublePage() {
        this.double = !this.double;
        this.current = this.getCurrentPages()[0];
        this._open(this.current);
        return this.double;
    }

    // --- single-page crossfade ---

    /**
     * Single-page navigation: fade the (preloaded or freshly added) target page in
     * over the current one, then drop the old. No reload, no blank, no jump.
     */
    async _crossfadeTo(target) {
        if (this._navigating) return;
        this._navigating = true;
        try {
            const previous = this.currentItem;
            this.current = target;
            const item = await this._acquire(target, this._anchor);
            this._preloaded.delete(target);
            await this._whenContent(item);
            await _crossfade(item, previous, this._fadeMs);
            if (previous) this.viewer.openseadragon.world.removeItem(previous);
            this.currentItem = item;
            this._emit();
            this._refreshPreload();
        } finally {
            this._navigating = false;
        }
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
        const resident = () => residentPages(this.current, this.total, { double: this.double });
        const keep = new Set(resident());
        for (const order of keep) {
            if (order === this.current) continue;
            if (!this._preloaded.has(order)) {
                this._acquire(order, this._anchor);
            }
        }
        for (const [order, p] of this._preloaded) {
            if (!keep.has(order)) {
                p.then((item) => {
                    try {
                        this.viewer.openseadragon.world.removeItem(item);
                    } catch (e) {}
                }).catch(() => {});
                this._preloaded.delete(order);
            }
        }
    }

    // --- double-page spread (snapshot crossfade) ---

    /**
     * Double-page navigation: freeze the current spread as a snapshot overlay, let the
     * library re-compose the new spread (columns:2) underneath, then fade the snapshot
     * out. Neighbour spreads are tile-prewarmed for near-instant sharpness.
     */
    async _navigateSpread(leader) {
        if (this._navigating) return;
        this._navigating = true;
        const overlay = this._snapshotOverlay();
        this.current = leader;
        try {
            await this._open(leader);
            this._fadeOverlay(overlay);
            this._prewarmSpreads();
        } catch (e) {
            if (overlay) overlay.remove();
        } finally {
            this._navigating = false;
        }
    }

    /**
     * Copies the current OSD canvas into an opacity overlay covering the viewer so the
     * fresh load() underneath stays hidden until faded out. Uses canvas drawImage (not
     * toDataURL) so cross-origin IIIF tiles don't taint the canvas.
     */
    _snapshotOverlay() {
        const osd = this.viewer.openseadragon;
        const src = osd.drawer && osd.drawer.canvas;
        const host = this.viewer.element;
        if (!src || !host) return null;
        const overlay = document.createElement('canvas');
        overlay.width = src.width;
        overlay.height = src.height;
        overlay.className = 'immersive__xfade';
        overlay.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:2;';
        try {
            overlay.getContext('2d').drawImage(src, 0, 0);
        } catch (e) {
            return null;
        }
        if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
        host.appendChild(overlay);
        return overlay;
    }

    /** Fades an overlay element out over `_fadeMs`, then removes it. */
    _fadeOverlay(overlay) {
        if (!overlay) return;
        let start = null;
        const dur = this._fadeMs;
        const step = (ts) => {
            if (start === null) start = ts;
            const t = dur <= 0 ? 1 : Math.min(1, (ts - start) / dur);
            overlay.style.opacity = String(1 - t);
            if (t < 1) requestAnimationFrame(step);
            else overlay.remove();
        };
        requestAnimationFrame(step);
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
            osd.addTiledImage({ tileSource: toTileSource(this.services[p]), opacity: 0, preload: true });
        }
    }

    // --- loading ---

    /**
     * Loads the page(s) for `order` via the library (a single page, or a columns:2
     * spread in double mode) and captures the single-page fit anchor. Used for the
     * initial open, mode toggles and every double-page spread change.
     */
    _open(order) {
        const pages = this.double ? computeSpread(order, this.total) : [order];
        this.viewer.config.sequence.columns = pages.length;
        const sources = pages.map((p) => toTileSource(this.services[p]));
        const loaded = this.viewer.load(sources, 0);
        this._prefetchAround(pages[pages.length - 1]);
        return loaded.then(() => {
            const world = this.viewer.openseadragon.world;
            this.currentItem = world.getItemAt(0);
            if (!this.double && this.currentItem) {
                this._anchor = _heightBand(this.currentItem.getBounds());
            }
            this._preloaded.clear();
            this._emit();
        });
    }

    /** Warms neighbour info.json in the browser cache so the next load is faster. */
    _prefetchAround(order) {
        for (let d = 1; d <= PREFETCH_RADIUS; d++) {
            for (const o of [order - d, order + d]) {
                if (o >= 0 && o < this.total) {
                    fetch(toTileSource(this.services[o])).catch(() => {});
                }
            }
        }
    }

    _emit() {
        this.onPageChange.emit(this.current);
    }
}
