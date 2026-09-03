/** Unit tests for the DOM-free layout-tag panel state: selection, range, cursor and detail bookkeeping. */
import { createLayoutTagsState } from '../ui/layoutTags/ivLayoutTagsState.mjs';

const INDEX = {
    tags: [
        { label: 'a', name: 'Alpha', pages: [2, 5, 9] },
        { label: 'b', name: 'Beta', styleClass: 'x y', pages: [5] },
    ],
};

function withIndex() {
    const s = createLayoutTagsState(10);
    s.setIndex(INDEX);
    return s;
}

test('selection toggles and the cursor prefers an entry on the open page, else the first entry', () => {
    const s = withIndex();
    s.toggleTag('a', [5]);
    expect(s.selected.has('a')).toBe(true);
    expect(s.cursorKey).toBe('5#a');
    s.toggleTag('b', [1]);
    expect(s.cursorKey).toBe('5#a');
    s.toggleTag('a', [1]);
    expect(s.cursorKey).toBe('5#b');
    s.toggleTag('b', [1]);
    expect(s.cursorKey).toBeNull();
});

test('range clamps, reports whether it changed and drops a cursor that fell out of range', () => {
    const s = withIndex();
    s.toggleTag('a', [2]);
    expect(s.setRange(0, 99, [2])).toBe(false);
    expect(s.setRange(4, 10, [2])).toBe(true);
    expect([s.from, s.to]).toEqual([4, 10]);
    expect(s.cursorKey).toBe('5#a');
    expect(s.rangeIsFull()).toBe(false);
    expect(s.inRangeCount(INDEX.tags[0])).toBe(2);
    expect(s.compositionKey()).toBe('false|a|4|10');
});

test('details distinguish pending, loaded and failed pages', () => {
    const s = withIndex();
    s.toggleTag('a', [2]);
    expect(s.occurrencesOf(2, 'a')).toBeUndefined();
    expect(s.missingPages([2, 5])).toEqual([2, 5]);
    s.markInFlight([2]);
    expect(s.missingPages([2, 5])).toEqual([5]);
    s.storeDetails([2], new Map([[2, { a: [{ rect: {} }] }]]));
    s.storeDetails([5], null);
    expect(s.occurrencesOf(2, 'a')).toHaveLength(1);
    expect(s.occurrencesOf(5, 'a')).toBeNull();
    expect(s.inFlight.size).toBe(0);
    expect(s.progress()).toEqual({ total: 3, loaded: 2, failed: true });
    expect(s.entries().map((e) => [e.page, e.pending, e.failed])).toEqual([
        [2, false, false],
        [5, false, true],
        [9, true, false],
    ]);
});

test('names, style tokens and the visible tag list come from the index', () => {
    const s = withIndex();
    expect(s.displayName('b')).toBe('Beta');
    expect(s.displayName('zzz')).toBe('zzz');
    expect(s.styleTokens('b')).toEqual(['x', 'y']);
    expect(s.styleTokens('a')).toEqual([]);
    s.setFilter('bet');
    expect(s.visibleTags().map((t) => t.label)).toEqual(['b']);
    s.setFailed();
    expect(s.failed).toBe(true);
    expect(s.compositionKey()).toBe('true||1|10');
});
