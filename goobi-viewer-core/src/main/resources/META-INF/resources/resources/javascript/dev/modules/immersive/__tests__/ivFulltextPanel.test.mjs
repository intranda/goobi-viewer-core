/** Unit tests for the fulltext panel wiring: load on open, empty state, double-page availability, teardown. */
import { jest } from '@jest/globals';
import { setupFulltextPanel } from '../ui/fulltext/ivFulltextPanel.mjs';
import { createKeyDispatcher } from '../shared/ivKeys.mjs';
import { Emitter } from '../engine/ivViewer.mjs';

const flush = () => new Promise((r) => setTimeout(r, 0));

function fakeViewer() {
    return {
        getCurrentOrder: () => 0,
        getCurrentPages: () => [0],
        isDoublePage: jest.fn(() => false),
        setTextRegions: jest.fn(() => new Map()),
        clearTextRegions: jest.fn(),
        setAnnotationRegions: jest.fn(() => new Map()),
        clearAnnotationRegions: jest.fn(),
        onPageChange: new Emitter(),
        onOpen: new Emitter(),
    };
}

function mountMarkup() {
    document.body.innerHTML = `
        <div class="immersive">
            <button data-immersive-panel="immersivePanelFulltext" title="Volltext" data-label-disabled="Nur bei Einzelseite"></button>
            <aside id="immersivePanelFulltext" class="immersive__panel immersive__panel-left">
                <div class="immersive__anno-toggle" hidden><input type="checkbox" id="immersiveAnnotationsToggle" /></div>
                <div id="immersiveAnnotationsColumns" hidden></div>
                <div id="immersiveFulltextLoader" hidden></div>
                <div id="immersiveFulltext" data-label-empty="Kein Volltext vorhanden" data-label-error="Volltext nicht ladbar"></div>
            </aside>
        </div>`;
    const viewer = fakeViewer();
    const unregister = jest.fn();
    const panels = { closePanels: jest.fn(), registerCloseHook: jest.fn(() => unregister) };
    const fulltext = setupFulltextPanel(viewer, 'PI', '/api/v1', panels, createKeyDispatcher());
    const panel = document.getElementById('immersivePanelFulltext');
    const open = () => {
        panel.classList.add('is-open');
        panel.dispatchEvent(new CustomEvent('immersive:panel-open'));
    };
    return { fulltext, viewer, panels, unregister, panel, open, box: document.getElementById('immersiveFulltext'), btn: document.querySelector('[data-immersive-panel]') };
}

const realFetch = global.fetch;
afterEach(() => {
    global.fetch = realFetch;
    document.body.innerHTML = '';
});

test('opening the panel loads the page text; a page without text shows the empty label', async () => {
    global.fetch = jest.fn(async () => ({ ok: true, status: 200, json: async () => ({ resources: [] }) }));
    const { open, box } = mountMarkup();
    open();
    await flush();
    await flush();
    expect(global.fetch).toHaveBeenCalled();
    expect(global.fetch.mock.calls[0][0]).toContain('/records/PI/');
    expect(box.textContent).toBe('Kein Volltext vorhanden');
    expect(box.classList.contains('-empty')).toBe(true);
    expect(box.classList.contains('-error')).toBe(false);
    expect(document.getElementById('immersiveFulltextLoader').hidden).toBe(true);
});

test('a failed text request shows the error label, not "no full text"', async () => {
    global.fetch = jest.fn(async () => ({ ok: false, status: 503, json: async () => ({}) }));
    const { open, box } = mountMarkup();
    open();
    await flush();
    await flush();
    expect(box.textContent).toBe('Volltext nicht ladbar');
    expect(box.classList.contains('-error')).toBe(true);
});

test('double-page mode disables the rail button and closes an open fulltext panel', () => {
    const { fulltext, viewer, panels, btn, open } = mountMarkup();
    expect(btn.getAttribute('aria-disabled')).toBe('false');
    open();
    viewer.isDoublePage.mockReturnValue(true);
    fulltext.updateFulltextAvail();
    expect(btn.getAttribute('aria-disabled')).toBe('true');
    expect(btn.getAttribute('tabindex')).toBe('-1');
    expect(btn.getAttribute('title')).toBe('Nur bei Einzelseite');
    expect(panels.closePanels).toHaveBeenCalled();
    viewer.isDoublePage.mockReturnValue(false);
    fulltext.updateFulltextAvail();
    expect(btn.getAttribute('title')).toBe('Volltext');
});

test('destroy unregisters the close hook and stops reacting to panel-open', async () => {
    global.fetch = jest.fn(async () => ({ ok: false, status: 404, text: async () => '', json: async () => ({}) }));
    const { fulltext, unregister, open } = mountMarkup();
    fulltext.destroy();
    expect(unregister).toHaveBeenCalled();
    open();
    await flush();
    expect(global.fetch).not.toHaveBeenCalled();
});
