/** Layout-tag panel wiring: index and detail loading, page range, cursor and image highlights; pick menu and hit list are sub-modules. */

import {
    VISIBLE_BATCH,
    FILTER_THRESHOLD,
    STROKE_ELEMENT_TYPE,
    entryKey,
    typeClassOf,
    visiblePages,
    batchRanges,
    loadLayoutTagIndex,
    loadLayoutTagDetails,
} from '../../data/ivLayoutTags.mjs';
import { createDisposer, noop } from '../../shared/ivLifecycle.mjs';
import { createLayoutTagsState } from './ivLayoutTagsState.mjs';
import { setupPickMenu } from './ivLayoutTagsPickMenu.mjs';
import { createHitList } from './ivLayoutTagsHitList.mjs';

const OWNER = 'layout-tags';
/** Modifier drawing an occurrence as a bar instead of a box. */
const STROKE_CLASS = '-stroke';

/**
 * Wires the layout-tag panel: the record-wide index is fetched once (on the
 * first open) and drives a multi-select pick menu (one entry per tag, or per
 * configured group of tags that are selected together), a page-range row and
 * a hit list grouped by page with one row per tag. Page details (occurrence
 * counts and rects) are loaded only for the rows in view, in small page-range
 * batches. Activating a hit opens the page and highlights every occurrence of
 * that tag on it; the highlights are redrawn on page change and cleared when
 * the panel closes.
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 * @param {{closePanels: Function, registerCloseHook: Function}} panels
 * @param {{register: Function}} keys  the immersive key dispatcher
 * @returns {{destroy: function():void}}
 */
