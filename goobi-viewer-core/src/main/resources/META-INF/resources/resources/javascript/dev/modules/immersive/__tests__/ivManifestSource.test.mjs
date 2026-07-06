import { jest } from '@jest/globals';
import {
    loadPageServices,
    loadPageLabels,
    loadPageText,
    parseManifestImageServices,
    parseManifestPageLabels,
    parsePageText,
    parsePageLines,
    loadPageLines,
    nestTextLevels,
    flattenTextLevels,
    loadPageTextLevels,
    _clearCache,
} from '../ivManifestSource.mjs';

const V2 = {
    sequences: [
        {
            canvases: [{ images: [{ resource: { service: { '@id': 'https://h/img/1' } } }] }, { images: [{ resource: { service: { '@id': 'https://h/img/2' } } }] }],
        },
    ],
};

function okResponse(body) {
    return { ok: true, status: 200, json: () => Promise.resolve(body) };
}

beforeEach(() => {
    _clearCache();
});

// ---------------------------------------------------------------------------
// parseManifestImageServices — IIIF v2/v3 manifest parsing
// ---------------------------------------------------------------------------

describe('parseManifestImageServices', function () {
    test('IIIF v2: service as object with @id', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        { images: [{ resource: { service: { '@id': 'https://example.org/iiif/image/1' } } }] },
                        { images: [{ resource: { service: { '@id': 'https://example.org/iiif/image/2' } } }] },
                    ],
                },
            ],
        };
        expect(parseManifestImageServices(manifest)).toEqual(['https://example.org/iiif/image/1', 'https://example.org/iiif/image/2']);
    });

    test('IIIF v2: service as an array (takes first element)', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        {
                            images: [
                                {
                                    resource: {
                                        service: [{ '@id': 'https://example.org/iiif/image/10' }, { '@id': 'https://example.org/iiif/image/10-extra' }],
                                    },
                                },
                            ],
                        },
                    ],
                },
            ],
        };
        expect(parseManifestImageServices(manifest)).toEqual(['https://example.org/iiif/image/10']);
    });

    test('IIIF v3: manifest with items array', function () {
        const manifest = {
            items: [
                { items: [{ items: [{ body: { service: { id: 'https://example.org/iiif3/image/1' } } }] }] },
                { items: [{ items: [{ body: { service: [{ id: 'https://example.org/iiif3/image/2' }] } }] }] },
            ],
        };
        expect(parseManifestImageServices(manifest)).toEqual(['https://example.org/iiif3/image/1', 'https://example.org/iiif3/image/2']);
    });

    test('malformed / empty manifest returns []', function () {
        expect(parseManifestImageServices({})).toEqual([]);
        expect(parseManifestImageServices(null)).toEqual([]);
        expect(parseManifestImageServices(undefined)).toEqual([]);
        expect(parseManifestImageServices({ sequences: [] })).toEqual([]);
    });

    test('v2: skips individual canvases that lack a resolvable service id', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        { images: [{ resource: { service: { '@id': 'https://example.org/iiif/image/1' } } }] },
                        // canvas without a service id
                        { images: [{ resource: { service: {} } }] },
                        { images: [{ resource: { service: { '@id': 'https://example.org/iiif/image/3' } } }] },
                    ],
                },
            ],
        };
        expect(parseManifestImageServices(manifest)).toEqual(['https://example.org/iiif/image/1', 'https://example.org/iiif/image/3']);
    });

    test('v3: skips canvases without a resolvable service (no throw)', function () {
        const manifest = {
            items: [
                { items: [{ items: [{ body: { service: { id: 'https://h3/1' } } }] }] },
                { items: [] }, // broken canvas
                { items: [{ items: [{ body: { service: { id: 'https://h3/3' } } }] }] },
            ],
        };
        expect(parseManifestImageServices(manifest)).toEqual(['https://h3/1', 'https://h3/3']);
    });
});

// ---------------------------------------------------------------------------
// parseManifestPageLabels — canvas labels, index-aligned with the services
// ---------------------------------------------------------------------------

