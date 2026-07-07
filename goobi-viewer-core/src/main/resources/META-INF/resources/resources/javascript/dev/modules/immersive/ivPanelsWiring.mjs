/** Left rail panel wiring: open/close, keyboard toggles, resize, TOC + metadata folds. */

import { offsetTopWithin } from './ivTextImageLink.mjs';
import { pickActiveTocPageNo, panelIdForKeyEvent } from './viewerImmersive.mjs';

/**
 * Wires the rail buttons to their left panels (open/close with ARIA + inert
 * bookkeeping) and registers the panel keyboard shortcuts on the dispatcher:
 * Escape closes the open panel, Alt+1-4 toggles the panels in rail order.
 *
 * @param {HTMLElement|null} immersiveRoot  the .immersive root element
 * @param {{register: Function}} keys  the immersive key dispatcher
 * @returns {{closePanels: Function, registerCloseHook: Function}}
 */
export function setupPanels(immersiveRoot, keys) {
    const panelButtons = document.querySelectorAll('[data-immersive-panel]');
    document.querySelectorAll('.immersive__panel--left').forEach((p) => (p.inert = true));
    const syncPanelOpenFlag = () => {
        if (immersiveRoot) {
            immersiveRoot.classList.toggle('immersive--panel-open', !!document.querySelector('.immersive__panel--left.is-open'));
        }
    };
    const closeHooks = [];
    let activePanelBtn = null;
    const closePanels = (restoreFocus = false) => {
        closeHooks.forEach((hook) => hook());
        const closedAny = !!document.querySelector('.immersive__panel--left.is-open');
        document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
            p.classList.remove('is-open');
            p.setAttribute('aria-hidden', 'true');
            p.inert = true;
        });
        panelButtons.forEach((b) => {
            b.classList.remove('immersive__tool-btn--active');
            b.setAttribute('aria-expanded', 'false');
        });
        syncPanelOpenFlag();
        if (restoreFocus && closedAny && activePanelBtn) activePanelBtn.focus();
        activePanelBtn = null;
    };
    panelButtons.forEach((btn) => {
        btn.addEventListener('click', () => {
            if (btn.getAttribute('aria-disabled') === 'true') return;
            const panel = document.getElementById(btn.dataset.immersivePanel);
            if (!panel) return;
            const wasOpen = panel.classList.contains('is-open');
            closePanels();
            if (!wasOpen) {
                panel.classList.add('is-open');
                panel.setAttribute('aria-hidden', 'false');
                panel.inert = false;
                btn.classList.add('immersive__tool-btn--active');
                btn.setAttribute('aria-expanded', 'true');
                activePanelBtn = btn;
                syncPanelOpenFlag();
                // Scroll via offset math, not scrollIntoView, so the page itself never moves.
                const active = panel.querySelector('.widget-toc__element.active');
                if (active) {
                    panel.scrollTop = Math.max(0, offsetTopWithin(active, panel) - panel.clientHeight / 2);
                }
                const focusTarget = Array.from(panel.querySelectorAll('input, a[href], button')).find((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
                if (focusTarget) {
                    focusTarget.focus({ preventScroll: true });
                } else {
                    if (!panel.hasAttribute('tabindex')) panel.setAttribute('tabindex', '-1');
                    panel.focus({ preventScroll: true });
                }
            }
        });
    });

    const overlayOpen = () => !!document.querySelector('#immersiveGridOverlay:not([hidden]), #immersiveShortcuts:not([hidden]), #immersivePageDropdown:not([hidden])');
    keys.register(40, (e) => {
        if (e.key === 'Escape') {
            if (!document.querySelector('.immersive__panel--left.is-open')) return false;
            closePanels(true);
            return true;
        }
        const panelId = panelIdForKeyEvent(e);
        if (!panelId || overlayOpen()) return false;
        const btn = document.querySelector(`[data-immersive-panel="${panelId}"]`);
        if (!btn) return false;
        e.preventDefault();
        btn.click();
        return true;
    });

    return {
        closePanels,
        /** Registers a hook that runs whenever the panels close (e.g. fulltext teardown). */
        registerCloseHook(hook) {
            if (typeof hook === 'function') closeHooks.push(hook);
        },
    };
}

/**
 * Adds the drag handle that resizes the left panels: one shared
 * --immersive-panel-width custom property, persisted in localStorage.
 */
export function setupPanelResize(immersiveRoot) {
    const immersiveViewer = immersiveRoot && immersiveRoot.querySelector('.immersive__viewer');
    if (!immersiveViewer) return;
    const WIDTH_KEY = 'immersive-panel-width';
    const RAIL_WIDTH = 40;
    const MIN_WIDTH = 240;
    const MAX_WIDTH_RATIO = 0.8;
    const maxWidth = () => immersiveViewer.getBoundingClientRect().width * MAX_WIDTH_RATIO;
    const clamp = (px) => Math.min(Math.max(px, MIN_WIDTH), maxWidth());
    const applyWidth = (px) => immersiveViewer.style.setProperty('--immersive-panel-width', Math.round(px) + 'px');
    let stored = NaN;
    try {
        stored = parseInt(localStorage.getItem(WIDTH_KEY), 10);
    } catch {}
    if (Number.isFinite(stored)) applyWidth(clamp(stored));

    const handle = document.createElement('div');
    handle.className = 'immersive__panel-resize-handle';
    handle.setAttribute('aria-hidden', 'true');
    immersiveViewer.appendChild(handle);
    handle.addEventListener('pointerdown', (e) => {
        e.preventDefault();
        handle.setPointerCapture(e.pointerId);
        immersiveRoot.classList.add('immersive--resizing');
        const viewerLeft = immersiveViewer.getBoundingClientRect().left;
        const widthAt = (ev) => clamp(ev.clientX - viewerLeft - RAIL_WIDTH);
        const onMove = (ev) => applyWidth(widthAt(ev));
        const onUp = (ev) => {
            handle.releasePointerCapture(e.pointerId);
            handle.removeEventListener('pointermove', onMove);
            handle.removeEventListener('pointerup', onUp);
            immersiveRoot.classList.remove('immersive--resizing');
            try {
                localStorage.setItem(WIDTH_KEY, String(Math.round(widthAt(ev))));
            } catch {}
        };
        handle.addEventListener('pointermove', onMove);
        handle.addEventListener('pointerup', onUp);
    });
}

/** Wires the "collapse all / expand all" toggle on the server-rendered TOC tree (shown only when the TOC nests). */
export function setupTocCollapseToggle() {
    const tocPanel = document.getElementById('immersivePanelMenu');
    const tocContainer = document.getElementById('widgetToc');
    const tocToggle = tocPanel && tocPanel.querySelector('[data-immersive-toc-toggle]');
    if (tocContainer && tocToggle && tocContainer.querySelector(".widget-toc__element[data-level='2']")) {
        tocToggle.hidden = false;
        const reflectTocToggle = () => {
            const collapsed = !!tocContainer.querySelector('.widget-toc__element--hidden');
            tocToggle.classList.toggle('immersive__toc-collapse--collapsed', collapsed);
            tocToggle.setAttribute('aria-expanded', String(!collapsed));
            const label = collapsed ? tocToggle.dataset.labelExpand : tocToggle.dataset.labelCollapse;
            tocToggle.setAttribute('aria-label', label);
            tocToggle.setAttribute('title', label);
        };
        tocToggle.addEventListener('click', () => {
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
}

/** Wires the metadata "more / less" fold that reveals the deeper structural blocks. */
export function setupMetadataToggle() {
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
            metadataToggle.addEventListener('click', () => {
                const willOpen = moreEl.hidden;
                moreEl.hidden = !willOpen;
                metadataToggle.setAttribute('aria-expanded', String(willOpen));
                syncMetadataToggleLabel(willOpen);
            });
        }
    }
}

/**
 * TOC drawer: entry clicks navigate in place (entries carry their 1-based page
 * number as data-page-no), and the active-section highlight follows the
 * currently visible page(s).
 *
 * @param {IvViewer} viewer
 */
export function setupTocSync(viewer) {
    const menuPanel = document.getElementById('immersivePanelMenu');
    if (!menuPanel) return;
    const tocEntries = () =>
        Array.from(menuPanel.querySelectorAll('.widget-toc__element[data-page-no]'))
            .filter((el) => el.dataset.level !== '0') // skip the hidden record root
            .map((el) => ({ el, no: Number(el.dataset.pageNo) }))
            .filter((x) => Number.isFinite(x.no) && x.no >= 1);

    const setTocActive = (el) => {
        if (el && el.dataset.iddoc && window.viewerJS && viewerJS.widgetToc) {
            viewerJS.widgetToc.setActive(el.dataset.iddoc.replace('iddoc_', ''));
            if (menuPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
            return;
        }
        menuPanel.querySelectorAll('.widget-toc__element.active, .widget-toc__element-link.active').forEach((x) => x.classList.remove('active'));
        if (el) {
            el.classList.add('active');
            if (menuPanel.classList.contains('is-open')) el.scrollIntoView({ block: 'nearest' });
        }
    };

    menuPanel.addEventListener('click', (e) => {
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
        const active = menuPanel.querySelector('.widget-toc__element.active[data-page-no]');
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
    viewer.onPageChange.subscribe(syncTocActive);
    syncTocActive();
}
