/**
 * Unit tests for iv_imageWindow.mjs
 *
 * Tests the pure helper functions: parseManifestImageServices, computeSpread,
 * framePages and residentPages.
 */
import { parseManifestImageServices, parseManifestPageLabels, computeSpread, framePages, residentPages } from '../iv_imageWindow.mjs';

// ---------------------------------------------------------------------------
// parseManifestImageServices
// ---------------------------------------------------------------------------

describe('parseManifestImageServices', function () {
    test('IIIF v2: service as object with @id', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        {
                            images: [
                                {
                                    resource: {
                                        service: { '@id': 'https://example.org/iiif/image/1' },
                                    },
                                },
                            ],
                        },
                        {
                            images: [
                                {
                                    resource: {
                                        service: { '@id': 'https://example.org/iiif/image/2' },
                                    },
                                },
                            ],
                        },
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
                {
                    items: [
                        {
                            items: [
                                {
                                    body: {
                                        service: { id: 'https://example.org/iiif3/image/1' },
                                    },
                                },
                            ],
                        },
                    ],
                },
                {
                    items: [
                        {
                            items: [
                                {
                                    body: {
                                        service: [{ id: 'https://example.org/iiif3/image/2' }],
                                    },
                                },
                            ],
                        },
                    ],
                },
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

    test('skips individual canvases that lack a resolvable service id', function () {
        const manifest = {
            sequences: [
                {
                    canvases: [
                        {
                            images: [
                                {
                                    resource: {
                                        service: { '@id': 'https://example.org/iiif/image/1' },
                                    },
                                },
                            ],
                        },
                        // canvas without a service id
                        {
                            images: [{ resource: { service: {} } }],
                        },
                        {
                            images: [
                                {
                                    resource: {
                                        service: { '@id': 'https://example.org/iiif/image/3' },
                                    },
                                },
                            ],
                        },
                    ],
                },
            ],
        };
        expect(parseManifestImageServices(manifest)).toEqual(['https://example.org/iiif/image/1', 'https://example.org/iiif/image/3']);
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

    test('IIIF v3 language map: first non-empty value wins', function () {
        const manifest = {
            items: [
                {
                    label: { none: ['5'] },
                    items: [{ items: [{ body: { service: { id: 'https://h3/1' } } }] }],
                },
            ],
        };
        expect(parseManifestPageLabels(manifest)).toEqual(['5']);
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

// ---------------------------------------------------------------------------
// computeSpread — book-layout page pairing
// ---------------------------------------------------------------------------

describe('computeSpread (book layout, cover alone)', () => {
    test('cover page stands alone', () => {
        expect(computeSpread(0, 40)).toEqual([0]);
    });
    test('pairs after the cover: (1,2),(3,4)', () => {
        expect(computeSpread(1, 40)).toEqual([1, 2]);
        expect(computeSpread(2, 40)).toEqual([1, 2]);
        expect(computeSpread(3, 40)).toEqual([3, 4]);
        expect(computeSpread(4, 40)).toEqual([3, 4]);
    });
    test('odd final page stands alone', () => {
        // pages 0..3 -> [0],[1,2],[3]
        expect(computeSpread(3, 4)).toEqual([3]);
    });
    test('even final page is paired', () => {
        // pages 0..4 -> [0],[1,2],[3,4]
        expect(computeSpread(4, 5)).toEqual([3, 4]);
    });
    test('clamps out-of-range order', () => {
        expect(computeSpread(99, 40)).toEqual([39]);
    });
    test('coverAlone:false pairs from the start', () => {
        expect(computeSpread(0, 40, { coverAlone: false })).toEqual([0, 1]);
        expect(computeSpread(2, 40, { coverAlone: false })).toEqual([2, 3]);
    });
});

// ---------------------------------------------------------------------------
// framePages — frame containing a given order
// ---------------------------------------------------------------------------

describe('framePages', () => {
    test('single mode: just the page', () => {
        expect(framePages(5, 40)).toEqual([5]);
        expect(framePages(5, 40, { double: false })).toEqual([5]);
    });
    test('double mode: the spread', () => {
        expect(framePages(0, 40, { double: true })).toEqual([0]); // cover alone
        expect(framePages(2, 40, { double: true })).toEqual([1, 2]);
    });
    test('clamps out of range', () => {
        expect(framePages(99, 40)).toEqual([39]);
    });
});

// ---------------------------------------------------------------------------
// residentPages — current frame + adjacent frames
// ---------------------------------------------------------------------------

describe('residentPages (current frame + adjacent frames)', () => {
    test('single mode keeps prev/current/next', () => {
        expect(residentPages(5, 40)).toEqual([4, 5, 6]);
    });
    test('single mode at start: no prev', () => {
        expect(residentPages(0, 40)).toEqual([0, 1]);
    });
    test('single mode at end: no next', () => {
        expect(residentPages(39, 40)).toEqual([38, 39]);
    });
    test('double mode keeps adjacent spreads', () => {
        // frames: [0],[1,2],[3,4]; at order 2 -> here[1,2], prev[0], next[3,4]
        expect(residentPages(2, 40, { double: true })).toEqual([0, 1, 2, 3, 4]);
    });
});
