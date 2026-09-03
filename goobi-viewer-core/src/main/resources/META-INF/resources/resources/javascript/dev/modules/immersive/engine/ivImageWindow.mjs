/**
 * Pure page/spread window math for the immersive image viewer.
 *
 * No DOM, no network, no OpenSeadragon dependencies — safe to unit-test and
 * to import in any context.
 */

/**
 * Returns the 0-based page indices shown together for the spread that contains
 * `order`. Book layout (LTR): the cover (page 0) stands alone, then pages are
 * paired (1,2),(3,4),… An odd final page stands alone.
 *
 * @param {number} order  0-based page index
 * @param {number} total  total page count
 * @param {{coverAlone?:boolean}} [opts]
 * @returns {number[]} one or two page indices, ascending
 */
export function computeSpread(order, total, { coverAlone = true } = {}) {
    const o = Math.max(0, Math.min(order, total - 1));
    let leader;
    if (coverAlone) {
        if (o === 0) return [0];
        leader = 1 + 2 * Math.floor((o - 1) / 2);
    } else {
        leader = o - (o % 2);
    }
    return leader + 1 <= total - 1 ? [leader, leader + 1] : [leader];
}

/**
 * Page indices that make up the frame containing `order`: just the page in
 * single mode, the spread in double mode.
 *
 * @param {number} order  0-based page index
 * @param {number} total  total page count
 * @param {{double?:boolean}} [opts]
 * @returns {number[]} one or two page indices, ascending
 */
export function framePages(order, total, { double = false } = {}) {
    const o = Math.max(0, Math.min(order, total - 1));
    return double ? computeSpread(o, total) : [o];
}

/**
 * Page indices to keep resident (current frame + the immediately adjacent
 * frames) so neighbour navigation is instant.
 *
 * @param {number} order  0-based page index
 * @param {number} total  total page count
 * @param {{double?:boolean}} [opts]
 * @returns {number[]} sorted, deduplicated page indices
 */
export function residentPages(order, total, { double = false } = {}) {
    const here = framePages(order, total, { double });
    const prev = framePages(here[0] - 1, total, { double });
    const next = framePages(here[here.length - 1] + 1, total, { double });
    return [...new Set([...prev, ...here, ...next])].filter((p) => p >= 0 && p < total).sort((a, b) => a - b);
}