describe('parseManifestPageLabels', function () {
    test('IIIF v2: string labels in canvas order', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        { label: ' - ', images: [{ resource: { service: { '@id': 'https://h/1' } } }] },
                        { label: '[1]', images: [{ resource: { service: { '@id': 'https://h/2' } } }] },
                        { label: '4', images: [{ resource: { service: { '@id': 'https://h/3' } } }] },
                    ],
                },
            ],
        };
        expect(parseManifestPageLabels(manifest)).toEqual([' - ', '[1]', '4']);
    });

    test('stays index-aligned with services: skipped canvases drop their label too', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        { label: 'A', images: [{ resource: { service: { '@id': 'https://h/1' } } }] },
                        { label: 'skip', images: [{ resource: { service: {} } }] }, // no id → skipped
                        { label: 'C', images: [{ resource: { service: { '@id': 'https://h/3' } } }] },
                    ],
                },
            ],
        };
        expect(parseManifestImageServices(manifest)).toEqual(['https://h/1', 'https://h/3']);
        expect(parseManifestPageLabels(manifest)).toEqual(['A', 'C']);
    });

    test('IIIF v2 @value label objects and label arrays resolve to the first non-empty value', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        { label: { '@value': 'S. 3' }, images: [{ resource: { service: { '@id': 'https://h/1' } } }] },
                        { label: [{ '@value': '' }, { '@value': '4' }], images: [{ resource: { service: { '@id': 'https://h/2' } } }] },
                    ],
                },
            ],
        };
        expect(parseManifestPageLabels(manifest)).toEqual(['S. 3', '4']);
    });

    test('IIIF v3 language map: first non-empty value wins when no language is preferred', function () {
        const manifest = {
            items: [{ label: { none: ['5'] }, items: [{ items: [{ body: { service: { id: 'https://h3/1' } } }] }] }],
        };
        expect(parseManifestPageLabels(manifest)).toEqual(['5']);
    });

    test('IIIF v3 language map: the preferred language wins over insertion order', function () {
        const manifest = {
            items: [{ label: { en: ['page 5'], de: ['Seite 5'] }, items: [{ items: [{ body: { service: { id: 'https://h3/1' } } }] }] }],
        };
        expect(parseManifestPageLabels(manifest, 'de')).toEqual(['Seite 5']);
        expect(parseManifestPageLabels(manifest, 'en')).toEqual(['page 5']);
    });

    test("IIIF v3 language map: falls back to 'none', then to any non-empty value", function () {
        const noneManifest = {
            items: [{ label: { en: ['page 5'], none: ['5'] }, items: [{ items: [{ body: { service: { id: 'https://h3/1' } } }] }] }],
        };
        expect(parseManifestPageLabels(noneManifest, 'de')).toEqual(['5']);
        const otherLangOnly = {
            items: [{ label: { fr: ['page 5'] }, items: [{ items: [{ body: { service: { id: 'https://h3/1' } } }] }] }],
        };
        expect(parseManifestPageLabels(otherLangOnly, 'de')).toEqual(['page 5']);
    });

    test('missing label yields empty string', function () {
        const manifest = {
            sequences: [{ canvases: [{ images: [{ resource: { service: { '@id': 'https://h/1' } } }] }] }],
        };
        expect(parseManifestPageLabels(manifest)).toEqual(['']);
    });

    test('empty / non-object manifests yield []', function () {
        expect(parseManifestPageLabels({})).toEqual([]);
        expect(parseManifestPageLabels(null)).toEqual([]);
    });
});

