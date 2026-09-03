/** Shared DOM contract: which immersive overlays and panels count as open for the keyboard guards. */

/** Grid, shortcuts modal, page dropdown and Bootstrap popovers; they swallow single-key shortcuts while open. */
export const OVERLAY_OPEN_SELECTOR = '#immersiveGridOverlay:not([hidden]), #immersiveShortcuts:not([hidden]), #immersivePageDropdown:not([hidden]), .popover.show';

/** An open left rail panel. */
export const PANEL_OPEN_SELECTOR = '.immersive__panel-left.is-open';

/**
 * Whether an immersive overlay is open. With `panels: true` an open left panel
 * counts as well (arrow and zoom keys, the `?` shortcut); without, panels are
 * ignored (Alt+digit toggles, the annotations key inside the fulltext panel).
 *
 * @param {{panels?: boolean}} [options]
 * @returns {boolean}
 */
export function isOverlayOpen({ panels = false } = {}) {
    return !!document.querySelector(panels ? `${OVERLAY_OPEN_SELECTOR}, ${PANEL_OPEN_SELECTOR}` : OVERLAY_OPEN_SELECTOR);
}

/** The OpenSeadragon mount; while it holds focus, OSD owns the single-letter keys (WASD panning). */
export const STAGE_SELECTOR = '[data-immersive-image]';

/** @returns {boolean} whether keyboard focus sits inside the image stage */
export function isStageFocused() {
    const stage = document.querySelector(STAGE_SELECTOR);
    return !!stage && stage.contains(document.activeElement);
}
