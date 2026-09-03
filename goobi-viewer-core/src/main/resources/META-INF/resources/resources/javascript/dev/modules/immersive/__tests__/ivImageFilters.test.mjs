/**
 * Unit tests for the image-filter rendering adapter: every active filter runs
 * exactly once per drawn frame, regardless of how often the library re-opens.
 * Regression guard for the compounding bug (contrast^N after N double-page
 * toggles) caused by the library stacking event forwarders per load().
 */
import { jest } from '@jest/globals';
import { bindImageFiltersRendering } from '../ui/ivImageFilters.mjs';
import { Emitter } from '../engine/ivViewer.mjs';

function fakeOsd() {
    const handlers = {};
    return {
        _handlers: handlers,
        addHandler(name, fn) {
            (handlers[name] = handlers[name] || []).push(fn);
        },
        fire(name) {
            (handlers[name] || []).forEach((fn) => fn());
        },
        forceRedraw: jest.fn(),
    };
}

function setup() {
    const osd = fakeOsd();
    const imageData = { data: 'pixels' };
    const context = {
        canvas: { width: 10, height: 5 },
        getImageData: jest.fn(() => imageData),
        putImageData: jest.fn(),
    };
    const image = { openseadragon: osd, getCanvasContext: () => context };
    const viewer = {
        viewer: image,
        onOpen: new Emitter(),
        onReset: new Emitter(),
        redraw: () => osd.forceRedraw(),
        getCanvasContext: () => context,
        onFrame: (fn) => {
            osd.addHandler('update-viewport', fn);
            return () => {};
        },
    };
    const filterA = { filterMethod: jest.fn((d) => d) };
    const filterB = { filterMethod: jest.fn(() => undefined) };
    const tag = { filters: [filterA, filterB] };
    bindImageFiltersRendering(viewer, tag);
    return { osd, context, image, viewer, filterA, filterB, tag };
}

describe('bindImageFiltersRendering', () => {
    test('start/close toggle the active set and force a redraw; isActive reflects it', () => {
        const { osd, filterA } = setup();
        expect(filterA.isActive()).toBe(false);
        filterA.start();
        expect(filterA.isActive()).toBe(true);
        expect(osd.forceRedraw).toHaveBeenCalledTimes(1);
        filterA.close();
        expect(filterA.isActive()).toBe(false);
        expect(osd.forceRedraw).toHaveBeenCalledTimes(2);
    });

    test('each active filter runs exactly once per update-viewport event', () => {
        const { osd, context, filterA, filterB } = setup();
        filterA.start();
        filterB.start();
        osd.fire('update-viewport');
        expect(filterA.filterMethod).toHaveBeenCalledTimes(1);
        expect(filterB.filterMethod).toHaveBeenCalledTimes(1);
        expect(context.putImageData).toHaveBeenCalledTimes(1);
        osd.fire('update-viewport');
        expect(filterA.filterMethod).toHaveBeenCalledTimes(2);
        expect(context.putImageData).toHaveBeenCalledTimes(2);
    });

    test('renders through the engine frame hook exactly once per registration', () => {
        const { osd, filterA } = setup();
        filterA.start();
        expect(osd._handlers['update-viewport']).toHaveLength(1);
    });

    test('without active filters the frame handler does not touch the canvas', () => {
        const { osd, context } = setup();
        osd.fire('update-viewport');
        expect(context.getImageData).not.toHaveBeenCalled();
        expect(context.putImageData).not.toHaveBeenCalled();
    });

    test('a filterMethod returning nothing keeps the previous image data (data = f(data) || data)', () => {
        const { osd, context, filterA, filterB } = setup();
        filterB.start();
        filterA.start();
        osd.fire('update-viewport');
        // filterA received the original data even though filterB returned undefined
        expect(filterA.filterMethod).toHaveBeenCalledWith({ data: 'pixels' });
        expect(context.putImageData).toHaveBeenCalledWith({ data: 'pixels' }, 0, 0);
    });
});

describe('setupImmersivePopoverA11y', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('binds the Escape handler once per popover element and moves focus into it on every show', async () => {
        const { setupImmersivePopoverA11y } = await import('../ui/ivImageFilters.mjs');
        document.body.innerHTML = `
            <button data-popover-element="#immersiveSharePopover" aria-describedby="pop1" aria-expanded="false"></button>
            <div id="pop1" class="popover show"><a href="#" id="link">x</a></div>`;
        const trigger = document.querySelector('[data-popover-element]');
        const pop = document.getElementById('pop1');
        const addListener = jest.spyOn(pop, 'addEventListener');
        const a11y = setupImmersivePopoverA11y();
        window.$(trigger).trigger('shown.bs.popover');
        window.$(trigger).trigger('shown.bs.popover');
        expect(addListener.mock.calls.filter((c) => c[0] === 'keydown')).toHaveLength(1);
        expect(trigger.getAttribute('aria-expanded')).toBe('true');
        expect(document.activeElement).toBe(document.getElementById('link'));
        a11y.destroy();
        window.$(trigger).trigger('hidden.bs.popover');
        expect(trigger.getAttribute('aria-expanded')).toBe('true');
    });
});