describe('loadPageServices', () => {
    test('fetches manifest and returns ordered image-service ids', async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(V2));

        const result = await loadPageServices('PPN1', 'https://h/api', fetchFn);

        expect(fetchFn).toHaveBeenCalledTimes(1);
        expect(fetchFn).toHaveBeenCalledWith('https://h/api/records/PPN1/manifest');
        expect(result).toEqual(['https://h/img/1', 'https://h/img/2']);
    });

    test('caches per pi — two calls with the same pi call fetchFn only once', async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(V2));

        await loadPageServices('PPN1', 'https://h/api', fetchFn);
        await loadPageServices('PPN1', 'https://h/api', fetchFn);

        expect(fetchFn).toHaveBeenCalledTimes(1);
    });

    test('throws on non-ok response', async () => {
        const fetchFn = jest.fn().mockResolvedValue({ ok: false, status: 404 });

        await expect(loadPageServices('PPN_MISSING', 'https://h/api', fetchFn)).rejects.toThrow();
    });

    test('retries after a failed fetch (does not cache rejection)', async () => {
        const fetchFn = jest.fn().mockRejectedValueOnce(new Error('net')).mockResolvedValueOnce(okResponse(V2));

        await expect(loadPageServices('PPN1', 'https://h/api', fetchFn)).rejects.toThrow('net');
        const result = await loadPageServices('PPN1', 'https://h/api', fetchFn);

        expect(result).toEqual(['https://h/img/1', 'https://h/img/2']);
        expect(fetchFn).toHaveBeenCalledTimes(2);
    });

    test('retries after a non-ok response (HTTP error is not cached)', async () => {
        const fetchFn = jest.fn().mockResolvedValueOnce({ ok: false, status: 503 }).mockResolvedValueOnce(okResponse(V2));

        await expect(loadPageServices('PPN1', 'https://h/api', fetchFn)).rejects.toThrow();
        const result = await loadPageServices('PPN1', 'https://h/api', fetchFn);

        expect(result).toEqual(['https://h/img/1', 'https://h/img/2']);
        expect(fetchFn).toHaveBeenCalledTimes(2);
    });
});

const V2_LABELS = {
    sequences: [
        {
            canvases: [
                { label: ' - ', images: [{ resource: { service: { '@id': 'https://h/img/1' } } }] },
                { label: '[1]', images: [{ resource: { service: { '@id': 'https://h/img/2' } } }] },
            ],
        },
    ],
};

describe('loadPageLabels', () => {
    test('fetches manifest and returns ordered canvas labels', async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(V2_LABELS));

        const result = await loadPageLabels('PPN1', 'https://h/api', fetchFn);

        expect(result).toEqual([' - ', '[1]']);
    });

    test('shares the memoized manifest fetch with loadPageServices (one request per pi)', async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(V2_LABELS));

        const [services, labels] = await Promise.all([loadPageServices('PPN1', 'https://h/api', fetchFn), loadPageLabels('PPN1', 'https://h/api', fetchFn)]);

        expect(services).toEqual(['https://h/img/1', 'https://h/img/2']);
        expect(labels).toEqual([' - ', '[1]']);
        expect(fetchFn).toHaveBeenCalledTimes(1);
    });

    test('passes the preferred label language through to the parser', async () => {
        const v3 = {
            items: [{ label: { en: ['page 5'], de: ['Seite 5'] }, items: [{ items: [{ body: { service: { id: 'https://h3/1' } } }] }] }],
        };
        const fetchFn = jest.fn().mockResolvedValue(okResponse(v3));

        expect(await loadPageLabels('PPN1', 'https://h/api', fetchFn, 'de')).toEqual(['Seite 5']);
    });
});

const TEXT_LIST = {
    '@type': 'sc:AnnotationList',
    resources: [{ resource: { chars: 'Sammlung' } }, { resource: { chars: 'gemeinverständlicher' } }],
};

describe('parsePageText', () => {
    test('joins the per-line chars with newlines', () => {
        expect(parsePageText(TEXT_LIST)).toBe('Sammlung\ngemeinverständlicher');
    });

    test('returns "" for an AnnotationList without resources (no OCR for the page)', () => {
        expect(parsePageText({ '@type': 'sc:AnnotationList' })).toBe('');
    });

    test('returns "" for null/undefined', () => {
        expect(parsePageText(null)).toBe('');
        expect(parsePageText(undefined)).toBe('');
    });
});

