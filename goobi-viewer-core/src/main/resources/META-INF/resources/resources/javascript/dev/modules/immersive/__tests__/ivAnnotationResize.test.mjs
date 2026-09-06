/** Unit tests for the annotation-column splitter: the split is anchored to the printed text, dragged on the window, keyed, reset and torn down. */
import { jest } from '@jest/globals';
import { setupAnnotationColumnResize } from '../ui/fulltext/ivAnnotationResize.mjs';

const SPLIT_KEY = 'immersive-anno-split';

/** Panel markup with a fulltext box 800px wide, spanning x = 100 to x = 900. */
function mount() {
    document.body.innerHTML = `
        <div class="immersive">
            <div class="immersive__viewer">
                <aside id="immersivePanelFulltext" class="immersive__panel immersive__panel-left is-open">
                    <div class="immersive__anno-columns" id="immersiveAnnotationsColumns" data-label-resize="Breite der Annotationsspalte anpassen"></div>
                    <div class="immersive__fulltext" id="immersiveFulltext"></div>
                </aside>
            </div>
        </div>`;
    const box = document.getElementById('immersiveFulltext');
    let room = 800;
    Object.defineProperty(box, 'clientWidth', { get: () => room, configurable: true });
    box.getBoundingClientRect = () => ({ left: 100, right: 100 + room, width: room });
    return {
        root: document.querySelector('.immersive'),
        panel: document.getElementById('immersivePanelFulltext'),
        columnsHead: document.getElementById('immersiveAnnotationsColumns'),
        box,
        setRoom: (px) => {
            room = px;
        },
    };
}

function create({ panel, box, columnsHead }) {
    const resize = setupAnnotationColumnResize({ panel, box, label: columnsHead.dataset.labelResize });
    resize.handle.setPointerCapture = jest.fn();
    resize.handle.releasePointerCapture = jest.fn();
    return resize;
}

const splitVar = (panel) => panel.style.getPropertyValue('--immersive-anno-split');

/** A drag tracks the pointer on the window, so only the pointerdown goes to the handle. */
const pointer = (type, clientX) => window.dispatchEvent(new MouseEvent(type, { clientX }));

const press = (handle, key, shiftKey = false) => {
    const event = new KeyboardEvent('keydown', { key, shiftKey, cancelable: true, bubbles: true });
    handle.dispatchEvent(event);
    return event;
};

