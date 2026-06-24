import { computeWindow, computeSpread } from './iv_imageWindow.mjs';

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

        this._open(this.current).then(() => this.onLoaded.emit(this.current));
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
        const leader = this.double ? computeSpread(target, this.total)[0] : target;
        if (leader === this.current) return;
        this.current = leader;
        this._open(leader);
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
        return loaded.then(() => this._emit());
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
