import { jest } from '@jest/globals';
import { buildLineSpans, indexById, mountTextImageLink, scrollTopToReveal, groupWordsIntoLines, buildWordSpans } from '../ivTextImageLink.mjs';

describe('buildLineSpans', () => {
    test('one span per line with data-iv-region-id and text set via textContent (escaped)', () => {
        const frag = buildLineSpans([
            { id: 'a', chars: 'Hallo' },
            { id: 'b', chars: '<b>x</b>' },
        ]);
        const box = document.createElement('div');
        box.appendChild(frag);
        const spans = box.querySelectorAll('span.immersive__fulltext-line');
        expect(spans).toHaveLength(2);
        expect(spans[0].dataset.ivRegionId).toBe('a');
        expect(spans[0].textContent).toBe('Hallo');
        // OCR text must not become markup:
        expect(spans[1].textContent).toBe('<b>x</b>');
        expect(box.querySelector('b')).toBeNull();
    });

    test('returns an empty fragment for [] / null', () => {
        expect(buildLineSpans([]).childNodes).toHaveLength(0);
        expect(buildLineSpans(null).childNodes).toHaveLength(0);
    });
});

describe('indexById', () => {
    test('maps elements by data-iv-region-id, skips elements without one', () => {
        const a = document.createElement('div');
        a.dataset.ivRegionId = 'a';
        const b = document.createElement('div');
        const map = indexById([a, b]);
        expect(map.get('a')).toBe(a);
        expect(map.size).toBe(1);
    });
});

describe('mountTextImageLink', () => {
    function setup() {
        const box = document.createElement('div');
        box.appendChild(
            buildLineSpans([
                { id: 'a', chars: 'A' },
                { id: 'b', chars: 'B' },
            ])
        );
        const overlayA = document.createElement('div');
        overlayA.dataset.ivRegionId = 'a';
        // 'b' intentionally has no overlay (line without #xywh)
        const regionEls = new Map([['a', overlayA]]);
        const link = mountTextImageLink({ box, regionEls });
        const spanA = box.querySelector('[data-iv-region-id="a"]');
        const spanB = box.querySelector('[data-iv-region-id="b"]');
        return { box, spanA, spanB, overlayA, link };
    }
    const enter = (el) => el.dispatchEvent(new Event('mouseenter'));
    const leave = (el) => el.dispatchEvent(new Event('mouseleave'));

    test('hovering a span activates span and its overlay (text → image)', () => {
        const { spanA, overlayA } = setup();
        enter(spanA);
        expect(spanA.classList.contains('is-linked-active')).toBe(true);
        expect(overlayA.classList.contains('is-linked-active')).toBe(true);
        leave(spanA);
        expect(spanA.classList.contains('is-linked-active')).toBe(false);
        expect(overlayA.classList.contains('is-linked-active')).toBe(false);
    });

    test('hovering an overlay activates overlay and its span (image → text)', () => {
        const { spanA, overlayA } = setup();
        enter(overlayA);
        expect(overlayA.classList.contains('is-linked-active')).toBe(true);
        expect(spanA.classList.contains('is-linked-active')).toBe(true);
        leave(overlayA);
        expect(overlayA.classList.contains('is-linked-active')).toBe(false);
        expect(spanA.classList.contains('is-linked-active')).toBe(false);
    });

    test('a span without an overlay does not crash and just activates itself', () => {
        const { spanB } = setup();
        expect(() => enter(spanB)).not.toThrow();
        expect(spanB.classList.contains('is-linked-active')).toBe(true);
    });

    test('destroy() removes listeners (hover no longer toggles)', () => {
        const { spanA, overlayA, link } = setup();
        link.destroy();
        enter(spanA);
        expect(spanA.classList.contains('is-linked-active')).toBe(false);
        expect(overlayA.classList.contains('is-linked-active')).toBe(false);
    });
});

