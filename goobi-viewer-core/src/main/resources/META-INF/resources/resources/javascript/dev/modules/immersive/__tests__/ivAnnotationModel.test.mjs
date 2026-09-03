/** Unit tests for the pure ALTO annotation model. */
import {
    altoElementId,
    hasAnnotations,
    wordTypeClasses,
    lineTypeModifier,
    nearestAnchorIndex,
    stackNoteTops,
    isAboveAnchor,
    rectCenterInside,
    overlayRegions,
} from '../data/ivAnnotationModel.mjs';

describe('altoElementId', () => {
    test('extracts the ALTO element id from a text annotation URI', () => {
        expect(altoElementId('https://host/api/v1/annotations/alto_PPN123_5_s-143?format=oa', 'PPN123', 5)).toBe('s-143');
    });

    test('handles the deployed URI form with a trailing slash before the query', () => {
        expect(altoElementId('http://localhost:8080/viewer/api/v1/annotations/alto_BV046334404_6_b-2/?format=oa', 'BV046334404', 6)).toBe('b-2');
        expect(altoElementId('https://h/a/alto_PPN123_5_s-1/', 'PPN123', 5)).toBe('s-1');
    });

    test('keeps underscores inside the element id (buildId fallback ids)', () => {
        expect(altoElementId('https://h/a/alto_PPN123_5_Word_10_20_30_40', 'PPN123', 5)).toBe('Word_10_20_30_40');
    });

    test('returns null for synthetic ids and foreign pages', () => {
        expect(altoElementId('line-3', 'PPN123', 5)).toBeNull();
        expect(altoElementId('https://h/a/alto_PPN123_6_s-1?format=oa', 'PPN123', 5)).toBeNull();
        expect(altoElementId(null, 'PPN123', 5)).toBeNull();
    });
});

describe('hasAnnotations', () => {
    test('false for null or all-empty layouts', () => {
        expect(hasAnnotations(null)).toBe(false);
        expect(hasAnnotations({ blocks: [], words: [], lines: [] })).toBe(false);
    });

    test('true as soon as any list has entries', () => {
        expect(hasAnnotations({ blocks: [{}], words: [], lines: [] })).toBe(true);
        expect(hasAnnotations({ blocks: [], words: [{}], lines: [] })).toBe(true);
        expect(hasAnnotations({ blocks: [], words: [], lines: [{}] })).toBe(true);
    });
});

describe('wordTypeClasses', () => {
    test('maps the semantic word types to their classes', () => {
        expect(wordTypeClasses(['underline'])).toEqual(['immersive__anno-word-underline']);
        expect(wordTypeClasses(['strikethrough'])).toEqual(['immersive__anno-word-strike']);
        expect(wordTypeClasses(['underline', 'strikethrough'])).toEqual(['immersive__anno-word-underline', 'immersive__anno-word-strike']);
    });

    test('ignores unknown types and duplicates', () => {
        expect(wordTypeClasses(['whatever', 'underline', 'underline'])).toEqual(['immersive__anno-word-underline']);
        expect(wordTypeClasses([])).toEqual([]);
        expect(wordTypeClasses(undefined)).toEqual([]);
    });
});

describe('lineTypeModifier', () => {
    test('maps semantic line types to overlay modifiers, first match wins', () => {
        expect(lineTypeModifier(['underline'])).toBe('underline');
        expect(lineTypeModifier(['strikethrough'])).toBe('strike');
        expect(lineTypeModifier(['mark'])).toBe('mark');
        expect(lineTypeModifier(['whatever', 'strikethrough'])).toBe('strike');
    });

    test('returns null for unrendered line types', () => {
        expect(lineTypeModifier(['whatever'])).toBeNull();
        expect(lineTypeModifier([])).toBeNull();
        expect(lineTypeModifier(undefined)).toBeNull();
    });
});

describe('nearestAnchorIndex', () => {
    const anchors = [
        { y: 100, h: 20 },
        { y: 200, h: 20 },
        { y: 300, h: 20 },
    ];

    test('picks the anchor whose vertical midpoint is closest', () => {
        expect(nearestAnchorIndex(105, anchors)).toBe(0);
        expect(nearestAnchorIndex(212, anchors)).toBe(1);
        expect(nearestAnchorIndex(1000, anchors)).toBe(2);
    });

    test('returns -1 without anchors', () => {
        expect(nearestAnchorIndex(100, [])).toBe(-1);
        expect(nearestAnchorIndex(100, undefined)).toBe(-1);
    });
});

