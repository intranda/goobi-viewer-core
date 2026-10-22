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
    s.toggleItem('a', [5]);
    expect(s.selected.has('a')).toBe(true);
    expect(s.cursorKey).toBe('5#a');
    s.toggleItem('b', [1]);
    expect(s.cursorKey).toBe('5#a');
    s.toggleItem('a', [1]);
    expect(s.cursorKey).toBe('5#b');
    s.toggleItem('b', [1]);
    expect(s.cursorKey).toBeNull();
});

test('range clamps, reports whether it changed and drops a cursor that fell out of range', () => {
    const s = withIndex();
    s.toggleItem('a', [2]);
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
    s.toggleItem('a', [2]);
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

test('names, style tokens and the visible item list come from the index', () => {
    const s = withIndex();
    expect(s.displayName('b')).toBe('Beta');
    expect(s.displayName('zzz')).toBe('zzz');
    expect(s.styleTokens('b')).toEqual(['x', 'y']);
    expect(s.styleTokens('a')).toEqual([]);
    s.setFilter('bet');
    expect(s.visibleItems().map((i) => i.key)).toEqual(['b']);
    s.setFailed();
    expect(s.failed).toBe(true);
    expect(s.compositionKey()).toBe('true||1|10');
});

test('a configured group is one item: toggling selects or clears all its tags, while the entries stay one per tag', () => {
    const s = createLayoutTagsState(10);
    s.setIndex({
        tags: [
            { label: 'u', name: 'Under', type: 'underline', group: 'marks', groupName: 'Marks', groupStyleClass: 'g7', pages: [2, 5] },
            { label: 'b', name: 'Beta', styleClass: 'x y', pages: [5] },
            { label: 'm', name: 'Mark', type: 'mark', group: 'marks', groupName: 'Marks', groupStyleClass: 'g7', pages: [7] },
        ],
    });
    expect(s.visibleItems().map((i) => i.key)).toEqual(['marks', 'b']);
    expect(s.displayName('marks')).toBe('Marks');
    expect(s.styleTokens('marks')).toEqual(['g7']);
    expect(s.styleTokens('u')).toEqual([]);
    expect(s.inRangeCount(s.itemByKey('marks'))).toBe(3);
    s.toggleItem('marks', [5]);
    expect([...s.selected].sort()).toEqual(['m', 'u']);
    expect(s.itemSelection('marks')).toBe('all');
    expect(s.selectedItems().map((i) => i.key)).toEqual(['marks']);
    expect(s.cursorKey).toBe('5#u');
    s.storeDetails([5], new Map([[5, { u: [{ rect: {} }, { rect: {} }] }]]));
    expect(s.entries().map((e) => [e.page, e.label, e.pending, e.count])).toEqual([
        [2, 'u', true, 0],
        [5, 'u', false, 2],
        [7, 'm', true, 0],
    ]);
    s.toggleItem('u', [5]);
    expect(s.itemSelection('marks')).toBe('some');
    s.toggleItem('marks', [5]);
    expect(s.itemSelection('marks')).toBe('all');
    s.toggleItem('marks', [5]);
    expect(s.selected.size).toBe(0);
    expect(s.cursorKey).toBeNull();
});
