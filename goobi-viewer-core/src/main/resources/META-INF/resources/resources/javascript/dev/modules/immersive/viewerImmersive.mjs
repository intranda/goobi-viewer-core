/**
 * Rewrites the page-number segment of an immersive URL path (or appends it).
 * The PI segment is never treated as the page number. Pure + tested.
 *
 * @param {string} pathname  e.g. "/viewer/immersive/PPN123/4/"
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 * @returns {string} rewritten pathname (keeps trailing slash)
 */
export function pageUrlPath(pathname, pi, pageNo) {
    const parts = pathname.split('/');
    for (let i = parts.length - 1; i >= 0; i--) {
        if (/^\d+$/.test(parts[i]) && parts[i] !== String(pi)) {
            parts[i] = String(pageNo);
            return parts.join('/');
        }
    }
    if (parts[parts.length - 1].length === 0) {
        parts[parts.length - 1] = String(pageNo);
        parts.push('');
    } else {
        parts.push(String(pageNo), '');
    }
    return parts.join('/');
}

/**
 * Picks the 1-based page number of the TOC section that should be highlighted.
 * A section owns the page range [its page, next section's page). Returns the
 * current `activeNo` unchanged while any visible page (single page, or either
 * page of a double-page spread) still falls in its range, so the highlight is
 * kept; otherwise the section owning the last visible page, or null when no
 * section starts at or before it. Pure + tested.
 *
 * @param {number[]} entryNos      1-based start pages of all TOC sections
 * @param {number[]} visiblePages  1-based page numbers currently shown
 * @param {number|null} activeNo   start page of the currently highlighted section
 * @returns {number|null}
 */
export function pickActiveTocPageNo(entryNos, visiblePages, activeNo = null) {
    if (!entryNos.length || !visiblePages.length) return activeNo;
    if (activeNo !== null) {
        const nextNo = Math.min(...entryNos.filter((n) => n > activeNo), Infinity);
        if (visiblePages.some((p) => p >= activeNo && p < nextNo)) return activeNo;
    }
    const top = Math.max(...visiblePages);
    let best = null;
    for (const no of entryNos) {
        if (no <= top && (best === null || no >= best)) best = no;
    }
    return best;
}

/**
 * URL-sync feature: pushes a history entry on page change (deep-linkable,
 * back/forward steps through pages) and navigates the viewer on popstate.
 * The viewer uses 0-based page orders; URLs use 1-based page numbers.
 *
 * @param {{onPageChange:{subscribe:Function}, goToPage:Function}} viewer
 * @param {string} pi
 */
export function attachUrlSync(viewer, pi) {
    viewer.onPageChange.subscribe((order) => {
        const url = new URL(window.location.href);
        url.pathname = pageUrlPath(url.pathname, pi, order + 1);
        window.history.pushState({ order }, '', url.toString());
    });
    window.addEventListener('popstate', (e) => {
        if (e.state && typeof e.state.order === 'number') {
            viewer.goToPage(e.state.order);
        }
    });
}

export const THUMB_SIZE_KEY = 'immersive-thumb-size';

export const THUMB_SIZE_MAX = 2;

const THUMB_SIZE_DEFAULT = 1;

/**
 * Normalizes a stored thumbnail-size value to a valid grid step. Values are
 * parsed with parseInt, so leading-numeric strings are accepted; anything
 * else falls back to the default (medium). Pure + tested.
 *
 * @param {*} value
 * @returns {number} integer step between 0 and THUMB_SIZE_MAX
 */
export function normalizeThumbSizeStep(value) {
    const n = parseInt(value, 10);
    return Number.isInteger(n) && n >= 0 && n <= THUMB_SIZE_MAX ? n : THUMB_SIZE_DEFAULT;
}

/**
 * Reads the persisted thumbnail-size step. Falls back to the default when
 * the storage operations throw (private mode) or the value is invalid.
 *
 * @param {Storage} storage  e.g. window.localStorage
 * @returns {number} integer step between 0 and THUMB_SIZE_MAX
 */
export function readThumbSizeStep(storage) {
    try {
        return normalizeThumbSizeStep(storage.getItem(THUMB_SIZE_KEY));
    } catch {
        return THUMB_SIZE_DEFAULT;
    }
}

/**
 * Persists the thumbnail-size step; invalid steps are normalized. Storage
 * errors (private mode, quota) are swallowed.
 *
 * @param {Storage} storage
 * @param {number|string} step
 */
export function writeThumbSizeStep(storage, step) {
    try {
        storage.setItem(THUMB_SIZE_KEY, String(normalizeThumbSizeStep(step)));
    } catch {}
}
