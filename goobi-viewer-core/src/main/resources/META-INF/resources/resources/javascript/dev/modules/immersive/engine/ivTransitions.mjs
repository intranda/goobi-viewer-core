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
