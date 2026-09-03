/** DOM helpers shared by the wiring modules: geometry, class tokens, element building. */

/**
 * Computes the scrollTop needed to center a line within its scroll container,
 * or null if the line is already fully visible. Pure (numbers only) so it is
 * testable without a layout engine.
 *
 * @param {{offsetTop:number, height:number, scrollTop:number, clientHeight:number, scrollHeight:number}} metrics
 * @returns {number|null}
 */
export function scrollTopToReveal(metrics) {
    const top = metrics.offsetTop;
    const bottom = metrics.offsetTop + metrics.height;
    if (top >= metrics.scrollTop && bottom <= metrics.scrollTop + metrics.clientHeight) return null;
    const target = metrics.offsetTop - metrics.clientHeight / 2 + metrics.height / 2;
    const max = Math.max(0, metrics.scrollHeight - metrics.clientHeight);
    return Math.min(Math.max(0, target), max);
}

/**
 * Cumulative offsetTop of `el` relative to `container` (walks the offsetParent
 * chain), for scrolling panel content into view without moving the page.
 *
 * @param {HTMLElement} el
 * @param {HTMLElement} container
 * @returns {number}
 */
export function offsetTopWithin(el, container) {
    let top = 0;
    for (let n = el; n && n !== container; n = n.offsetParent) top += n.offsetTop;
    return top;
}

/**
 * Splits a configured styleClass attribute into class tokens; blank or missing
 * values yield an empty list. Pure + tested.
 *
 * @param {string} [styleClass]  the configured CSS class(es) of an author mapping
 * @returns {string[]}
 */
export function styleClassTokens(styleClass) {
    return String(styleClass || '')
        .split(/\s+/)
        .filter(Boolean);
}

/**
 * Creates an element with optional class list and text content.
 *
 * @param {string} tag
 * @param {string} [cls]
 * @param {string} [text]
 * @returns {HTMLElement}
 */
export function el(tag, cls, text) {
    const node = document.createElement(tag);
    if (cls) node.className = cls;
    if (text != null) node.textContent = text;
    return node;
}
