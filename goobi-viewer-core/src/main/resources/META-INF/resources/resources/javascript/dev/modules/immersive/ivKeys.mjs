/** Central keydown dispatcher for the immersive view. */

import { isTypingTarget } from './viewerImmersive.mjs';

/** Overlays/panels/popovers that own the arrow and zoom keys while open. */
const OVERLAY_OPEN_SELECTOR =
    '#immersiveGridOverlay:not([hidden]), #immersiveShortcuts:not([hidden]), #immersivePageDropdown:not([hidden]), .immersive__panel-left.is-open, .popover.show';

/**
 * Creates the immersive key dispatcher: one document-level keydown listener,
 * handlers consulted in ascending priority order, the first handler that
 * returns true consumes the event. This keeps the overlay precedence (modal →
 * grid → dropdown → panels) declared in one place instead of guard selectors
 * scattered over independent listeners. Pure + tested.
 *
 * @returns {{register:Function, handleEvent:Function, attach:Function}}
 */
export function createKeyDispatcher() {
    const handlers = [];
    return {
        /**
         * @param {number} priority  lower runs first
         * @param {(event: KeyboardEvent) => boolean} handler  true = consumed
         */
        register(priority, handler) {
            handlers.push({ priority, handler });
            handlers.sort((a, b) => a.priority - b.priority);
        },
        /** @returns {boolean} whether any handler consumed the event */
        handleEvent(event) {
            for (const { handler } of handlers) {
                if (handler(event)) return true;
            }
            return false;
        },
        /** Binds the single keydown listener. */
        attach(target) {
            target.addEventListener('keydown', (e) => {
                if (this.handleEvent(e)) e.preventDefault();
            });
        },
    };
}

/**
 * Registers the viewer navigation shortcuts (page Left/Right, reset 0, zoom
 * +/- and Shift+Up/Down, rotate r / Shift+R) on the shared dispatcher so they
 * work without first clicking the OSD stage -- the same keys the help modal
 * advertises. Bails when the user is typing, when an overlay/panel/popover is
 * open, when Ctrl/Meta/Alt is held (browser zoom, Alt+digit panels), or when
 * the stage itself has focus -- then OSD's own canvas-key handler owns the
 * keys, so nothing fires twice. Zoom keys are ignored when the record forbids
 * zooming; rotation stays available. Plain Up/Down stay with OSD (image
 * panning) and page scroll.
 *
 * @param {{register: Function}} keys  the immersive key dispatcher
 * @param {IvViewer} viewer
 * @param {HTMLElement} stageEl        the OSD mount element ([data-immersive-image])
 * @param {number} [priority=50]       runs after modal (10), grid (20), dropdown (30), panels (40)
 */
export function registerViewerNavKeys(keys, viewer, stageEl, priority = 50) {
    keys.register(priority, (e) => {
        if (e.ctrlKey || e.metaKey || e.altKey) return false;
        const key = e.key;
        const zoomIn = key === '+' || key === '=' || (e.shiftKey && key === 'ArrowUp');
        const zoomOut = key === '-' || key === '_' || (e.shiftKey && key === 'ArrowDown');
        const prev = !e.shiftKey && key === 'ArrowLeft';
        const next = !e.shiftKey && key === 'ArrowRight';
        const reset = key === '0';
        const rotateCW = key === 'r';
        const rotateCCW = key === 'R';
        if (!zoomIn && !zoomOut && !prev && !next && !reset && !rotateCW && !rotateCCW) return false;
        if (isTypingTarget(e.target)) return false;
        if (stageEl && stageEl.contains(document.activeElement)) return false;
        if (document.querySelector(OVERLAY_OPEN_SELECTOR)) return false;
        if ((zoomIn || zoomOut) && !viewer.allowZoom) return false;
        if (prev) viewer.prev();
        else if (next) viewer.next();
        else if (reset) viewer.resetView();
        else if (rotateCW) viewer.rotateRight();
        else if (rotateCCW) viewer.rotateLeft();
        else if (zoomIn) viewer.zoomIn();
        else viewer.zoomOut();
        return true;
    });
}
