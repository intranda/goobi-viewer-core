/**
 * Bootstrap split: the immersive chrome (panels, shortcuts, metadata fold) must
 * wire even without VIEW_IMAGES, and the image engine must NOT be built when the
 * server left out the [data-immersive-image] mount (access-denied placeholder).
 */
import { jest } from '@jest/globals';
import { initImmersiveViewer } from '../ivInit.mjs';

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
