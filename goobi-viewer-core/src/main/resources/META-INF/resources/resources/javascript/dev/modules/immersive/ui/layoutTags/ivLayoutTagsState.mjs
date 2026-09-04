/** Layout-tag panel state: selection, page range, cursor and per-page details, without any DOM. */

import { tagPages, hitPages, flatEntries, entryKey, cursorIndex, clampRange, filterTags } from '../../data/ivLayoutTags.mjs';
import { styleClassTokens } from '../../shared/ivDom.mjs';

/**
 * Creates the panel state. Queries derive everything from the index, the
 * selection, the range and the loaded details; transitions keep the cursor
 * valid (it prefers an entry on the pages the viewer currently shows).
 *
 * @param {number} pageCount
 * @returns {object}
 */
export function createLayoutTagsState(pageCount) {
    const s = { index: null, failed: false, selected: new Set(), from: 1, to: pageCount, filter: '', cursorKey: null, details: new Map(), inFlight: new Set() };
    const allTags = () => (s.index ? s.index.tags : []);
    const tagByLabel = (label) => allTags().find((t) => t.label === label) || null;
    const selectedTags = () => allTags().filter((t) => s.selected.has(t.label));
    const occurrencesOf = (page, label) => {
        if (!s.details.has(page)) return undefined;
        const byLabel = s.details.get(page);
        return byLabel ? byLabel[label] || [] : null;
    };
    const entries = () => flatEntries(selectedTags(), s.from, s.to, occurrencesOf);
    const preferCursorOnPages = (pages) => {
        const list = entries();
        const onPage = list.find((e) => pages.includes(e.page));
        if (onPage) s.cursorKey = entryKey(onPage);
        else if (!s.cursorKey && list.length) s.cursorKey = entryKey(list[0]);
    };
    const keepOrResetCursor = (pages) => {
        if (!s.selected.size) {
            s.cursorKey = null;
            return;
        }
        if (!entries().some((e) => entryKey(e) === s.cursorKey)) {
            s.cursorKey = null;
            preferCursorOnPages(pages);
        }
    };
    return {
        get pageCount() {
            return pageCount;
        },
        get index() {
            return s.index;
        },
        get failed() {
            return s.failed;
        },
        get selected() {
            return s.selected;
        },
        get from() {
            return s.from;
        },
        get to() {
            return s.to;
        },
        get cursorKey() {
            return s.cursorKey;
        },
        get details() {
            return s.details;
        },
        get inFlight() {
            return s.inFlight;
        },
        allTags,
        tagByLabel,
        selectedTags,
        occurrencesOf,
        entries,
        cursorEntry() {
            const list = entries();
            return list[cursorIndex(list, s.cursorKey)] || null;
        },
        displayName(label) {
            const tag = tagByLabel(label);
            return tag ? tag.name : label;
        },
        styleTokens(label) {
            const tag = tagByLabel(label);
            return tag ? styleClassTokens(tag.styleClass) : [];
        },
        tagType(label) {
            const tag = tagByLabel(label);
            return tag && tag.type ? tag.type : '';
        },
        visibleTags: () => filterTags(allTags(), s),
        inRangeCount: (tag) => tagPages(tag, s.from, s.to).length,
        rangeIsFull: () => s.from === 1 && s.to === pageCount,
        compositionKey: () => [s.failed, Array.from(s.selected).sort().join(','), s.from, s.to].join('|'),
        progress() {
            const pages = hitPages(selectedTags(), s.from, s.to);
            return { total: pages.length, loaded: pages.filter((p) => s.details.has(p)).length, failed: pages.some((p) => s.details.get(p) === null) };
        },
        setIndex(index) {
            s.index = index;
            s.failed = false;
        },
        setFailed() {
            s.failed = true;
        },
        setFilter(text) {
            s.filter = text;
        },
        setCursor(key) {
            s.cursorKey = key;
        },
        preferCursorOnPages,
        toggleTag(label, currentPages) {
            if (s.selected.has(label)) s.selected.delete(label);
            else s.selected.add(label);
            keepOrResetCursor(currentPages);
        },
        setRange(from, to, currentPages) {
            const next = clampRange(from, to, pageCount);
            if (next.from === s.from && next.to === s.to) return false;
            s.from = next.from;
            s.to = next.to;
            keepOrResetCursor(currentPages);
            return true;
        },
        missingPages: (pages) => pages.filter((p) => !s.details.has(p) && !s.inFlight.has(p)),
        markInFlight(pages) {
            pages.forEach((p) => s.inFlight.add(p));
        },
        storeDetails(pages, details) {
            pages.forEach((p) => {
                s.inFlight.delete(p);
                s.details.set(p, details ? details.get(p) || {} : null);
            });
        },
    };
}
