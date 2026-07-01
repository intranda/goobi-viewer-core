import ZoomableImage from './media/zoomableImage.mjs';
import ShareImageFragment from './media/shareImageFragment.mjs';
import Voyager3dView from './media/voyager3DViewer.mjs';
import IvViewer from './media/ivViewer.mjs';
import { loadPageServices, loadPageLabels, loadPageRegions } from './media/ivManifestSource.mjs';
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
    // Closed panels sit off-screen (transform); start them inert so their focusable children
    // stay out of the tab order and the a11y tree until the panel is actually opened.
    document.querySelectorAll('.immersive__panel--left').forEach((p) => (p.inert = true));
    // Flag the root while a left panel is open so CSS can hide the floating title and
    // prev chevron over the image (the title + close live in the panel header now).
    const syncPanelOpenFlag = () => {
        if (immersiveRoot) {
            immersiveRoot.classList.toggle('immersive--panel-open', !!document.querySelector('.immersive__panel--left.is-open'));
        }
    };
    // Set by the fulltext block; clears its image overlays + hover wiring when the panel closes.
    let onFulltextClose = null;
    // The rail button that opened the current panel, so focus can return to it on close.
    let activePanelBtn = null;
    const closePanels = (restoreFocus = false) => {
        if (typeof onFulltextClose === 'function') onFulltextClose();
        const closedAny = !!document.querySelector('.immersive__panel--left.is-open');
        document.querySelectorAll('.immersive__panel--left.is-open').forEach((p) => {
            p.classList.remove('is-open');
            p.setAttribute('aria-hidden', 'true');
            // Off-screen again: make it inert so it drops out of tab order + screen reader.
            p.inert = true;
        });
        panelButtons.forEach((b) => {
            b.classList.remove('immersive__tool-btn--active');
            b.setAttribute('aria-expanded', 'false');
        });
        syncPanelOpenFlag();
        // Return focus to the rail button that opened the panel (focus would otherwise be
        // lost when the panel becomes inert, e.g. on Escape from inside the panel).
        if (restoreFocus && closedAny && activePanelBtn) activePanelBtn.focus();
        activePanelBtn = null;
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
                panel.inert = false;
                btn.classList.add('immersive__tool-btn--active');
                btn.setAttribute('aria-expanded', 'true');
                activePanelBtn = btn;
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
                // Move focus into the panel so keyboard users land inside it. Prefer a
                // meaningful control; fall back to the panel container itself.
                const focusTarget = panel.querySelector('input, a[href], button');
                if (focusTarget) {
                    focusTarget.focus({ preventScroll: true });
                } else {
                    if (!panel.hasAttribute('tabindex')) panel.setAttribute('tabindex', '-1');
                    panel.focus({ preventScroll: true });
                }
            }
        });
    });
    // No close button in the panel anymore: Escape closes the open panel (re-clicking
    // the burger toggles it shut too).
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && document.querySelector('.immersive__panel--left.is-open')) {
            closePanels(true);
        }
    });

    // Sidebar resize: one drag handle at the open panel's right edge sets a single
    // --immersive-panel-width on .immersive__viewer, so all left panels share one width.
    // The chosen width persists in localStorage and is re-applied (clamped) on load.
    const immersiveViewer = immersiveRoot && immersiveRoot.querySelector('.immersive__viewer');
    if (immersiveViewer) {
        const WIDTH_KEY = 'immersive-panel-width';
        const RAIL_WIDTH = 40; // left tool rail; panels start at left: 40px
        const MIN_WIDTH = 240;
        const maxWidth = () => immersiveViewer.getBoundingClientRect().width * 0.8;
        const clamp = (px) => Math.min(Math.max(px, MIN_WIDTH), maxWidth());
        const applyWidth = (px) => immersiveViewer.style.setProperty('--immersive-panel-width', Math.round(px) + 'px');
        let stored = NaN;
        try {
            stored = parseInt(localStorage.getItem(WIDTH_KEY), 10);
        } catch (e) {
            // localStorage may be unavailable (private mode / blocked) -- defaults apply.
        }
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
                } catch (err) {
                    // ignore: nothing to persist if storage is unavailable
                }
            };
            handle.addEventListener('pointermove', onMove);
            handle.addEventListener('pointerup', onUp);
        });
    }

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

    // Metadata "more / less" fold: the first 1-2 blocks (ISANCHOR rule, server-rendered)
    // stay; deeper structural blocks (chapters) live in #immersiveMetadataMore and are
    // revealed by this toggle. Server-rendered, so bound once (no viewer needed).
    const metadataToggle = document.querySelector('[data-immersive-metadata-toggle]');
    if (metadataToggle) {
        const moreEl = document.getElementById(metadataToggle.dataset.immersiveMetadataToggle);
        if (moreEl) {
            // Only the visible label should be in the accessible name (else it reads "more less").
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

    // Accessible focus/keyboard management for the three immersive Bootstrap popovers
    // (Share / Cite / Filter). The shared popovers controller isn't touched: we only hook
    // its Bootstrap shown/hidden events on these specific triggers to move focus into the
    // portaled popover on open, trap Escape to close + restore focus, and sync aria-expanded.
    setupImmersivePopoverA11y();

    loadPageServices(pi, apiBase)
        .then((services) => {
            const viewer = new IvViewer({ element: el, services, startOrder, maxZoom });
            window.ivViewer = viewer;
            attachUrlSync(viewer, pi);
            // The <imageFilters> tag lives inside #immersiveFilterPopover, which Bootstrap
            // only portals into the DOM when the popover is first shown. Mount lazily on that
            // event (guarded once) instead of on load, when the target isn't rendered yet.
            viewer.onLoaded.subscribe(() => bindImageFiltersMount(viewer));

            const indicator = document.getElementById('immersivePageIndicator');
            const titlePage = document.getElementById('immersiveTitlePage');
            const total = viewer.getPageCount();
            const workTitle = (document.querySelector('.immersive__title-text')?.textContent || '').trim();
            const updateIndicator = () => {
                const pages = viewer.getCurrentPages().map((p) => p + 1);
                const label = pages.length > 1 ? `${pages[0]}–${pages[pages.length - 1]}` : `${pages[0]}`;
                if (indicator) indicator.textContent = `${label} / ${total}`;
                // Mirror the page next to the work title, e.g. "(5 / 40)".
                if (titlePage) titlePage.textContent = `(${label} / ${total})`;
                // Name the (otherwise silent) OSD canvas for screen readers, updated per page.
                const imageSurface = el.querySelector('.openseadragon-canvas') || el;
                imageSurface.setAttribute('role', 'img');
                imageSurface.setAttribute('aria-label', workTitle ? `${workTitle}, ${label} / ${total}` : `${label} / ${total}`);
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

            // Title page picker: clicking the work title opens a dropdown with a "go to page"
            // input and a scrollable list of the IIIF manifest page labels. Selecting jumps
            // the viewer. Labels share the memoized manifest fetch, so this costs no request.
            const setupPageDropdown = (labels) => {
                const trigger = document.querySelector('[data-immersive-title-trigger]');
                const dropdown = document.getElementById('immersivePageDropdown');
                const list = document.getElementById('immersivePageList');
                const input = document.getElementById('immersivePageInput');
                if (!trigger || !dropdown || !list) return;
                // A single-page record has nothing to pick: leave the title non-interactive.
                if (total < 2) {
                    trigger.classList.add('immersive__title-trigger--static');
                    return;
                }
                if (input) input.max = String(total);
                // Name the listbox for screen readers, reusing the "go to page" label.
                const listLabel = input && input.labels && input.labels[0] ? input.labels[0].textContent.trim() : '';
                if (listLabel) list.setAttribute('aria-label', listLabel);

                // One row per page as "<running number>: <raw manifest label>", e.g.
                // "1: -", "3: [1]", "8: 5" -- the manifest label is shown verbatim
                // (a blank " - " label trims to "-"), like the classic page dropdown.
                // role="option"/aria-selected live on the button (the actual option); the
                // <li> is presentational so the listbox exposes one option per button.
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
                    // Roving tabindex: only the current option is tabbable.
                    btn.tabIndex = -1;
                    const num = document.createElement('span');
                    num.className = 'immersive__page-dropdown-num';
                    num.textContent = `${order + 1}:`;
                    const lbl = document.createElement('span');
                    lbl.className = 'immersive__page-dropdown-itemlabel';
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
                        btn.classList.toggle('is-active', on);
                        btn.setAttribute('aria-selected', on ? 'true' : 'false');
                        // Roving tabindex follows the current page so Tab lands on it.
                        btn.tabIndex = on && !rovingSet ? 0 : -1;
                        if (on) rovingSet = true;
                    });
                    // No current option in the list -> keep the first one reachable via Tab.
                    if (!rovingSet && items.length) items[0].tabIndex = 0;
                };

                // Move the roving tabindex to a given option and focus it (keyboard nav).
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
                    // Centre the active row WITHIN the list only -- never via scrollIntoView /
                    // focus(), which would scroll the page and visibly shift the image.
                    const active = list.querySelector('.immersive__page-dropdown-item.is-active');
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
                // Listbox keyboard nav: arrows move the roving focus, Home/End jump to the
                // ends, Enter/Space activate the focused option, Escape closes the dropdown.
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
                    if (e.key === 'Escape' && isOpen()) {
                        close();
                        trigger.focus();
                    }
                });
                viewer.onPageChange.subscribe(markActive);
                markActive();
            };
            loadPageLabels(pi, apiBase).then(setupPageDropdown).catch(() => {});

            // Fulltext: in-place IIIF content search → result list (left panel) + image hit highlights.
            const fts = { hits: [], idx: -1, term: '' };
            const resultsBox = document.getElementById('immersiveSearchResults');
            const resultsList = document.getElementById('immersiveResultsList');
            const hitCounter = document.getElementById('immersiveHitCounter');
            const hitsOnOrder = (order) => fts.hits.filter((h) => h.order === order);

            const renderHighlights = () => {
                // Highlight every match on the current page(s), but flag the active hit so it
                // stands out (e.g. two matches of the same word on one page).
                const activeHit = fts.hits[fts.idx];
                const rects = viewer.getCurrentPages().flatMap((o) =>
                    hitsOnOrder(o)
                        .filter((h) => h.rect)
                        .map((h) => ({ ...h.rect, active: h === activeHit }))
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
                    // Teaser: context before + the highlighted match + context after. Falls
                    // back to the plain matched word when the backend sends no before/after.
                    if (h.before || h.after) {
                        const mark = document.createElement('mark');
                        mark.className = 'immersive__results-match';
                        mark.textContent = h.match || h.snippet || '';
                        // The backend trims the boundary whitespace from before/after -- restore
                        // a single space so the match doesn't glue to the context ("Geschichtedas").
                        snippet.append(
                            document.createTextNode(h.before ? `${h.before} ` : ''),
                            mark,
                            document.createTextNode(h.after ? ` ${h.after}` : '')
                        );
                    } else {
                        snippet.textContent = h.snippet || '';
                    }
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

                // Reset affordance: an (×) inside the field (+ Escape) that empties the input
                // and clears the result list and image highlights. Shown only when there's text.
                const clearBtn = document.createElement('button');
                clearBtn.type = 'button';
                clearBtn.className = 'immersive__search-clear';
                clearBtn.textContent = '×';
                clearBtn.setAttribute('aria-label', (resultsBox && resultsBox.dataset.labelReset) || 'Reset');
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
                        resetSearch();
                    }
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
                // Disabled -> drop out of the tab order so it can't be focused/activated.
                fulltextBtn.setAttribute('tabindex', doublePage ? '-1' : '0');
                const title = (doublePage && fulltextBtn.dataset.titleDisabled) || fulltextTitleDefault;
                fulltextBtn.setAttribute('title', title);
                fulltextBtn.setAttribute('aria-label', title);
                if (doublePage && fulltextPanel && fulltextPanel.classList.contains('is-open')) closePanels();
            };
            updateFulltextAvail();

            // Overview: thumbnail grid overlay (lazy-mounted).
            const gridOverlay = document.getElementById('immersiveGridOverlay');
            const gridLoader = document.getElementById('immersiveGridLoader');
            const gridClose = gridOverlay && gridOverlay.querySelector('.immersive__grid-close');
            const gridTrigger = document.querySelector('[data-immersive-action="overview"]:not(.immersive__grid-close)');
            let gridMounted = false;
            let gridTag = null;
            // Element to restore focus to when the overlay closes (the opener).
            let gridOpener = null;
            // Focus-trap keydown handler, added on open and removed on close.
            let gridTrapHandler = null;
            const gridFocusables = () =>
                Array.from(gridOverlay.querySelectorAll('a[href],button,input,[tabindex]:not([tabindex="-1"])')).filter(
                    (n) => !n.hidden && !n.disabled && n.offsetParent !== null
                );
            const gridActions = new rxjs.Subject();
            gridActions.subscribe((e) => {
                if (e && e.action === 'clickImage' && typeof e.value === 'number') {
                    viewer.goToPage(e.value);
                    closeGrid();
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
            const openGrid = () => {
                // Remember the opener so focus returns there on close.
                gridOpener = gridTrigger || document.activeElement;
                gridOverlay.hidden = false;
                // Modal dialog semantics + accessible name (reuse the trigger's label).
                gridOverlay.setAttribute('role', 'dialog');
                gridOverlay.setAttribute('aria-modal', 'true');
                const gridLabel = gridTrigger && gridTrigger.getAttribute('aria-label');
                if (gridLabel) gridOverlay.setAttribute('aria-label', gridLabel);
                // Focus trap: keep Tab/Shift+Tab cycling within the overlay.
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
                // Move focus into the dialog (the close button).
                if (gridClose) gridClose.focus();
            };
            const closeGrid = () => {
                if (!gridOverlay || gridOverlay.hidden) return;
                gridOverlay.hidden = true;
                if (gridTrapHandler) {
                    gridOverlay.removeEventListener('keydown', gridTrapHandler);
                    gridTrapHandler = null;
                }
                // Return focus to whatever opened the overlay.
                const restore = gridOpener || gridTrigger;
                gridOpener = null;
                if (restore && typeof restore.focus === 'function') restore.focus();
            };
            const toggleGrid = () => {
                if (!gridOverlay) return;
                if (gridOverlay.hidden) openGrid();
                else closeGrid();
            };

            // Esc closes the open overview overlay (mirrors the close button).
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && gridOverlay && !gridOverlay.hidden) {
                    closeGrid();
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
            const fullscreenBtn = document.querySelector('[data-immersive-action="fullscreen"]');
            document.addEventListener('fullscreenchange', () => {
                // Expose the fullscreen toggle's on/off state (no exit-label message wired
                // in the markup, so at least announce pressed state).
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
 * Defers the imageFilters mount until the Filter popover is first shown. Bootstrap only
 * portals #immersiveFilterPopover (and its <imageFilters> child) into the DOM on show, so
 * mounting on load finds nothing and the popover opens empty. Mounts once on the first
 * shown.bs.popover; the origin-clean check may still hide the button before it can open.
 */
function bindImageFiltersMount(viewer) {
    const btn = document.querySelector('[data-popover-element="#immersiveFilterPopover"]');
    if (!btn) return;
    // Origin-tainted tiles can't be filtered (CORS): hide the Filter button up front.
    const image = viewer.viewer;
    const originClean = typeof image.isOriginClean !== 'function' || image.isOriginClean();
    if (!originClean) {
        btn.hidden = true;
        return;
    }
    const $ = window.$ || window.jQuery;
    if (!$) {
        // No jQuery -> fall back to the immediate (pre-portal) mount attempt.
        mountImageFilters(viewer);
        return;
    }
    // Mount once, the first time Bootstrap shows the popover (element now in the DOM).
    // riot fills the popover *after* Popper positioned the still-empty shell, so the
    // grown content would hang below the trigger on that first open; reposition once
    // mounted so it sits above the button like on every later open.
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
        // Guard against double-binding if init runs more than once.
        if (!trigger || trigger.dataset.a11yBound === 'true') return;
        trigger.dataset.a11yBound = 'true';

        // Resolve the portaled .popover element: Bootstrap sets aria-describedby on the
        // trigger while shown; fall back to the last visible .popover in the DOM.
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
            // Escape inside the popover closes it and returns focus to the trigger.
            pop.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    $(trigger).popover('hide');
                    trigger.focus();
                }
            });
            // Move focus into the popover (first control, else the container itself).
            const first = pop.querySelector(FOCUSABLE);
            if (first) {
                // Bootstrap tooltips fire on focus, so auto-focusing the first control on
                // open would flash its tooltip (e.g. the share links' "share on X"). Disable
                // it across the programmatic focus, then re-enable so hover/focus still work.
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
            // The popover was removed from the DOM: if focus was inside it (or fell to body),
            // pull it back to the trigger so keyboard users aren't stranded.
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
