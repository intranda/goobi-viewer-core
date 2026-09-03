import {
    ANNOTATIONS_KEY,
    applyTextMarkup,
    overlayRegions,
    rectCenterInside,
    readAnnotationsEnabled,
    writeAnnotationsEnabled,
    altoElementId,
    hasAnnotations,
    wordTypeClasses,
    lineTypeModifier,
    styleClassTokens,
    nearestAnchorIndex,
    stackNoteTops,
    isAboveAnchor,
    setupAnnotationsToggle,
    mountBlockAnnotations,
    applyRegionMarkup,
} from '../ivAnnotations.mjs';
import { createKeyDispatcher } from '../ivKeys.mjs';
import { jest } from '@jest/globals';

describe('readAnnotationsEnabled / writeAnnotationsEnabled', () => {
    test('reads only the literal "true" as enabled, default off', () => {
        const store = new Map();
        const storage = { getItem: (k) => (store.has(k) ? store.get(k) : null), setItem: (k, v) => store.set(k, v) };
        expect(readAnnotationsEnabled(storage)).toBe(false);
        writeAnnotationsEnabled(storage, true);
        expect(store.get(ANNOTATIONS_KEY)).toBe('true');
        expect(readAnnotationsEnabled(storage)).toBe(true);
        writeAnnotationsEnabled(storage, false);
        expect(readAnnotationsEnabled(storage)).toBe(false);
    });

    test('falls back to off and swallows errors when storage throws (private mode)', () => {
        const throwing = {
            getItem: () => {
                throw new Error('denied');
            },
            setItem: () => {
                throw new Error('denied');
            },
        };
        expect(readAnnotationsEnabled(throwing)).toBe(false);
        expect(() => writeAnnotationsEnabled(throwing, true)).not.toThrow();
    });
});

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

describe('styleClassTokens', () => {
    test('splits a configured styleClass into class tokens', () => {
        expect(styleClassTokens('alto-annotation--jacob-grimm')).toEqual(['alto-annotation--jacob-grimm']);
        expect(styleClassTokens('kunde--hand-a  extra')).toEqual(['kunde--hand-a', 'extra']);
    });

    test('blank or missing values yield an empty list', () => {
        expect(styleClassTokens('')).toEqual([]);
        expect(styleClassTokens('   ')).toEqual([]);
        expect(styleClassTokens(undefined)).toEqual([]);
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
        // Real-world case (BV046334404 p.49/57): margin notes written slightly
        // high of their line flipped a full card row up with a midpoint rule.
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
        // Offsets in the cluster: 0 and 48; candidate tops 100 and 110-48=62,
        // median (average of both) = 81 — displacement is split up/down.
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
        // Five cards (h 40, gap 8 => 48 cadence) desire tops 24 apart: pure
        // push-down would leave the last card 96px below its line; the balanced
        // cluster keeps the middle card exactly on its line.
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
        // Second cluster's balanced top would collide with the first — they merge.
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

describe('applyRegionMarkup (jsdom)', () => {
    const uri = (id) => `http://h/api/v1/annotations/alto_PPN1_5_${id}/?format=oa`;

    const build = () => {
        const regionEls = new Map();
        ['s-2', 's-3', 'l-9'].forEach((id) => regionEls.set(uri(id), document.createElement('div')));
        return regionEls;
    };

    test('marks every tagged word overlay via its ALTO element id', () => {
        const regionEls = build();
        applyRegionMarkup(
            regionEls,
            {
                words: [
                    { id: 's-2', types: ['underline'] },
                    { id: 's-3', types: ['strikethrough'] },
                ],
            },
            'PPN1',
            5
        );
        expect(regionEls.get(uri('s-2')).className).toBe('immersive__anno-word-underline');
        expect(regionEls.get(uri('s-3')).className).toBe('immersive__anno-word-strike');
        expect(regionEls.get(uri('l-9')).className).toBe('');
    });

    test('a word without an overlay of its own is skipped, missing inputs are tolerated', () => {
        const regionEls = build();
        expect(() => applyRegionMarkup(regionEls, { words: [{ id: 'nope', types: ['underline'] }] }, 'PPN1', 5)).not.toThrow();
        expect(() => applyRegionMarkup(null, { words: [] }, 'PPN1', 5)).not.toThrow();
        expect(() => applyRegionMarkup(regionEls, null, 'PPN1', 5)).not.toThrow();
    });
});

describe('setupAnnotationsToggle (jsdom)', () => {
    const storage = { getItem: () => null, setItem: () => {} };

    const buildToggle = () => {
        const root = document.createElement('div');
        root.className = 'immersive';
        const toggleRow = document.createElement('div');
        toggleRow.className = 'immersive__anno-toggle';
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        toggleRow.appendChild(checkbox);
        root.appendChild(toggleRow);
        return { root, toggleRow, checkbox };
    };

    test('hides the whole toggle row on pages without annotations', () => {
        const { root, toggleRow, checkbox } = buildToggle();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage });
        toggle.setPageData(null);
        expect(toggleRow.hidden).toBe(true);
        toggle.setPageData({ blocks: [], words: [], lines: [] });
        expect(toggleRow.hidden).toBe(true);
    });

    test('shows the toggle row again once a page has annotations', () => {
        const { root, toggleRow, checkbox } = buildToggle();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage });
        toggle.setPageData(null);
        toggle.setPageData({ blocks: [{ id: 'b-1', author: 'gr_jacob_grimm' }], words: [], lines: [] });
        expect(toggleRow.hidden).toBe(false);
        expect(checkbox.disabled).toBe(false);
    });

    test('applies the root state class only while the page actually has annotations', () => {
        const persistedOn = { getItem: () => 'true', setItem: () => {} };
        const { root, checkbox } = buildToggle();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage: persistedOn });
        // Persisted "on", but no page data yet: the class (and its min-width /
        // two-column styling) must not leak onto annotation-less pages.
        expect(root.classList.contains('-annotations-on')).toBe(false);
        toggle.setPageData({ blocks: [{ id: 'b-1', author: 'gr_jacob_grimm' }], words: [], lines: [] });
        expect(root.classList.contains('-annotations-on')).toBe(true);
        toggle.setPageData(null);
        expect(root.classList.contains('-annotations-on')).toBe(false);
    });

    test('onChange returns an unsubscribe; destroy detaches checkbox and shortcut', () => {
        const { root, checkbox } = buildToggle();
        const keys = createKeyDispatcher();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage, keys });
        toggle.setPageData({ blocks: [{ id: 'b-1', author: 'gr_jacob_grimm' }], words: [], lines: [] });
        const seen = jest.fn();
        toggle.onChange(seen)();
        checkbox.checked = true;
        checkbox.dispatchEvent(new Event('change'));
        expect(seen).not.toHaveBeenCalled();
        expect(root.classList.contains('-annotations-on')).toBe(true);
        toggle.destroy();
        checkbox.checked = false;
        checkbox.dispatchEvent(new Event('change'));
        expect(root.classList.contains('-annotations-on')).toBe(true);
        expect(keys.handleEvent({ key: 'a', target: document.body })).toBe(false);
    });
});

