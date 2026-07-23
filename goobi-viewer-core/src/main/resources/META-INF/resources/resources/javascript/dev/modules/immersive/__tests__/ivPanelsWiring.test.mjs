/**
 * Unit tests for the panel state machine: open/switch/close with ARIA + inert
 * bookkeeping, close hooks, and the Escape / Alt+digit shortcuts through the
 * key dispatcher.
 */
import { jest } from '@jest/globals';
import { setupPanels, setupPanelResize } from '../ivPanelsWiring.mjs';
import { createKeyDispatcher } from '../ivKeys.mjs';

function mountMarkup() {
    document.body.innerHTML = `
        <div class="immersive">
            <button data-immersive-panel="immersivePanelToc" aria-expanded="false"></button>
            <button data-immersive-panel="immersivePanelSearch" aria-expanded="false"></button>
            <button data-immersive-panel="immersivePanelMetadata" aria-expanded="false" aria-disabled="true"></button>
            <aside id="immersivePanelToc" class="immersive__panel immersive__panel-left" aria-hidden="true"></aside>
            <aside id="immersivePanelSearch" class="immersive__panel immersive__panel-left" aria-hidden="true"></aside>
            <aside id="immersivePanelMetadata" class="immersive__panel immersive__panel-left" aria-hidden="true"></aside>
        </div>`;
    const root = document.querySelector('.immersive');
    const keys = createKeyDispatcher();
    const panels = setupPanels(root, keys);
    const btn = (id) => document.querySelector(`[data-immersive-panel="${id}"]`);
    const panel = (id) => document.getElementById(id);
    return { root, keys, panels, btn, panel };
}

const altDigit = (code) => ({ key: code.replace('Digit', ''), code, altKey: true, ctrlKey: false, metaKey: false, preventDefault: jest.fn() });

describe('setupPanels', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('clicking a rail button opens its panel with full ARIA/inert bookkeeping', () => {
        const { root, btn, panel } = mountMarkup();
        expect(panel('immersivePanelToc').inert).toBe(true);
        btn('immersivePanelToc').click();
        expect(panel('immersivePanelToc').classList.contains('is-open')).toBe(true);
        expect(panel('immersivePanelToc').getAttribute('aria-hidden')).toBe('false');
        expect(panel('immersivePanelToc').inert).toBe(false);
        expect(btn('immersivePanelToc').getAttribute('aria-expanded')).toBe('true');
        expect(root.classList.contains('-panel-open')).toBe(true);
    });

    test('clicking another button switches panels; clicking the same button closes', () => {
        const { root, btn, panel } = mountMarkup();
        btn('immersivePanelToc').click();
        btn('immersivePanelSearch').click();
        expect(panel('immersivePanelToc').classList.contains('is-open')).toBe(false);
        expect(panel('immersivePanelSearch').classList.contains('is-open')).toBe(true);
        btn('immersivePanelSearch').click();
        expect(panel('immersivePanelSearch').classList.contains('is-open')).toBe(false);
        expect(root.classList.contains('-panel-open')).toBe(false);
    });

    test('an aria-disabled button is a no-op', () => {
        const { btn, panel } = mountMarkup();
        btn('immersivePanelMetadata').click();
        expect(panel('immersivePanelMetadata').classList.contains('is-open')).toBe(false);
    });

    test('close hooks run on every close (teardown contract for the fulltext panel)', () => {
        const { panels, btn } = mountMarkup();
        const hook = jest.fn();
        panels.registerCloseHook(hook);
        btn('immersivePanelToc').click(); // opening closes first -> hook runs
        panels.closePanels();
        expect(hook).toHaveBeenCalledTimes(2);
    });

    test('Escape via the dispatcher closes the open panel and restores focus to its button', () => {
        const { keys, btn, panel } = mountMarkup();
        btn('immersivePanelToc').click();
        expect(keys.handleEvent({ key: 'Escape' })).toBe(true);
        expect(panel('immersivePanelToc').classList.contains('is-open')).toBe(false);
        expect(document.activeElement).toBe(btn('immersivePanelToc'));
    });

    test('Escape without an open panel is not consumed (falls through the dispatcher)', () => {
        const { keys } = mountMarkup();
        expect(keys.handleEvent({ key: 'Escape' })).toBe(false);
    });

    test('Alt+digit toggles the addressed panel through the dispatcher', () => {
        const { keys, panel } = mountMarkup();
        const e = altDigit('Digit1');
        expect(keys.handleEvent(e)).toBe(true);
        expect(e.preventDefault).toHaveBeenCalled();
        expect(panel('immersivePanelToc').classList.contains('is-open')).toBe(true);
        keys.handleEvent(altDigit('Digit3'));
        expect(panel('immersivePanelToc').classList.contains('is-open')).toBe(false);
        expect(panel('immersivePanelSearch').classList.contains('is-open')).toBe(true);
        keys.handleEvent(altDigit('Digit3'));
        expect(panel('immersivePanelSearch').classList.contains('is-open')).toBe(false);
    });

    test('Alt+digit is ignored while an immersive overlay is open', () => {
        const { keys, panel } = mountMarkup();
        const grid = document.createElement('div');
        grid.id = 'immersiveGridOverlay';
        document.body.appendChild(grid);
        expect(keys.handleEvent(altDigit('Digit1'))).toBe(false);
        expect(panel('immersivePanelToc').classList.contains('is-open')).toBe(false);
    });
});

describe('setupPanelResize', () => {
    const WIDTH_KEY = 'immersive-panel-width';

    function mountResizeMarkup() {
        document.body.innerHTML = `
            <div class="immersive">
                <div class="immersive__viewer"></div>
            </div>`;
        const root = document.querySelector('.immersive');
        const viewer = root.querySelector('.immersive__viewer');
        viewer.getBoundingClientRect = () => ({ left: 0, width: 1000 });
        return { root, viewer };
    }

    afterEach(() => {
        document.body.innerHTML = '';
        sessionStorage.clear();
        localStorage.clear();
    });

    test('restores the stored width from sessionStorage, ignoring localStorage', () => {
        sessionStorage.setItem(WIDTH_KEY, '300');
        localStorage.setItem(WIDTH_KEY, '555');
        const { root, viewer } = mountResizeMarkup();
        setupPanelResize(root);
        expect(viewer.style.getPropertyValue('--immersive-panel-width')).toBe('300px');
    });

    test('persists the dragged width to sessionStorage only', () => {
        const { root, viewer } = mountResizeMarkup();
        setupPanelResize(root);
        const handle = viewer.querySelector('.immersive__panel-resize-handle');
        handle.setPointerCapture = jest.fn();
        handle.releasePointerCapture = jest.fn();
        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 440 }));
        handle.dispatchEvent(new MouseEvent('pointerup', { clientX: 440 }));
        expect(sessionStorage.getItem(WIDTH_KEY)).toBe('400');
        expect(localStorage.getItem(WIDTH_KEY)).toBeNull();
    });
});
