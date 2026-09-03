/** Deep-linkable page URLs: history entries per page change, popstate navigation. */

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
 * URL-sync feature: pushes a history entry per new page (deep-linkable,
 * back/forward steps through pages) and navigates the viewer on popstate. An
 * emit for the page already in the URL (initial open, double-page toggle, a
 * popstate-driven change) replaces the entry instead of pushing a duplicate.
 * The viewer uses 0-based page orders; URLs use 1-based page numbers.
 *
 * @param {{onPageChange:{subscribe:Function}, goToPage:Function}} viewer
 * @param {string} pi
 * @returns {{destroy: function():void}}
 */
export function attachUrlSync(viewer, pi) {
    const unsubscribe = viewer.onPageChange.subscribe((order) => {
        const url = new URL(window.location.href);
        url.pathname = pageUrlPath(url.pathname, pi, order + 1);
        if (url.pathname === window.location.pathname) window.history.replaceState({ order }, '', url.toString());
        else window.history.pushState({ order }, '', url.toString());
    });
    const onPopState = (e) => {
        if (e.state && typeof e.state.order === 'number') {
            viewer.goToPage(e.state.order);
        }
    };
    window.addEventListener('popstate', onPopState);
    return {
        destroy() {
            if (typeof unsubscribe === 'function') unsubscribe();
            window.removeEventListener('popstate', onPopState);
        },
    };
}
