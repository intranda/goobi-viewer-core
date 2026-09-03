/**
 * Unit tests for the layout-tag panel logic: endpoint parsing, range clamping,
 * hit-list derivation, cursor identity and visibility-driven batching.
 */
import { jest } from '@jest/globals';
import {
    formatMessage,
    parseLayoutTagIndex,
    parseCoordinates,
    parseLayoutTagDetails,
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
    _clearCache,
} from '../data/ivLayoutTags.mjs';

const TAGS = [
    { label: 'gr_unterstrichen', name: 'Unterstreichungen', styleClass: '', pages: [7, 8, 25, 49] },
    { label: 'gr_jacob_grimm', name: 'Jacob Grimm', styleClass: 'alto-annotation--jacob-grimm', pages: [8, 49, 200] },
    { label: 'gr_andere_linie', name: 'Andere Linie', styleClass: '', pages: [300] },
];

describe('formatMessage', () => {
    test('replaces indexed placeholders and blanks missing ones', () => {
        expect(formatMessage('{0} von {1} Seiten', 3, 12)).toBe('3 von 12 Seiten');
        expect(formatMessage('{0} und {1} weitere', 'A')).toBe('A und  weitere');
        expect(formatMessage(null, 1)).toBe('');
    });
});

describe('parseLayoutTagIndex', () => {
    test('normalizes tags: label required, name falls back to label, pages ascending and unique', () => {
        const parsed = parseLayoutTagIndex({
            pi: 'PI',
            pageCount: 10,
            tags: [{ label: 'a', pages: [5, 3, 3, '4', 0] }, { name: 'no label' }, { label: 'b', name: 'B', styleClass: 'x', pages: [] }],
        });
        expect(parsed).toEqual({
            pi: 'PI',
            pageCount: 10,
            tags: [
                { label: 'a', name: 'a', styleClass: '', pages: [3, 4, 5] },
                { label: 'b', name: 'B', styleClass: 'x', pages: [] },
            ],
        });
    });

    test('tolerates malformed input', () => {
        expect(parseLayoutTagIndex(null)).toEqual({ pi: '', pageCount: 0, tags: [] });
        expect(parseLayoutTagIndex({ tags: 'nope' }).tags).toEqual([]);
    });
});

describe('parseCoordinates', () => {
    test('converts x1,y1,x2,y2 to an x,y,w,h rect', () => {
        expect(parseCoordinates('367.0,1239.0,509.0,1274.0')).toEqual({ x: 367, y: 1239, w: 142, h: 35 });
    });

    test('rejects degenerate and unparsable boxes', () => {
        expect(parseCoordinates('0.0,0.0,0.0,0.0')).toBeNull();
        expect(parseCoordinates('1,2,3')).toBeNull();
        expect(parseCoordinates('a,b,c,d')).toBeNull();
        expect(parseCoordinates(undefined)).toBeNull();
    });
});

describe('parseLayoutTagDetails', () => {
    const response = {
        pi: 'PI',
        pages: [
            {
                pageOrder: 47,
                tags: [
                    {
                        value: 'gr_jacob_grimm',
                        references: [
                            { id: 'b-3', coordinates: '367.0,1239.0,509.0,1274.0', content: 'male.', page: 47 },
                            { id: 'b-4', coordinates: '1595.0,1234.0,1819.0,1299.0', content: 'unten', page: 47 },
                        ],
                    },
                    { value: 'gr_printed', references: [{ id: 'b-1', coordinates: '0.0,0.0,0.0,0.0', content: '', page: 47 }] },
                ],
            },
            { pageOrder: 48, tags: [] },
        ],
    };

    test('groups drawable occurrences by page and label and marks the whole range as loaded', () => {
        const details = parseLayoutTagDetails(response, 46, 48);
        expect(Array.from(details.keys())).toEqual([46, 47, 48]);
        expect(details.get(46)).toEqual({});
        expect(details.get(48)).toEqual({});
        expect(details.get(47).gr_printed).toBeUndefined();
        expect(details.get(47).gr_jacob_grimm).toEqual([
            { id: 'b-3', rect: { x: 367, y: 1239, w: 142, h: 35 }, content: 'male.' },
            { id: 'b-4', rect: { x: 1595, y: 1234, w: 224, h: 65 }, content: 'unten' },
        ]);
    });

    test('falls back to the group page when a reference carries no page, and to firstPage for multi-page groups', () => {
        const details = parseLayoutTagDetails({ pages: [{ firstPage: 3, lastPage: 3, tags: [{ value: 't', references: [{ id: 'x', coordinates: '0,0,5,5' }] }] }] }, 3, 3);
        expect(details.get(3).t).toHaveLength(1);
    });

    test('malformed input yields loaded-but-empty pages', () => {
        expect(Array.from(parseLayoutTagDetails(null, 1, 2).entries())).toEqual([
            [1, {}],
            [2, {}],
        ]);
    });
});

