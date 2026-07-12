/**
 * Unit tests for the immersive key dispatcher: one document-level keydown
 * listener, handlers consulted in priority order, first consumer wins.
 */
import { jest } from '@jest/globals';
import { createKeyDispatcher, registerViewerNavKeys } from '../ivKeys.mjs';

describe('createKeyDispatcher', () => {
    test('consults handlers in priority order and stops at the first consumer', () => {
        const keys = createKeyDispatcher();
        const calls = [];
        keys.register(20, (e) => {
            calls.push('grid');
            return e.key === 'g';
        });
        keys.register(10, (e) => {
            calls.push('modal');
            return e.key === 'm';
        });

        expect(keys.handleEvent({ key: 'm' })).toBe(true);
        expect(calls).toEqual(['modal']);

        calls.length = 0;
        expect(keys.handleEvent({ key: 'g' })).toBe(true);
        expect(calls).toEqual(['modal', 'grid']);

        calls.length = 0;
        expect(keys.handleEvent({ key: 'x' })).toBe(false);
        expect(calls).toEqual(['modal', 'grid']);
    });

    test('handlers registered late still slot in by priority (async setups)', () => {
        const keys = createKeyDispatcher();
        const calls = [];
        keys.register(40, () => {
            calls.push('panels');
            return true;
        });
        keys.register(30, () => {
            calls.push('dropdown');
            return true;
        });
        keys.handleEvent({ key: 'Escape' });
        expect(calls).toEqual(['dropdown']);
    });

    test('attach binds a single document keydown listener that runs the dispatch', () => {
        const keys = createKeyDispatcher();
        const seen = jest.fn(() => true);
        keys.register(10, seen);
        keys.attach(document);
        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'a' }));
        expect(seen).toHaveBeenCalledTimes(1);
    });
});

describe('registerViewerNavKeys', () => {
    let keys;
    let viewer;
    let stageEl;

    const dispatch = (key, extra = {}) => keys.handleEvent({ key, target: document.body, ...extra });

    beforeEach(() => {
        document.body.innerHTML = `
            <div class="immersive">
                <div data-immersive-image tabindex="-1"><div class="osd-canvas" tabindex="0"></div></div>
            </div>`;
        stageEl = document.querySelector('[data-immersive-image]');
        keys = createKeyDispatcher();
        viewer = {
            allowZoom: true,
            prev: jest.fn(),
            next: jest.fn(),
            resetView: jest.fn(),
            zoomIn: jest.fn(),
            zoomOut: jest.fn(),
            rotateLeft: jest.fn(),
            rotateRight: jest.fn(),
        };
        registerViewerNavKeys(keys, viewer, stageEl);
    });

    test('pages with the arrow keys when nothing owns them', () => {
        expect(dispatch('ArrowRight')).toBe(true);
        expect(viewer.next).toHaveBeenCalledTimes(1);
        expect(dispatch('ArrowLeft')).toBe(true);
        expect(viewer.prev).toHaveBeenCalledTimes(1);
    });

    test('resets on 0 and zooms on the +/- keys of either layout', () => {
        expect(dispatch('0')).toBe(true);
        expect(viewer.resetView).toHaveBeenCalledTimes(1);
        // German '+' and US 'Shift+=' both arrive as '+' / '='; both zoom in
        expect(dispatch('+')).toBe(true);
        expect(dispatch('=')).toBe(true);
        expect(viewer.zoomIn).toHaveBeenCalledTimes(2);
        expect(dispatch('-')).toBe(true);
        expect(dispatch('_')).toBe(true);
        expect(viewer.zoomOut).toHaveBeenCalledTimes(2);
    });

    test('zooms on Shift+Up/Down but leaves plain Up/Down to OSD panning', () => {
        expect(dispatch('ArrowUp', { shiftKey: true })).toBe(true);
        expect(viewer.zoomIn).toHaveBeenCalledTimes(1);
        expect(dispatch('ArrowDown', { shiftKey: true })).toBe(true);
        expect(viewer.zoomOut).toHaveBeenCalledTimes(1);
        expect(dispatch('ArrowUp')).toBe(false);
        expect(dispatch('ArrowDown')).toBe(false);
    });

    test('rotates on r / Shift+R and stays available without zoom rights', () => {
        expect(dispatch('r')).toBe(true);
        expect(viewer.rotateRight).toHaveBeenCalledTimes(1);
        expect(dispatch('R', { shiftKey: true })).toBe(true);
        expect(viewer.rotateLeft).toHaveBeenCalledTimes(1);
        viewer.allowZoom = false;
        expect(dispatch('r')).toBe(true);
        expect(viewer.rotateRight).toHaveBeenCalledTimes(2);
    });

    test('ignores the zoom keys when the record forbids zooming', () => {
        viewer.allowZoom = false;
        expect(dispatch('+')).toBe(false);
        expect(dispatch('-')).toBe(false);
        expect(dispatch('ArrowUp', { shiftKey: true })).toBe(false);
        expect(viewer.zoomIn).not.toHaveBeenCalled();
        expect(viewer.zoomOut).not.toHaveBeenCalled();
        // paging stays available without zoom rights
        expect(dispatch('ArrowRight')).toBe(true);
        expect(viewer.next).toHaveBeenCalledTimes(1);
    });

    test('leaves the keys alone while the user is typing', () => {
        const input = document.createElement('input');
        document.body.appendChild(input);
        expect(keys.handleEvent({ key: 'ArrowRight', target: input })).toBe(false);
        expect(viewer.next).not.toHaveBeenCalled();
    });

    test('defers to OSD when the stage itself has focus (no double navigation)', () => {
        stageEl.querySelector('.osd-canvas').focus();
        expect(stageEl.contains(document.activeElement)).toBe(true);
        expect(dispatch('ArrowRight')).toBe(false);
        expect(viewer.next).not.toHaveBeenCalled();
    });

    test('bails while an overlay, panel or popover is open', () => {
        for (const html of [
            '<div id="immersivePageDropdown"></div>',
            '<div id="immersiveGridOverlay"></div>',
            '<div class="immersive__panel-left is-open"></div>',
            '<div class="popover show"></div>',
        ]) {
            document.body.insertAdjacentHTML('beforeend', html);
            expect(dispatch('ArrowRight')).toBe(false);
            document.body.lastElementChild.remove();
        }
        expect(viewer.next).not.toHaveBeenCalled();
    });

    test('bails on modifier combinations so browser zoom and Alt+digit survive', () => {
        expect(dispatch('+', { ctrlKey: true })).toBe(false);
        expect(dispatch('-', { metaKey: true })).toBe(false);
        expect(dispatch('ArrowRight', { altKey: true })).toBe(false);
        expect(viewer.zoomIn).not.toHaveBeenCalled();
        expect(viewer.next).not.toHaveBeenCalled();
    });

    test('ignores keys it does not own', () => {
        expect(dispatch('a')).toBe(false);
        expect(dispatch('ArrowUp')).toBe(false);
        expect(dispatch('ArrowDown')).toBe(false);
    });
});
