import ZoomableImage from './media/zoomableImage.mjs';
import ShareImageFragment from './media/shareImageFragment.mjs';
import Voyager3dView from './media/voyager3DViewer.mjs';
import IvViewer from './media/ivViewer.mjs';
import { loadPageServices, loadPageRegions } from './media/ivManifestSource.mjs';
import { buildLineSpans, buildWordSpans, mountTextImageLink } from './media/ivTextImageLink.mjs';
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

    // Slide-out panels (TOC / search): bind open/close immediately -- before the IIIF
    // services fetch -- so the server-rendered sidebar opens without waiting for the
    // first image. The triggering button is marked active while its panel is open.
    const immersiveRoot = el.closest('.immersive');
    const panelButtons = document.querySelectorAll('[data-immersive-panel]');
    // Flag the root while a left panel is open so CSS can hide the floating title and
    // prev chevron over the image (the title + close live in the panel header now).
    const syncPanelOpenFlag = () => {
        if (immersiveRoot) {
            immersiveRoot.classList.toggle('immersive--panel-open', !!document.querySelector('.immersive__panel--left.is-open'));
        }
    };
    // Set by the fulltext block; clears its image overlays + hover wiring when the panel closes.
    let onFulltextClose = null;
    const closePanels = () => {
        if (typeof onFulltextClose === 'function') onFulltextClose();
        document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
            p.classList.remove('is-open');
            p.setAttribute('aria-hidden', 'true');
        });
        panelButtons.forEach((b) => {
            b.classList.remove('immersive__tool-btn--active');
            b.setAttribute('aria-expanded', 'false');
        });
        syncPanelOpenFlag();
    };
    panelButtons.forEach((btn) => {
        btn.addEventListener('click', () => {
            // A disabled rail tool (e.g. fulltext in double-page mode) must not open its panel.
            if (btn.getAttribute('aria-disabled') === 'true') return;
            const panel = document.getElementById(btn.dataset.immersivePanel);
            if (!panel) return;
            const wasOpen = panel.classList.contains('is-open');
            closePanels();
            if (!wasOpen) {
                panel.classList.add('is-open');
                panel.setAttribute('aria-hidden', 'false');
                btn.classList.add('immersive__tool-btn--active');
                btn.setAttribute('aria-expanded', 'true');
                syncPanelOpenFlag();
                // Bring the active TOC entry into view (e.g. reloaded on a page far down,
                // so the highlighted section isn't left off-screen at the top). Scroll the
                // panel via offset math, not scrollIntoView, so the whole page never moves.
                const active = panel.querySelector('.widget-toc__element.active');
                if (active) {
                    let top = 0;
                    for (let n = active; n && n !== panel; n = n.offsetParent) top += n.offsetTop;
                    panel.scrollTop = Math.max(0, top - panel.clientHeight / 2);
                }
            }
        });
    });
    // No close button in the panel anymore: Escape closes the open panel (re-clicking
    // the burger toggles it shut too).
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && document.querySelector('.immersive__panel--left.is-open')) {
            closePanels();
        }
    });

    // TOC "collapse all / expand all" toggle: the tree is server-rendered, so wire it up
    // immediately (no viewer needed) and show it right away -- only when the TOC actually
    // nests. Collapse folds to the top-level chapters (the record root is hidden, so we
    // never fold to it); state-driven, so a click expands all if anything is collapsed.
    const tocPanel = document.getElementById('immersivePanelMenu');
    const tocContainer = document.getElementById('widgetToc');
    const tocToggle = tocPanel && tocPanel.querySelector('[data-immersive-toc-toggle]');
    if (tocContainer && tocToggle && tocContainer.querySelector(".widget-toc__element[data-level='2']")) {
        tocToggle.hidden = false;
        const reflectTocToggle = () => {
            const collapsed = !!tocContainer.querySelector('.widget-toc__element--hidden');
            tocToggle.classList.toggle('immersive__toc-collapse--collapsed', collapsed);
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

            // Fulltext (page OCR): render the current page's transcription in the left panel,
            // DFG-Viewer style. The text endpoint is per single page, so this is single-page
            // only -- double-page mode disables the rail button (see updateFulltextAvail).
            const fulltextPanel = document.getElementById('immersivePanelFulltext');
            const fulltextBox = document.getElementById('immersiveFulltext');
            const fulltextLoader = document.getElementById('immersiveFulltextLoader');
            const fulltextBtn = document.querySelector('[data-immersive-panel="immersivePanelFulltext"]');
            const fulltextTitleDefault = fulltextBtn ? fulltextBtn.getAttribute('title') : '';
            if (fulltextPanel && fulltextBox) {
                let granularity = 'line';
                let fulltextReq = 0;
                let currentLink = null;

                const clearFulltextLink = () => {
                    if (currentLink) {
                        currentLink.destroy();
                        currentLink = null;
                    }
                    viewer.clearTextRegions();
                    if (fulltextLoader) fulltextLoader.hidden = true;
                };
                // Close-Hook: when any panel close runs, drop our overlays + wiring.
                onFulltextClose = clearFulltextLink;

                // Out-of-order guard: only the latest page request paints its text.
                const loadFulltext = async () => {
                    const order = viewer.getCurrentPages()[0];
                    if (order === undefined) return;
                    const req = ++fulltextReq;
                    clearFulltextLink();
                    if (fulltextLoader) fulltextLoader.hidden = false;
                    fulltextBox.textContent = '';
                    fulltextBox.classList.remove('immersive__fulltext--empty');
                    let regions = null;
                    try {
                        regions = await loadPageRegions(pi, apiBase, order, granularity);
                    } catch (e) {
                        regions = null;
                    }
                    if (req !== fulltextReq) return;
                    if (fulltextLoader) fulltextLoader.hidden = true;
                    if (regions && regions.length) {
                        const fragment = granularity === 'word' ? buildWordSpans(regions) : buildLineSpans(regions);
                        fulltextBox.replaceChildren(fragment);
                        const regionEls = viewer.setTextRegions(regions);
                        currentLink = mountTextImageLink({ box: fulltextBox, regionEls, scrollContainer: fulltextPanel });
                    } else {
                        fulltextBox.textContent = fulltextBox.dataset.labelEmpty || '';
                        fulltextBox.classList.add('immersive__fulltext--empty');
                    }
                };

                // Granularity toggle (v1: 'word' is disabled in the markup; hook is ready).
                const granularityBtns = fulltextPanel.querySelectorAll('[data-immersive-granularity]');
                granularityBtns.forEach((b) => {
                    b.addEventListener('click', () => {
                        if (b.disabled) return;
                        granularity = b.dataset.immersiveGranularity;
                        granularityBtns.forEach((x) => {
                            const on = x === b;
                            x.classList.toggle('is-active', on);
                            x.setAttribute('aria-pressed', String(on));
                        });
                        if (fulltextPanel.classList.contains('is-open')) loadFulltext();
                    });
                });

                if (fulltextBtn) {
                    fulltextBtn.addEventListener('click', () => {
                        if (fulltextPanel.classList.contains('is-open')) loadFulltext();
                    });
                }
                viewer.onPageChange.subscribe(() => {
                    if (fulltextPanel.classList.contains('is-open')) loadFulltext();
                });
            }

            // Fulltext is per single page: in double-page mode grey out + disable the rail
            // button (its tooltip explains why) and close the panel if it was open.
            const updateFulltextAvail = () => {
                if (!fulltextBtn) return;
                const doublePage = !!(viewer.isDoublePage && viewer.isDoublePage());
                fulltextBtn.classList.toggle('immersive__tool-btn--disabled', doublePage);
                fulltextBtn.setAttribute('aria-disabled', String(doublePage));
                const title = (doublePage && fulltextBtn.dataset.titleDisabled) || fulltextTitleDefault;
                fulltextBtn.setAttribute('title', title);
                fulltextBtn.setAttribute('aria-label', title);
                if (doublePage && fulltextPanel && fulltextPanel.classList.contains('is-open')) closePanels();
            };
            updateFulltextAvail();

            // Overview: thumbnail grid overlay (lazy-mounted).
            const gridOverlay = document.getElementById('immersiveGridOverlay');
            const gridLoader = document.getElementById('immersiveGridLoader');
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
                    // Hide the loading screen as soon as the first thumbnail paints
                    // (safety timeout in case the manifest/images never resolve).
                    let gridLoaderDone = false;
                    let gridLoaderTimer;
                    const hideGridLoader = () => {
                        if (gridLoaderDone) return;
                        gridLoaderDone = true;
                        clearTimeout(gridLoaderTimer);
                        if (gridLoader) gridLoader.hidden = true;
                    };
                    // <img> load events don't bubble -> listen in the capture phase
                    const thumbsMount = document.getElementById('immersiveThumbnails');
                    if (thumbsMount) thumbsMount.addEventListener('load', hideGridLoader, { capture: true, once: true });
                    gridLoaderTimer = setTimeout(hideGridLoader, 8000);
                } else {
                    syncGridSelection();
                }
            };

            // Esc closes the open overview overlay (mirrors the close button).
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && gridOverlay && !gridOverlay.hidden) {
                    gridOverlay.hidden = true;
                }
            });

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
                        document.querySelector('.immersive__viewer')?.classList.toggle('is-double-page', on);
                        updateFulltextAvail();
                    }
                });
            });

            // Native fullscreen runs on .immersive__viewer; Bootstrap appends the
            // share/cite/filter popovers to <body>, which is outside the fullscreen
            // element, so they don't paint. Re-home those popovers into the fullscreen
            // element while fullscreen is active, and restore the default on exit.
            document.addEventListener('fullscreenchange', () => {
                document.querySelectorAll('[data-popover-element]').forEach((trigger) => {
                    const $trigger = window.$ && window.$(trigger);
                    const inst = $trigger && $trigger.data('bs.popover');
                    if (!inst) return;
                    $trigger.popover('hide');
                    if (document.fullscreenElement) {
                        if (inst.config._savedContainer === undefined) {
                            inst.config._savedContainer = inst.config.container;
                        }
                        inst.config.container = document.fullscreenElement;
                    } else if (inst.config._savedContainer !== undefined) {
                        inst.config.container = inst.config._savedContainer;
                        delete inst.config._savedContainer;
                    }
                });
            });

            // (Panel open/close is bound earlier -- before loadPageServices -- so the
            // sidebar opens immediately, without waiting for the first image to load.)

            // TOC drawer: clicking an entry navigates in place (no reload) and
            // highlights that section immediately, so the click intent always wins --
            // regardless of load latency or which page of a double-page spread the
            // section starts on. Entries carry their 1-based physical page number as
            // data-page-no; entries without one fall through to normal navigation.
            const menuPanel = document.getElementById('immersivePanelMenu');
            if (menuPanel) {
                const tocEntries = () =>
                    Array.from(menuPanel.querySelectorAll('.widget-toc__element[data-page-no]'))
                        .filter((el) => el.dataset.level !== '0') // skip the hidden record root
                        .map((el) => ({ el, no: Number(el.dataset.pageNo) }))
                        .filter((x) => Number.isFinite(x.no) && x.no >= 1);

                const setTocActive = (el) => {
                    // Tree-view: let the widget set active + expand collapsed ancestors, so a
                    // section reached via the grid/chevrons inside a collapsed branch opens up.
                    // (Its own scroll is a no-op here -- the panel scrolls, not the list -- so
                    // we still scrollIntoView ourselves when the panel is open.)
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
