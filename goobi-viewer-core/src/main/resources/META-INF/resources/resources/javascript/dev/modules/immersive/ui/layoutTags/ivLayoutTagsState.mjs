/** Layout-tag panel state: selection, page range, cursor and per-page details, without any DOM. */

import { tagPages, hitPages, flatEntries, entryKey, cursorIndex, clampRange, groupItems, filterItems } from '../../data/ivLayoutTags.mjs';
import { styleClassTokens } from '../../shared/ivDom.mjs';

/**
 * Creates the panel state. Queries derive everything from the index, the
 * selection, the range and the loaded details; transitions keep the cursor
 * valid (it prefers an entry on the pages the viewer currently shows). The
 * selection holds tag labels; the pick items (one per tag, or one per
 * configured group of tags) are derived from the index and only ease the
 * selection — the hit list keeps one entry per tag.
 *
 * @param {number} pageCount
 * @returns {object}
 */
export function createLayoutTagsState(pageCount) {
    const s = { index: null, items: [], failed: false, selected: new Set(), from: 1, to: pageCount, filter: '', cursorKey: null, details: new Map(), inFlight: new Set() };
    const allTags = () => (s.index ? s.index.tags : []);
    const tagByLabel = (label) => allTags().find((t) => t.label === label) || null;
    const itemByKey = (key) => s.items.find((i) => i.key === key) || null;
    const selectedTags = () => allTags().filter((t) => s.selected.has(t.label));
    const selectedItems = () => s.items.filter((i) => i.labels.some((label) => s.selected.has(label)));
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
        itemByKey,
        selectedTags,
        selectedItems,
        occurrencesOf,
        entries,
        cursorEntry() {
            const list = entries();
            return list[cursorIndex(list, s.cursorKey)] || null;
        },
        /** Display name of a pick item key or a tag label. */
        displayName(key) {
            const item = itemByKey(key);
            if (item) return item.name;
            const tag = tagByLabel(key);
            return tag ? tag.name : key;
        },
        /** Style tokens of a pick item key (a group's own configured class) or a tag label; empty without a configured class. */
        styleTokens(key) {
            const item = itemByKey(key);
            if (item) return styleClassTokens(item.styleClass);
            const tag = tagByLabel(key);
            return tag ? styleClassTokens(tag.styleClass) : [];
        },
        tagType(label) {
            const tag = tagByLabel(label);
            return tag && tag.type ? tag.type : '';
        },
        /** Whether 'all', 'some' or 'none' of an item's tags are selected. */
        itemSelection(key) {
            const item = itemByKey(key);
            if (!item) return s.selected.has(key) ? 'all' : 'none';
            const on = item.labels.filter((label) => s.selected.has(label)).length;
            return on === item.labels.length ? 'all' : on ? 'some' : 'none';
        },
        visibleItems: () => filterItems(s.items, s),
        inRangeCount: (item) => tagPages(item, s.from, s.to).length,
        rangeIsFull: () => s.from === 1 && s.to === pageCount,
        compositionKey: () => [s.failed, Array.from(s.selected).sort().join(','), s.from, s.to].join('|'),
        progress() {
            const pages = hitPages(selectedTags(), s.from, s.to);
            return { total: pages.length, loaded: pages.filter((p) => s.details.has(p)).length, failed: pages.some((p) => s.details.get(p) === null) };
        },
        setIndex(index) {
            s.index = index;
            s.items = groupItems(index ? index.tags : []);
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
        /** Toggles a pick item: all of its tags off when they all were on, else all on. A tag label toggles that tag alone. */
        toggleItem(key, currentPages) {
            const item = itemByKey(key);
            const labels = item ? item.labels : [key];
            const allOn = labels.every((label) => s.selected.has(label));
            labels.forEach((label) => (allOn ? s.selected.delete(label) : s.selected.add(label)));
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