describe('scrollTopToReveal', () => {
    test('returns null when the line is already fully visible', () => {
        expect(scrollTopToReveal({ offsetTop: 100, height: 20, scrollTop: 50, clientHeight: 200, scrollHeight: 1000 })).toBeNull();
    });
    test('centers a line below the viewport', () => {
        expect(scrollTopToReveal({ offsetTop: 600, height: 20, scrollTop: 0, clientHeight: 200, scrollHeight: 1000 })).toBe(510);
    });
    test('centers a line above the viewport', () => {
        expect(scrollTopToReveal({ offsetTop: 100, height: 20, scrollTop: 400, clientHeight: 200, scrollHeight: 1000 })).toBe(10);
    });
    test('clamps to 0 (never negative)', () => {
        expect(scrollTopToReveal({ offsetTop: 5, height: 20, scrollTop: 300, clientHeight: 200, scrollHeight: 1000 })).toBe(0);
    });
    test('clamps to max scroll (scrollHeight - clientHeight)', () => {
        expect(scrollTopToReveal({ offsetTop: 990, height: 20, scrollTop: 0, clientHeight: 200, scrollHeight: 1000 })).toBe(800);
    });
});

describe('groupWordsIntoLines', () => {
    const W = (id, y, h) => ({ id, chars: id, rect: { x: 0, y, w: 10, h } });
    test('groups vertically-overlapping words into one line, splits when no overlap', () => {
        const out = groupWordsIntoLines([W('a', 100, 40), W('b', 105, 25), W('c', 200, 30)]);
        expect(out.map((l) => l.map((w) => w.id))).toEqual([['a', 'b'], ['c']]);
    });
    test('tolerates within-line top variation (ascenders/descenders)', () => {
        const out = groupWordsIntoLines([W('a', 100, 60), W('b', 118, 40)]);
        expect(out.map((l) => l.map((w) => w.id))).toEqual([['a', 'b']]);
    });
    test('words without rect stay on the current line', () => {
        const out = groupWordsIntoLines([W('a', 100, 40), { id: 'x', chars: 'x', rect: null }, W('b', 103, 40)]);
        expect(out.map((l) => l.map((w) => w.id))).toEqual([['a', 'x', 'b']]);
    });
    test('empty / null → []', () => {
        expect(groupWordsIntoLines([])).toEqual([]);
        expect(groupWordsIntoLines(null)).toEqual([]);
    });
});

describe('buildWordSpans', () => {
    const W = (id, y, h) => ({ id, chars: id, rect: { x: 0, y, w: 10, h } });
    test('renders line blocks with inline word spans (matches line layout)', () => {
        const frag = buildWordSpans([W('a', 100, 40), W('b', 105, 25), W('c', 200, 30)]);
        const box = document.createElement('div');
        box.appendChild(frag);
        const lines = box.querySelectorAll('span.immersive__fulltext-line');
        expect(lines).toHaveLength(2);
        const words = box.querySelectorAll('span.immersive__fulltext-word');
        expect(words).toHaveLength(3);
        expect(words[0].dataset.ivRegionId).toBe('a');
        expect(lines[0].textContent).toBe('a b');
        expect(lines[1].textContent).toBe('c');
    });
    test('skips blank-chars words (no empty span, no double space)', () => {
        const frag = buildWordSpans([
            { id: 'a', chars: 'a', rect: { x: 0, y: 100, w: 10, h: 40 } },
            { id: 'sp', chars: '', rect: { x: 0, y: 102, w: 5, h: 40 } },
            { id: 'b', chars: 'b', rect: { x: 0, y: 103, w: 10, h: 40 } },
        ]);
        const box = document.createElement('div');
        box.appendChild(frag);
        expect(box.querySelectorAll('span.immersive__fulltext-word')).toHaveLength(2);
        expect(box.querySelector('span.immersive__fulltext-line').textContent).toBe('a b');
    });
    test('escapes word text (no markup injection)', () => {
        const frag = buildWordSpans([{ id: 'w', chars: '<b>x</b>', rect: { x: 0, y: 0, w: 1, h: 1 } }]);
        const box = document.createElement('div');
        box.appendChild(frag);
        expect(box.querySelector('b')).toBeNull();
        expect(box.querySelector('.immersive__fulltext-word').textContent).toBe('<b>x</b>');
    });
    test('empty / null → empty fragment', () => {
        expect(buildWordSpans([]).childNodes).toHaveLength(0);
        expect(buildWordSpans(null).childNodes).toHaveLength(0);
    });
});
