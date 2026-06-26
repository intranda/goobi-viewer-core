import ZoomableImage from './media/zoomableImage.mjs';
import ShareImageFragment from './media/shareImageFragment.mjs';
import Voyager3dView from './media/voyager3DViewer.mjs';
import IvViewer from './media/ivViewer.mjs';
import { loadPageServices } from './media/ivManifestSource.mjs';
import { attachUrlSync } from './viewer/viewerImmersive.mjs';
import { search, nextIndex, prevIndex } from './media/ivFulltextSearch.mjs';

window.ShareImageFragment = ShareImageFragment;

window.zoomableImageLoaded = new rxjs.Subject();

document.addEventListener('DOMContentLoaded', () => {
    // Legacy object/fullscreen image view — only when its mount is present.
    if (document.querySelector('[data-image="zoomable"]')) {
        window.image = new ZoomableImage();
        window.image
            .load()
            .then((image) => {
                window.zoomableImageLoaded.next(image);
            })
            .catch((e) => {
                window.zoomableImageLoaded.error(e);
            });
    }

    window.voyager3dView = new Voyager3dView();

    // Immersive image viewer — only when its mount is present.
    const immersiveEl = document.querySelector('[data-immersive-image]');
    if (immersiveEl) {
        initImmersiveViewer(immersiveEl);
    }
});

/**
 * Bootstraps the immersive image viewer: fetches the IIIF manifest, instantiates
 * the engine, and attaches the URL-sync + minimal paging controls. Everything
 * else hooks onto viewer.onPageChange (no engine changes needed to extend).
 *
 * @param {HTMLElement} el  the [data-immersive-image] mount element
 */
