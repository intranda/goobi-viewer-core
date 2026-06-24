import { computeWindow, computeSpread, residentPages } from './iv_imageWindow.mjs';

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

/** Animates opacity of OSD TiledImages from 0→1 (incoming) and 1→0 (outgoing) over durationMs via rAF. */
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
 * Widens a bounds rect into a tall band: same y and height, 3x width, same centre.
 * Used as a fitBounds anchor so OSD constrains the image by HEIGHT (the band is wide
 * enough that width never constrains) — pages of differing scan aspect then render at
 * a uniform height, vertically centred, so paging never jumps vertically.
 */
function _heightBand(rect) {
    const band = rect.clone();
    band.x = rect.x - rect.width;
    band.width = rect.width * 3;
    return band;
}

/**
 * Pure navigation decision: given a target page index, the currently loaded
 * window {start,end} (end exclusive) and the total page count, decide whether a
 * new window must be loaded into OSD and the in-window index of the (clamped)
 * target. When the target already sits in the loaded window we navigate in
 * place (no reload, no flicker); only crossing the window edge reloads.
 *
 * @returns {{reload:boolean, indexInWindow:number, window?:{start:number,end:number}}}
 */
export function resolveNavigation(targetOrder, win, total, windowSize) {
    const target = Math.max(0, Math.min(targetOrder, total - 1));
    if (target >= win.start && target < win.end) {
        return { reload: false, indexInWindow: target - win.start };
    }
    const next = computeWindow(target, total, windowSize);
    return { reload: true, window: { start: next.start, end: next.end }, indexInWindow: next.indexInWindow };
}

// Sequence config mirrors the proven single-image path in zoomableImage.mjs
// (getSequenceSettings('single')). _arrangeImageSequence reads it on every open,
// so it must be present even though we load one image at a time.
const _sequence = { columns: 1, useWindowing: true, windowSize: 100, windowExpandThreshold: 10, windowExpandSize: 50 };
const PREFETCH_RADIUS = 1;

