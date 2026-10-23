/**
 * Bootstrap split: the immersive chrome (panels, shortcuts, metadata fold) must
 * wire even without VIEW_IMAGES, and the image engine must NOT be built when the
 * server left out the [data-immersive-image] mount (access-denied placeholder).
 */
import { jest } from '@jest/globals';
import { initImmersiveViewer } from '../ivInit.mjs';

const flush = () => new Promise((r) => setTimeout(r, 0));

function chromeMarkup({ withImage } = {}) {
    document.body.innerHTML = `
        <div class="immersive">
            <div class="immersive__toolbar immersive__toolbar-left">
                <button data-immersive-panel="immersivePanelMetadata" aria-expanded="false"></button>
                <button data-immersive-shortcuts-trigger aria-expanded="false"></button>
            </div>
            ${withImage ? '<div class="immersive__canvas"><div data-immersive-image data-pi="PI" data-api-base="/api/v1" data-start-order="0"></div></div>' : '<div class="immersive__canvas"><div class="immersive__image-denied"></div></div>'}
            <aside id="immersivePanelMetadata" class="immersive__panel immersive__panel-left" aria-hidden="true"></aside>
            <div id="immersiveShortcuts" hidden><div class="immersive__shortcuts-dialog"><button class="immersive__shortcuts-close"></button></div></div>
        </div>`;
    return document.querySelector('.immersive');
}

describe('initImmersiveViewer bootstrap split', () => {
    beforeEach(() => {
        // Presence lets the test assert the engine is (not) constructed; the no-image
        // path must never reach it.
        global.ImageView = { Image: jest.fn(), Controls: { Zoom: class {}, Rotation: class {} } };
    });

    afterEach(() => {
        delete global.ImageView;
        document.body.innerHTML = '';
    });

    test('without an image mount: chrome wires, metadata panel toggles, no engine is built', () => {
        const root = chromeMarkup({ withImage: false });
        expect(() => initImmersiveViewer(root)).not.toThrow();
        expect(global.ImageView.Image).not.toHaveBeenCalled();

        const btn = document.querySelector('[data-immersive-panel="immersivePanelMetadata"]');
        btn.click();
        expect(document.getElementById('immersivePanelMetadata').classList.contains('is-open')).toBe(true);
        btn.click();
        expect(document.getElementById('immersivePanelMetadata').classList.contains('is-open')).toBe(false);
    });

    test('the shortcuts dispatcher is attached even without the image viewer', () => {
        const root = chromeMarkup({ withImage: false });
        initImmersiveViewer(root);
        document.dispatchEvent(new KeyboardEvent('keydown', { key: '?', bubbles: true }));
        expect(document.getElementById('immersiveShortcuts').hidden).toBe(false);
    });
});

describe('initImmersiveViewer teardown', () => {
    beforeEach(() => {
        global.ImageView = { Image: jest.fn(), Controls: { Zoom: class {}, Rotation: class {} } };
    });

    afterEach(() => {
        delete global.ImageView;
        document.body.innerHTML = '';
    });

    test('returns a handle whose destroy detaches the chrome and the key dispatcher', () => {
        const root = chromeMarkup({ withImage: false });
        const view = initImmersiveViewer(root);
        expect(typeof view.destroy).toBe('function');
        view.destroy();
        document.dispatchEvent(new KeyboardEvent('keydown', { key: '?', bubbles: true }));
        expect(document.getElementById('immersiveShortcuts').hidden).toBe(true);
        document.querySelector('[data-immersive-panel="immersivePanelMetadata"]').click();
        expect(document.getElementById('immersivePanelMetadata').classList.contains('is-open')).toBe(false);
    });
});

