/** DOM tests for joining annotations onto rendered text and image overlays. */
import { applyTextMarkup, applyRegionMarkup } from '../ui/fulltext/ivAnnotationMarkup.mjs';

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

describe('applyTextMarkup (jsdom)', () => {
    test('joins word types onto the rendered spans via ALTO element id', () => {
        const box = document.createElement('div');
        const block = document.createElement('div');
        block.dataset.ivRegionId = 'http://h/api/v1/annotations/alto_PI1_2_b-2/?format=oa';
        const word = document.createElement('span');
        word.dataset.ivRegionId = 'http://h/api/v1/annotations/alto_PI1_2_s-1/?format=oa';
        block.appendChild(word);
        box.appendChild(block);
        const layout = { words: [{ id: 's-1', tags: ['gr_unterstrichen'], types: ['underline'], styleClasses: ['alto-word--unterstrichen'] }] };
        const els = applyTextMarkup(box, layout, 'PI1', 2);
        expect(word.classList.contains('immersive__anno-word-underline')).toBe(true);
        expect(word.classList.contains('alto-word--unterstrichen')).toBe(true);
        expect(els.get('b-2')).toBe(block);
    });
});
