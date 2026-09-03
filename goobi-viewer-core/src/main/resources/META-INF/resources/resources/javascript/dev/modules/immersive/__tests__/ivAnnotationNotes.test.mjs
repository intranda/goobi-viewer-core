/** DOM tests for the margin-note cards: anchoring, layout, hover coupling and teardown. */
import { mountBlockAnnotations } from '../ui/fulltext/ivAnnotationNotes.mjs';

describe('mountBlockAnnotations (jsdom)', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    const buildPage = () => {
        const box = document.createElement('div');
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
        const card = box.querySelector('.immersive__anno-note');
        expect(card.classList.contains('alto-annotation--jacob-grimm')).toBe(true);
        expect(card.tagName).toBe('BUTTON');
        expect(card.getAttribute('type')).toBe('button');
        expect(card.querySelector('.immersive__anno-note-author').textContent).toBe('Jacob Grimm');
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
