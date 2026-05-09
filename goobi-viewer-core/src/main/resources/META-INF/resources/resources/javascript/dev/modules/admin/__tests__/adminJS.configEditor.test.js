/**
 * Unit tests for adminJS.configEditor.
 *
 * The source uses the `var adminJS = (function(admin){...})(adminJS || {}, jQuery)`
 * IIFE pattern. require() does not work for it because the module-local
 * `var adminJS` shadows any pre-seeded global. We load it via indirect
 * eval the same way crowdsourcing-loader.js does.
 *
 * Most of `init()` reaches into viewerJS.WebSocket, CodeMirror, the
 * jQuery `.tooltip` plugin and the live DOM, so we don't try to
 * exercise it. We focus on the small pure-ish methods: `isReadOnly`
 * (truth table) and `loadBackup` (data.status branches).
 */
const fs = require('fs');
const path = require('path');

global.adminJS = {};
(0, eval)(fs.readFileSync(path.join(__dirname, '..', 'adminJS.configEditor.js'), 'utf8'));

const configEditor = global.adminJS.configEditor;

beforeEach(() => {
    // Each test seeds its own config to avoid leaking across cases.
    configEditor.config = {};
});

describe('adminJS.configEditor.isReadOnly', () => {
    test('is true when the file is readable but not writable', () => {
        configEditor.config = { currentFileIsReadable: true, currentFileIsWritable: false };
        expect(configEditor.isReadOnly()).toBe(true);
    });

    test('is false when the file is writable', () => {
        configEditor.config = { currentFileIsReadable: true, currentFileIsWritable: true };
        expect(configEditor.isReadOnly()).toBe(false);
    });

    test('is falsy when the file is not readable at all', () => {
        configEditor.config = { currentFileIsReadable: false, currentFileIsWritable: false };
        // The implementation returns the && short-circuited value (false), not a coerced boolean.
        expect(configEditor.isReadOnly()).toBeFalsy();
    });
});

describe('adminJS.configEditor.loadBackup', () => {
    let showOverlaySpy;

    beforeEach(() => {
        // showOverlayBar is invoked from inside loadBackup's setTimeout
        // call. We replace it with a spy so we can assert without
        // needing the real DOM-mutating implementation.
        showOverlaySpy = jest.spyOn(configEditor, 'showOverlayBar').mockImplementation(() => {});
    });

    afterEach(() => {
        showOverlaySpy.mockRestore();
    });

    test('triggers showOverlayBar when status is "success"', async () => {
        // The source wraps the call in $(document).ready(...). jQuery
        // queues that callback as a microtask even when the document
        // is already loaded, so we yield once before asserting.
        configEditor.loadBackup({ status: 'success' });
        await new Promise((resolve) => setTimeout(resolve, 0));
        expect(showOverlaySpy).toHaveBeenCalledWith(true);
    });

    test('is a no-op when status is anything other than "success"', async () => {
        configEditor.loadBackup({ status: 'error' });
        configEditor.loadBackup({ status: 'pending' });
        configEditor.loadBackup({});
        await new Promise((resolve) => setTimeout(resolve, 0));
        expect(showOverlaySpy).not.toHaveBeenCalled();
    });
});

describe('adminJS.configEditor.showOverlayBar / hideOverlayBar', () => {
    beforeEach(() => {
        document.body.innerHTML = '<div class="admin__overlay-bar"></div>';
    });

    test('showOverlayBar adds the -slideIn class', () => {
        configEditor.showOverlayBar();
        const $bar = $('.admin__overlay-bar');
        expect($bar.hasClass('-slideIn')).toBe(true);
        expect($bar.hasClass('-fixed')).toBe(false);
    });

    test('showOverlayBar(true) also adds -fixed so the bar stays put', () => {
        configEditor.showOverlayBar(true);
        const $bar = $('.admin__overlay-bar');
        expect($bar.hasClass('-slideIn')).toBe(true);
        expect($bar.hasClass('-fixed')).toBe(true);
    });

    test('hideOverlayBar swaps -slideIn for -slideOut when not pinned', () => {
        const $bar = $('.admin__overlay-bar');
        $bar.addClass('-slideIn');
        configEditor.hideOverlayBar();
        expect($bar.hasClass('-slideIn')).toBe(false);
        expect($bar.hasClass('-slideOut')).toBe(true);
    });

    test('hideOverlayBar leaves -fixed bars alone', () => {
        const $bar = $('.admin__overlay-bar');
        $bar.addClass('-slideIn -fixed');
        configEditor.hideOverlayBar();
        expect($bar.hasClass('-slideIn')).toBe(true);
        expect($bar.hasClass('-fixed')).toBe(true);
    });
});
