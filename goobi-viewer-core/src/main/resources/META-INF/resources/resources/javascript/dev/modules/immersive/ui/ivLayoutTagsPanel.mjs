/** Layout-tag panel wiring: tag pick menu, page range, per-page hit list with lazy detail loading, image highlights. */

import {
    VISIBLE_BATCH,
    FILTER_THRESHOLD,
    formatMessage,
    tagPages,
    hitPages,
    flatEntries,
    entryKey,
    cursorIndex,
    clampRange,
    filterTags,
    visiblePages,
    batchRanges,
    loadLayoutTagIndex,
    loadLayoutTagDetails,
} from '../data/ivLayoutTags.mjs';
import { styleClassTokens, el } from '../shared/ivDom.mjs';
import { createDisposer, noop } from '../shared/ivLifecycle.mjs';

/** Number of colour dots the pick trigger shows for the selected tags. */
const TRIGGER_DOTS = 4;

/** Key-dispatcher priority of the pick menu's Escape: under the page dropdown (30), above the panels (40). */
const PICK_KEY_PRIORITY = 35;

/**
 * Wires the layout-tag panel: the record-wide index is fetched once (on the
 * first open) and drives a multi-select pick menu, a page-range row and a hit
 * list grouped by page. Page details (occurrence counts and rects) are loaded
 * only for the rows in view, in small page-range batches. Activating a hit
 * opens the page and highlights every occurrence of that tag on it; the
 * highlights are redrawn on page change and cleared when the panel closes.
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
    const pickDots = q('[data-immersive-layout-tags="pick-dots"]');
    const pickText = q('[data-immersive-layout-tags="pick-text"]');
    const pickMenu = q('[data-immersive-layout-tags="pick-menu"]');
    const filterWrap = q('[data-immersive-layout-tags="filter"]');
    const filterInput = filterWrap && filterWrap.querySelector('input');
    const pickList = q('[data-immersive-layout-tags="pick-list"]');
    const loader = q('[data-immersive-layout-tags="loader"]');
    const hitsMount = q('[data-immersive-layout-tags="hits"]');
    const progress = q('[data-immersive-layout-tags="progress"]');
    if (!rangeFrom || !rangeTo || !pickTrigger || !pickMenu || !pickList || !hitsMount) return noop;
    const bag = createDisposer();
    const labels = panel.dataset;
    const pageCount = viewer.getPageCount();

    const state = {
        index: null,
        failed: false,
        selected: new Set(),
        from: 1,
        to: pageCount,
        filter: '',
        cursorKey: null,
        details: new Map(),
        inFlight: new Set(),
    };

    const panelOpen = () => panel.classList.contains('is-open');
    const currentPages = () => viewer.getCurrentPages().map((o) => o + 1);
    const rangeIsFull = () => state.from === 1 && state.to === pageCount;
    const allTags = () => (state.index ? state.index.tags : []);
    const tagByLabel = (label) => allTags().find((t) => t.label === label) || null;
    const selectedTags = () => allTags().filter((t) => state.selected.has(t.label));
    const occurrencesOf = (page, label) => {
        if (!state.details.has(page)) return undefined;
        const byLabel = state.details.get(page);
        return byLabel ? byLabel[label] || [] : null;
    };
    const entries = () => flatEntries(selectedTags(), state.from, state.to, occurrencesOf);
    const displayName = (label) => {
        const tag = tagByLabel(label);
        return tag ? tag.name : label;
    };
    const styleTokens = (label) => {
        const tag = tagByLabel(label);
        return tag ? styleClassTokens(tag.styleClass) : [];
    };

    /* PICK MENU */

    const isPickOpen = () => !pickMenu.hidden;
    const onPickOutside = (e) => {
        if (!pickMenu.contains(e.target) && !pickTrigger.contains(e.target)) closePick();
    };
    const openPick = () => {
        if (isPickOpen()) return;
        pickMenu.hidden = false;
        pickTrigger.setAttribute('aria-expanded', 'true');
        document.addEventListener('pointerdown', onPickOutside, true);
        const first = filterWrap && !filterWrap.hidden && filterInput ? filterInput : pickMenu.querySelector('input[type="checkbox"]:not([disabled])');
        if (first) first.focus();
    };
    const closePick = (restoreFocus = false) => {
        if (!isPickOpen()) return;
        pickMenu.hidden = true;
        pickTrigger.setAttribute('aria-expanded', 'false');
        document.removeEventListener('pointerdown', onPickOutside, true);
        if (restoreFocus) pickTrigger.focus();
    };

    /** Rebuilds the checkbox rows of the pick menu from the index, range and filter. */
    const buildPickRows = () => {
        pickList.replaceChildren();
        const list = filterTags(allTags(), state);
        if (!list.length) {
            pickList.append(el('p', 'immersive__layout-tags-pick-none', labels.labelFilterNone || ''));
            return;
        }
        list.forEach((tag, i) => {
            const inRange = tagPages(tag, state.from, state.to).length;
            const row = el('div', 'immersive__layout-tags-pick-row');
            row.classList.add(...styleTokens(tag.label));
            row.dataset.tagLabel = tag.label;
            const id = `immersiveLayoutTagsPick-${i}`;
            const cb = el('input', 'immersive__layout-tags-pick-checkbox');
            cb.type = 'checkbox';
            cb.id = id;
            cb.addEventListener('change', () => toggleTag(tag.label));
            const label = el('label', 'immersive__layout-tags-pick-label');
            label.htmlFor = id;
            label.title = tag.label;
            const dot = el('span', 'immersive__layout-tags-dot');
            dot.setAttribute('aria-hidden', 'true');
            label.append(dot, el('span', 'immersive__layout-tags-pick-name', tag.name));
            row.append(cb, label, el('span', 'immersive__layout-tags-pick-count', String(inRange)));
            pickList.append(row);
        });
        syncPickRows();
    };

    /** Aligns checked/disabled state and in-range counts of the existing rows with the state. */
    const syncPickRows = () => {
        pickList.querySelectorAll('.immersive__layout-tags-pick-row').forEach((row) => {
            const tag = tagByLabel(row.dataset.tagLabel);
            if (!tag) return;
            const inRange = tagPages(tag, state.from, state.to).length;
            const selected = state.selected.has(tag.label);
            const cb = row.querySelector('input');
            const count = row.querySelector('.immersive__layout-tags-pick-count');
            row.classList.toggle('-empty', !inRange);
            cb.checked = selected;
            cb.disabled = !inRange && !selected;
            if (count) count.textContent = String(inRange);
        });
    };

    /** Trigger text, colour dots and accessible name follow the selection. */
    const syncPick = () => {
        const chosen = selectedTags();
        if (pickDots) {
            pickDots.replaceChildren();
            chosen
                .filter((tag) => styleClassTokens(tag.styleClass).length)
                .slice(0, TRIGGER_DOTS)
                .forEach((tag) => {
                    const dot = el('span', 'immersive__layout-tags-dot');
                    dot.classList.add(...styleClassTokens(tag.styleClass));
                    pickDots.append(dot);
                });
        }
        let text = labels.labelPick || '';
        if (chosen.length === 1 || chosen.length === 2) text = chosen.map((t) => t.name).join(', ');
        else if (chosen.length > 2) text = formatMessage(labels.labelPickMore, chosen[0].name, chosen.length - 1);
        if (pickText) pickText.textContent = text;
        pickTrigger.setAttribute('aria-label', chosen.length ? formatMessage(labels.labelPickSelected, chosen.length) : labels.labelPick || '');
        syncPickRows();
    };

    bag.listen(pickTrigger, 'click', () => (isPickOpen() ? closePick() : openPick()));
    if (filterInput) {
        bag.listen(filterInput, 'input', () => {
            state.filter = filterInput.value;
            buildPickRows();
        });
    }
    bag.add(
        keys.register(PICK_KEY_PRIORITY, (e) => {
            if (e.key !== 'Escape' || !isPickOpen()) return false;
            closePick(true);
            return true;
        })
    );

    /* PAGE RANGE */

    /** Shows either OK (unsaved input) or the reset button (narrowed range), never both. */
    const syncRangeButtons = () => {
        const dirty = Number(rangeFrom.value) !== state.from || Number(rangeTo.value) !== state.to;
        if (rangeApply) rangeApply.hidden = !dirty;
        if (rangeReset) rangeReset.hidden = dirty || rangeIsFull();
    };

    /** Writes the state into the fields unless one of them is being edited. */
    const syncRange = () => {
        const editing = document.activeElement === rangeFrom || document.activeElement === rangeTo;
        if (!editing) {
            rangeFrom.value = String(state.from);
            rangeTo.value = String(state.to);
        }
        syncRangeButtons();
    };

    const setRange = (from, to) => {
        const next = clampRange(from, to, pageCount);
        if (next.from === state.from && next.to === state.to) {
            syncRange();
            return;
        }
        state.from = next.from;
        state.to = next.to;
        if (!entries().some((e) => entryKey(e) === state.cursorKey)) {
            state.cursorKey = null;
            preferCursorOnCurrentPage();
        }
        render(true);
        loadCursorPage();
        pumpVisible();
        renderHighlights();
    };

    const applyRange = () => {
        setRange(rangeFrom.value === '' ? 1 : rangeFrom.value, rangeTo.value === '' ? pageCount : rangeTo.value);
        rangeFrom.value = String(state.from);
        rangeTo.value = String(state.to);
        syncRangeButtons();
        if (document.activeElement === document.body) rangeFrom.focus();
    };

    [rangeFrom, rangeTo].forEach((input) => {
        input.max = String(pageCount);
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
            setRange(1, pageCount);
            rangeFrom.value = String(state.from);
            rangeTo.value = String(state.to);
            syncRangeButtons();
        });
    }

    /* DETAIL LOADING */

    /** Fetches the details of the given pages in contiguous range batches; a failed batch marks its pages as failed. */
    const loadPages = (pages) => {
        const missing = pages.filter((p) => !state.details.has(p) && !state.inFlight.has(p));
        if (!missing.length) return;
        batchRanges(missing, VISIBLE_BATCH).forEach(({ start, end }) => {
            const span = [];
            for (let p = start; p <= end; p++) {
                if (!state.details.has(p) && !state.inFlight.has(p)) span.push(p);
            }
            span.forEach((p) => state.inFlight.add(p));
            loadLayoutTagDetails(pi, apiBase, start, end)
                .then((details) => {
                    span.forEach((p) => state.inFlight.delete(p));
                    span.forEach((p) => state.details.set(p, details.get(p) || {}));
                })
                .catch((e) => {
                    span.forEach((p) => state.inFlight.delete(p));
                    console.warn('immersive layout tags failed', e);
                    span.forEach((p) => state.details.set(p, null));
                })
                .then(() => {
                    if (!state.cursorKey) preferCursorOnCurrentPage();
                    render();
                    renderHighlights();
                    pumpVisible();
                });
        });
    };

    /** Loads only the pages whose placeholder rows are in (or near) the panel's view. */
    const pumpVisible = () => {
        if (!panelOpen() || !state.selected.size) return;
        const box = panel.getBoundingClientRect();
        const rows = Array.from(hitsMount.querySelectorAll('.immersive__layout-tags-hit.-pending'))
            .filter((btn) => !state.inFlight.has(Number(btn.dataset.pageNo)))
            .map((btn) => {
                const r = btn.getBoundingClientRect();
                return { page: Number(btn.dataset.pageNo), top: r.top, bottom: r.bottom };
            });
        const wanted = visiblePages(box, rows, VISIBLE_BATCH);
        if (wanted.length) loadPages(wanted);
    };

    /* CURSOR AND NAVIGATION */

    /** Fetches the cursor's page ahead of the visible rows so its highlights appear first. */
    const loadCursorPage = () => {
        const list = entries();
        const active = list[cursorIndex(list, state.cursorKey)];
        if (active && active.pending) loadPages([active.page]);
    };

    /** Puts the cursor on the first entry of a page that is open right now, without turning pages. */
    const preferCursorOnCurrentPage = () => {
        const list = entries();
        const pages = currentPages();
        const onPage = list.find((e) => pages.includes(e.page));
        if (onPage) state.cursorKey = entryKey(onPage);
        else if (!state.cursorKey && list.length) state.cursorKey = entryKey(list[0]);
    };

    const toggleTag = (label) => {
        if (state.selected.has(label)) state.selected.delete(label);
        else state.selected.add(label);
        if (!state.selected.size) state.cursorKey = null;
        else if (!entries().some((e) => entryKey(e) === state.cursorKey)) {
            state.cursorKey = null;
            preferCursorOnCurrentPage();
        }
        render(true);
        loadCursorPage();
        pumpVisible();
        renderHighlights();
    };

    const goToEntry = (i) => {
        const list = entries();
        if (!list.length) return;
        const entry = list[Math.max(0, Math.min(i, list.length - 1))];
        state.cursorKey = entryKey(entry);
        markActive();
        if (entry.pending) loadPages([entry.page]);
        if (!currentPages().includes(entry.page)) viewer.goToPage(entry.page - 1);
        else renderHighlights();
    };

    /** Draws every occurrence of the cursor's tag on the open page(s); nothing while the panel is closed. */
    const renderHighlights = () => {
        if (!panelOpen()) return;
        const list = entries();
        const active = list[cursorIndex(list, state.cursorKey)];
        if (!active || !currentPages().includes(active.page)) {
            viewer.clearHighlights();
            return;
        }
        const className = styleTokens(active.label).join(' ');
        const rects = (occurrencesOf(active.page, active.label) || []).map((o) => ({ ...o.rect, order: active.page - 1, className }));
        viewer.setHighlights(rects);
    };

    /* HIT LIST */

    let revealedKey = null;

    /**
     * Writes the per-page state (still loading, number of occurrences) into the
     * existing rows. Which rows exist follows from the index alone, so arriving
     * page details never rebuild the list — they only fill it in.
     */
    const syncHitCounts = () => {
        hitsMount.querySelectorAll('.immersive__layout-tags-hit').forEach((btn) => {
            const page = Number(btn.dataset.pageNo);
            const label = btn.dataset.tagLabel;
            const occ = occurrencesOf(page, label);
            const count = occ ? occ.length : 0;
            const countEl = btn.querySelector('.immersive__layout-tags-hit-count');
            btn.classList.toggle('-pending', occ === undefined);
            btn.classList.toggle('-error', occ === null);
            countEl.textContent = occ === undefined ? '…' : occ === null ? '!' : count > 1 ? `(${count})` : '';
            let name = `${displayName(label)}, ${labels.labelPage || ''} ${page}`;
            if (occ === null) name += `, ${labels.labelError || ''}`;
            else if (occ !== undefined && count > 1) name += `, ${formatMessage(labels.labelOccurrences, count)}`;
            btn.setAttribute('aria-label', name);
            btn.title = occ === null ? labels.labelError || '' : '';
        });
    };

    /** Rebuilds the page-grouped hit list; rows carry page and label for the visibility pump and the count sync. */
    const buildHits = () => {
        // A rebuilt list is a new list: the active row is worth showing again.
        revealedKey = null;
        hitsMount.replaceChildren();
        if (state.failed) {
            hitsMount.append(el('p', 'immersive__layout-tags-empty', labels.labelError || ''));
            return;
        }
        if (!state.selected.size) {
            hitsMount.append(el('p', 'immersive__layout-tags-empty', labels.labelNoneSelected || ''));
            return;
        }
        const list = entries();
        if (!list.length) {
            hitsMount.append(el('p', 'immersive__layout-tags-empty', (rangeIsFull() ? labels.labelEmpty : labels.labelEmptyRange) || ''));
            return;
        }
        const ul = el('ul', 'immersive__layout-tags-hits');
        let currentPage = null;
        let typeList = null;
        list.forEach((entry, i) => {
            if (entry.page !== currentPage) {
                currentPage = entry.page;
                const pageLi = el('li', 'immersive__layout-tags-page');
                const pageNo = el('span', 'immersive__layout-tags-pageno');
                pageNo.append(el('span', 'sr-only', `${labels.labelPage || ''} `), document.createTextNode(String(entry.page)));
                typeList = el('ul', 'immersive__layout-tags-types');
                pageLi.append(pageNo, typeList);
                ul.append(pageLi);
            }
            const item = el('li');
            const btn = el('button', 'immersive__layout-tags-hit');
            btn.type = 'button';
            btn.classList.add(...styleTokens(entry.label));
            btn.dataset.spotIndex = String(i);
            btn.dataset.spotKey = entryKey(entry);
            btn.dataset.pageNo = String(entry.page);
            btn.dataset.tagLabel = entry.label;
            const dot = el('span', 'immersive__layout-tags-dot');
            dot.setAttribute('aria-hidden', 'true');
            btn.append(dot, el('span', 'immersive__layout-tags-hit-name', displayName(entry.label)), el('span', 'immersive__layout-tags-hit-count'));
            btn.addEventListener('click', () => goToEntry(i));
            item.append(btn);
            typeList.append(item);
        });
        hitsMount.append(ul);
        syncHitCounts();
    };

    /**
     * Brings the active hit into view with offset math (no scrollIntoView, the
     * page must not move). Only ever on a cursor change: every arriving page
     * batch re-renders, and revealing on each of those would drag the list back
     * up under a user who is scrolling.
     */
    const revealActiveSpot = () => {
        if (state.cursorKey === revealedKey) return;
        const row = hitsMount.querySelector('.immersive__layout-tags-hit.-active');
        if (!row) return;
        revealedKey = state.cursorKey;
        const stickyHeight = sticky ? sticky.offsetHeight : 0;
        const top = row.offsetTop - panel.offsetTop;
        const bottom = top + row.offsetHeight;
        const viewTop = panel.scrollTop + stickyHeight;
        const viewBottom = panel.scrollTop + panel.clientHeight;
        if (top < viewTop) panel.scrollTop = Math.max(0, top - stickyHeight - 8);
        else if (bottom > viewBottom) panel.scrollTop = bottom - panel.clientHeight + 8;
    };

    const markActive = () => {
        const list = entries();
        const cursor = cursorIndex(list, state.cursorKey);
        hitsMount.querySelectorAll('.immersive__layout-tags-hit').forEach((btn) => {
            const on = list.length > 0 && Number(btn.dataset.spotIndex) === cursor;
            btn.classList.toggle('-active', on);
            if (on) btn.setAttribute('aria-current', 'true');
            else btn.removeAttribute('aria-current');
        });
        revealActiveSpot();
        if (progress) {
            const pages = hitPages(selectedTags(), state.from, state.to);
            const loaded = pages.filter((p) => state.details.has(p)).length;
            const failed = pages.some((p) => state.details.get(p) === null);
            let text = '';
            if (failed) text = labels.labelError || '';
            else if (pages.length && loaded < pages.length) text = formatMessage(labels.labelProgressLoading, loaded, pages.length);
            else if (pages.length === 1) text = labels.labelProgressDoneOne || '';
            else if (pages.length) text = formatMessage(labels.labelProgressDone, pages.length);
            progress.textContent = text;
        }
    };

    let builtKey = null;

    /**
     * Rebuilds the hit list only when its composition changed (selection, range,
     * error), keeping keyboard focus on the same entry; arriving page details go
     * through the count sync instead.
     */
    const render = (force = false) => {
        const key = [state.failed, Array.from(state.selected).sort().join(','), state.from, state.to].join('|');
        if (force || key !== builtKey) {
            builtKey = key;
            const focused = document.activeElement;
            const focusKey = focused && focused.dataset ? focused.dataset.spotKey : null;
            buildHits();
            if (focusKey) {
                const again = hitsMount.querySelector(`[data-spot-key="${focusKey}"]`);
                if (again) again.focus({ preventScroll: true });
            }
        } else {
            syncHitCounts();
        }
        syncPick();
        syncRange();
        markActive();
    };

    /* LOADING AND WIRING */

    let indexRequest = null;
    const ensureIndex = () => {
        if (state.index) return Promise.resolve(state.index);
        if (indexRequest) return indexRequest;
        if (loader) loader.hidden = false;
        indexRequest = loadLayoutTagIndex(pi, apiBase, fetch, document.documentElement.lang)
            .then((index) => {
                state.index = index;
                state.failed = false;
                if (filterWrap) filterWrap.hidden = index.tags.length < FILTER_THRESHOLD;
                buildPickRows();
                return index;
            })
            .catch((e) => {
                console.error('immersive layout-tag index failed', e);
                state.failed = true;
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
            if (!panelOpen()) return;
            if (!state.cursorKey) preferCursorOnCurrentPage();
            render(true);
            loadCursorPage();
            pumpVisible();
            renderHighlights();
        });
    });

    bag.add(
        panels.registerCloseHook(() => {
            if (!panelOpen()) return;
            closePick();
            viewer.clearHighlights();
        })
    );

    bag.add(
        viewer.onPageChange.subscribe(() => {
            if (!panelOpen() || !state.index) return;
            const pages = currentPages();
            const onPage = entries().find((e) => pages.includes(e.page));
            if (onPage) state.cursorKey = entryKey(onPage);
            markActive();
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
            closePick();
            viewer.clearHighlights();
            bag.dispose();
        },
    };
}