describe('loadPageText', () => {
    test('fetches the 1-based page-text endpoint for a 0-based order and returns the text', async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(TEXT_LIST));

        const text = await loadPageText('PPN1', 'https://h/api', 14, fetchFn);

        expect(fetchFn).toHaveBeenCalledWith('https://h/api/records/PPN1/pages/15/text/');
        expect(text).toBe('Sammlung\ngemeinverständlicher');
    });

    test('returns "" on a non-ok response', async () => {
        const fetchFn = jest.fn().mockResolvedValue({ ok: false, status: 404 });

        expect(await loadPageText('PPN1', 'https://h/api', 0, fetchFn)).toBe('');
    });
});

const LINES_LIST = {
    '@type': 'sc:AnnotationList',
    resources: [
        { '@id': 'anno-1', on: 'https://h/iiif/PPN1/pages/3/canvas#xywh=10,20,100,30', resource: { chars: 'Sammlung' } },
        { on: 'https://h/iiif/PPN1/pages/3/canvas', resource: { chars: 'ohne Box' } },
    ],
};

describe('parsePageLines', () => {
    test('parses chars, rect (from #xywh) and id per line', () => {
        expect(parsePageLines(LINES_LIST)).toEqual([
            { id: 'anno-1', chars: 'Sammlung', rect: { x: 10, y: 20, w: 100, h: 30 } },
            { id: 'line-1', chars: 'ohne Box', rect: null },
        ]);
    });

    test('returns [] for missing resources / null / undefined', () => {
        expect(parsePageLines({ '@type': 'sc:AnnotationList' })).toEqual([]);
        expect(parsePageLines(null)).toEqual([]);
        expect(parsePageLines(undefined)).toEqual([]);
    });

    test('parses rect when `on` is an object with @id', () => {
        const list = { resources: [{ on: { '@id': 'https://h/x#xywh=5,6,7,8' }, resource: { chars: 'X' } }] };
        expect(parsePageLines(list)).toEqual([{ id: 'line-0', chars: 'X', rect: { x: 5, y: 6, w: 7, h: 8 } }]);
    });

    test('skips null entries in resources without throwing', () => {
        expect(parsePageLines({ resources: [null] })).toEqual([{ id: 'line-0', chars: '', rect: null }]);
    });

    test('parses rect from a structured oa:SpecificResource on.selector.value (real page-text shape)', () => {
        const list = {
            resources: [
                {
                    '@id': 'http://h/api/annotations/alto_PI_1_TextLine_1/?format=oa',
                    resource: { '@type': 'cnt:ContentAsText', chars: 'Sammlung' },
                    on: {
                        selector: { value: 'xywh=2904,779,187,58', '@type': 'oa:FragmentSelector' },
                        full: 'http://h/api/records/PI/pages/1/canvas/',
                        '@type': 'oa:SpecificResource',
                    },
                },
            ],
        };
        expect(parsePageLines(list)).toEqual([
            {
                id: 'http://h/api/annotations/alto_PI_1_TextLine_1/?format=oa',
                chars: 'Sammlung',
                rect: { x: 2904, y: 779, w: 187, h: 58 },
            },
        ]);
    });
});

describe('loadPageLines', () => {
    test('fetches the 1-based page-text endpoint and returns structured lines', async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(LINES_LIST));
        const lines = await loadPageLines('PPN1', 'https://h/api', 2, fetchFn);
        expect(fetchFn).toHaveBeenCalledWith('https://h/api/records/PPN1/pages/3/text/');
        expect(lines[0]).toEqual({ id: 'anno-1', chars: 'Sammlung', rect: { x: 10, y: 20, w: 100, h: 30 } });
        expect(lines).toHaveLength(2);
        expect(lines[1]).toEqual({ id: 'line-1', chars: 'ohne Box', rect: null });
    });

    test('returns [] on a non-ok response', async () => {
        const fetchFn = jest.fn().mockResolvedValue({ ok: false, status: 404 });
        expect(await loadPageLines('PPN1', 'https://h/api', 0, fetchFn)).toEqual([]);
    });
});

// ---------------------------------------------------------------------------
// nestTextLevels
// ---------------------------------------------------------------------------