describe('mountBlockAnnotations (jsdom)', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    const buildPage = () => {
        const box = document.createElement('div');
        // jsdom has no layout: force the two-column mode (width above COLLAPSE_BELOW).
        Object.defineProperty(box, 'clientWidth', { get: () => 700 });
        const line = document.createElement('div');
        line.dataset.ivRegionId = 'http://h/api/v1/annotations/alto_PI1_2_l-1/?format=oa';
        box.appendChild(line);
        document.body.appendChild(box);
        const flat = [{ id: 'http://h/api/v1/annotations/alto_PI1_2_l-1/?format=oa', level: 'line', rect: { x: 0, y: 100, w: 500, h: 24 } }];
        const els = new Map([['l-1', line]]);
        return { box, flat, els };
    };

    test('an above-anchor card shifts the text via --immersive-anno-shift; destroy clears it', () => {
        const { box, flat, els } = buildPage();
        const layout = {
            blocks: [
                {
                    id: 'b-9',
                    author: 'gr_jacob_grimm',
                    authorName: 'Jacob Grimm',
                    styleClass: 'alto-annotation--jacob-grimm',
                    rect: { x: 0, y: 60, w: 200, h: 20 },
                    text: 'Kopfnotiz',
                },
            ],
        };
        const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els });
        expect(mounted).not.toBeNull();
        // the configured styleClass is the theme's coloring hook on the card
        const card = box.querySelector('.immersive__anno-note');
        expect(card.classList.contains('alto-annotation--jacob-grimm')).toBe(true);
        // the entry is a real button (opens the detail popover, keyboard-reachable);
        // the sr-only author is part of its accessible name
        expect(card.tagName).toBe('BUTTON');
        expect(card.getAttribute('type')).toBe('button');
        expect(card.querySelector('.immersive__anno-note-author').textContent).toBe('Jacob Grimm');
        // jsdom heights are 0, so the shift equals NOTE_GAP
        expect(box.style.getPropertyValue('--immersive-anno-shift')).toBe('2px');
        mounted.destroy();
        expect(box.style.getPropertyValue('--immersive-anno-shift')).toBe('');
    });

    test('cards anchored ON their line leave the text unshifted', () => {
        const { box, flat, els } = buildPage();
        const layout = { blocks: [{ id: 'b-9', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 200, w: 200, h: 20 }, text: 'Randnotiz' }] };
        const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els });
        expect(box.style.getPropertyValue('--immersive-anno-shift')).toBe('');
        mounted.destroy();
    });
});

describe('applyTextMarkup (jsdom)', () => {
    test('joins word types onto the rendered spans via ALTO element id', () => {
        const box = document.createElement('div');
        const block = document.createElement('div');
        block.dataset.ivRegionId = 'http://h/api/v1/annotations/alto_PI1_2_b-2/?format=oa';
        const word = document.createElement('span');
        word.dataset.ivRegionId = 'http://h/api/v1/annotations/alto_PI1_2_s-1/?format=oa';
        block.appendChild(word);
        box.appendChild(block);
        const layout = { words: [{ id: 's-1', tags: ['gr_unterstrichen'], types: ['underline'] }] };
        const els = applyTextMarkup(box, layout, 'PI1', 2);
        expect(word.classList.contains('immersive__anno-word-underline')).toBe(true);
        expect(els.get('b-2')).toBe(block);
    });
});
