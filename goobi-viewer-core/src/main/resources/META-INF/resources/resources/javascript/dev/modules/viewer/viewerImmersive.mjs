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