/** Shorthand for a region fixture: id, text and bounding box. */
function region(id, chars, x, y, w, h) {
    return { id, chars, rect: x == null ? null : { x, y, w, h } };
}

describe('nestTextLevels', () => {
    const BLOCKS = [region('b1', 'Erster Absatz', 0, 0, 100, 100), region('b2', 'Zweiter Absatz', 0, 110, 100, 100)];
    const LINES = [region('l1', 'Zeile eins', 5, 10, 90, 20), region('l2', 'Zeile zwei', 5, 40, 90, 20), region('l3', 'Zeile drei', 5, 120, 90, 20)];
    const WORDS = [region('w1', 'Zeile', 6, 12, 30, 16), region('w2', 'eins', 40, 12, 30, 16), region('w3', 'drei', 6, 122, 30, 16)];

    test('nests lines into containing blocks and words into containing lines', () => {
        const blocks = nestTextLevels({ blocks: BLOCKS, lines: LINES, words: WORDS });
        expect(blocks.map((b) => b.id)).toEqual(['b1', 'b2']);
        expect(blocks[0].lines.map((l) => l.id)).toEqual(['l1', 'l2']);
        expect(blocks[1].lines.map((l) => l.id)).toEqual(['l3']);
        expect(blocks[0].lines[0].words.map((w) => w.id)).toEqual(['w1', 'w2']);
        expect(blocks[0].lines[1].words).toEqual([]);
        expect(blocks[1].lines[0].words.map((w) => w.id)).toEqual(['w3']);
    });

    test('drops blank words (ALTO spaces) and words outside every line', () => {
        const words = [region('w1', ' ', 6, 12, 30, 16), region('w2', 'weit-weg', 6, 500, 30, 16), region('w3', 'eins', 40, 12, 30, 16)];
        const blocks = nestTextLevels({ blocks: BLOCKS, lines: LINES, words });
        expect(blocks[0].lines[0].words.map((w) => w.id)).toEqual(['w3']);
    });

    test('groups consecutive lines without a containing block into a synthetic block', () => {
        const lines = [region('l1', 'im Block', 5, 10, 90, 20), region('l2', 'draussen A', 5, 500, 90, 20), region('l3', 'draussen B', 5, 530, 90, 20)];
        const blocks = nestTextLevels({ blocks: [BLOCKS[0]], lines, words: [] });
        expect(blocks).toHaveLength(2);
        expect(blocks[0].id).toBe('b1');
        expect(blocks[1].id).toBeNull();
        expect(blocks[1].lines.map((l) => l.id)).toEqual(['l2', 'l3']);
    });

    test('a line without a rect stays with the previous line block (no split)', () => {
        const lines = [region('l1', 'Zeile eins', 5, 10, 90, 20), region('l2', 'ohne Box', null), region('l3', 'Zeile zwei', 5, 40, 90, 20)];
        const blocks = nestTextLevels({ blocks: [BLOCKS[0]], lines, words: [] });
        expect(blocks).toHaveLength(1);
        expect(blocks[0].lines.map((l) => l.id)).toEqual(['l1', 'l2', 'l3']);
    });

    test('without blocks all lines land in one synthetic block', () => {
        const blocks = nestTextLevels({ blocks: [], lines: LINES, words: [] });
        expect(blocks).toHaveLength(1);
        expect(blocks[0].id).toBeNull();
        expect(blocks[0].lines.map((l) => l.id)).toEqual(['l1', 'l2', 'l3']);
    });

    test('degenerate blocks (line-granularity fallback of older APIs) are ignored', () => {
        const blocksLikeLines = LINES.map((l) => ({ ...l, id: `copy-${l.id}` }));
        const blocks = nestTextLevels({ blocks: blocksLikeLines, lines: LINES, words: [] });
        expect(blocks).toHaveLength(1);
        expect(blocks[0].id).toBeNull();
    });

    test('overlapping blocks: the smaller (more specific) block wins', () => {
        const blocks = [region('outer', '', 0, 0, 200, 200), region('inner', '', 0, 0, 100, 50)];
        const lines = [region('l1', 'innen', 5, 10, 90, 20)];
        const nested = nestTextLevels({ blocks, lines, words: [] });
        expect(nested).toHaveLength(1);
        expect(nested[0].id).toBe('inner');
    });

    test('empty lines input yields no blocks', () => {
        expect(nestTextLevels({ blocks: BLOCKS, lines: [], words: [] })).toEqual([]);
    });
});