function initImmersiveViewer(el) {
    const pi = el.dataset.pi;
    const apiBase = el.dataset.apiBase;
    const startOrder = Number(el.dataset.startOrder) || 0;
    const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom) : undefined;

    loadPageServices(pi, apiBase)
        .then((services) => {
            const viewer = new IvViewer({ element: el, services, startOrder, maxZoom });
            window.ivViewer = viewer;
            attachUrlSync(viewer, pi);
            viewer.onLoaded.subscribe(() => mountImageFilters(viewer));

            const indicator = document.getElementById('immersivePageIndicator');
            const total = viewer.getPageCount();
            const updateIndicator = () => {
                if (!indicator) return;
                const pages = viewer.getCurrentPages().map((p) => p + 1);
                const label = pages.length > 1 ? `${pages[0]}–${pages[pages.length - 1]}` : `${pages[0]}`;
                indicator.textContent = `${label} / ${total}`;
            };
            updateIndicator();
            viewer.onPageChange.subscribe(() => updateIndicator());

            // Page chevrons: only show an arrow when paging that way is possible
            // (hide prev on the first page, next on the last page).
            const prevChevron = document.querySelector('.immersive__chevron[data-immersive-page="prev"]');
            const nextChevron = document.querySelector('.immersive__chevron[data-immersive-page="next"]');
            const updateChevrons = () => {
                const pages = viewer.getCurrentPages();
                if (!pages.length) return;
                if (prevChevron) prevChevron.hidden = Math.min(...pages) <= 0;
                if (nextChevron) nextChevron.hidden = Math.max(...pages) >= total - 1;
            };
            updateChevrons();
            viewer.onPageChange.subscribe(updateChevrons);

            // Fulltext: in-place IIIF content search → result list (left panel) + image hit highlights.
            const fts = { hits: [], idx: -1, term: '' };
            const resultsBox = document.getElementById('immersiveSearchResults');
            const resultsList = document.getElementById('immersiveResultsList');
            const hitCounter = document.getElementById('immersiveHitCounter');
            const hitsOnOrder = (order) => fts.hits.filter((h) => h.order === order);

            const renderHighlights = () => {
                const rects = viewer.getCurrentPages().flatMap((o) =>
                    hitsOnOrder(o)
                        .map((h) => h.rect)
                        .filter(Boolean)
                );
                viewer.setHighlights(rects);
            };

            const renderResults = () => {
                if (!resultsBox || !resultsList) return;
                resultsBox.hidden = !fts.term;
                hitCounter.textContent = fts.hits.length ? `${fts.idx + 1} / ${fts.hits.length}` : '';
                resultsList.innerHTML = '';
                if (fts.term && !fts.hits.length) {
                    const empty = document.createElement('li');
                    empty.className = 'immersive__results-empty';
                    empty.textContent = resultsBox.dataset.labelEmpty;
                    resultsList.appendChild(empty);
                    return;
                }
                fts.hits.forEach((h, i) => {
                    const li = document.createElement('li');
                    li.className = 'immersive__results-item' + (i === fts.idx ? ' is-active' : '');
                    const page = document.createElement('span');
                    page.className = 'immersive__results-page';
                    page.textContent = h.page;
                    const snippet = document.createElement('span');
                    snippet.className = 'immersive__results-snippet';
                    snippet.textContent = h.snippet || '';
                    li.append(page, snippet);
                    li.addEventListener('click', () => gotoHit(i));
                    resultsList.appendChild(li);
                });
            };

            const gotoHit = (i) => {
                if (!fts.hits.length) return;
                fts.idx = (i + fts.hits.length) % fts.hits.length;
                const hit = fts.hits[fts.idx];
                if (!viewer.getCurrentPages().includes(hit.order)) viewer.goToPage(hit.order);
                else renderHighlights();
                renderResults();
            };

            const runSearch = async (term) => {
                fts.term = term;
                fts.hits = term ? await search(pi, apiBase, term) : [];
                fts.idx = fts.hits.length ? 0 : -1;
                renderResults();
                if (fts.idx >= 0) gotoHit(0);
                else viewer.clearHighlights();
            };

            viewer.onPageChange.subscribe(renderHighlights);

            const searchPanel = document.getElementById('immersivePanelSearch');
            const searchForm = searchPanel && searchPanel.querySelector('form');
            const searchInput = searchPanel && searchPanel.querySelector('input[type="text"]');
            if (searchForm && searchInput) {
                searchForm.addEventListener('submit', (e) => {
                    e.preventDefault();
                    runSearch(searchInput.value.trim());
                });
            }
            document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
                btn.addEventListener('click', () => {
                    const n = fts.hits.length;
                    gotoHit(btn.dataset.immersiveHit === 'next' ? nextIndex(fts.idx, n) : prevIndex(fts.idx, n));
                });
            });

            // Overview: thumbnail grid overlay (lazy-mounted).
            const gridOverlay = document.getElementById('immersiveGridOverlay');
            let gridMounted = false;
            let gridTag = null;
            const gridActions = new rxjs.Subject();
            gridActions.subscribe((e) => {
                if (e && e.action === 'clickImage' && typeof e.value === 'number') {
                    viewer.goToPage(e.value);
                    if (gridOverlay) gridOverlay.hidden = true;
                }
            });
            // The grid highlights the current page via opts.index -- the 0-based
            // canvas index, which equals the viewer's 0-based page order. Keep it in
            // sync so the right sheet stays selected as the page changes.
            const currentOrder = () => {
                const pages = viewer.getCurrentPages ? viewer.getCurrentPages() : [];
                return pages.length ? pages[0] : 0;
            };
            const syncGridSelection = () => {
                if (gridTag) {
                    gridTag.opts.index = currentOrder();
                    gridTag.update();
                }
            };
            viewer.onPageChange.subscribe(() => {
                if (gridOverlay && !gridOverlay.hidden) syncGridSelection();
            });
            const toggleGrid = () => {
                if (!gridOverlay) return;
                const opening = gridOverlay.hidden;
                gridOverlay.hidden = !opening;
                if (!opening) return;
                if (!gridMounted) {
                    gridTag = riot.mount('#immersiveThumbnails', 'thumbnails', {
                        source: `${apiBase}/records/${pi}/manifest`,
                        type: 'sequence',
                        actionlistener: gridActions,
                        imagesize: '!320,440', // IIIF size string (fit within 320x440, crisp on HiDPI)
                        index: currentOrder(),
                    })[0];
                    gridMounted = true;
                } else {
                    syncGridSelection();
                }
            };

            document.querySelectorAll('[data-immersive-page]').forEach((btn) => {
                btn.addEventListener('click', () => (btn.dataset.immersivePage === 'next' ? viewer.next() : viewer.prev()));
            });
            document.querySelectorAll('[data-immersive-action]').forEach((btn) => {
                btn.addEventListener('click', () => {
                    const action = btn.dataset.immersiveAction;
                    if (action === 'zoom-in') viewer.zoomIn();
                    else if (action === 'zoom-out') viewer.zoomOut();
                    else if (action === 'rotate-left') viewer.rotateLeft();
                    else if (action === 'rotate-right') viewer.rotateRight();
                    else if (action === 'reset') viewer.resetView();
                    else if (action === 'fullscreen') toggleImmersiveFullscreen();
                    else if (action === 'overview') toggleGrid();
                    else if (action === 'double-page') {
                        const on = viewer.toggleDoublePage();
                        btn.setAttribute('aria-pressed', String(on));
                        btn.classList.toggle('immersive__tool-btn--active', on);
                    }
                });
            });

            // Left slide-out panels (TOC / search). The triggering button is marked active
            // while its panel is open so the rail can show the brand accent on it.
            const panelButtons = document.querySelectorAll('[data-immersive-panel]');
            panelButtons.forEach((btn) => {
                btn.addEventListener('click', () => {
                    const panel = document.getElementById(btn.dataset.immersivePanel);
                    if (!panel) return;
                    const wasOpen = panel.classList.contains('is-open');
                    document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
                        p.classList.remove('is-open');
                        p.setAttribute('aria-hidden', 'true');
                    });
                    panelButtons.forEach((b) => {
                        b.classList.remove('immersive__tool-btn--active');
                        b.setAttribute('aria-expanded', 'false');
                    });
                    if (!wasOpen) {
                        panel.classList.add('is-open');
                        panel.setAttribute('aria-hidden', 'false');
                        btn.classList.add('immersive__tool-btn--active');
                        btn.setAttribute('aria-expanded', 'true');
                    }
                });
            });

            // TOC drawer: clicking an entry navigates in place (no reload) and
            // highlights that section immediately, so the click intent always wins --
            // regardless of load latency or which page of a double-page spread the
            // section starts on. Entries carry their 1-based physical page number as
            // data-page-no; entries without one fall through to normal navigation.
            const menuPanel = document.getElementById('immersivePanelMenu');
            if (menuPanel) {
                const tocEntries = () =>
                    Array.from(menuPanel.querySelectorAll('.widget-toc__element[data-page-no]'))
                        .map((el) => ({ el, no: Number(el.dataset.pageNo) }))
                        .filter((x) => Number.isFinite(x.no) && x.no >= 1);

                const setTocActive = (el) => {
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

                // Keep the highlight on the section the reader is in and let it follow
                // along when paging via the chevrons/grid/search. A section owns the
                // page range [pageNo, nextPageNo). The active section is kept while any
                // visible page (single page, or either page of a double-page spread)
                // still falls in its range; otherwise the section owning the last
                // visible page takes over. Range-based, so a section starting on the
                // right page of a spread no longer mis-picks its neighbour.
                const syncTocActive = () => {
                    const entries = tocEntries();
                    const pages = viewer.getCurrentPages().map((p) => p + 1);
                    if (!entries.length || !pages.length) return;
                    const active = menuPanel.querySelector('.widget-toc__element.active[data-page-no]');
                    if (active) {
                        const no = Number(active.dataset.pageNo);
                        const nextNo = Math.min(Infinity, ...entries.map((x) => x.no).filter((n) => n > no));
                        if (pages.some((p) => p >= no && p < nextNo)) return;
                    }
                    const top = Math.max(...pages);
                    let best = null;
                    entries.forEach((x) => {
                        if (x.no <= top && (!best || x.no >= best.no)) best = x;
                    });
                    setTocActive(best ? best.el : null);
                };
                viewer.onPageChange.subscribe(syncTocActive);
                syncTocActive();
            }
        })
        .catch((e) => console.error('immersive viewer init failed', e));
}

/**
 * Mounts the reused imageFilters riot tag on the viewer's live ImageView.Image so the
 * Filter popover adjusts brightness/contrast/etc. Pixel filters need an origin-clean
 * canvas (CORS); if the tiles taint it, the Filter button is hidden instead.
 */
function mountImageFilters(viewer) {
    const btn = document.querySelector('[data-popover-element="#immersiveFilterPopover"]');
    if (!btn || !document.querySelector('imageFilters') || !window.immersiveFilterConfig) return;
    const image = viewer.viewer;
    const originClean = typeof image.isOriginClean !== 'function' || image.isOriginClean();
    if (originClean) {
        riot.mount('imageFilters', { image, config: window.immersiveFilterConfig });
    } else {
        btn.hidden = true;
    }
}

/** Toggles native browser fullscreen on the immersive viewer hero (H3 = real fullscreen, Esc exits). */
function toggleImmersiveFullscreen() {
    const el = document.querySelector('.immersive__viewer');
    if (!el) return;
    if (document.fullscreenElement) {
        document.exitFullscreen();
    } else {
        el.requestFullscreen?.();
    }
}
