import ZoomableImage from './media/zoomableImage.mjs';
import ShareImageFragment from './media/shareImageFragment.mjs';
import Voyager3dView from './media/voyager3DViewer.mjs';
import IvViewer from './immersive/ivViewer.mjs';
import { loadPageServices, loadPageLabels, loadPageRegions } from './immersive/ivManifestSource.mjs';
import { buildLineSpans, buildWordSpans, mountTextImageLink, offsetTopWithin } from './immersive/ivTextImageLink.mjs';
import { search, nextIndex, prevIndex } from './immersive/ivFulltextSearch.mjs';
import { attachUrlSync, pickActiveTocPageNo } from './immersive/viewerImmersive.mjs';

window.ShareImageFragment = ShareImageFragment;

window.zoomableImageLoaded = new rxjs.Subject();

document.addEventListener('DOMContentLoaded', () => {
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
    const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom, 10) : undefined;
    const immersiveRoot = el.closest('.immersive');
    const panelButtons = document.querySelectorAll('[data-immersive-panel]');
    document.querySelectorAll('.immersive__panel--left').forEach((p) => (p.inert = true));
    const syncPanelOpenFlag = () => {
        if (immersiveRoot) {
            immersiveRoot.classList.toggle('immersive--panel-open', !!document.querySelector('.immersive__panel--left.is-open'));
        }
    };
    let onFulltextClose = null;
    let activePanelBtn = null;
    const closePanels = (restoreFocus = false) => {
        if (typeof onFulltextClose === 'function') onFulltextClose();
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
    document.addEventListener('keydown', (e) => {
        if (e.key !== 'Escape') return;
        if (document.querySelector('#immersiveGridOverlay:not([hidden]), #immersivePageDropdown:not([hidden])')) return;
        if (document.querySelector('.immersive__panel--left.is-open')) {
            closePanels(true);
        }
    });

    setupPanelResize(immersiveRoot);
    setupTocCollapseToggle();
    setupMetadataToggle();
    setupImmersivePopoverA11y();

    loadPageServices(pi, apiBase)
        .then((services) => {
            const viewer = new IvViewer({ element: el, services, startOrder, maxZoom });
            window.ivViewer = viewer;
            attachUrlSync(viewer, pi);
            viewer.onLoaded.subscribe(() => bindImageFiltersMount(viewer));

            const indicator = document.getElementById('immersivePageIndicator');
            const titlePage = document.getElementById('immersiveTitlePage');
            const total = viewer.getPageCount();
            const workTitle = (document.querySelector('.immersive__title-text')?.textContent || '').trim();
            const updateIndicator = () => {
                const pages = viewer.getCurrentPages().map((p) => p + 1);
                const label = pages.length > 1 ? `${pages[0]}–${pages[pages.length - 1]}` : `${pages[0]}`;
                if (indicator) indicator.textContent = `${label} / ${total}`;
                if (titlePage) titlePage.textContent = `(${label} / ${total})`;
                const imageSurface = el.querySelector('.openseadragon-canvas') || el;
                imageSurface.setAttribute('role', 'img');
                imageSurface.setAttribute('aria-label', workTitle ? `${workTitle}, ${label} / ${total}` : `${label} / ${total}`);
            };
            updateIndicator();
            viewer.onPageChange.subscribe(() => updateIndicator());

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

            loadPageLabels(pi, apiBase, fetch, document.documentElement.lang)
                .then((labels) => setupPageDropdown(viewer, labels))
                .catch((e) => console.warn('immersive page labels failed', e));

            setupFulltextSearch(viewer, pi, apiBase);

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
                onFulltextClose = clearFulltextLink;

                const loadFulltext = async () => {
                    const order = currentOrder(viewer);
                    const req = ++fulltextReq;
                    clearFulltextLink();
                    if (fulltextLoader) fulltextLoader.hidden = false;
                    fulltextBox.textContent = '';
                    fulltextBox.classList.remove('immersive__fulltext--empty');
                    let regions = null;
                    try {
                        regions = await loadPageRegions(pi, apiBase, order, granularity);
                    } catch {
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

                const granularityBtns = fulltextPanel.querySelectorAll('[data-immersive-granularity]');
                granularityBtns.forEach((b) => {
                    b.addEventListener('click', () => {
                        if (b.disabled) return;
                        granularity = b.dataset.immersiveGranularity;
                        granularityBtns.forEach((x) => {
                            const on = x === b;
                            x.classList.toggle('immersive__fulltext-granularity-btn--active', on);
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

            const updateFulltextAvail = () => {
                if (!fulltextBtn) return;
                const doublePage = !!(viewer.isDoublePage && viewer.isDoublePage());
                fulltextBtn.classList.toggle('immersive__tool-btn--disabled', doublePage);
                fulltextBtn.setAttribute('aria-disabled', String(doublePage));
                fulltextBtn.setAttribute('tabindex', doublePage ? '-1' : '0');
                const title = (doublePage && fulltextBtn.dataset.titleDisabled) || fulltextTitleDefault;
                fulltextBtn.setAttribute('title', title);
                fulltextBtn.setAttribute('aria-label', title);
                if (doublePage && fulltextPanel && fulltextPanel.classList.contains('is-open')) closePanels();
            };
            updateFulltextAvail();

            const toggleGrid = setupOverviewGrid(viewer, pi, apiBase);

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

            const fullscreenBtn = document.querySelector('[data-immersive-action="fullscreen"]');
            document.addEventListener('fullscreenchange', () => {
                if (fullscreenBtn) {
                    fullscreenBtn.setAttribute('aria-pressed', String(!!document.fullscreenElement));
                    const exitLabel = fullscreenBtn.dataset.labelExit;
                    const enterLabel = fullscreenBtn.dataset.labelEnter || fullscreenBtn.getAttribute('aria-label');
                    const label = document.fullscreenElement && exitLabel ? exitLabel : enterLabel;
                    if (label) {
                        fullscreenBtn.setAttribute('aria-label', label);
                        fullscreenBtn.setAttribute('title', label);
                    }
                }
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

            setupTocSync(viewer);
        })
        .catch((e) => console.error('immersive viewer init failed', e));
}

/** First visible page as 0-based order (the leading page of a double-page spread). */
function currentOrder(viewer) {
    const pages = viewer.getCurrentPages ? viewer.getCurrentPages() : [];
    return pages.length ? pages[0] : 0;
}

/**
 * Adds the drag handle that resizes the left panels: one shared
 * --immersive-panel-width custom property, persisted in localStorage.
 */
function setupPanelResize(immersiveRoot) {
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
function setupTocCollapseToggle() {
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
function setupMetadataToggle() {
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
 * Builds the title page picker: a "go to page" input plus a listbox with one
 * option per page, labelled with the manifest page labels.
 *
 * @param {IvViewer} viewer
 * @param {string[]} labels page labels, index-aligned with the page orders
 */
function setupPageDropdown(viewer, labels) {
    const total = viewer.getPageCount();
    const trigger = document.querySelector('[data-immersive-title-trigger]');
    const dropdown = document.getElementById('immersivePageDropdown');
    const list = document.getElementById('immersivePageList');
    const input = document.getElementById('immersivePageInput');
    if (!trigger || !dropdown || !list) return;
    if (total < 2) {
        trigger.classList.add('immersive__title-trigger--static');
        return;
    }
    if (input) input.max = String(total);
    const listLabel = input && input.labels && input.labels[0] ? input.labels[0].textContent.trim() : '';
    if (listLabel) list.setAttribute('aria-label', listLabel);

    const items = [];
    for (let order = 0; order < total; order++) {
        const li = document.createElement('li');
        li.setAttribute('role', 'presentation');
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'immersive__page-dropdown-item';
        btn.dataset.order = String(order);
        btn.setAttribute('role', 'option');
        btn.setAttribute('aria-selected', 'false');
        btn.tabIndex = -1;
        const num = document.createElement('span');
        num.className = 'immersive__page-dropdown-number';
        num.textContent = `${order + 1}:`;
        const lbl = document.createElement('span');
        lbl.className = 'immersive__page-dropdown-item-label';
        lbl.textContent = (labels[order] || '').trim();
        btn.append(num, lbl);
        li.append(btn);
        list.append(li);
        items.push(btn);
    }

    const markActive = () => {
        const current = viewer.getCurrentPages();
        let rovingSet = false;
        items.forEach((btn) => {
            const on = current.includes(Number(btn.dataset.order));
            btn.classList.toggle('immersive__page-dropdown-item--active', on);
            btn.setAttribute('aria-selected', on ? 'true' : 'false');
            btn.tabIndex = on && !rovingSet ? 0 : -1;
            if (on) rovingSet = true;
        });
        if (!rovingSet && items.length) items[0].tabIndex = 0;
    };

    const focusOption = (idx) => {
        if (idx < 0) idx = 0;
        if (idx > items.length - 1) idx = items.length - 1;
        const target = items[idx];
        if (!target) return;
        items.forEach((btn) => (btn.tabIndex = btn === target ? 0 : -1));
        target.focus();
    };

    const isOpen = () => !dropdown.hidden;
    const onOutside = (e) => {
        if (!dropdown.contains(e.target) && !trigger.contains(e.target)) close();
    };
    const close = () => {
        if (!isOpen()) return;
        dropdown.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        document.removeEventListener('pointerdown', onOutside, true);
    };
    const open = () => {
        if (isOpen()) return;
        markActive();
        dropdown.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        document.addEventListener('pointerdown', onOutside, true);
        const active = list.querySelector('.immersive__page-dropdown-item--active');
        if (active) {
            list.scrollTop = Math.max(0, active.offsetTop - list.offsetTop - (list.clientHeight - active.clientHeight) / 2);
        }
        if (input) {
            input.value = '';
            input.focus({ preventScroll: true });
        }
    };

    trigger.addEventListener('click', () => (isOpen() ? close() : open()));
    list.addEventListener('click', (e) => {
        const btn = e.target.closest('.immersive__page-dropdown-item');
        if (!btn) return;
        viewer.goToPage(Number(btn.dataset.order));
        close();
    });
    list.addEventListener('keydown', (e) => {
        const cur = items.indexOf(document.activeElement);
        if (cur === -1 && !['Escape'].includes(e.key)) return;
        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                focusOption(cur + 1);
                break;
            case 'ArrowUp':
                e.preventDefault();
                focusOption(cur - 1);
                break;
            case 'Home':
                e.preventDefault();
                focusOption(0);
                break;
            case 'End':
                e.preventDefault();
                focusOption(items.length - 1);
                break;
            case 'Enter':
            case ' ':
            case 'Spacebar':
                e.preventDefault();
                viewer.goToPage(Number(items[cur].dataset.order));
                close();
                trigger.focus();
                break;
            case 'Escape':
                e.preventDefault();
                close();
                trigger.focus();
                break;
            default:
                break;
        }
    });
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (e.key !== 'Enter') return;
            e.preventDefault();
            const n = parseInt(input.value, 10);
            if (Number.isFinite(n) && n >= 1 && n <= total) {
                viewer.goToPage(n - 1);
                close();
            }
        });
    }
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && isOpen() && !document.querySelector('#immersiveGridOverlay:not([hidden])')) {
            close();
            trigger.focus();
        }
    });
    viewer.onPageChange.subscribe(markActive);
    markActive();
}

/**
 * Wires the in-place IIIF content search: result list in the left panel,
 * prev/next hit buttons, and hit highlights on the image.
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 */
function setupFulltextSearch(viewer, pi, apiBase) {
    const searchState = { hits: [], activeIndex: -1, term: '' };
    const resultsBox = document.getElementById('immersiveSearchResults');
    const resultsList = document.getElementById('immersiveResultsList');
    const hitCounter = document.getElementById('immersiveHitCounter');
    const hitsOnOrder = (order) => searchState.hits.filter((h) => h.order === order);

    const renderHighlights = () => {
        const activeHit = searchState.hits[searchState.activeIndex];
        const rects = viewer.getCurrentPages().flatMap((o) =>
            hitsOnOrder(o)
                .filter((h) => h.rect)
                .map((h) => ({ ...h.rect, active: h === activeHit }))
        );
        viewer.setHighlights(rects);
    };

    const renderResults = () => {
        if (!resultsBox || !resultsList) return;
        resultsBox.hidden = !searchState.term;
        if (hitCounter) {
            hitCounter.textContent = searchState.hits.length
                ? `${searchState.activeIndex + 1} / ${searchState.hits.length}`
                : (searchState.term && resultsBox.dataset.labelEmpty) || '';
        }
        resultsList.innerHTML = '';
        if (searchState.term && !searchState.hits.length) {
            const empty = document.createElement('li');
            empty.className = 'immersive__results-empty';
            empty.textContent = resultsBox.dataset.labelEmpty || '';
            resultsList.appendChild(empty);
            return;
        }
        searchState.hits.forEach((h, i) => {
            const li = document.createElement('li');
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'immersive__results-item' + (i === searchState.activeIndex ? ' immersive__results-item--active' : '');
            const page = document.createElement('span');
            page.className = 'immersive__results-page';
            page.textContent = h.page;
            const snippet = document.createElement('span');
            snippet.className = 'immersive__results-snippet';
            if (h.before || h.after) {
                const mark = document.createElement('mark');
                mark.className = 'immersive__results-match';
                mark.textContent = h.match || h.snippet || '';
                snippet.append(document.createTextNode(h.before ? `${h.before} ` : ''), mark, document.createTextNode(h.after ? ` ${h.after}` : ''));
            } else {
                snippet.textContent = h.snippet || '';
            }
            btn.append(page, snippet);
            btn.addEventListener('click', () => goToHit(i));
            li.append(btn);
            resultsList.appendChild(li);
        });
    };

    const goToHit = (i) => {
        if (!searchState.hits.length) return;
        searchState.activeIndex = (i + searchState.hits.length) % searchState.hits.length;
        const hit = searchState.hits[searchState.activeIndex];
        if (!viewer.getCurrentPages().includes(hit.order)) viewer.goToPage(hit.order);
        else renderHighlights();
        renderResults();
    };

    const runSearch = async (term) => {
        searchState.term = term;
        searchState.hits = term
            ? await search(pi, apiBase, term).catch((e) => {
                  console.error('immersive fulltext search failed', e);
                  return [];
              })
            : [];
        searchState.activeIndex = searchState.hits.length ? 0 : -1;
        renderResults();
        if (searchState.activeIndex >= 0) goToHit(0);
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

        const clearBtn = document.createElement('button');
        clearBtn.type = 'button';
        clearBtn.className = 'immersive__search-clear';
        clearBtn.textContent = '×';
        clearBtn.setAttribute('aria-label', (resultsBox && resultsBox.dataset.labelReset) || '');
        clearBtn.hidden = !searchInput.value;
        const group = searchInput.closest('.input-group') || searchInput.parentElement;
        group.insertBefore(clearBtn, group.querySelector('.input-group-addon') || null);
        const syncClear = () => {
            clearBtn.hidden = !searchInput.value;
        };
        const resetSearch = () => {
            searchInput.value = '';
            syncClear();
            runSearch('');
            searchInput.focus();
        };
        searchInput.addEventListener('input', syncClear);
        clearBtn.addEventListener('click', resetSearch);
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && searchInput.value) {
                e.preventDefault();
                e.stopPropagation();
                resetSearch();
            }
        });
    }
    document.querySelectorAll('[data-immersive-hit]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const n = searchState.hits.length;
            goToHit(btn.dataset.immersiveHit === 'next' ? nextIndex(searchState.activeIndex, n) : prevIndex(searchState.activeIndex, n));
        });
    });
}

/**
 * Mounts the thumbnail grid overlay lazily (riot `thumbnails` tag) with modal
 * focus handling and keeps its selection on the current page.
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 * @returns {function():void} toggles the overlay
 */
function setupOverviewGrid(viewer, pi, apiBase) {
    const GRID_LOADER_TIMEOUT_MS = 8000;
    const gridOverlay = document.getElementById('immersiveGridOverlay');
    const gridLoader = document.getElementById('immersiveGridLoader');
    const gridClose = gridOverlay && gridOverlay.querySelector('.immersive__grid-close');
    const gridTrigger = document.querySelector('[data-immersive-action="overview"]:not(.immersive__grid-close)');
    let gridMounted = false;
    let gridTag = null;
    let gridOpener = null;
    let gridTrapHandler = null;
    const gridFocusables = () =>
        Array.from(gridOverlay.querySelectorAll('a[href],button,input,[tabindex]:not([tabindex="-1"])')).filter((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
    const gridActions = new rxjs.Subject();
    gridActions.subscribe((e) => {
        if (e && e.action === 'clickImage' && typeof e.value === 'number') {
            viewer.goToPage(e.value);
            closeGrid();
        }
    });
    const syncGridSelection = () => {
        if (gridTag) {
            gridTag.opts.index = currentOrder(viewer);
            gridTag.update();
        }
    };
    viewer.onPageChange.subscribe(() => {
        if (gridOverlay && !gridOverlay.hidden) syncGridSelection();
    });
    const openGrid = () => {
        gridOpener = gridTrigger || document.activeElement;
        gridOverlay.hidden = false;
        gridOverlay.setAttribute('role', 'dialog');
        gridOverlay.setAttribute('aria-modal', 'true');
        const gridLabel = gridTrigger && gridTrigger.getAttribute('aria-label');
        if (gridLabel) gridOverlay.setAttribute('aria-label', gridLabel);
        gridTrapHandler = (e) => {
            if (e.key !== 'Tab') return;
            const f = gridFocusables();
            if (!f.length) return;
            const first = f[0];
            const last = f[f.length - 1];
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        };
        gridOverlay.addEventListener('keydown', gridTrapHandler);
        if (!gridMounted) {
            gridTag = riot.mount('#immersiveThumbnails', 'thumbnails', {
                source: `${apiBase}/records/${pi}/manifest`,
                type: 'sequence',
                actionlistener: gridActions,
                imagesize: '!320,440',
                index: currentOrder(viewer),
            })[0];
            gridMounted = true;
            let gridLoaderDone = false;
            let gridLoaderTimer;
            const hideGridLoader = () => {
                if (gridLoaderDone) return;
                gridLoaderDone = true;
                clearTimeout(gridLoaderTimer);
                if (gridLoader) gridLoader.hidden = true;
            };
            const thumbsMount = document.getElementById('immersiveThumbnails');
            if (thumbsMount) thumbsMount.addEventListener('load', hideGridLoader, { capture: true, once: true });
            gridLoaderTimer = setTimeout(hideGridLoader, GRID_LOADER_TIMEOUT_MS);
        } else {
            syncGridSelection();
        }
        if (gridClose) gridClose.focus();
    };
    const closeGrid = () => {
        if (!gridOverlay || gridOverlay.hidden) return;
        gridOverlay.hidden = true;
        if (gridTrapHandler) {
            gridOverlay.removeEventListener('keydown', gridTrapHandler);
            gridTrapHandler = null;
        }
        const restore = gridOpener || gridTrigger;
        gridOpener = null;
        if (restore && typeof restore.focus === 'function') restore.focus();
    };
    const toggleGrid = () => {
        if (!gridOverlay) return;
        if (gridOverlay.hidden) openGrid();
        else closeGrid();
    };

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && gridOverlay && !gridOverlay.hidden) {
            closeGrid();
        }
    });
    return toggleGrid;
}

