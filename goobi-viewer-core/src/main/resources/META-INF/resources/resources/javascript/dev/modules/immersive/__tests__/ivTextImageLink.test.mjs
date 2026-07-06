import { jest } from '@jest/globals';
import { indexById, mountTextImageLink, scrollTopToReveal, offsetTopWithin, buildTextLevels } from '../ivTextImageLink.mjs';

/** Flat line spans as test fixtures (one region per line, no nesting). */
function lineSpans(lines) {
    const frag = document.createDocumentFragment();
    lines.forEach(({ id, chars }) => {
        const span = document.createElement('span');
        span.className = 'immersive__fulltext-line';
        span.dataset.ivRegionId = id;
        span.textContent = chars;
        frag.appendChild(span);
    });
    return frag;
}

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
            lineSpans([
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

    // jsdom has no layout: stub the offset/scroll metrics the reveal path reads.
    function setupWithScrollContainer({ spanOffsetTop }) {
        const scrollContainer = document.createElement('div');
        Object.defineProperty(scrollContainer, 'clientHeight', { value: 200 });
        Object.defineProperty(scrollContainer, 'scrollHeight', { value: 1000 });
        scrollContainer.scrollTop = 0;
        const box = document.createElement('div');
        box.appendChild(lineSpans([{ id: 'a', chars: 'A' }]));
        scrollContainer.appendChild(box);
        const spanA = box.querySelector('[data-iv-region-id="a"]');
        Object.defineProperty(spanA, 'offsetTop', { value: spanOffsetTop });
        Object.defineProperty(spanA, 'offsetHeight', { value: 20 });
        Object.defineProperty(spanA, 'offsetParent', { value: scrollContainer });
        const overlayA = document.createElement('div');
        overlayA.dataset.ivRegionId = 'a';
        mountTextImageLink({ box, regionEls: new Map([['a', overlayA]]), scrollContainer });
        return { scrollContainer, spanA, overlayA };
    }

    test('hovering an overlay scrolls its off-screen span into view (centered)', () => {
        const { scrollContainer, overlayA } = setupWithScrollContainer({ spanOffsetTop: 600 });
        enter(overlayA);
        expect(scrollContainer.scrollTop).toBe(510);
    });

    test('hovering an overlay leaves the scroll alone when the span is visible', () => {
        const { scrollContainer, overlayA } = setupWithScrollContainer({ spanOffsetTop: 100 });
        enter(overlayA);
        expect(scrollContainer.scrollTop).toBe(0);
    });

    test('hovering the text side never scrolls', () => {
        const { scrollContainer, spanA } = setupWithScrollContainer({ spanOffsetTop: 600 });
        enter(spanA);
        expect(scrollContainer.scrollTop).toBe(0);
    });
});

