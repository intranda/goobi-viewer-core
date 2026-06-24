/**
 * Unit tests for iv_imageWindow.mjs
 *
 * Tests two pure helper functions:
 *   - parseManifestImageServices: extract IIIF image-service IDs from a manifest
 *   - computeWindow: compute a centered, clamped page-index window
 */
import { parseManifestImageServices, computeWindow, computeSpread } from '../iv_imageWindow.mjs';

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
// computeWindow — required contract examples
// ---------------------------------------------------------------------------

describe('computeWindow', function () {
    test('start of range: window begins at 0', function () {
        expect(computeWindow(0, 7, 5)).toEqual({ start: 0, end: 5, indexInWindow: 0 });
    });

    test('end of range: window clamps at total', function () {
        expect(computeWindow(6, 7, 5)).toEqual({ start: 2, end: 7, indexInWindow: 4 });
    });

    test('middle: window is centered', function () {
        expect(computeWindow(3, 7, 5)).toEqual({ start: 1, end: 6, indexInWindow: 2 });
    });

    test('windowSize >= total: returns entire range', function () {
        expect(computeWindow(2, 3, 5)).toEqual({ start: 0, end: 3, indexInWindow: 2 });
    });

    test('total === 0: returns zero window', function () {
        expect(computeWindow(0, 0, 5)).toEqual({ start: 0, end: 0, indexInWindow: 0 });
    });

    test('windowSize === 1: window contains only current page', function () {
        expect(computeWindow(4, 10, 1)).toEqual({ start: 4, end: 5, indexInWindow: 0 });
    });

    test('windowSize === total: full range, indexInWindow equals currentIndex', function () {
        expect(computeWindow(3, 7, 7)).toEqual({ start: 0, end: 7, indexInWindow: 3 });
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
