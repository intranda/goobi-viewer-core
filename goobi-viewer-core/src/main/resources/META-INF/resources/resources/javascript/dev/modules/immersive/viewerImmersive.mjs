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