describe('initImmersiveViewer engine bootstrap failures', () => {
    const realFetch = global.fetch;
    beforeEach(() => {
        global.ImageView = { Image: jest.fn(), Controls: { Zoom: class {}, Rotation: class {} } };
    });
    afterEach(() => {
        global.fetch = realFetch;
        delete global.ImageView;
        document.body.innerHTML = '';
    });

    test('a failed manifest shows the stage error placeholder instead of an empty stage', async () => {
        global.fetch = jest.fn(() => Promise.reject(new Error('manifest down')));
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});
        const root = chromeMarkup({ withImage: true });
        const mount = root.querySelector('[data-immersive-image]');
        mount.dataset.pi = 'PI_FAIL';
        mount.dataset.msgError = 'Anzeige nicht ladbar';
        initImmersiveViewer(root);
        await flush();
        await flush();
        await flush();
        const alert = mount.querySelector('[role="alert"]');
        expect(alert).not.toBeNull();
        expect(alert.textContent).toBe('Anzeige nicht ladbar');
        expect(global.ImageView.Image).not.toHaveBeenCalled();
        error.mockRestore();
    });

    test('a setup step failing after the engine is built tears the engine down and shows the placeholder', async () => {
        const manifest = { sequences: [{ canvases: [{ images: [{ resource: { service: { '@id': 'https://img.test/1' } } }] }] }] };
        global.fetch = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve(manifest) }));
        const destroy = jest.fn();
        global.ImageView = {
            Image: class {
                constructor(opts) {
                    this.element = opts.element;
                    this.config = { sequence: opts.sequence };
                    // no addOnceHandler: the first wiring step after construction (onceTileLoaded) throws
                    this.openseadragon = {
                        addHandler() {},
                        removeHandler() {},
                        addOverlay() {},
                        removeOverlay() {},
                        destroy,
                        world: { getItemAt: () => null, removeItem() {}, getItemCount: () => 0 },
                        viewport: {},
                        drawer: {},
                    };
                }
                load() {
                    return Promise.resolve();
                }
            },
            Controls: { Zoom: class {}, Rotation: class {} },
        };
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});
        const root = chromeMarkup({ withImage: true });
        const mount = root.querySelector('[data-immersive-image]');
        mount.dataset.pi = 'PI_SETUP_FAIL';
        mount.dataset.msgError = 'Anzeige nicht ladbar';
        const view = initImmersiveViewer(root);
        for (let i = 0; i < 6; i++) await flush();
        expect(destroy).toHaveBeenCalledTimes(1);
        expect(mount.querySelector('[role="alert"]')).not.toBeNull();
        expect(() => view.destroy()).not.toThrow();
        expect(destroy).toHaveBeenCalledTimes(1);
        error.mockRestore();
    });

    test('a manifest without pages shows the placeholder instead of an empty viewer', async () => {
        global.fetch = jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve({ sequences: [{ canvases: [] }] }) }));
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});
        const root = chromeMarkup({ withImage: true });
        const mount = root.querySelector('[data-immersive-image]');
        mount.dataset.pi = 'PI_EMPTY';
        mount.dataset.msgError = 'Anzeige nicht ladbar';
        initImmersiveViewer(root);
        for (let i = 0; i < 4; i++) await flush();
        expect(global.ImageView.Image).not.toHaveBeenCalled();
        expect(mount.querySelector('[role="alert"]')).not.toBeNull();
        error.mockRestore();
    });

    test('a manifest failure arriving after destroy shows nothing', async () => {
        let reject;
        global.fetch = jest.fn(() => new Promise((_, rej) => (reject = rej)));
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});
        const root = chromeMarkup({ withImage: true });
        const mount = root.querySelector('[data-immersive-image]');
        mount.dataset.pi = 'PI_LATE_FAIL';
        mount.dataset.msgError = 'Anzeige nicht ladbar';
        const view = initImmersiveViewer(root);
        view.destroy();
        reject(new Error('manifest down'));
        for (let i = 0; i < 4; i++) await flush();
        expect(mount.querySelector('[role="alert"]')).toBeNull();
        error.mockRestore();
    });

    test('destroy clears the stage-loader timeout', () => {
        jest.useFakeTimers();
        try {
            global.fetch = jest.fn(() => new Promise(() => {}));
            const root = chromeMarkup({ withImage: true });
            root.querySelector('[data-immersive-image]').dataset.pi = 'PI_PENDING';
            const view = initImmersiveViewer(root);
            expect(jest.getTimerCount()).toBe(1);
            view.destroy();
            expect(jest.getTimerCount()).toBe(0);
        } finally {
            jest.useRealTimers();
        }
    });
});