describe('range and hit derivation', () => {
    test('tagPages / hitPages restrict to the range and merge pages ascending', () => {
        expect(tagPages(TAGS[0], 8, 30)).toEqual([8, 25]);
        expect(tagPages(null, 1, 9)).toEqual([]);
        expect(hitPages(TAGS, 1, 626)).toEqual([7, 8, 25, 49, 200, 300]);
        expect(hitPages([TAGS[0], TAGS[1]], 40, 60)).toEqual([49]);
    });

    test('flatEntries lists one entry per page and tag, pending until the page detail is loaded', () => {
        const loaded = { 8: { gr_unterstrichen: [{}, {}], gr_jacob_grimm: [] } };
        const occurrencesOf = (page, label) => (loaded[page] ? loaded[page][label] || [] : undefined);
        expect(flatEntries([TAGS[0], TAGS[1]], 7, 8, occurrencesOf)).toEqual([
            { page: 7, label: 'gr_unterstrichen', pending: true, failed: false, count: 0 },
            { page: 8, label: 'gr_unterstrichen', pending: false, failed: false, count: 2 },
            { page: 8, label: 'gr_jacob_grimm', pending: false, failed: false, count: 0 },
        ]);
    });

    test('cursorIndex resolves the exact key, then the page, then the first entry', () => {
        const entries = flatEntries(TAGS, 1, 626, () => undefined);
        expect(entryKey(entries[2])).toBe('8#gr_jacob_grimm');
        expect(cursorIndex(entries, '8#gr_jacob_grimm')).toBe(2);
        expect(cursorIndex(entries, '8#gone')).toBe(1);
        expect(cursorIndex(entries, '999#gone')).toBe(0);
        expect(cursorIndex(entries, null)).toBe(0);
    });

    test('clampRange clamps, swaps and falls back on empty input', () => {
        expect(clampRange(40, 60, 626)).toEqual({ from: 40, to: 60 });
        expect(clampRange(60, 40, 626)).toEqual({ from: 40, to: 60 });
        expect(clampRange(-5, 9999, 626)).toEqual({ from: 1, to: 626 });
        expect(clampRange('', '', 626)).toEqual({ from: 1, to: 626 });
        expect(clampRange('abc', 12.4, 626)).toEqual({ from: 1, to: 12 });
    });

    test('filterTags keeps selected tags visible, drops out-of-range tags and applies the text filter', () => {
        const selected = new Set(['gr_andere_linie']);
        expect(filterTags(TAGS, { selected, filter: '', from: 1, to: 100 }).map((t) => t.label)).toEqual(['gr_unterstrichen', 'gr_jacob_grimm', 'gr_andere_linie']);
        expect(filterTags(TAGS, { selected, filter: 'jacob', from: 1, to: 626 }).map((t) => t.label)).toEqual(['gr_jacob_grimm', 'gr_andere_linie']);
        expect(filterTags(TAGS, { selected: new Set(), filter: 'GR_UNTER', from: 1, to: 626 }).map((t) => t.label)).toEqual(['gr_unterstrichen']);
    });
});

