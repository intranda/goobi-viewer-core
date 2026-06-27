import { jest } from '@jest/globals';
import { loadPageServices, loadPageText, parsePageText, parsePageLines, loadPageLines, loadPageRegions, _clearCache } from '../ivManifestSource.mjs';

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

describe('loadPageRegions', () => {
    test("granularity 'line' delegates to loadPageLines", async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(LINES_LIST));
        const regions = await loadPageRegions('PPN1', 'https://h/api', 2, 'line', fetchFn);
        expect(regions).toEqual(parsePageLines(LINES_LIST));
    });

    test("granularity 'word' fetches ?granularity=word and parses regions", async () => {
        const fetchFn = jest.fn().mockResolvedValue(okResponse(LINES_LIST));
        const regions = await loadPageRegions('PPN1', 'https://h/api', 2, 'word', fetchFn);
        expect(fetchFn).toHaveBeenCalledWith('https://h/api/records/PPN1/pages/3/text/?granularity=word');
        expect(regions).toEqual(parsePageLines(LINES_LIST));
    });

    test("granularity 'word' returns [] on a non-ok response", async () => {
        const fetchFn = jest.fn().mockResolvedValue({ ok: false, status: 404 });
        expect(await loadPageRegions('PPN1', 'https://h/api', 0, 'word', fetchFn)).toEqual([]);
    });
});
