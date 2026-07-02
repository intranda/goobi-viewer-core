// media/__tests__/ivFulltextSearch.test.mjs
import { jest } from '@jest/globals';
import { parseSearchHits, search, nextIndex, prevIndex } from '../ivFulltextSearch.mjs';

describe('parseSearchHits', () => {
    const list = {
        '@type': 'sc:AnnotationList',
        within: { total: 2 },
        resources: [
            { '@type': 'oa:Annotation', resource: [{ value: 'der Ring' }], on: 'http://x/records/AC1/pages/19/canvas/#xywh=10,20,30,40' },
            { '@type': 'oa:Annotation', resource: { value: 'auf der Farm' }, on: 'http://x/records/AC1/pages/5/canvas/' },
        ],
    };
    test('extracts page (1-based), rect and snippet', () => {
        expect(parseSearchHits(list)).toEqual([
            { page: 19, rect: { x: 10, y: 20, w: 30, h: 40 }, snippet: 'der Ring' },
            { page: 5, rect: null, snippet: 'auf der Farm' },
        ]);
    });
    test('malformed/empty input → []', () => {
        expect(parseSearchHits(null)).toEqual([]);
        expect(parseSearchHits({})).toEqual([]);
        expect(parseSearchHits({ resources: [{ on: 'no-page' }] })).toEqual([]);
    });
    test('attaches before/match/after context from the hits block (by annotation id)', () => {
        const withContext = {
            resources: [{ '@id': 'anno1', resource: { value: 'Ring' }, on: 'http://x/records/AC1/pages/19/canvas/#xywh=10,20,30,40' }],
            hits: [{ '@type': 'search:Hit', annotations: ['anno1'], match: 'Ring', before: 'der goldene ', after: ' der Macht' }],
        };
        expect(parseSearchHits(withContext)).toEqual([
            { id: 'anno1', page: 19, rect: { x: 10, y: 20, w: 30, h: 40 }, snippet: 'Ring', before: 'der goldene ', match: 'Ring', after: ' der Macht' },
        ]);
    });
    test('attaches context when hit.annotations is a single id instead of an array', () => {
        const withContext = {
            resources: [{ '@id': 'anno1', resource: { value: 'Ring' }, on: 'http://x/records/AC1/pages/19/canvas/#xywh=10,20,30,40' }],
            hits: [{ '@type': 'search:Hit', annotations: 'anno1', match: 'Ring', before: 'a', after: 'b' }],
        };
        expect(parseSearchHits(withContext)[0]).toMatchObject({ match: 'Ring', before: 'a', after: 'b' });
    });
    test('resolves page and rect from `on` given as an object with @id', () => {
        const list = { resources: [{ resource: { value: 'x' }, on: { '@id': 'http://x/records/AC1/pages/9/canvas/#xywh=1,2,3,4' } }] };
        expect(parseSearchHits(list)).toEqual([{ page: 9, rect: { x: 1, y: 2, w: 3, h: 4 }, snippet: 'x' }]);
    });
});

describe('hit navigation index', () => {
    test('wraps forward/backward', () => {
        expect(nextIndex(2, 3)).toBe(0);
        expect(prevIndex(0, 3)).toBe(2);
    });
    test('empty → -1', () => {
        expect(nextIndex(0, 0)).toBe(-1);
        expect(prevIndex(0, 0)).toBe(-1);
    });
});

