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
            const gridActions = new rxjs.Subject();
            gridActions.subscribe((e) => {
                if (e && e.action === 'clickImage' && typeof e.value === 'number') {
                    viewer.goToPage(e.value);
                    if (gridOverlay) gridOverlay.hidden = true;
                }
            });
            const toggleGrid = () => {
                if (!gridOverlay) return;
                const opening = gridOverlay.hidden;
                gridOverlay.hidden = !opening;
                if (opening && !gridMounted) {
                    riot.mount('#immersiveThumbnails', 'thumbnails', {
                        source: `${apiBase}/records/${pi}/manifest`,
                        type: 'sequence',
                        actionlistener: gridActions,
                        imagesize: '!160,220', // IIIF size string (fit within 160x220)
                    });
                    gridMounted = true;
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

            // Left slide-out panels (TOC / search).
            document.querySelectorAll('[data-immersive-panel]').forEach((btn) => {
                btn.addEventListener('click', () => {
                    const panel = document.getElementById(btn.dataset.immersivePanel);
                    if (!panel) return;
                    const wasOpen = panel.classList.contains('is-open');
                    document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
                        p.classList.remove('is-open');
                        p.setAttribute('aria-hidden', 'true');
                    });
                    if (!wasOpen) {
                        panel.classList.add('is-open');
                        panel.setAttribute('aria-hidden', 'false');
                    }
                });
            });
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
