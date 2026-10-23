/**
 * Unit tests for the panel state machine: open/switch/close with ARIA + inert
 * bookkeeping, close hooks, and the Escape / Alt+digit shortcuts through the
 * key dispatcher.
 */
import { jest } from '@jest/globals';
import {
    setupPanels,
    setupPanelResize,
    setupToc,
    setupTocSync,
    setupTocCollapseToggle,
    setupMetadataToggle,
    pickActiveTocPageNo,
    panelIdForKeyEvent,
} from '../ui/ivPanelsWiring.mjs';
import { Emitter } from '../engine/ivViewer.mjs';
import { createKeyDispatcher } from '../shared/ivKeys.mjs';

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

    test('Alt+digit is ignored while a Bootstrap popover (share, cite, filter) is open', () => {
        const { keys, panel } = mountMarkup();
        document.body.insertAdjacentHTML('beforeend', '<div class="popover show"></div>');
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

    /** The handle position follows --immersive-panel-width, so the clamp must never
     *  let the variable drop below the open panel's own min-width (annotations
     *  two-column mode raises it) — otherwise the handle detaches from the panel edge. */
    test('drag clamps to the open panel min-width so the handle stays on the panel edge', () => {
        document.body.innerHTML = `
            <div class="immersive">
                <div class="immersive__viewer">
                    <aside id="immersivePanelFulltext" class="immersive__panel immersive__panel-left is-open"></aside>
                </div>
            </div>`;
        const root = document.querySelector('.immersive');
        const viewer = document.querySelector('.immersive__viewer');
        viewer.getBoundingClientRect = () => ({ left: 0, width: 2000 });
        document.getElementById('immersivePanelFulltext').style.minWidth = '560px';

        setupPanelResize(root);
        const handle = viewer.querySelector('.immersive__panel-resize-handle');
        handle.setPointerCapture = jest.fn();
        handle.releasePointerCapture = jest.fn();

        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 740 }));
        handle.dispatchEvent(new MouseEvent('pointermove', { clientX: 300 }));
        expect(viewer.style.getPropertyValue('--immersive-panel-width')).toBe('560px');
        handle.dispatchEvent(new MouseEvent('pointerup', { clientX: 300 }));
        expect(sessionStorage.getItem(WIDTH_KEY)).toBe('560');
    });

    test('a panel growing beyond the width variable (annotations min-width) syncs the variable to the panel edge', () => {
        const roCallbacks = [];
        global.ResizeObserver = class {
            constructor(cb) {
                roCallbacks.push(cb);
            }
            observe() {}
            disconnect() {}
        };
        try {
            document.body.innerHTML = `
                <div class="immersive">
                    <div class="immersive__viewer">
                        <aside id="immersivePanelFulltext" class="immersive__panel immersive__panel-left is-open"></aside>
                    </div>
                </div>`;
            const root = document.querySelector('.immersive');
            const viewer = document.querySelector('.immersive__viewer');
            const panel = document.getElementById('immersivePanelFulltext');
            setupPanelResize(root);
            const handle = viewer.querySelector('.immersive__panel-resize-handle');
            Object.defineProperty(handle, 'offsetParent', { get: () => viewer });
            viewer.getBoundingClientRect = () => ({ left: 0, width: 2000 });
            panel.getBoundingClientRect = () => ({ width: 560 });
            // Handle center at rail (40) + variable resolving to the 340px CSS fallback
            // — the first-visit case with no inline style and no stored width.
            handle.getBoundingClientRect = () => ({ left: 374, width: 12 });
            roCallbacks.forEach((cb) => cb());
            expect(viewer.style.getPropertyValue('--immersive-panel-width')).toBe('560px');
            expect(handle.getAttribute('aria-valuenow')).toBe('560');
        } finally {
            delete global.ResizeObserver;
        }
    });

    test('drag without a raised panel min-width clamps to the global minimum', () => {
        document.body.innerHTML = `
            <div class="immersive">
                <div class="immersive__viewer">
                    <aside id="immersivePanelToc" class="immersive__panel immersive__panel-left is-open"></aside>
                </div>
            </div>`;
        const root = document.querySelector('.immersive');
        const viewer = document.querySelector('.immersive__viewer');
        viewer.getBoundingClientRect = () => ({ left: 0, width: 2000 });

        setupPanelResize(root);
        const handle = viewer.querySelector('.immersive__panel-resize-handle');
        handle.setPointerCapture = jest.fn();
        handle.releasePointerCapture = jest.fn();

        handle.dispatchEvent(new MouseEvent('pointerdown', { clientX: 740 }));
        handle.dispatchEvent(new MouseEvent('pointermove', { clientX: 100 }));
        expect(viewer.style.getPropertyValue('--immersive-panel-width')).toBe('240px');
        handle.dispatchEvent(new MouseEvent('pointerup', { clientX: 100 }));
    });

    test('the handle is a named, focusable separator that arrow, Home and End keys resize', () => {
        document.body.innerHTML = `
            <div class="immersive" data-label-panel-resize="Panelbreite ändern">
                <div class="immersive__viewer">
                    <aside id="immersivePanelToc" class="immersive__panel immersive__panel-left is-open"></aside>
                    <div class="immersive__canvas"></div>
                </div>
            </div>`;
        const root = document.querySelector('.immersive');
        const viewer = root.querySelector('.immersive__viewer');
        viewer.getBoundingClientRect = () => ({ left: 0, width: 1000 });
        sessionStorage.setItem(WIDTH_KEY, '300');
        setupPanelResize(root);
        const handle = viewer.querySelector('.immersive__panel-resize-handle');
        expect(handle.nextElementSibling.className).toBe('immersive__canvas');
        expect(handle.getAttribute('role')).toBe('separator');
        expect(handle.getAttribute('aria-orientation')).toBe('vertical');
        expect(handle.hasAttribute('aria-hidden')).toBe(false);
        expect(handle.tabIndex).toBe(0);
        expect(handle.getAttribute('aria-label')).toBe('Panelbreite ändern');
        expect(handle.getAttribute('aria-valuenow')).toBe('300');
        expect(handle.getAttribute('aria-valuemin')).toBe('240');
        expect(handle.getAttribute('aria-valuemax')).toBe('800');

        const reachedDocument = jest.fn();
        document.addEventListener('keydown', reachedDocument);
        const press = (key, shiftKey = false) => handle.dispatchEvent(new KeyboardEvent('keydown', { key, shiftKey, bubbles: true, cancelable: true }));
        const width = () => viewer.style.getPropertyValue('--immersive-panel-width');
        try {
            press('ArrowRight');
            expect(width()).toBe('316px');
            press('ArrowLeft', true);
            expect(width()).toBe('252px');
            press('End');
            expect(width()).toBe('800px');
            press('Home');
            expect(width()).toBe('240px');
            expect(handle.getAttribute('aria-valuenow')).toBe('240');
            expect(sessionStorage.getItem(WIDTH_KEY)).toBe('240');
            expect(reachedDocument).not.toHaveBeenCalled();
        } finally {
            document.removeEventListener('keydown', reachedDocument);
        }
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

describe('setupPanels teardown', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('registerCloseHook returns an unregister function', () => {
        const { panels, btn } = mountMarkup();
        const hook = jest.fn();
        const off = panels.registerCloseHook(hook);
        off();
        btn('immersivePanelToc').click();
        panels.closePanels();
        expect(hook).not.toHaveBeenCalled();
    });

    test('replayOpen re-sends the open event for a panel that is already open', () => {
        const { panels, btn, panel } = mountMarkup();
        const seen = jest.fn();
        panel('immersivePanelToc').addEventListener('immersive:panel-open', seen);
        panels.replayOpen();
        expect(seen).not.toHaveBeenCalled();
        btn('immersivePanelToc').click();
        panels.replayOpen();
        expect(seen).toHaveBeenCalledTimes(2);
    });

    test('destroy detaches the rail buttons and the keyboard shortcuts', () => {
        const { keys, panels, btn, panel } = mountMarkup();
        panels.destroy();
        btn('immersivePanelToc').click();
        expect(panel('immersivePanelToc').classList.contains('is-open')).toBe(false);
        expect(keys.handleEvent(altDigit('Digit1'))).toBe(false);
    });
});

describe('pickActiveTocPageNo', function () {
    test('keeps the active section while any visible page is in its range', function () {
        expect(pickActiveTocPageNo([1, 5, 9], [5, 6], 5)).toBe(5);
        // double-page spread: the left page belongs to the previous section, the
        // right page still falls in the active one -> keep it
        expect(pickActiveTocPageNo([1, 5, 9], [4, 5], 5)).toBe(5);
    });

    test('a section starting on the right page of a spread wins when none is active', function () {
        expect(pickActiveTocPageNo([1, 6], [5, 6])).toBe(6);
    });

    test('the next section takes over once the active one leaves the visible range', function () {
        expect(pickActiveTocPageNo([1, 6], [6, 7], 1)).toBe(6);
    });

    test('without an active section the owner of the last visible page wins', function () {
        expect(pickActiveTocPageNo([1, 5, 9], [7])).toBe(5);
        expect(pickActiveTocPageNo([1, 5, 9], [9, 10])).toBe(9);
    });

    test('returns null when no section starts at or before the visible pages', function () {
        expect(pickActiveTocPageNo([10], [2])).toBeNull();
    });

    test('the last section owns all pages to the end', function () {
        expect(pickActiveTocPageNo([1], [40], 1)).toBe(1);
    });

    test('empty entries or pages leave the active section untouched', function () {
        expect(pickActiveTocPageNo([], [5], 3)).toBe(3);
        expect(pickActiveTocPageNo([1, 5], [], 5)).toBe(5);
    });
});

describe('panelIdForKeyEvent', function () {
    const altDigit = (code) => ({ altKey: true, ctrlKey: false, metaKey: false, code });

    test('maps Alt+1-5 to the rail panels in toolbar order (TOC, fulltext, search, metadata, layout tags)', function () {
        expect(panelIdForKeyEvent(altDigit('Digit1'))).toBe('immersivePanelToc');
        expect(panelIdForKeyEvent(altDigit('Digit2'))).toBe('immersivePanelFulltext');
        expect(panelIdForKeyEvent(altDigit('Digit3'))).toBe('immersivePanelSearch');
        expect(panelIdForKeyEvent(altDigit('Digit4'))).toBe('immersivePanelMetadata');
        expect(panelIdForKeyEvent(altDigit('Digit5'))).toBe('immersivePanelLayoutTags');
    });

    test('bare digits are no shortcut (WCAG 2.1.4: no printable single-character shortcuts)', function () {
        expect(panelIdForKeyEvent({ altKey: false, ctrlKey: false, metaKey: false, code: 'Digit1' })).toBeNull();
    });

    test('Ctrl/Cmd combinations stay with the browser (tab switching)', function () {
        expect(panelIdForKeyEvent({ altKey: true, ctrlKey: true, metaKey: false, code: 'Digit1' })).toBeNull();
        expect(panelIdForKeyEvent({ altKey: true, ctrlKey: false, metaKey: true, code: 'Digit1' })).toBeNull();
    });

    test('numpad digits and other codes are ignored (Alt+numpad types characters on Windows)', function () {
        expect(panelIdForKeyEvent(altDigit('Numpad1'))).toBeNull();
        expect(panelIdForKeyEvent(altDigit('Digit6'))).toBeNull();
        expect(panelIdForKeyEvent(altDigit('KeyA'))).toBeNull();
        expect(panelIdForKeyEvent(altDigit(undefined))).toBeNull();
    });
});

describe('TOC and metadata wiring', () => {
    if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => {};

    function tocMarkup() {
        document.body.innerHTML = `
            <aside id="immersivePanelToc" class="immersive__panel immersive__panel-left is-open">
                <button data-immersive-toc-toggle data-label-collapse="Alle zu" data-label-expand="Alle auf" hidden></button>
                <div id="widgetToc">
                    <div class="widget-toc__element" data-level="0" data-page-no="1"><span class="widget-toc__element-link"><a href="#">Werk</a></span></div>
                    <div class="widget-toc__element parent widget-toc__element--expanded" data-level="1" data-page-no="1"><button class="widget-toc__toggle" aria-expanded="true"></button><span class="widget-toc__element-link"><a href="#">Kapitel 1</a></span></div>
                    <div class="widget-toc__element" data-level="2" data-page-no="3"><span class="widget-toc__element-link"><a href="#">Abschnitt 1.1</a></span></div>
                    <div class="widget-toc__element" data-level="1" data-page-no="6"><span class="widget-toc__element-link"><a href="#">Kapitel 2</a></span></div>
                </div>
            </aside>`;
        const viewer = { getCurrentPages: () => [0], goToPage: jest.fn(), onPageChange: new Emitter() };
        return {
            viewer,
            panel: document.getElementById('immersivePanelToc'),
            toggle: document.querySelector('[data-immersive-toc-toggle]'),
            entry: (no) => document.querySelector(`.widget-toc__element[data-page-no="${no}"]:not([data-level="0"])`),
        };
    }

    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('the collapse toggle appears for nested TOCs and folds level 2 and deeper', () => {
        const { panel, toggle } = tocMarkup();
        setupTocCollapseToggle(panel);
        expect(toggle.hidden).toBe(false);
        expect(toggle.getAttribute('aria-expanded')).toBe('true');
        expect(toggle.getAttribute('aria-label')).toBe('Alle zu');
        toggle.click();
        const nested = document.querySelector('.widget-toc__element[data-level="2"]');
        const parent = document.querySelector('.widget-toc__element.parent');
        expect(nested.classList.contains('widget-toc__element--hidden')).toBe(true);
        expect(parent.classList.contains('widget-toc__element--expanded')).toBe(false);
        expect(parent.querySelector('.widget-toc__toggle').getAttribute('aria-expanded')).toBe('false');
        expect(toggle.classList.contains('-collapsed')).toBe(true);
        expect(toggle.getAttribute('aria-label')).toBe('Alle auf');
        toggle.click();
        expect(nested.classList.contains('widget-toc__element--hidden')).toBe(false);
        expect(parent.classList.contains('widget-toc__element--expanded')).toBe(true);
    });

    test('a TOC entry navigates in place and the active section follows the visible pages', () => {
        const { viewer, panel, entry } = tocMarkup();
        let pages = [0];
        viewer.getCurrentPages = () => pages;
        setupTocSync(viewer, panel);
        expect(entry(1).classList.contains('active')).toBe(true);
        entry(6).querySelector('a').click();
        expect(viewer.goToPage).toHaveBeenCalledWith(5);
        expect(entry(6).classList.contains('active')).toBe(true);
        pages = [2];
        viewer.onPageChange.emit(2);
        expect(entry(3).classList.contains('active')).toBe(true);
        pages = [1];
        viewer.onPageChange.emit(1);
        expect(entry(1).classList.contains('active')).toBe(true);
    });

    test('setupToc wires both parts and destroy detaches them; without the panel it is a no-op', () => {
        const { viewer, entry, toggle } = tocMarkup();
        const toc = setupToc(viewer);
        toc.destroy();
        entry(6).querySelector('a').click();
        expect(viewer.goToPage).not.toHaveBeenCalled();
        toggle.click();
        expect(document.querySelector('.widget-toc__element[data-level="2"]').classList.contains('widget-toc__element--hidden')).toBe(false);
        document.body.innerHTML = '';
        expect(() => setupToc(viewer).destroy()).not.toThrow();
    });

    test('the metadata fold toggles the hidden block and swaps the button labels', () => {
        document.body.innerHTML = `
            <button data-immersive-metadata-toggle="immersiveMetadataMore" aria-expanded="false">
                <span class="immersive__metadata-toggle-more">mehr</span><span class="immersive__metadata-toggle-less">weniger</span>
            </button>
            <div id="immersiveMetadataMore" hidden></div>`;
        const fold = setupMetadataToggle();
        const btn = document.querySelector('[data-immersive-metadata-toggle]');
        const more = document.getElementById('immersiveMetadataMore');
        expect(btn.querySelector('.immersive__metadata-toggle-more').getAttribute('aria-hidden')).toBe('false');
        expect(btn.querySelector('.immersive__metadata-toggle-less').getAttribute('aria-hidden')).toBe('true');
        btn.click();
        expect(more.hidden).toBe(false);
        expect(btn.getAttribute('aria-expanded')).toBe('true');
        expect(btn.querySelector('.immersive__metadata-toggle-more').getAttribute('aria-hidden')).toBe('true');
        fold.destroy();
        btn.click();
        expect(more.hidden).toBe(false);
    });
});
