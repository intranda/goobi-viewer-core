/** Overlay layers on the OpenSeadragon world: highlight sets per owner, OCR text regions and drawn annotation regions. */

export class IvOverlays {
    /**
     * @param {object} opts
     * @param {() => object} opts.osd          the live OpenSeadragon instance
     * @param {(order: number) => object|null} opts.itemFor  TiledImage showing a page order, or null
     * @param {() => object|null} opts.currentItem  the TiledImage of the current page
     */
    constructor({ osd, itemFor, currentItem }) {
        this._osd = osd;
        this._itemFor = itemFor;
        this._currentItem = currentItem;
        this._highlights = new Map();
        this._textRegions = [];
        this._annoRegions = [];
    }

    _place(el, item, rect) {
        this._osd().addOverlay({ element: el, location: item.imageToViewportRectangle(rect.x, rect.y, rect.w, rect.h) });
    }

    _drop(els) {
        const osd = this._osd();
        els.forEach((el) => osd.removeOverlay(el));
    }

    /**
     * Draws highlight rectangles for `owner`; every owner keeps its own set, so one
     * panel never wipes another's marks. Rect coordinates are image pixels of the
     * page given by `rect.order`; rects for pages outside the current view are
     * skipped. An optional `className` per rect is appended.
     */
    setHighlights(rects, owner = 'default') {
        this.clearHighlights(owner);
        const els = (rects || []).flatMap((r) => {
            const item = this._itemFor(r.order);
            if (!item) return [];
            const el = document.createElement('div');
            el.className = ['immersive__highlight', r.active ? '-active' : '', r.className || ''].filter(Boolean).join(' ');
            this._place(el, item, r);
            return [el];
        });
        this._highlights.set(owner, els);
    }

    /** Removes the highlights of `owner`, or of every owner when omitted. */
    clearHighlights(owner) {
        const owners = owner === undefined ? Array.from(this._highlights.keys()) : [owner];
        owners.forEach((key) => {
            this._drop(this._highlights.get(key) || []);
            this._highlights.delete(key);
        });
    }

    /**
     * Draws OCR boxes as hoverable, id-tagged overlays and returns a Map id → element.
     * A region's `level` ('block' | 'line' | 'word') becomes a class modifier; pass
     * regions block → line → word so words stack on top and win pointer hits. Every
     * region is drawn and cleared, even with a duplicate id; the map keeps the topmost (last) element per id.
     */
    setTextRegions(regions) {
        this.clearTextRegions();
        const item = this._currentItem();
        const map = new Map();
        if (!item) return map;
        (regions || []).forEach((r) => {
            if (!r.rect) return;
            const el = document.createElement('div');
            el.className = r.level ? `immersive__text-region immersive__text-region-${r.level}` : 'immersive__text-region';
            el.dataset.ivRegionId = r.id;
            this._place(el, item, r.rect);
            this._textRegions.push(el);
            map.set(r.id, el);
        });
        return map;
    }

    clearTextRegions() {
        this._drop(this._textRegions);
        this._textRegions = [];
    }

    /** Draws ALTO layout annotations (drawn lines, note areas) as overlays; each region's `modifier` becomes a class suffix. */
    setAnnotationRegions(regions) {
        this.clearAnnotationRegions();
        const item = this._currentItem();
        const map = new Map();
        if (!item) return map;
        (regions || []).forEach((r) => {
            if (!r.rect) return;
            const el = document.createElement('div');
            el.className = ['immersive__anno-region', r.modifier ? `immersive__anno-region-${r.modifier}` : '', r.className || ''].filter(Boolean).join(' ');
            el.dataset.ivAnnoId = r.id;
            this._place(el, item, r.rect);
            this._annoRegions.push(el);
            map.set(r.id, el);
        });
        return map;
    }

    clearAnnotationRegions() {
        this._drop(this._annoRegions);
        this._annoRegions = [];
    }

    destroy() {
        this.clearHighlights();
        this.clearTextRegions();
        this.clearAnnotationRegions();
    }
}