describe('isAboveAnchor', () => {
    const anchor = { y: 100, h: 24 };

    test('handwriting jitter above the line MIDPOINT stays on the line', () => {
        expect(isAboveAnchor(106, anchor)).toBe(false);
        expect(isAboveAnchor(111, anchor)).toBe(false);
    });

    test('a note whose midpoint lies above the line TOP goes above the content', () => {
        expect(isAboveAnchor(99, anchor)).toBe(true);
        expect(isAboveAnchor(40, anchor)).toBe(true);
    });

    test('false without an anchor', () => {
        expect(isAboveAnchor(50, null)).toBe(false);
    });
});

describe('stackNoteTops', () => {
    test('keeps non-overlapping notes at their desired top', () => {
        expect(
            stackNoteTops(
                [
                    { top: 0, height: 30 },
                    { top: 100, height: 30 },
                ],
                8
            )
        ).toEqual([0, 100]);
    });

    test('balances an overlapping pair around its desired tops instead of pushing only down', () => {
        expect(
            stackNoteTops(
                [
                    { top: 100, height: 40 },
                    { top: 110, height: 40 },
                ],
                8
            )
        ).toEqual([81, 129]);
    });

    test('a dense run stays centered on its lines instead of accumulating downward drift', () => {
        const tops = stackNoteTops(
            [
                { top: 200, height: 40 },
                { top: 224, height: 40 },
                { top: 248, height: 40 },
                { top: 272, height: 40 },
                { top: 296, height: 40 },
            ],
            8
        );
        expect(tops[2]).toBe(248);
        expect(tops).toEqual([152, 200, 248, 296, 344]);
    });

    test('a cluster never goes above the container top or into the previous cluster', () => {
        expect(
            stackNoteTops(
                [
                    { top: 0, height: 50 },
                    { top: 20, height: 30 },
                ],
                8
            )
        ).toEqual([0, 58]);
        const tops = stackNoteTops(
            [
                { top: 0, height: 100 },
                { top: 90, height: 100 },
            ],
            8
        );
        expect(tops[1] - tops[0]).toBe(108);
        expect(tops[0]).toBeGreaterThanOrEqual(0);
    });

    test('handles an empty list', () => {
        expect(stackNoteTops([], 8)).toEqual([]);
    });
});

describe('overlayRegions', () => {
    test('maps rendered line types to overlay descriptors and skips unrendered ones', () => {
        const layout = {
            lines: [
                { id: 'g-1', rect: { x: 1, y: 2, w: 3, h: 4 }, tags: ['gr_unterstrichen'], types: ['underline'] },
                { id: 'g-2', rect: { x: 5, y: 6, w: 7, h: 8 }, tags: ['gr_andere_linie'] },
                { id: 'g-3', rect: null, tags: ['gr_durchgestrichen'], types: ['strikethrough'] },
            ],
        };
        expect(overlayRegions(layout)).toEqual([{ id: 'g-1', rect: { x: 1, y: 2, w: 3, h: 4 }, modifier: 'underline' }]);
        expect(overlayRegions(null)).toEqual([]);
    });

    test('adds every author block as a note region', () => {
        const layout = {
            blocks: [
                { id: 'b-2', rect: { x: 1, y: 2, w: 3, h: 4 }, margin: 'left', author: 'gr_jacob_grimm' },
                { id: 'b-5', rect: { x: 5, y: 6, w: 7, h: 8 }, author: 'gr_jacob_grimm' },
                { id: 'b-9', rect: { x: 9, y: 9, w: 9, h: 9 } },
            ],
        };
        expect(overlayRegions(layout)).toEqual([
            { id: 'b-2', rect: { x: 1, y: 2, w: 3, h: 4 }, modifier: 'note' },
            { id: 'b-5', rect: { x: 5, y: 6, w: 7, h: 8 }, modifier: 'note' },
        ]);
    });
});

describe('rectCenterInside', () => {
    test('true when the inner center lies inside the outer rect', () => {
        expect(rectCenterInside({ x: 10, y: 10, w: 10, h: 10 }, { x: 0, y: 0, w: 30, h: 30 })).toBe(true);
        expect(rectCenterInside({ x: 40, y: 10, w: 10, h: 10 }, { x: 0, y: 0, w: 30, h: 30 })).toBe(false);
    });

    test('false without rects', () => {
        expect(rectCenterInside(null, { x: 0, y: 0, w: 1, h: 1 })).toBe(false);
        expect(rectCenterInside({ x: 0, y: 0, w: 1, h: 1 }, undefined)).toBe(false);
    });
});
