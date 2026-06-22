import { jest } from '@jest/globals';
import { loadPageServices, _clearCache } from '../ivManifestSource.mjs';

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