describe('setupAnnotationColumnResize', () => {
    /** Animation frames stay queued until a test flushes them, so the drag's coalescing is observable. */
    let frames = [];
    const flushFrames = () => frames.splice(0).forEach((cb) => cb && cb(0));

    beforeEach(() => {
        frames = [];
        global.requestAnimationFrame = (cb) => frames.push(cb);
        global.cancelAnimationFrame = (id) => {
            frames[id - 1] = null;
        };
    });

    afterEach(() => {
        document.body.innerHTML = '';
        sessionStorage.clear();
        delete global.requestAnimationFrame;
        delete global.cancelAnimationFrame;
    });

    test('writes no split of its own and announces the default column', () => {
        const dom = mount();
        const { handle } = create(dom);
        expect(splitVar(dom.panel)).toBe('');
        expect(handle.getAttribute('role')).toBe('separator');
        expect(handle.getAttribute('aria-orientation')).toBe('vertical');
        expect(handle.getAttribute('aria-label')).toBe('Breite der Annotationsspalte anpassen');
        expect(handle.tabIndex).toBe(0);
        // the LESS default leaves the column 160px of the 800px panel
        expect(handle.getAttribute('aria-valuenow')).toBe('20');
        expect(handle.getAttribute('aria-valuetext')).toBe('20%');
        // 120px minimum for the column, 200px for the printed text
        expect(handle.getAttribute('aria-valuemin')).toBe('15');
        expect(handle.getAttribute('aria-valuemax')).toBe('74');
    });

    test('restores the stored split', () => {
        sessionStorage.setItem(SPLIT_KEY, '300');
        const dom = mount();
        const { handle } = create(dom);
        expect(splitVar(dom.panel)).toBe('300px');
        expect(handle.getAttribute('aria-valuenow')).toBe('61');
    });

    test('a garbled stored split falls back to the default', () => {
        sessionStorage.setItem(SPLIT_KEY, 'breit');
        const dom = mount();
        create(dom);
        expect(splitVar(dom.panel)).toBe('');
    });

    test('dragging follows the pointer, writes once per frame and persists on release', () => {
        const dom = mount();
        const { handle } = create(dom);
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        expect(dom.root.classList.contains('-anno-resizing')).toBe(true);
        pointer('pointermove', 600);
        pointer('pointermove', 500);
        expect(frames.length).toBe(1);
        expect(splitVar(dom.panel)).toBe('');
        flushFrames();
        // pointer 500 − box left 100 − half the 12px gap
        expect(splitVar(dom.panel)).toBe('394px');
        expect(sessionStorage.getItem(SPLIT_KEY)).toBeNull();
        pointer('pointerup', 500);
        expect(dom.root.classList.contains('-anno-resizing')).toBe(false);
        expect(splitVar(dom.panel)).toBe('394px');
        expect(sessionStorage.getItem(SPLIT_KEY)).toBe('394');
    });

    /** The split belongs to the printed text: resizing the panel hands the difference to
     *  the annotation column and leaves the rule where the user put it. */
    test('a narrower panel leaves the split alone until the column would fall below its minimum', () => {
        const dom = mount();
        const { handle } = create(dom);
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 500 }));
        pointer('pointerup', 500);
        expect(splitVar(dom.panel)).toBe('394px');
        dom.setRoom(500);
        expect(splitVar(dom.panel)).toBe('394px');
        // 500 − 120 column − 12 gap is all the printed text can have now
        press(handle, 'ArrowRight');
        expect(splitVar(dom.panel)).toBe('368px');
        dom.setRoom(800);
        press(handle, 'ArrowRight');
        expect(splitVar(dom.panel)).toBe('384px');
    });

    /** The note relayout a drag triggers used to pull the handle out of the DOM, which
     *  dropped the pointer capture and left the splitter hanging on the pointer. */
    test('a drag keeps tracking the pointer even when the handle leaves the DOM, and lets go on release', () => {
        const dom = mount();
        const { handle } = create(dom);
        dom.box.appendChild(handle);
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        handle.remove();
        pointer('pointermove', 500);
        flushFrames();
        expect(splitVar(dom.panel)).toBe('394px');
        pointer('pointerup', 500);
        pointer('pointermove', 300);
        flushFrames();
        expect(splitVar(dom.panel)).toBe('394px');
    });

    test('a second pointer cannot start a drag while one is running', () => {
        const dom = mount();
        const { handle } = create(dom);
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 300 }));
        pointer('pointerup', 500);
        pointer('pointermove', 300);
        flushFrames();
        expect(splitVar(dom.panel)).toBe('394px');
    });

    test('a cancelled drag keeps the split it had', () => {
        const dom = mount();
        const { handle } = create(dom);
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        pointer('pointermove', 500);
        flushFrames();
        window.dispatchEvent(new MouseEvent('pointercancel', { clientX: 200 }));
        expect(splitVar(dom.panel)).toBe('394px');
        expect(dom.root.classList.contains('-anno-resizing')).toBe(false);
    });

    test('dragging past the ends clamps to what the two columns need', () => {
        const dom = mount();
        const { handle } = create(dom);
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        pointer('pointerup', 150);
        expect(splitVar(dom.panel)).toBe('200px');
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        pointer('pointerup', 890);
        expect(splitVar(dom.panel)).toBe('668px');
    });

    test('the arrow keys move the rule, Shift takes a larger step, and the keys stay with the splitter', () => {
        const dom = mount();
        const { handle } = create(dom);
        expect(press(handle, 'ArrowRight').defaultPrevented).toBe(true);
        expect(splitVar(dom.panel)).toBe('644px');
        press(handle, 'ArrowLeft');
        expect(splitVar(dom.panel)).toBe('628px');
        press(handle, 'ArrowLeft', true);
        expect(splitVar(dom.panel)).toBe('564px');
        expect(handle.getAttribute('aria-valuenow')).toBe('28');
        expect(sessionStorage.getItem(SPLIT_KEY)).toBe('564');
        expect(press(handle, 'ArrowUp').defaultPrevented).toBe(false);
    });

    test('a double click hands the column back to its default width', () => {
        const dom = mount();
        const { handle } = create(dom);
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        pointer('pointerup', 500);
        expect(sessionStorage.getItem(SPLIT_KEY)).toBe('394');
        handle.dispatchEvent(new MouseEvent('dblclick'));
        expect(splitVar(dom.panel)).toBe('');
        expect(sessionStorage.getItem(SPLIT_KEY)).toBeNull();
        expect(handle.getAttribute('aria-valuenow')).toBe('20');
    });

    test('destroy removes the handle, the pending frame and the split', () => {
        const dom = mount();
        const resize = create(dom);
        dom.box.appendChild(resize.handle);
        resize.handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 700 }));
        pointer('pointermove', 500);
        resize.destroy();
        flushFrames();
        expect(dom.box.querySelector('.immersive__anno-resize-handle')).toBeNull();
        expect(splitVar(dom.panel)).toBe('');
        expect(dom.root.classList.contains('-anno-resizing')).toBe(false);
        pointer('pointermove', 300);
        flushFrames();
        expect(splitVar(dom.panel)).toBe('');
    });

    test('without a panel or box the API stays inert', () => {
        const resize = setupAnnotationColumnResize({ panel: null, box: null });
        expect(resize.handle).toBeNull();
        expect(() => resize.destroy()).not.toThrow();
    });
});
