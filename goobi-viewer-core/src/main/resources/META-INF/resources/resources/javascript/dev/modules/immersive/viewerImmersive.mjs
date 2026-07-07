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

/**
 * Whether a key-event target sits in a typing context — a form field or a
 * contenteditable region (also via ancestors) — where single-character
 * shortcuts like `?` must not fire. Pure + tested.
 *
 * @param {EventTarget|null} element  e.g. a keydown event's target
 * @returns {boolean}
 */
export function isTypingTarget(element) {
    if (!element || typeof element.closest !== 'function') return false;
    return !!element.closest('input, textarea, select, [contenteditable]:not([contenteditable="false"])');
}

/** Rail panel ids in toolbar order; the Alt+digit shortcuts 1-4 map onto this. */
const PANEL_SHORTCUT_IDS = ['immersivePanelMenu', 'immersivePanelFulltext', 'immersivePanelSearch', 'immersivePanelMetadata'];

/**
 * The rail panel an Alt+digit shortcut toggles: Alt+1-4 address the panels in
 * toolbar order (TOC, fulltext, search, metadata). Matches on `code`, not
 * `key`, because macOS Option+digit produces characters ('¡', '™', …); bare
 * digits are deliberately no shortcut (WCAG 2.1.4 discourages printable
 * single-character shortcuts) and Ctrl/Cmd combinations stay with the browser.
 * Pure + tested.
 *
 * @param {{altKey:boolean, ctrlKey:boolean, metaKey:boolean, code:string}} event
 * @returns {string|null} panel element id, or null
 */
export function panelIdForKeyEvent({ altKey, ctrlKey, metaKey, code }) {
    if (!altKey || ctrlKey || metaKey) return null;
    const index = ['Digit1', 'Digit2', 'Digit3', 'Digit4'].indexOf(code);
    return index === -1 ? null : PANEL_SHORTCUT_IDS[index];
}

/**
 * First visible page as 0-based order (the leading page of a double-page
 * spread), 0 when the viewer has no pages yet. Pure + tested.
 *
 * @param {{getCurrentPages?: Function}} viewer
 * @returns {number}
 */
export function currentOrder(viewer) {
    const pages = viewer.getCurrentPages ? viewer.getCurrentPages() : [];
    return pages.length ? pages[0] : 0;
}

/**
 * OSD viewport margins that keep the fitted page clear of the floating chrome.
 * Desktop reserves generous side margins; small viewports shrink them so the
 * page actually uses the screen (bars stay clear via the top/bottom values:
 * title pill 36px + inset, bottom bar 36-40px + inset). Pure + tested.
 *
 * @param {number} viewportWidth  window.innerWidth at viewer construction
 * @returns {{top:number, bottom:number, left:number, right:number}}
 */
export function stageMargins(viewportWidth) {
    if (viewportWidth <= 768) {
        return { top: 56, bottom: 60, left: 16, right: 16 };
    }
    return { top: 64, bottom: 72, left: 64, right: 64 };
}

/**
 * Whether a platform string names an Apple platform, where modifier keys are
 * conventionally shown as symbols (⌥, ⇧) instead of their PC names.
 * Pure + tested.
 *
 * @param {string} [platform]  navigator.userAgentData?.platform or navigator.platform
 * @returns {boolean}
 */
export function isMacPlatform(platform) {
    return /mac|iphone|ipad|ipod/i.test(platform || '');
}

/**
 * Mac display variant of a modifier key: the Apple symbol plus the key name
 * for the keycap (a bare ⇧ reads like an arrow key next to real ↑/↓ caps),
 * and the spoken name for assistive technology. Symbol and name stay separate
 * so the keycap can flex-center the symbol glyph independently of the text
 * baseline. Null for keys that read the same on every platform. Pure + tested.
 *
 * @param {string} key  lowercase modifier name from a `data-key` attribute
 * @returns {{symbol:string, name:string, label:string}|null}
 */
export function macKeyLabel(key) {
    const labels = {
        alt: { symbol: '⌥', name: 'Option', label: 'Option' },
        shift: { symbol: '⇧', name: 'Shift', label: 'Shift' },
    };
    return labels[key] || null;
}