export function setupLayoutTagsPanel(viewer, pi, apiBase, panels, keys) {
    const panel = document.getElementById('immersivePanelLayoutTags');
    if (!panel) return noop;
    const q = (sel) => panel.querySelector(sel);
    const sticky = q('[data-immersive-layout-tags="sticky"]');
    const rangeFrom = q('[data-immersive-layout-tags="from"]');
    const rangeTo = q('[data-immersive-layout-tags="to"]');
    const rangeApply = q('[data-immersive-layout-tags="apply"]');
    const rangeReset = q('[data-immersive-layout-tags="reset"]');
    const pickTrigger = q('[data-immersive-layout-tags="pick-trigger"]');
    const pickMenu = q('[data-immersive-layout-tags="pick-menu"]');
    const pickList = q('[data-immersive-layout-tags="pick-list"]');
    const filterWrap = q('[data-immersive-layout-tags="filter"]');
    const loader = q('[data-immersive-layout-tags="loader"]');
    const hitsMount = q('[data-immersive-layout-tags="hits"]');
    if (!rangeFrom || !rangeTo || !pickTrigger || !pickMenu || !pickList || !hitsMount) return noop;
    const bag = createDisposer();
    let destroyed = false;
    const labels = panel.dataset;
    const state = createLayoutTagsState(viewer.getPageCount());
    const panelOpen = () => panel.classList.contains('is-open');
    const currentPages = () => viewer.getCurrentPages().map((o) => o + 1);

    const pick = setupPickMenu({
        state,
        labels,
        trigger: pickTrigger,
        menu: pickMenu,
        list: pickList,
        dots: q('[data-immersive-layout-tags="pick-dots"]'),
        text: q('[data-immersive-layout-tags="pick-text"]'),
        filterWrap,
        filterInput: filterWrap && filterWrap.querySelector('input'),
        keys,
        onToggle: (key) => toggleItem(key),
    });
    bag.add(() => pick.destroy());
    const hits = createHitList({ state, labels, mount: hitsMount, panel, sticky, progress: q('[data-immersive-layout-tags="progress"]'), onActivate: (i) => goToEntry(i) });

    /* PAGE RANGE */

    const syncRangeButtons = () => {
        const dirty = Number(rangeFrom.value) !== state.from || Number(rangeTo.value) !== state.to;
        if (rangeApply) rangeApply.hidden = !dirty;
        if (rangeReset) rangeReset.hidden = dirty || state.rangeIsFull();
    };
    const syncRange = () => {
        const editing = document.activeElement === rangeFrom || document.activeElement === rangeTo;
        if (!editing) {
            rangeFrom.value = String(state.from);
            rangeTo.value = String(state.to);
        }
        syncRangeButtons();
    };
    const setRange = (from, to) => {
        if (!state.setRange(from, to, currentPages())) {
            syncRange();
            return;
        }
        refresh(true);
    };
    const applyRange = () => {
        setRange(rangeFrom.value === '' ? 1 : rangeFrom.value, rangeTo.value === '' ? state.pageCount : rangeTo.value);
        rangeFrom.value = String(state.from);
        rangeTo.value = String(state.to);
        syncRangeButtons();
        if (document.activeElement === document.body) rangeFrom.focus();
    };
    [rangeFrom, rangeTo].forEach((input) => {
        input.max = String(state.pageCount);
        bag.listen(input, 'input', syncRangeButtons);
        bag.listen(input, 'keydown', (e) => {
            if (e.key !== 'Enter') return;
            e.preventDefault();
            applyRange();
        });
    });
    if (rangeApply) bag.listen(rangeApply, 'click', applyRange);
    if (rangeReset) {
        bag.listen(rangeReset, 'click', () => {
            setRange(1, state.pageCount);
            rangeFrom.value = String(state.from);
            rangeTo.value = String(state.to);
            syncRangeButtons();
        });
    }

    /* DETAIL LOADING */

    const loadPages = (pages) => {
        const missing = state.missingPages(pages);
        if (!missing.length) return;
        batchRanges(missing, VISIBLE_BATCH).forEach(({ start, end }) => {
            const span = [];
            for (let p = start; p <= end; p++) {
                if (!state.details.has(p) && !state.inFlight.has(p)) span.push(p);
            }
            state.markInFlight(span);
            loadLayoutTagDetails(pi, apiBase, start, end)
                .then((details) => state.storeDetails(span, details))
                .catch((e) => {
                    console.warn('immersive layout tags failed', e);
                    state.storeDetails(span, null);
                })
                .then(() => {
                    if (destroyed) return;
                    if (!state.cursorKey) state.preferCursorOnPages(currentPages());
                    render();
                    renderHighlights();
                    pumpVisible();
                })
                .catch((e) => console.warn('immersive layout tags render failed', e));
        });
    };
    const pumpVisible = () => {
        if (!panelOpen() || !state.selected.size) return;
        const wanted = visiblePages(panel.getBoundingClientRect(), hits.pendingRows(), VISIBLE_BATCH);
        if (wanted.length) loadPages(wanted);
    };
    const loadCursorPage = () => {
        const active = state.cursorEntry();
        if (active && active.pending) loadPages([active.page]);
    };

    /* CURSOR AND HIGHLIGHTS */

    const render = (force = false) => {
        hits.render(force);
        pick.sync();
        syncRange();
    };
    const refresh = (force) => {
        render(force);
        loadCursorPage();
        pumpVisible();
        renderHighlights();
    };
    const toggleItem = (key) => {
        state.toggleItem(key, currentPages());
        refresh(true);
    };
    const goToEntry = (i) => {
        const list = state.entries();
        if (!list.length) return;
        const entry = list[Math.max(0, Math.min(i, list.length - 1))];
        state.setCursor(entryKey(entry));
        hits.markActive();
        if (entry.pending) loadPages([entry.page]);
        if (!currentPages().includes(entry.page)) viewer.goToPage(entry.page - 1);
        else renderHighlights();
    };
    const renderHighlights = () => {
        if (!panelOpen()) return;
        const active = state.cursorEntry();
        if (!active || !currentPages().includes(active.page)) {
            viewer.clearHighlights(OWNER);
            return;
        }
        const tokens = [...state.styleTokens(active.label), typeClassOf(state.tagType(active.label))];
        const rects = (state.occurrencesOf(active.page, active.label) || []).map((o) => ({
            ...o.rect,
            order: active.page - 1,
            className: [...tokens, o.elementType === STROKE_ELEMENT_TYPE ? STROKE_CLASS : ''].filter(Boolean).join(' '),
            active: true,
        }));
        viewer.setHighlights(rects, OWNER);
    };

    /* INDEX AND EVENTS */

    let indexRequest = null;
    const ensureIndex = () => {
        if (state.index) return Promise.resolve(state.index);
        if (indexRequest) return indexRequest;
        if (loader) loader.hidden = false;
        indexRequest = loadLayoutTagIndex(pi, apiBase, fetch, document.documentElement.lang)
            .then((index) => {
                state.setIndex(index);
                pick.setFilterVisible(index.tags.length >= FILTER_THRESHOLD);
                pick.build();
                return index;
            })
            .catch((e) => {
                console.error('immersive layout-tag index failed', e);
                state.setFailed();
                return null;
            })
            .then((index) => {
                indexRequest = null;
                if (loader) loader.hidden = true;
                return index;
            });
        return indexRequest;
    };

    bag.listen(panel, 'immersive:panel-open', () => {
        ensureIndex().then(() => {
            if (destroyed || !panelOpen()) return;
            if (!state.cursorKey) state.preferCursorOnPages(currentPages());
            refresh(true);
            if (!state.selected.size && state.allTags().length) {
                pick.open({ focus: false });
            }
        });
    });
    bag.add(
        panels.registerCloseHook(() => {
            if (!panelOpen()) return;
            pick.close();
            viewer.clearHighlights(OWNER);
        })
    );
    bag.add(
        viewer.onPageChange.subscribe(() => {
            if (!panelOpen() || !state.index) return;
            const pages = currentPages();
            const cursor = state.cursorEntry();
            const onPage = cursor && pages.includes(cursor.page) ? cursor : state.entries().find((e) => pages.includes(e.page));
            if (onPage) state.setCursor(entryKey(onPage));
            hits.markActive();
            renderHighlights();
            if (onPage && onPage.pending) loadPages([onPage.page]);
        })
    );
    bag.add(viewer.onOpen.subscribe(renderHighlights));
    let scrollQueued = false;
    bag.listen(panel, 'scroll', () => {
        if (scrollQueued) return;
        scrollQueued = true;
        requestAnimationFrame(() => {
            scrollQueued = false;
            if (sticky) sticky.classList.toggle('-scrolled', panel.scrollTop > 0);
            pumpVisible();
        });
    });

    syncRange();
    return {
        destroy() {
            destroyed = true;
            pick.close();
            viewer.clearHighlights(OWNER);
            bag.dispose();
        },
    };
}
