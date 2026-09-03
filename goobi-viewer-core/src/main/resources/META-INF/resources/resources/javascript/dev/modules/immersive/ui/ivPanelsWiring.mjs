/** Left rail panel wiring: open/close, keyboard toggles, resize, TOC + metadata folds. */

import { offsetTopWithin } from '../shared/ivDom.mjs';
import { isOverlayOpen, PANEL_OPEN_SELECTOR } from '../shared/ivSelectors.mjs';
import { env } from '../shared/ivEnv.mjs';
import { createDisposer, noop } from '../shared/ivLifecycle.mjs';

/** Rail panel ids in toolbar order; the Alt+digit shortcuts 1-5 map onto this. */
const PANEL_SHORTCUT_IDS = ['immersivePanelToc', 'immersivePanelFulltext', 'immersivePanelSearch', 'immersivePanelMetadata', 'immersivePanelLayoutTags'];

/**
 * The rail panel an Alt+digit shortcut toggles: Alt+1-5 address the panels in
 * toolbar order (TOC, fulltext, search, metadata, layout tags). Matches on
 * `code`, not `key`, because macOS Option+digit produces characters ('¡', '™',
 * …); bare digits are deliberately no shortcut (WCAG 2.1.4 discourages
 * printable single-character shortcuts) and Ctrl/Cmd combinations stay with
 * the browser. Pure + tested.
 *
 * @param {{altKey:boolean, ctrlKey:boolean, metaKey:boolean, code:string}} event
 * @returns {string|null} panel element id, or null
 */
