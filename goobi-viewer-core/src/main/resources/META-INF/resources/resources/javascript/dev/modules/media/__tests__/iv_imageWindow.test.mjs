/**
 * Unit tests for iv_imageWindow.mjs
 *
 * Tests two pure helper functions:
 *   - parseManifestImageServices: extract IIIF image-service IDs from a manifest
 *   - computeWindow: compute a centered, clamped page-index window
 */
import { parseManifestImageServices, computeWindow } from '../iv_imageWindow.mjs';

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
