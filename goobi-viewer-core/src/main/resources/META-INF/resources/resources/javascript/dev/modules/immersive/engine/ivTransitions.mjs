/** Frame-based transitions for the stage: tweens, tile crossfades, the frozen snapshot overlay and the height-fit band. */

/**
 * Runs a rAF tween over `durationMs`, calling `onFrame` with the progress 0→1
 * each frame (a non-positive duration jumps straight to 1). Resolves when done.
 *
 * @param {number} durationMs
 * @param {(t: number) => void} onFrame
 * @returns {Promise<void>}
 */
export function tween(durationMs, onFrame) {
    return new Promise((resolve) => {
        let start = null;
        const step = (ts) => {
            if (start === null) start = ts;
            const t = durationMs <= 0 ? 1 : Math.min(1, (ts - start) / durationMs);
            onFrame(t);
            if (t < 1) requestAnimationFrame(step);
            else resolve();
        };
        requestAnimationFrame(step);
    });
}

/** Tweens one TiledImage's opacity 0→1 while fading another 1→0. */
export function crossfade(incoming, outgoing, durationMs) {
    return tween(durationMs, (t) => {
        if (incoming) incoming.setOpacity(t);
        if (outgoing) outgoing.setOpacity(1 - t);
    });
}

/**
 * The current view expressed relative to the page row, so it survives a reload
 * that rebuilds the world: `zoom` is the magnification per row width and `x`/`y`
 * are the centre as a fraction of the row. Null whenever there is nothing
 * meaningful to keep. Pure + tested.
 *
 * @param {{x:number, y:number, width:number, height:number}|null} row  union bounds of the displayed pages
 * @param {{x:number, y:number}|null} center  viewport centre (the animation target, not the current frame)
 * @param {number} zoom  viewport zoom (likewise the target)
 * @param {number} pages  number of pages the row is made of
 * @returns {{zoom:number, x:number, y:number, pages:number}|null}
 */
export function captureView(row, center, zoom, pages) {
    if (!row || !center || !(row.width > 0) || !(row.height > 0) || !(zoom > 0) || !(pages > 0)) {
        return null;
    }
    return { zoom: zoom * row.width, x: (center.x - row.x) / row.width, y: (center.y - row.y) / row.height, pages };
}

/**
 * The zoom and centre a captured view maps to on the new row, or null when it
 * must not be applied: without a view, without a usable row, or when the row is
 * made of a different number of pages - a spread and a single page want their
 * own fit, not the same magnification. Pure + tested.
 *
 * @param {{zoom:number, x:number, y:number, pages:number}|null} view
 * @param {{x:number, y:number, width:number, height:number}|null} row
 * @param {number} pages  number of pages the new row is made of
 * @returns {{zoom:number, center:{x:number, y:number}}|null}
 */
export function restoredView(view, row, pages) {
    if (!view || !row || !(row.width > 0) || !(row.height > 0) || view.pages !== pages) {
        return null;
    }
    return { zoom: view.zoom / row.width, center: { x: row.x + view.x * row.width, y: row.y + view.y * row.height } };
}

/**
 * Widens a bounds rect to a tall band (same y/height, 3x width, same centre).
 * As a fitBounds anchor this makes OSD fit by height, so pages of differing scan
 * aspect render at a uniform height and paging never jumps vertically.
 */
export function heightBand(rect) {
    const band = rect.clone();
    band.x = rect.x - rect.width;
    band.width = rect.width * 3;
    return band;
}

/**
 * Copies the current OSD canvas into an opacity overlay covering `host`, so a
 * fresh load underneath stays hidden until faded out. Uses drawImage (not
 * toDataURL) so cross-origin IIIF tiles do not taint the canvas.
 *
 * @returns {HTMLCanvasElement|null}
 */
export function snapshotOverlay(osd, host) {
    const src = osd.drawer && osd.drawer.canvas;
    if (!src || !host) return null;
    const overlay = document.createElement('canvas');
    overlay.width = src.width;
    overlay.height = src.height;
    overlay.className = 'immersive__xfade';
    try {
        overlay.getContext('2d').drawImage(src, 0, 0);
    } catch {
        return null;
    }
    if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
    host.appendChild(overlay);
    return overlay;
}

/** Fades an overlay element out over `durationMs`, then removes it. */
export function fadeOverlay(overlay, durationMs) {
    if (!overlay) return Promise.resolve();
    return tween(durationMs, (t) => {
        overlay.style.opacity = String(1 - t);
    }).then(() => overlay.remove());
}