describe('offsetTopWithin', () => {
    test('sums offsetTop along the offsetParent chain up to the container', () => {
        const container = {};
        const parent = { offsetTop: 100, offsetParent: container };
        const el = { offsetTop: 10, offsetParent: parent };
        expect(offsetTopWithin(el, container)).toBe(110);
    });
    test('returns the element offset when it is a direct child', () => {
        const container = {};
        expect(offsetTopWithin({ offsetTop: 42, offsetParent: container }, container)).toBe(42);
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

describe('buildTextLevels', () => {
    const RECT = { x: 0, y: 0, w: 10, h: 10 };
    const NESTED = [
        {
            id: 'b1',
            chars: '',
            rect: RECT,
            lines: [
                {
                    id: 'l1',
                    chars: 'Hallo Welt',
                    rect: RECT,
                    words: [
                        { id: 'w1', chars: 'Hallo', rect: RECT },
                        { id: 'w2', chars: 'Welt', rect: RECT },
                    ],
                },
                { id: 'l2', chars: 'ohne Woerter', rect: RECT, words: [] },
            ],
        },
        { id: null, chars: '', rect: null, lines: [{ id: 'l3', chars: 'verwaist', rect: RECT, words: [] }] },
    ];

    test('renders block > line > word with region ids and single-space word joins', () => {
        const box = document.createElement('div');
        box.appendChild(buildTextLevels(NESTED));
        const blocks = box.querySelectorAll('div.immersive__fulltext-block');
        expect(blocks).toHaveLength(2);
        expect(blocks[0].dataset.ivRegionId).toBe('b1');
        const lines = blocks[0].querySelectorAll('span.immersive__fulltext-line');
        expect(lines).toHaveLength(2);
        expect(lines[0].dataset.ivRegionId).toBe('l1');
        expect(lines[0].querySelectorAll('span.immersive__fulltext-word')).toHaveLength(2);
        expect(lines[0].textContent).toBe('Hallo Welt');
        expect(lines[1].textContent).toBe('ohne Woerter');
        expect(lines[1].querySelectorAll('span').length).toBe(0);
    });

    test('a synthetic block (id null) renders without a region id', () => {
        const box = document.createElement('div');
        box.appendChild(buildTextLevels(NESTED));
        const blocks = box.querySelectorAll('div.immersive__fulltext-block');
        expect(blocks[1].dataset.ivRegionId).toBeUndefined();
        expect(blocks[1].textContent).toBe('verwaist');
    });

    test('escapes OCR text at every level (no markup injection)', () => {
        const nested = [{ id: 'b', chars: '', rect: RECT, lines: [{ id: 'l', chars: '<i>x</i>', rect: RECT, words: [{ id: 'w', chars: '<b>y</b>', rect: RECT }] }] }];
        const box = document.createElement('div');
        box.appendChild(buildTextLevels(nested));
        expect(box.querySelector('b, i')).toBeNull();
        expect(box.querySelector('.immersive__fulltext-word').textContent).toBe('<b>y</b>');
    });

    test('empty / null → empty fragment', () => {
        expect(buildTextLevels([]).childNodes).toHaveLength(0);
        expect(buildTextLevels(null).childNodes).toHaveLength(0);
    });
});

describe('mountTextImageLink with parent cascade', () => {
    const enter = (el) => el.dispatchEvent(new Event('mouseenter'));
    const leave = (el) => el.dispatchEvent(new Event('mouseleave'));

    function setupCascade() {
        const box = document.createElement('div');
        box.appendChild(
            buildTextLevels([
                {
                    id: 'b1',
                    chars: '',
                    rect: { x: 0, y: 0, w: 10, h: 10 },
                    lines: [{ id: 'l1', chars: 'Hallo', rect: { x: 0, y: 0, w: 10, h: 5 }, words: [{ id: 'w1', chars: 'Hallo', rect: { x: 0, y: 0, w: 5, h: 5 } }] }],
                },
            ])
        );
        const overlays = new Map(
            ['b1', 'l1', 'w1'].map((id) => {
                const el = document.createElement('div');
                el.dataset.ivRegionId = id;
                return [id, el];
            })
        );
        const parents = new Map([
            ['w1', 'l1'],
            ['l1', 'b1'],
        ]);
        const link = mountTextImageLink({ box, regionEls: overlays, parents });
        const active = (id) => {
            const span = box.querySelector(`[data-iv-region-id="${id}"]`);
            return {
                span: span ? span.classList.contains('is-linked-active') : null,
                overlay: overlays.get(id).classList.contains('is-linked-active'),
            };
        };
        return { box, overlays, link, active };
    }

    test('hovering a word overlay activates word, line and block on both sides', () => {
        const { overlays, active } = setupCascade();
        enter(overlays.get('w1'));
        expect(active('w1')).toEqual({ span: true, overlay: true });
        expect(active('l1')).toEqual({ span: true, overlay: true });
        expect(active('b1')).toEqual({ span: true, overlay: true });
    });

    test('leaving a word overlay clears the whole chain', () => {
        const { overlays, active } = setupCascade();
        enter(overlays.get('w1'));
        leave(overlays.get('w1'));
        expect(active('w1')).toEqual({ span: false, overlay: false });
        expect(active('l1')).toEqual({ span: false, overlay: false });
        expect(active('b1')).toEqual({ span: false, overlay: false });
    });

    test('hovering a line overlay activates line and block but not the word', () => {
        const { overlays, active } = setupCascade();
        enter(overlays.get('l1'));
        expect(active('l1')).toEqual({ span: true, overlay: true });
        expect(active('b1')).toEqual({ span: true, overlay: true });
        expect(active('w1')).toEqual({ span: false, overlay: false });
    });

    test('text-side hover still activates only its own level (DOM nesting adds the rest)', () => {
        const { box, overlays, active } = setupCascade();
        enter(box.querySelector('[data-iv-region-id="w1"]'));
        expect(active('w1')).toEqual({ span: true, overlay: true });
        expect(overlays.get('l1').classList.contains('is-linked-active')).toBe(false);
    });

    // jsdom has no layout: stub the metrics the reveal path reads.
    function setupCascadeWithScroll() {
        const scrollContainer = document.createElement('div');
        Object.defineProperty(scrollContainer, 'clientHeight', { value: 200 });
        Object.defineProperty(scrollContainer, 'scrollHeight', { value: 1000 });
        scrollContainer.scrollTop = 0;
        const box = document.createElement('div');
        box.appendChild(
            buildTextLevels([
                {
                    id: 'b1',
                    chars: '',
                    rect: { x: 0, y: 0, w: 10, h: 10 },
                    lines: [{ id: 'l1', chars: 'Hallo', rect: { x: 0, y: 0, w: 10, h: 5 }, words: [{ id: 'w1', chars: 'Hallo', rect: { x: 0, y: 0, w: 5, h: 5 } }] }],
                },
            ])
        );
        scrollContainer.appendChild(box);
        ['b1', 'l1', 'w1'].forEach((id) => {
            const el = box.querySelector(`[data-iv-region-id="${id}"]`);
            Object.defineProperty(el, 'offsetTop', { value: 600 });
            Object.defineProperty(el, 'offsetHeight', { value: 20 });
            Object.defineProperty(el, 'offsetParent', { value: scrollContainer });
        });
        const overlays = new Map(
            ['b1', 'l1', 'w1'].map((id) => {
                const el = document.createElement('div');
                el.dataset.ivRegionId = id;
                return [id, el];
            })
        );
        const parents = new Map([
            ['w1', 'l1'],
            ['l1', 'b1'],
        ]);
        const revealIds = new Set(['l1', 'w1']);
        mountTextImageLink({ box, regionEls: overlays, parents, scrollContainer, revealIds });
        return { scrollContainer, overlays };
    }

    test('hovering a word overlay scrolls its off-screen span into view', () => {
        const { scrollContainer, overlays } = setupCascadeWithScroll();
        enter(overlays.get('w1'));
        expect(scrollContainer.scrollTop).toBe(510);
    });

    test('hovering a block overlay never scrolls (grazing a paragraph must not jump the panel)', () => {
        const { scrollContainer, overlays } = setupCascadeWithScroll();
        enter(overlays.get('b1'));
        expect(scrollContainer.scrollTop).toBe(0);
    });
});
