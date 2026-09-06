/** DOM tests for the margin-note cards: anchoring, layout, hover coupling and teardown. */
import { mountBlockAnnotations } from '../ui/fulltext/ivAnnotationNotes.mjs';

describe('mountBlockAnnotations (jsdom)', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    const buildPage = () => {
        const box = document.createElement('div');
        let width = 700;
        const setWidth = (px) => {
            width = px;
        };
        Object.defineProperty(box, 'clientWidth', { get: () => width });
        const line = document.createElement('div');
        line.dataset.ivRegionId = 'http://h/api/v1/annotations/alto_PI1_2_l-1/?format=oa';
        box.appendChild(line);
        document.body.appendChild(box);
        const flat = [{ id: 'http://h/api/v1/annotations/alto_PI1_2_l-1/?format=oa', level: 'line', rect: { x: 0, y: 100, w: 500, h: 24 } }];
        const els = new Map([['l-1', line]]);
        return { box, flat, els, setWidth };
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
        const card = box.querySelector('.immersive__anno-note');
        expect(card.classList.contains('alto-annotation--jacob-grimm')).toBe(true);
        expect(card.tagName).toBe('BUTTON');
        expect(card.getAttribute('type')).toBe('button');
        expect(card.querySelector('.immersive__anno-note-author').textContent).toBe('Jacob Grimm');
        expect(box.style.getPropertyValue('--immersive-anno-shift')).toBe('2px');
        mounted.destroy();
        expect(box.style.getPropertyValue('--immersive-anno-shift')).toBe('');
    });

    test('the card text carries the word markup of the annotation (underline + tag class), without region ids', () => {
        const { box, flat, els } = buildPage();
        const line = els.get('l-1');
        line.className = 'immersive__fulltext-line';
        ['s-1', 's-2'].forEach((id, i) => {
            if (i > 0) line.append(' ');
            const word = document.createElement('span');
            word.className = 'immersive__fulltext-word';
            word.dataset.ivRegionId = `http://h/api/v1/annotations/alto_PI1_2_${id}/?format=oa`;
            word.textContent = i ? 'notiz' : 'Rand';
            line.appendChild(word);
        });
        const layout = {
            blocks: [{ id: 'b-9', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 90, w: 600, h: 60 }, text: 'Rand notiz' }],
            words: [{ id: 's-2', tags: ['gr_unterstrichen'], types: ['underline'], styleClasses: ['alto-word--unterstrichen'] }],
        };
        const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els });
        const text = box.querySelector('.immersive__anno-note-text');
        expect(text.textContent).toBe('Rand notiz');
        const spans = Array.from(text.querySelectorAll('span'));
        expect(spans.map((s) => s.className)).toEqual(['', 'immersive__anno-word-underline alto-word--unterstrichen']);
        expect(text.querySelector('[data-iv-region-id]')).toBeNull();
        mounted.destroy();
    });

    test('only a card whose text overflows is marked as expandable', () => {
        const { box, flat, els } = buildPage();
        const layout = {
            blocks: [
                { id: 'b-1', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 100, w: 200, h: 20 }, text: 'kurz' },
                { id: 'b-2', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 300, w: 200, h: 20 }, text: 'sehr lang' },
            ],
        };
        const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els });
        const [shortCard, longCard] = Array.from(box.querySelectorAll('.immersive__anno-note'));
        const clamp = (card, full) => {
            const text = card.querySelector('.immersive__anno-note-text');
            Object.defineProperty(text, 'clientHeight', { get: () => 40, configurable: true });
            Object.defineProperty(text, 'scrollHeight', { get: () => full, configurable: true });
        };
        clamp(shortCard, 40);
        clamp(longCard, 96);
        mounted.relayout();
        expect(shortCard.classList.contains('-truncated')).toBe(false);
        expect(longCard.classList.contains('-truncated')).toBe(true);
        mounted.destroy();
    });

    test('the column splitter joins the margin column, ahead of the cards, and leaves with them when they collapse', () => {
        const { box, flat, els, setWidth } = buildPage();
        const layout = { blocks: [{ id: 'b-9', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 200, w: 200, h: 20 }, text: 'Randnotiz' }] };
        const resizer = { handle: document.createElement('div') };
        let removals = 0;
        const domRemove = resizer.handle.remove.bind(resizer.handle);
        resizer.handle.remove = () => {
            removals++;
            domRemove();
        };
        const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els, resizer });
        expect(box.contains(resizer.handle)).toBe(true);
        expect(resizer.handle.nextElementSibling.className).toBe('immersive__anno-margin');
        // a drag relayouts on every frame; taking the handle out would drop its pointer capture
        mounted.relayout();
        expect(removals).toBe(0);
        expect(resizer.handle.nextElementSibling.className).toBe('immersive__anno-margin');
        setWidth(400);
        mounted.relayout();
        expect(box.classList.contains('-anno-collapsed')).toBe(true);
        expect(box.contains(resizer.handle)).toBe(false);
        setWidth(700);
        mounted.relayout();
        expect(box.contains(resizer.handle)).toBe(true);
        mounted.destroy();
        expect(box.contains(resizer.handle)).toBe(false);
    });

    /** The split is anchored to the printed text, so a panel resize changes only the column's
     *  width; watching the text box alone misses it and leaves the cards at stale tops. */
    test('relayouts when the margin column changes size, not only when the text does', () => {
        const observed = [];
        global.ResizeObserver = class {
            observe(el) {
                observed.push(el);
            }
            disconnect() {}
        };
        try {
            const { box, flat, els } = buildPage();
            const layout = { blocks: [{ id: 'b-9', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 200, w: 200, h: 20 }, text: 'Randnotiz' }] };
            const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els });
            expect(observed).toContain(box);
            expect(observed).toContain(box.querySelector('.immersive__anno-margin'));
            mounted.destroy();
        } finally {
            delete global.ResizeObserver;
        }
    });

    test('cards are stacked with the height they have once marked as expandable', () => {
        const { box, flat, els } = buildPage();
        const layout = {
            blocks: [
                { id: 'b-1', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 100, w: 200, h: 20 }, text: 'sehr lang' },
                { id: 'b-2', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 100, w: 200, h: 20 }, text: 'kurz' },
            ],
        };
        const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els });
        const [longCard, shortCard] = Array.from(box.querySelectorAll('.immersive__anno-note'));
        // the expandable card grows by 4px of padding once it is marked
        [longCard, shortCard].forEach((card) => {
            Object.defineProperty(card, 'offsetHeight', { get: () => (card.classList.contains('-truncated') ? 44 : 40) });
            const text = card.querySelector('.immersive__anno-note-text');
            Object.defineProperty(text, 'clientHeight', { get: () => 40 });
            Object.defineProperty(text, 'scrollHeight', { get: () => (card === longCard ? 96 : 40) });
        });
        mounted.relayout();
        expect(longCard.classList.contains('-truncated')).toBe(true);
        expect(longCard.style.top).toBe('0px');
        // 44px card + 2px gap, not the 40px it measured before it was marked
        expect(shortCard.style.top).toBe('46px');
        mounted.destroy();
    });

    test('cards anchored ON their line leave the text unshifted', () => {
        const { box, flat, els } = buildPage();
        const layout = { blocks: [{ id: 'b-9', author: 'gr_jacob_grimm', authorName: 'Jacob Grimm', rect: { x: 0, y: 200, w: 200, h: 20 }, text: 'Randnotiz' }] };
        const mounted = mountBlockAnnotations({ box, layout, pi: 'PI1', pageNo: 2, flat, els });
        expect(box.style.getPropertyValue('--immersive-anno-shift')).toBe('');
        mounted.destroy();
    });
});