// ---------------------------------------------------------------------------
// flattenTextLevels
// ---------------------------------------------------------------------------

describe('flattenTextLevels', () => {
    test('emits blocks, then lines, then words, with parent ids', () => {
        const nested = nestTextLevels({
            blocks: [region('b1', '', 0, 0, 100, 100)],
            lines: [region('l1', 'Zeile', 5, 10, 90, 20)],
            words: [region('w1', 'Zeile', 6, 12, 30, 16)],
        });
        const flat = flattenTextLevels(nested);
        expect(flat.map((r) => [r.id, r.level, r.parentId])).toEqual([
            ['b1', 'block', null],
            ['l1', 'line', 'b1'],
            ['w1', 'word', 'l1'],
        ]);
        expect(flat[0].rect).toEqual({ x: 0, y: 0, w: 100, h: 100 });
    });

    test('skips synthetic blocks and regions without a rect as overlays', () => {
        const nested = nestTextLevels({
            blocks: [],
            lines: [region('l1', 'Zeile', 5, 10, 90, 20), region('l2', 'ohne Box', null)],
            words: [],
        });
        const flat = flattenTextLevels(nested);
        expect(flat.map((r) => [r.id, r.level, r.parentId])).toEqual([['l1', 'line', null]]);
    });
});

// ---------------------------------------------------------------------------
// loadPageTextLevels
// ---------------------------------------------------------------------------

describe('loadPageTextLevels', () => {
    const LIST_FOR = {
        '': { resources: [{ '@id': 'l1', resource: { chars: 'Zeile' }, on: { selector: { value: 'xywh=5,10,90,20' } } }] },
        '?granularity=word': { resources: [{ '@id': 'w1', resource: { chars: 'Zeile' }, on: { selector: { value: 'xywh=6,12,30,16' } } }] },
        '?granularity=block': { resources: [{ '@id': 'b1', resource: { chars: 'Absatz' }, on: { selector: { value: 'xywh=0,0,100,100' } } }] },
    };

    test('fetches block, line and word granularity and returns the nested levels', async () => {
        const fetchFn = jest.fn((url) => Promise.resolve(okResponse(LIST_FOR[url.replace('https://h/api/records/PPN1/pages/3/text/', '')])));
        const blocks = await loadPageTextLevels('PPN1', 'https://h/api', 2, fetchFn);
        const urls = fetchFn.mock.calls.map((c) => c[0]).sort();
        expect(urls).toEqual([
            'https://h/api/records/PPN1/pages/3/text/',
            'https://h/api/records/PPN1/pages/3/text/?granularity=block',
            'https://h/api/records/PPN1/pages/3/text/?granularity=word',
        ]);
        expect(blocks).toHaveLength(1);
        expect(blocks[0].id).toBe('b1');
        expect(blocks[0].lines[0].id).toBe('l1');
        expect(blocks[0].lines[0].words[0].id).toBe('w1');
    });

    test('a failing block or word request degrades gracefully to the remaining levels', async () => {
        const fetchFn = jest.fn((url) => {
            if (url.includes('granularity')) return Promise.resolve({ ok: false, status: 500 });
            return Promise.resolve(okResponse(LIST_FOR['']));
        });
        const blocks = await loadPageTextLevels('PPN1', 'https://h/api', 2, fetchFn);
        expect(blocks).toHaveLength(1);
        expect(blocks[0].id).toBeNull();
        expect(blocks[0].lines.map((l) => l.id)).toEqual(['l1']);
        expect(blocks[0].lines[0].words).toEqual([]);
    });
});