describe('search (paged fetch, injected fetchFn)', () => {
    test('merges all within-pages and stamps 0-based order', async () => {
        const page1 = { within: { last: 'x?q=wort&page=2' }, resources: [{ resource: [{ value: 'a' }], on: 'x/pages/2/canvas/#xywh=1,1,1,1' }] };
        const page2 = { within: { last: 'x?q=wort&page=2' }, resources: [{ resource: [{ value: 'b' }], on: 'x/pages/4/canvas/' }] };
        const calls = [];
        const fetchFn = (url) => {
            calls.push(url);
            return Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(url.includes('page=2') ? page2 : page1)) });
        };
        const hits = await search('PI1', 'http://api', 'wort', fetchFn, 2);
        expect(hits.map((h) => h.order)).toEqual([1, 3]);
        expect(calls[0]).toContain('/records/PI1/manifest/search?q=wort');
    });

    test('stops at within.last even when total overcounts (no page=2 fetch)', async () => {
        const page1 = { within: { total: 5, last: 'x?q=w&page=1' }, resources: [{ resource: [{ value: 'a' }], on: 'x/pages/3/canvas/#xywh=0,0,9,9' }] };
        const calls = [];
        const fetchFn = (url) => {
            calls.push(url);
            return Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(page1)) });
        };
        const hits = await search('PI', 'http://api', 'w', fetchFn, 10);
        expect(hits.map((h) => h.order)).toEqual([2]);
        expect(calls.length).toBe(1);
    });

    test('dedupes overlapping pages by annotation id (broken backend paging restarts page 2)', async () => {
        const page1 = {
            within: { last: 'x?q=w&page=2' },
            resources: [{ '@id': 'A', resource: [{ value: 'x' }], on: 'x/pages/2/canvas/#xywh=1,1,1,1' }],
        };
        const page2 = {
            within: { last: 'x?q=w&page=2' },
            resources: [
                { '@id': 'A', resource: [{ value: 'x' }], on: 'x/pages/2/canvas/#xywh=1,1,1,1' },
                { '@id': 'B', resource: [{ value: 'y' }], on: 'x/pages/3/canvas/#xywh=2,2,2,2' },
            ],
        };
        const fetchFn = (url) => Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(url.includes('page=2') ? page2 : page1)) });
        const hits = await search('PI', 'http://api', 'w', fetchFn, 5);
        expect(hits.map((h) => h.id)).toEqual(['A', 'B']);
    });

    test('stops once a page repeats with nothing new, even if within.last claims many more pages', async () => {
        const same = {
            within: { last: 'x?q=w&page=16' },
            resources: [{ '@id': 'A', resource: [{ value: 'x' }], on: 'x/pages/2/canvas/#xywh=1,1,1,1' }],
        };
        const calls = [];
        const fetchFn = (url) => {
            calls.push(url);
            return Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(same)) });
        };
        const hits = await search('PI', 'http://api', 'w', fetchFn, 50);
        expect(hits.map((h) => h.id)).toEqual(['A']);
        expect(calls.length).toBe(2); // page 1 + page 2 (adds nothing) -> stop, not 16
    });

    test('recovers resources from backend-malformed JSON (search:Hit without annotations)', async () => {
        const broken =
            '{"within":{"last":"x?page=1"},"resources":[{"resource":[{"value":"England"}],"on":"x/pages/7/canvas/#xywh=1,2,3,4"}],"hits":[{"@type":"search:Hit","annotations"}],"startIndex":0}';
        const fetchFn = () => Promise.resolve({ ok: true, text: () => Promise.resolve(broken) });
        const hits = await search('PI', 'http://api', 'England', fetchFn, 5);
        expect(hits).toEqual([{ page: 7, rect: { x: 1, y: 2, w: 3, h: 4 }, snippet: 'England', order: 6 }]);
    });

    test('valid JSON containing the literal string "annotations" is not corrupted by the repair', async () => {
        // The value serializes as "value":"annotations"} -- exactly the pattern the JSON
        // repair rewrites. Valid JSON must be parsed as-is, never rewritten.
        const page = { within: { last: 'x?page=1' }, resources: [{ resource: [{ value: 'annotations' }], on: 'x/pages/3/canvas/#xywh=1,2,3,4' }] };
        const fetchFn = () => Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(page)) });
        const hits = await search('PI', 'http://api', 'annotations', fetchFn, 5);
        expect(hits).toEqual([{ page: 3, rect: { x: 1, y: 2, w: 3, h: 4 }, snippet: 'annotations', order: 2 }]);
    });

    test('unparseable JSON yields no hits instead of throwing', async () => {
        const fetchFn = () => Promise.resolve({ ok: true, text: () => Promise.resolve('{invalid') });
        await expect(search('PI', 'http://api', 'w', fetchFn, 5)).resolves.toEqual([]);
    });

    test('returns [] when the first page response is not ok', async () => {
        const fetchFn = jest.fn().mockResolvedValue({ ok: false, status: 500 });
        await expect(search('PI', 'http://api', 'w', fetchFn, 5)).resolves.toEqual([]);
        expect(fetchFn).toHaveBeenCalledTimes(1);
    });

    test('keeps page-1 hits when a later page fails', async () => {
        const page1 = { within: { last: 'x?q=w&page=3' }, resources: [{ '@id': 'A', resource: [{ value: 'a' }], on: 'x/pages/2/canvas/#xywh=1,1,1,1' }] };
        const fetchFn = jest
            .fn()
            .mockResolvedValueOnce({ ok: true, text: () => Promise.resolve(JSON.stringify(page1)) })
            .mockResolvedValueOnce({ ok: false, status: 500 });
        const hits = await search('PI', 'http://api', 'w', fetchFn, 5);
        expect(hits.map((h) => h.id)).toEqual(['A']);
        expect(fetchFn).toHaveBeenCalledTimes(2);
    });

    test('dedupes id-less hits via the page/rect/snippet fallback key', async () => {
        const dup = { resource: [{ value: 'x' }], on: 'x/pages/2/canvas/#xywh=1,1,1,1' };
        const page1 = { within: { last: 'x?q=w&page=2' }, resources: [dup] };
        const page2 = { within: { last: 'x?q=w&page=2' }, resources: [dup, { resource: [{ value: 'y' }], on: 'x/pages/3/canvas/#xywh=2,2,2,2' }] };
        const fetchFn = (url) => Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(url.includes('page=2') ? page2 : page1)) });
        const hits = await search('PI', 'http://api', 'w', fetchFn, 5);
        expect(hits.map((h) => h.snippet)).toEqual(['x', 'y']);
    });

    test('stops fetching at maxPages even when every page adds new hits', async () => {
        const calls = [];
        const fetchFn = (url) => {
            calls.push(url);
            const m = url.match(/page=(\d+)/);
            const p = m ? Number(m[1]) : 1;
            const page = { within: { last: 'x?q=w&page=99' }, resources: [{ '@id': `A${p}`, resource: [{ value: 'x' }], on: `x/pages/${p}/canvas/#xywh=1,1,1,1` }] };
            return Promise.resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(page)) });
        };
        const hits = await search('PI', 'http://api', 'w', fetchFn, 3);
        expect(calls).toHaveLength(3);
        expect(hits.map((h) => h.id)).toEqual(['A1', 'A2', 'A3']);
    });
});