/**
 * Immersive image viewer engine. Wraps a single live ImageView.Image instance
 * and swaps the displayed page IN-PLACE on navigation (no JSF page reload),
 * reusing OSD's proven single-image contain-fit. Neighbours are prefetched so
 * the next swap is fast. Emits page changes; knows nothing about buttons/URLs/overlays.
 *
 * NOTE: a windowed multi-image variant (load a sliding window once, jump in place
 * via setCurrentImage + goHome) was prototyped to also remove the brief tile load
 * between pages. It was reverted: ImageView's 'fixed' (contain-fit) mode is built
 * for a single image, and its goHome fit math (ZoomControls 'fixed' branch handles
 * only bottom/right margins) over-zooms non-first images in the stacked multi-image
 * world, breaking the fit. The pure window math (`resolveNavigation`) is kept and
 * tested for a future iteration that adds custom per-page fitBounds with margin insets.
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
        this.double = false; // double-page (book spread) mode
        this._preloaded = new Map(); // page order -> Promise<TiledImage> (in-flight or settled)
        // crossfade duration; honour prefers-reduced-motion (no fade, but still a
        // preloaded -> flicker-free hard swap)
        this._fadeMs = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 160;
        this._navigating = false; // guard against overlapping crossfades
        this.currentItem = null; // the visible single-page TiledImage (single mode)
        // Constant fit anchor captured from the initial load-based (correctly fit) page.
        // Every crossfaded page is fit into the SAME anchor so pages of slightly different
        // scan size stay centered at the same position (no jump).
        this._anchor = null; // OpenSeadragon.Rect for single-page fit
        this.onPageChange = new Emitter();
        this.onLoaded = new Emitter();

        this.viewer = new ImageView.Image({
            element: opts.element,
            // 'fixed' = fit the whole image into the (full-bleed) viewport, like the
            // fullscreen view. 'toWidth' would grow the canvas height and blow the
            // image up in this wide container.
            fittingMode: 'fixed',
            // Inset the image so it never sits under the overlay chrome: the page is
            // centered on the dark stage with margins for the title (top), bottom bar,
            // and the left/right icon rails - this is what makes it read as a framed
            // viewer area rather than an image bleeding to the edges.
            margins: { top: 64, bottom: 72, left: 64, right: 64 },
            zoom: { enabled: true, max: opts.maxZoom },
            sequence: _sequence,
            navigator: { enabled: false },
        });
        this.zoom = new ImageView.Controls.Zoom(this.viewer);
        this.rotation = new ImageView.Controls.Rotation(this.viewer);

        this._open(this.current).then(() => {
            this._refreshPreload();
            this.onLoaded.emit(this.current);
        });
    }

    getCurrentOrder() {
        return this.current;
    }
    getPageCount() {
        return this.total;
    }
    isDoublePage() {
        return this.double;
    }

    /** Returns the 0-based page indices currently displayed (1 or 2). */
    getCurrentPages() {
        return this.double ? computeSpread(this.current, this.total) : [this.current];
    }

    /**
     * Toggles double-page mode and re-opens the spread/page for the current
     * position. Returns the new state.
     */
    toggleDoublePage() {
        this.double = !this.double;
        this.current = this.getCurrentPages()[0]; // snap to spread leader (or keep page in single mode)
        this._open(this.current);
        return this.double;
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
        this.goToPage(this.current - 1); // -1 lands in the previous spread; goToPage snaps to its leader
    }

    // --- image controls (delegated to the OSD wrapper) ---
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
    resetView() {
        this.rotation.rotateTo(0);
        if (this.double) {
            this.viewer.openseadragon.viewport.goHome(true);
        } else {
            this.zoom.goHome();
        }
    }

    /**
     * Navigate to the spread/page containing `order`. In double-page mode the
     * target snaps to the spread leader so paging is spread-by-spread.
     * @param {number} order 0-based page index
     */
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

    /**
     * Double-page spread navigation, flicker-free without hand-placing images:
     * freeze the current spread as a pixel snapshot overlay, let the library
     * re-compose the new spread cleanly (columns:2) underneath, then fade the
     * snapshot out. Neighbour spreads are tile-prewarmed so the new spread is
     * sharp almost immediately.
     */
    async _navigateSpread(leader) {
        if (this._navigating) return;
        this._navigating = true;
        const overlay = this._snapshotOverlay();
        this.current = leader;
        try {
            await this._open(leader); // resolves on first tile of the recomposed spread
            this._fadeOverlay(overlay);
            this._prewarmSpreads();
        } catch (e) {
            if (overlay) overlay.remove();
        } finally {
            this._navigating = false;
        }
    }

    /**
     * Copies the current OSD canvas into an opacity overlay covering the viewer,
     * so a fresh load() underneath stays hidden until the overlay is faded out.
     * Uses canvas drawImage (not toDataURL) so cross-origin IIIF tiles don't taint.
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

    /** Fades an overlay element to transparent over _fadeMs, then removes it. */
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
     * Warms OSD's tile cache for the adjacent spreads (hidden preloaded images)
     * so the next spread's load() renders almost immediately. The temp images are
     * invisible and get cleared by the next open(); their tiles stay cached.
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

    /**
     * Returns a cached promise for the TiledImage of `order`, adding it as a
     * hidden, preloaded image (coincident via fitBounds) if not already present
     * or in-flight. The promise is stored in _preloaded keyed by order, so an
     * in-flight preload and an on-demand navigation share ONE world image (no
     * duplicate). Resolves as soon as the image is ADDED (not when fully loaded).
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

    /**
     * Resolves when the tiled image has rendered its first (low-res) tile, or is
     * already fully loaded — so we crossfade to visible content (never to blank)
     * without waiting for the full-resolution load.
     */
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

    /**
     * Single-page navigation via crossfade: ensure the target image (preloaded or
     * freshly added, coincident), fade it in while fading the current out, remove
     * the old. No reload, no blank, no positional jump.
     */
    async _crossfadeTo(target) {
        if (this._navigating) return;
        this._navigating = true;
        try {
            const previous = this.currentItem;
            this.current = target;
            const item = await this._acquire(target, this._anchor);
            this._preloaded.delete(target); // it is becoming the visible page
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
     * Keeps the resident neighbour pages (±1 frame) preloaded as hidden coincident
     * images and evicts everything else, so neighbour paging is instant and memory
     * stays bounded. Single-page (crossfade) path only.
     */
    _refreshPreload() {
        const resident = () => residentPages(this.current, this.total, { double: this.double });
        const keep = new Set(resident());
        for (const order of keep) {
            if (order === this.current) continue;
            if (!this._preloaded.has(order)) {
                this._acquire(order, this._anchor); // fire-and-forget; same constant anchor
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

    /**
     * Loads the page(s) for `order`. In double-page mode this loads the spread
     * (1–2 pages) the order belongs to and arranges them side by side via the
     * library's column layout; the column-aware Extent + homeFillsViewer fit the
     * whole spread (margins like single mode). Single mode loads one page.
     * The viewer instance stays alive → in-place swap, no page reload.
     */
    _open(order) {
        const pages = this.double ? computeSpread(order, this.total) : [order];
        // columns is read at open time by _arrangeImageSequence + Extent.
        this.viewer.config.sequence.columns = pages.length;
        const sources = pages.map((p) => toTileSource(this.services[p]));
        const loaded = this.viewer.load(sources, 0);
        this._prefetchAround(pages[pages.length - 1]);
        return loaded.then(() => {
            const world = this.viewer.openseadragon.world;
            this.currentItem = world.getItemAt(0);
            // single-page constant height-band anchor so crossfaded pages keep a
            // uniform height / stable vertical position (see _heightBand)
            if (!this.double && this.currentItem) {
                this._anchor = _heightBand(this.currentItem.getBounds());
            }
            this._preloaded.clear();
            this._emit();
        });
    }

    /** Warms neighbour info.json in the browser cache so the next swap is fast. */
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