describe('visibility batching', () => {
    test('visiblePages takes rows within one panel height around the viewport, unique, capped', () => {
        const panel = { top: 100, bottom: 300 };
        const rows = [
            { page: 1, top: -150, bottom: -130 },
            { page: 2, top: -50, bottom: -30 },
            { page: 3, top: 120, bottom: 140 },
            { page: 3, top: 150, bottom: 170 },
            { page: 4, top: 480, bottom: 500 },
            { page: 5, top: 520, bottom: 540 },
        ];
        expect(visiblePages(panel, rows, 12)).toEqual([2, 3, 4]);
        expect(visiblePages(panel, rows, 2)).toEqual([2, 3]);
    });

    test('batchRanges groups into contiguous ranges no wider than the batch', () => {
        expect(batchRanges([5, 3, 4, 9, 400, 401], 8)).toEqual([
            { start: 3, end: 9 },
            { start: 400, end: 401 },
        ]);
        expect(batchRanges([1, 2, 3, 4, 5], 2)).toEqual([
            { start: 1, end: 2 },
            { start: 3, end: 4 },
            { start: 5, end: 5 },
        ]);
        expect(batchRanges([], 8)).toEqual([]);
    });
});

describe('loaders (injected fetchFn)', () => {
    beforeEach(() => _clearCache());

    test('loadLayoutTagIndex fetches once per pi and parses the response', async () => {
        const fetchFn = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve({ pi: 'PI', pageCount: 3, tags: [{ label: 'a', pages: [2, 1] }] }) }));
        const [a, b] = await Promise.all([loadLayoutTagIndex('PI', 'http://api', fetchFn), loadLayoutTagIndex('PI', 'http://api', fetchFn)]);
        expect(fetchFn).toHaveBeenCalledTimes(1);
        expect(fetchFn.mock.calls[0][0]).toBe('http://api/records/PI/layout/tags/index');
        expect(a).toBe(b);
        expect(a.tags[0].pages).toEqual([1, 2]);
    });

    test('loadLayoutTagIndex passes the UI language for the display names', async () => {
        const fetchFn = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve({ tags: [] }) }));
        await loadLayoutTagIndex('PI', 'http://api', fetchFn, 'de');
        expect(fetchFn.mock.calls[0][0]).toBe('http://api/records/PI/layout/tags/index?lang=de');
    });

    test('a failed index request is evicted so the next call retries', async () => {
        const fetchFn = jest
            .fn()
            .mockResolvedValueOnce({ ok: false, status: 500 })
            .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ tags: [] }) });
        await expect(loadLayoutTagIndex('PI', 'http://api', fetchFn)).rejects.toThrow('HTTP 500');
        await expect(loadLayoutTagIndex('PI', 'http://api', fetchFn)).resolves.toEqual({ pi: '', pageCount: 0, tags: [] });
        expect(fetchFn).toHaveBeenCalledTimes(2);
    });

    test('loadLayoutTagDetails requests the range and marks every page of it as loaded', async () => {
        const fetchFn = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve({ pages: [] }) }));
        const details = await loadLayoutTagDetails('PI', 'http://api', 41, 43, fetchFn);
        expect(fetchFn.mock.calls[0][0]).toBe('http://api/records/PI/layout/tags/?start=41&end=43');
        expect(Array.from(details.keys())).toEqual([41, 42, 43]);
    });

    test('loadLayoutTagDetails rejects on HTTP errors', async () => {
        const fetchFn = () => Promise.resolve({ ok: false, status: 403 });
        await expect(loadLayoutTagDetails('PI', 'http://api', 1, 1, fetchFn)).rejects.toThrow('HTTP 403');
    });
});

describe('flatEntries with failed pages', () => {
    test('a page whose details failed is neither pending nor counted, and flagged', () => {
        const tags = [{ label: 't', pages: [1, 2] }];
        const occurrencesOf = (page) => (page === 1 ? null : undefined);
        const [failed, pending] = flatEntries(tags, 1, 2, occurrencesOf);
        expect(failed).toEqual(expect.objectContaining({ page: 1, pending: false, failed: true, count: 0 }));
        expect(pending).toEqual(expect.objectContaining({ page: 2, pending: true, failed: false }));
    });
});