/**
 * TOC drawer: entry clicks navigate in place (entries carry their 1-based page
 * number as data-page-no), and the active-section highlight follows the
 * currently visible page(s).
 *
 * @param {IvViewer} viewer
 */
function setupTocSync(viewer) {
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

const FILTER_TRIGGER_SELECTOR = '[data-popover-element="#immersiveFilterPopover"]';

/**
 * Whether the viewer's canvas is origin-clean (CORS): pixel filters would throw on a
 * tainted canvas. Checked both up front and again at mount time, because the taint
 * status can change once tiles have actually loaded.
 */
function isViewerOriginClean(viewer) {
    const image = viewer.viewer;
    return typeof image.isOriginClean !== 'function' || image.isOriginClean();
}

/**
 * Defers the imageFilters mount until the Filter popover is first shown: Bootstrap only
 * portals #immersiveFilterPopover (and its <imageFilters> child) into the DOM on show,
 * so mounting on load would find nothing and the popover would open empty. After the
 * mount the popover is repositioned, because riot fills it only after Popper has
 * already measured the still-empty shell.
 */
function bindImageFiltersMount(viewer) {
    const btn = document.querySelector(FILTER_TRIGGER_SELECTOR);
    if (!btn) return;
    if (!isViewerOriginClean(viewer)) {
        btn.hidden = true;
        return;
    }
    const $ = window.$ || window.jQuery;
    if (!$) {
        mountImageFilters(viewer);
        return;
    }
    $(btn).one('shown.bs.popover', () => {
        mountImageFilters(viewer);
        $(btn).popover('update');
    });
}

/**
 * Adds focus/keyboard management to the three immersive Bootstrap popovers (Share / Cite /
 * Filter) without touching the shared popovers controller. On open, focus moves into the
 * portaled popover; Escape inside it closes and returns focus to the trigger; aria-expanded
 * is kept in sync. Hooks only these triggers via Bootstrap's shown/hidden.bs.popover events.
 */
function setupImmersivePopoverA11y() {
    const $ = window.$ || window.jQuery;
    if (!$) return;
    const FOCUSABLE = 'a[href],button,input,select,textarea,[tabindex]:not([tabindex="-1"])';
    const selectors = ['#immersiveSharePopover', '#immersiveCitationPopover', '#immersiveFilterPopover'];
    selectors.forEach((sel) => {
        const trigger = document.querySelector(`[data-popover-element="${sel}"]`);
        if (!trigger || trigger.dataset.a11yBound === 'true') return;
        trigger.dataset.a11yBound = 'true';

        const popoverEl = () => {
            const id = trigger.getAttribute('aria-describedby');
            const byId = id && document.getElementById(id);
            if (byId) return byId;
            const all = Array.from(document.querySelectorAll('.popover')).filter((p) => p.offsetParent !== null);
            return all.length ? all[all.length - 1] : null;
        };

        $(trigger).on('shown.bs.popover', () => {
            trigger.setAttribute('aria-expanded', 'true');
            const pop = popoverEl();
            if (!pop) return;
            pop.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    $(trigger).popover('hide');
                    trigger.focus();
                }
            });
            const first = pop.querySelector(FOCUSABLE);
            if (first) {
                const $first = $(first);
                const hasTip = typeof $first.tooltip === 'function' && !!$first.data('bs.tooltip');
                if (hasTip) $first.tooltip('disable');
                first.focus();
                if (hasTip) $first.tooltip('enable');
            } else {
                if (!pop.hasAttribute('tabindex')) pop.setAttribute('tabindex', '-1');
                pop.focus();
            }
        });

        $(trigger).on('hidden.bs.popover', () => {
            trigger.setAttribute('aria-expanded', 'false');
            const active = document.activeElement;
            if (!active || active === document.body) trigger.focus();
        });
    });
}

/**
 * Mounts the reused imageFilters riot tag on the viewer's live ImageView.Image so the
 * Filter popover adjusts brightness/contrast/etc. Pixel filters need an origin-clean
 * canvas (CORS); if the tiles taint it, the Filter button is hidden instead.
 */
function mountImageFilters(viewer) {
    const btn = document.querySelector(FILTER_TRIGGER_SELECTOR);
    if (!btn || !document.querySelector('imageFilters') || !window.immersiveFilterConfig) return;
    if (isViewerOriginClean(viewer)) {
        riot.mount('imageFilters', { image: viewer.viewer, config: window.immersiveFilterConfig });
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
