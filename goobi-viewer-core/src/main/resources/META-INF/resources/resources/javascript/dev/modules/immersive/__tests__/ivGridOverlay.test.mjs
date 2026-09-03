/** Unit tests for the thumbnail grid overlay: lazy riot mount, modal state, Escape, thumbnail size, teardown. */
import { jest } from '@jest/globals';
import { setupOverviewGrid } from '../ui/ivGridOverlay.mjs';
import { createKeyDispatcher } from '../shared/ivKeys.mjs';
import { Emitter } from '../engine/ivViewer.mjs';

class FakeSubject {
    constructor() {
        this.fns = [];
    }
    subscribe(fn) {
        this.fns.push(fn);
        return { unsubscribe: jest.fn() };
    }
    next(v) {
        this.fns.forEach((fn) => fn(v));
    }
}

function fakeViewer() {
    return { getCurrentPages: () => [0], getCurrentOrder: () => 0, goToPage: jest.fn(), onPageChange: new Emitter() };
}

function mountMarkup() {
    document.body.innerHTML = `
        <div class="immersive">
            <button data-immersive-action="overview" aria-label="Übersicht" aria-expanded="false"></button>
            <div class="immersive__grid" id="immersiveGridOverlay" hidden>
                <button class="immersive__grid-close"></button>
                <button class="immersive__grid-top" hidden></button>
                <input type="range" id="immersiveGridSize" min="0" max="2" value="1" data-label-small="Klein" data-label-medium="Mittel" data-label-large="Groß" />
                <div class="immersive__grid-scroll"><div id="immersiveGridLoader"></div><div id="immersiveThumbnails"></div></div>
            </div>
        </div>`;
    const tag = { opts: {}, update: jest.fn(), unmount: jest.fn() };
    global.riot = { mount: jest.fn(() => [tag]) };
    global.rxjs = { Subject: FakeSubject };
    const keys = createKeyDispatcher();
    const viewer = fakeViewer();
    const grid = setupOverviewGrid(viewer, 'PI', '/api/v1', keys);
    return { grid, keys, viewer, tag, overlay: document.getElementById('immersiveGridOverlay'), trigger: document.querySelector('[data-immersive-action="overview"]') };
}

afterEach(() => {
    document.body.innerHTML = '';
    localStorage.clear();
    delete global.riot;
    delete global.rxjs;
});

test('opens as a modal dialog and mounts the riot thumbnails once', () => {
    const { grid, overlay, trigger, tag } = mountMarkup();
    grid.toggleGrid();
    expect(overlay.hidden).toBe(false);
    expect(overlay.getAttribute('role')).toBe('dialog');
    expect(overlay.getAttribute('aria-label')).toBe('Übersicht');
    expect(trigger.getAttribute('aria-expanded')).toBe('true');
    expect(document.activeElement).toBe(overlay.querySelector('.immersive__grid-close'));
    expect(global.riot.mount).toHaveBeenCalledTimes(1);
    expect(global.riot.mount.mock.calls[0][2]).toEqual(expect.objectContaining({ source: '/api/v1/records/PI/manifest', index: 0 }));
    grid.toggleGrid();
    expect(overlay.hidden).toBe(true);
    expect(document.activeElement).toBe(trigger);
    grid.toggleGrid();
    expect(global.riot.mount).toHaveBeenCalledTimes(1);
    expect(tag.update).toHaveBeenCalled();
});

test('Escape closes through the dispatcher; a thumbnail click navigates and closes', () => {
    const { grid, keys, viewer, overlay } = mountMarkup();
    grid.toggleGrid();
    expect(keys.handleEvent({ key: 'Escape' })).toBe(true);
    expect(overlay.hidden).toBe(true);
    expect(keys.handleEvent({ key: 'Escape' })).toBe(false);
    grid.toggleGrid();
    global.riot.mount.mock.calls[0][2].actionlistener.next({ action: 'clickImage', value: 3 });
    expect(viewer.goToPage).toHaveBeenCalledWith(3);
    expect(overlay.hidden).toBe(true);
});

test('the size slider updates the grid step, its spoken value and the persisted preference', () => {
    const { overlay } = mountMarkup();
    const slider = document.getElementById('immersiveGridSize');
    expect(overlay.dataset.thumbSize).toBe('1');
    slider.value = '2';
    slider.dispatchEvent(new Event('input'));
    expect(overlay.dataset.thumbSize).toBe('2');
    expect(slider.getAttribute('aria-valuetext')).toBe('Groß');
    expect(localStorage.getItem('immersive-thumb-size')).toBe('2');
});

test('destroy closes the grid and detaches slider and Escape', () => {
    const { grid, keys, overlay } = mountMarkup();
    grid.toggleGrid();
    grid.destroy();
    expect(overlay.hidden).toBe(true);
    const slider = document.getElementById('immersiveGridSize');
    slider.value = '0';
    slider.dispatchEvent(new Event('input'));
    expect(overlay.dataset.thumbSize).toBe('1');
    grid.toggleGrid();
    expect(keys.handleEvent({ key: 'Escape' })).toBe(false);
});

test('destroy unmounts the riot grid but keeps its mount element', () => {
    const { grid, tag } = mountMarkup();
    grid.toggleGrid();
    grid.destroy();
    expect(tag.unmount).toHaveBeenCalledWith(true);
    expect(document.getElementById('immersiveThumbnails')).not.toBeNull();
});