export function panelIdForKeyEvent({ altKey, ctrlKey, metaKey, code }) {
    if (!altKey || ctrlKey || metaKey) return null;
    const index = ['Digit1', 'Digit2', 'Digit3', 'Digit4', 'Digit5'].indexOf(code);
    return index === -1 ? null : PANEL_SHORTCUT_IDS[index];
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
 * Wires the rail buttons to their left panels (open/close with ARIA + inert
 * bookkeeping) and registers the panel keyboard shortcuts on the dispatcher:
 * Escape closes the open panel, Alt+1-5 toggles the panels in rail order. On
 * open, focus moves to the panel's [data-immersive-panel-focus] element if it
 * declares one, else to its first visible control.
 *
 * @param {HTMLElement|null} immersiveRoot  the .immersive root element
 * @param {{register: Function}} keys  the immersive key dispatcher
 * @returns {{closePanels: Function, registerCloseHook: Function, replayOpen: Function, destroy: Function}}
 */
export function setupPanels(immersiveRoot, keys) {
    const bag = createDisposer();
    const panelButtons = document.querySelectorAll('[data-immersive-panel]');
    document.querySelectorAll('.immersive__panel-left').forEach((p) => (p.inert = true));
    const syncPanelOpenFlag = () => {
        if (immersiveRoot) {
            immersiveRoot.classList.toggle('-panel-open', !!document.querySelector('.immersive__panel-left.is-open'));
        }
    };
    const closeHooks = [];
    let activePanelBtn = null;
    const closePanels = (restoreFocus = false) => {
        closeHooks.forEach((hook) => hook());
        const closedAny = !!document.querySelector('.immersive__panel-left.is-open');
        document.querySelectorAll('.immersive__panel-left.is-open').forEach((p) => {
            p.classList.remove('is-open');
            p.setAttribute('aria-hidden', 'true');
            p.inert = true;
        });
        panelButtons.forEach((b) => {
            b.classList.remove('-active');
            b.setAttribute('aria-expanded', 'false');
        });
        syncPanelOpenFlag();
        if (restoreFocus && closedAny && activePanelBtn) activePanelBtn.focus();
        activePanelBtn = null;
    };
    panelButtons.forEach((btn) => {
        bag.listen(btn, 'click', () => {
            if (btn.getAttribute('aria-disabled') === 'true') return;
            const panel = document.getElementById(btn.dataset.immersivePanel);
            if (!panel) return;
            const wasOpen = panel.classList.contains('is-open');
            closePanels();
            if (!wasOpen) {
                panel.classList.add('is-open');
                panel.setAttribute('aria-hidden', 'false');
                panel.inert = false;
                btn.classList.add('-active');
                btn.setAttribute('aria-expanded', 'true');
                activePanelBtn = btn;
                syncPanelOpenFlag();
                panel.dispatchEvent(new CustomEvent('immersive:panel-open', { bubbles: false }));
                // Scroll via offset math, not scrollIntoView, so the page itself never moves.
                const active = panel.querySelector('.widget-toc__element.active');
                if (active) {
                    panel.scrollTop = Math.max(0, offsetTopWithin(active, panel) - panel.clientHeight / 2);
                }
                const isMetadataPanel = panel.id === 'immersivePanelMetadata';
                const focusTarget = isMetadataPanel
                    ? panel.querySelector('.immersive__panel-title')
                    : panel.querySelector('[data-immersive-panel-focus]') ||
                      Array.from(panel.querySelectorAll('input, a[href], button')).find((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
                if (focusTarget) {
                    if (!focusTarget.hasAttribute('tabindex')) focusTarget.setAttribute('tabindex', '-1');
                    focusTarget.focus({ preventScroll: true });
                } else {
                    if (!panel.hasAttribute('tabindex')) panel.setAttribute('tabindex', '-1');
                    panel.focus({ preventScroll: true });
                }
            }
        });
    });

    const unregisterKeys = keys.register(40, (e) => {
        if (e.key === 'Escape') {
            if (!document.querySelector('.immersive__panel-left.is-open')) return false;
            closePanels(true);
            return true;
        }
        const panelId = panelIdForKeyEvent(e);
        if (!panelId || isOverlayOpen()) return false;
        const btn = document.querySelector(`[data-immersive-panel="${panelId}"]`);
        if (!btn) return false;
        e.preventDefault();
        btn.click();
        return true;
    });
    bag.add(unregisterKeys);

    return {
        closePanels,
        /** Registers a hook that runs whenever the panels close (e.g. fulltext teardown); returns its unregister. */
        registerCloseHook(hook) {
            if (typeof hook !== 'function') return () => {};
            closeHooks.push(hook);
            return () => {
                const i = closeHooks.indexOf(hook);
                if (i !== -1) closeHooks.splice(i, 1);
            };
        },
        /** Re-sends the open event for an already open panel, for modules that were wired after the click. */
        replayOpen() {
            const open = document.querySelector(PANEL_OPEN_SELECTOR);
            if (open) open.dispatchEvent(new CustomEvent('immersive:panel-open', { bubbles: false }));
        },
        destroy: bag.dispose,
    };
}

/**
 * Adds the drag handle that resizes the left panels: one shared
 * --immersive-panel-width custom property, persisted in sessionStorage.
 *
 * @returns {{destroy: function():void}}
 */
export function setupPanelResize(immersiveRoot) {
    const immersiveViewer = immersiveRoot && immersiveRoot.querySelector('.immersive__viewer');
    if (!immersiveViewer) return noop;
    const WIDTH_KEY = 'immersive-panel-width';
    const RAIL_WIDTH = 40;
    const MIN_WIDTH = 240;
    const MAX_WIDTH_RATIO = 0.8;
    const maxWidth = () => immersiveViewer.getBoundingClientRect().width * MAX_WIDTH_RATIO;
    // The handle is positioned by the width variable, not the panel edge — so the
    // clamp floor must include the open panel's own min-width (raised in the
    // annotations two-column mode), or the handle would detach from the panel.
    const minWidth = () => {
        const open = immersiveRoot.querySelector('.immersive__panel-left.is-open');
        const panelMin = open ? parseFloat(getComputedStyle(open).minWidth) || 0 : 0;
        return Math.max(MIN_WIDTH, panelMin);
    };
    const clamp = (px) => Math.min(Math.max(px, minWidth()), maxWidth());
    const applyWidth = (px) => immersiveViewer.style.setProperty('--immersive-panel-width', Math.round(px) + 'px');
    let stored = NaN;
    try {
        stored = parseInt(sessionStorage.getItem(WIDTH_KEY), 10);
    } catch {}
    if (Number.isFinite(stored)) applyWidth(clamp(stored));

    const handle = document.createElement('div');
    handle.className = 'immersive__panel-resize-handle';
    handle.setAttribute('aria-hidden', 'true');
    immersiveViewer.appendChild(handle);
    // A panel can grow beyond the width variable without a drag (the annotations
    // two-column mode raises its min-width, e.g. when the checkbox is toggled).
    // Sync the variable up to the real panel width in that case, or the handle
    // would sit unreachable inside the panel instead of on its edge. The current
    // variable value is read off the handle's GEOMETRY (it is positioned by the
    // variable) — the inline style alone misses the CSS fallback, media-query
    // and fullscreen values a first-time visitor runs on.
    let observer = null;
    if (typeof ResizeObserver !== 'undefined') {
        observer = new ResizeObserver(() => {
            const open = immersiveViewer.querySelector('.immersive__panel-left.is-open');
            if (!open || !handle.offsetParent) return;
            const panelWidth = open.getBoundingClientRect().width;
            const handleRect = handle.getBoundingClientRect();
            const varWidth = handleRect.left + handleRect.width / 2 - immersiveViewer.getBoundingClientRect().left - RAIL_WIDTH;
            if (panelWidth > varWidth + 1) applyWidth(panelWidth);
        });
        immersiveViewer.querySelectorAll('.immersive__panel-left').forEach((p) => observer.observe(p));
    }
    handle.addEventListener('pointerdown', (e) => {
        e.preventDefault();
        handle.setPointerCapture(e.pointerId);
        immersiveRoot.classList.add('-resizing');
        const viewerLeft = immersiveViewer.getBoundingClientRect().left;
        // Bounds are fixed for the duration of a drag — no style recalc per move.
        const dragMin = minWidth();
        const dragMax = maxWidth();
        const widthAt = (ev) => Math.min(Math.max(ev.clientX - viewerLeft - RAIL_WIDTH, dragMin), dragMax);
        const onMove = (ev) => applyWidth(widthAt(ev));
        const onUp = (ev) => {
            handle.releasePointerCapture(e.pointerId);
            handle.removeEventListener('pointermove', onMove);
            handle.removeEventListener('pointerup', onUp);
            immersiveRoot.classList.remove('-resizing');
            try {
                sessionStorage.setItem(WIDTH_KEY, String(Math.round(widthAt(ev))));
            } catch {}
        };
        handle.addEventListener('pointermove', onMove);
        handle.addEventListener('pointerup', onUp);
    });
    return {
        destroy() {
            if (observer) observer.disconnect();
            handle.remove();
        },
    };
}

/**
 * Single entry point for all TOC-panel wiring. Looks up the panel once and
 * bails early when it is not in the DOM (conditional server-render).
 *
 * @param {IvViewer} viewer
 * @returns {{destroy: function():void}}
 */
export function setupToc(viewer) {
    const tocPanel = document.getElementById('immersivePanelToc');
    if (!tocPanel) return noop;
    const parts = [setupTocCollapseToggle(tocPanel), setupTocSync(viewer, tocPanel)];
    return {
        destroy() {
            parts.forEach((part) => part.destroy());
        },
    };
}

/** Wires the "collapse all / expand all" toggle on the server-rendered TOC tree (shown only when the TOC nests). */
export function setupTocCollapseToggle(tocPanel = document.getElementById('immersivePanelToc')) {
    const bag = createDisposer();
    const tocContainer = document.getElementById('widgetToc');
    const tocToggle = tocPanel && tocPanel.querySelector('[data-immersive-toc-toggle]');
    if (tocContainer && tocToggle && tocContainer.querySelector(".widget-toc__element[data-level='2']")) {
        tocToggle.hidden = false;
        const reflectTocToggle = () => {
            const collapsed = !!tocContainer.querySelector('.widget-toc__element--hidden');
            tocToggle.classList.toggle('-collapsed', collapsed);
            tocToggle.setAttribute('aria-expanded', String(!collapsed));
            const label = collapsed ? tocToggle.dataset.labelExpand : tocToggle.dataset.labelCollapse;
            tocToggle.setAttribute('aria-label', label);
            tocToggle.setAttribute('title', label);
        };
        bag.listen(tocToggle, 'click', () => {
            if (tocContainer.querySelector('.widget-toc__element--hidden')) {
                tocContainer.querySelectorAll('.widget-toc__element--hidden').forEach((li) => li.classList.remove('widget-toc__element--hidden'));
                tocContainer.querySelectorAll('.widget-toc__element.parent').forEach((li) => {
                    li.classList.add('widget-toc__element--expanded');
                    const t = li.querySelector('.widget-toc__toggle');
                    if (t) t.setAttribute('aria-expanded', 'true');
                });
            } else {
                tocContainer.querySelectorAll('.widget-toc__element').forEach((li) => {
                    const level = Number(li.dataset.level);
                    if (level >= 2) li.classList.add('widget-toc__element--hidden');
                    if (level >= 1 && li.classList.contains('parent')) {
                        li.classList.remove('widget-toc__element--expanded');
                        const t = li.querySelector('.widget-toc__toggle');
                        if (t) t.setAttribute('aria-expanded', 'false');
                    }
                });
            }
            reflectTocToggle();
        });
        reflectTocToggle();
    }
    return { destroy: bag.dispose };
}

/** Wires the metadata "more / less" fold that reveals the deeper structural blocks. */
export function setupMetadataToggle() {
    const bag = createDisposer();
    const metadataToggle = document.querySelector('[data-immersive-metadata-toggle]');
    if (metadataToggle) {
        const moreEl = document.getElementById(metadataToggle.dataset.immersiveMetadataToggle);
        if (moreEl) {
            const moreLabel = metadataToggle.querySelector('.immersive__metadata-toggle-more');
            const lessLabel = metadataToggle.querySelector('.immersive__metadata-toggle-less');
            const syncMetadataToggleLabel = (open) => {
                if (moreLabel) moreLabel.setAttribute('aria-hidden', String(open));
                if (lessLabel) lessLabel.setAttribute('aria-hidden', String(!open));
            };
            syncMetadataToggleLabel(!moreEl.hidden);
            bag.listen(metadataToggle, 'click', () => {
                const willOpen = moreEl.hidden;
                moreEl.hidden = !willOpen;
                metadataToggle.setAttribute('aria-expanded', String(willOpen));
                syncMetadataToggleLabel(willOpen);
            });
        }
    }
    return { destroy: bag.dispose };
}

/**
 * TOC drawer: entry clicks navigate in place (entries carry their 1-based page
 * number as data-page-no), and the active-section highlight follows the
 * currently visible page(s).
 *
 * @param {IvViewer} viewer
 * @param {HTMLElement} [tocPanel]
 * @returns {{destroy: function():void}}
 */
export function setupTocSync(viewer, tocPanel = document.getElementById('immersivePanelToc')) {
    if (!tocPanel) return noop;
    const bag = createDisposer();
    const tocEntries = () =>
        Array.from(tocPanel.querySelectorAll('.widget-toc__element[data-page-no]'))
            .filter((el) => el.dataset.level !== '0') // skip the hidden record root
            .map((el) => ({ el, no: Number(el.dataset.pageNo) }))
            .filter((x) => Number.isFinite(x.no) && x.no >= 1);

    const setTocActive = (el) => {
        if (el && el.dataset.iddoc && env.viewerJS && env.viewerJS.widgetToc) {
            env.viewerJS.widgetToc.setActive(el.dataset.iddoc.replace('iddoc_', ''));
            if (tocPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
            return;
        }
        tocPanel.querySelectorAll('.widget-toc__element.active, .widget-toc__element-link.active').forEach((x) => x.classList.remove('active'));
        if (el) {
            el.classList.add('active');
            if (tocPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
        }
    };

    bag.listen(tocPanel, 'click', (e) => {
        const link = e.target.closest('.widget-toc__element-link a');
        if (!link) return;
        const element = link.closest('.widget-toc__element');
        const pageNo = element ? Number(element.dataset.pageNo) : NaN;
        if (!Number.isFinite(pageNo) || pageNo < 1) return;
        e.preventDefault();
        setTocActive(element);
        viewer.goToPage(pageNo - 1);
    });

    const syncTocActive = () => {
        const entries = tocEntries();
        const pages = viewer.getCurrentPages().map((p) => p + 1);
        if (!entries.length || !pages.length) return;
        const active = tocPanel.querySelector('.widget-toc__element.active[data-page-no]');
        const activeNo = active ? Number(active.dataset.pageNo) : null;
        const targetNo = pickActiveTocPageNo(
            entries.map((x) => x.no),
            pages,
            activeNo
        );
        if (active && targetNo === activeNo) return;
        let best = null;
        entries.forEach((x) => {
            if (x.no === targetNo) best = x;
        });
        setTocActive(best ? best.el : null);
    };
    bag.add(viewer.onPageChange.subscribe(syncTocActive));
    syncTocActive();
    return { destroy: bag.dispose };
}
