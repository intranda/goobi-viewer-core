/**
 * Unit tests for viewerJS.overlay.
 *
 * The init() helper iterates over [data-overlay='content'] markers and
 * binds clicks on the trigger buttons. open() builds and resolves a
 * Promise around an .overlay element. We focus on the dispatcher
 * branches (modal vs default), the closable / fullscreen flags, and
 * the open()/close lifecycle.
 *
 * jsdom does not ship `CSS.escape`; the source calls it on every
 * data-overlay-trigger value. We provide a minimal stub that mirrors
 * the spec behavior we need (just escaping characters that would
 * break a CSS selector).
 */
global.CSS = global.CSS || { escape: (s) => String(s).replace(/[^\w-]/g, (c) => '\\' + c) };

const viewerJS = require('../viewerJS.overlay.js');

beforeAll(async () => {
    // The source registers a `$(document).ready(viewer.overlay.init)`
    // at load time; yield once so any pending document.ready callbacks
    // have run on the empty body before tests start.
    await new Promise((r) => setTimeout(r, 0));
});

beforeEach(() => {
    document.body.innerHTML = '';
});

describe('init: dispatch on data-overlay-type', () => {
    test('default type calls viewer.overlay.open with the overlay children', () => {
        document.body.innerHTML = `
            <div data-overlay="content" data-overlay-trigger="btn">
                <span class="content">child</span>
            </div>
            <button id="btn">Open</button>`;
        const openSpy = jest.spyOn(viewerJS.overlay, 'open').mockImplementation(() => Promise.resolve());

        viewerJS.overlay.init();
        $('#btn').trigger('click');

        expect(openSpy).toHaveBeenCalledTimes(1);
        // First arg is the children jQuery set with class .content.
        const $node = openSpy.mock.calls[0][0];
        expect($node.is('.content')).toBe(true);
        openSpy.mockRestore();
    });

    test('modal type calls viewer.overlay.openModal instead of open', () => {
        document.body.innerHTML = `
            <div data-overlay="content" data-overlay-trigger="btn" data-overlay-type="modal">
                <span class="content">child</span>
            </div>
            <button id="btn">Open</button>`;
        viewerJS.overlay.openModal = jest.fn();
        viewerJS.overlay.init();

        $('#btn').trigger('click');

        expect(viewerJS.overlay.openModal).toHaveBeenCalledTimes(1);
        delete viewerJS.overlay.openModal;
    });

    test('passes data-overlay-closable === "false" through as closable=false', () => {
        document.body.innerHTML = `
            <div data-overlay="content" data-overlay-trigger="btn" data-overlay-closable="false">
                <span class="content"></span>
            </div>
            <button id="btn">Open</button>`;
        const openSpy = jest.spyOn(viewerJS.overlay, 'open').mockImplementation(() => Promise.resolve());

        viewerJS.overlay.init();
        $('#btn').trigger('click');

        // Args: (node, closable, fullscreen)
        expect(openSpy.mock.calls[0][1]).toBe(false);
        openSpy.mockRestore();
    });

    test('passes data-overlay-fullscreen === "true" through as fullscreen=true', () => {
        document.body.innerHTML = `
            <div data-overlay="content" data-overlay-trigger="btn" data-overlay-fullscreen="true">
                <span class="content"></span>
            </div>
            <button id="btn">Open</button>`;
        const openSpy = jest.spyOn(viewerJS.overlay, 'open').mockImplementation(() => Promise.resolve());

        viewerJS.overlay.init();
        $('#btn').trigger('click');

        expect(openSpy.mock.calls[0][2]).toBe(true);
        openSpy.mockRestore();
    });
});

describe('open(node, closable, fullscreen)', () => {
    function setupOverlayDom() {
        document.body.innerHTML = `
            <div class="overlay overlay-plain">
                <button data-overlay-action="dismiss" style="display:none">x</button>
            </div>
            <div id="content"><p>Body</p></div>`;
    }

    test('rejects when no .overlay.overlay-plain element is in the DOM', async () => {
        document.body.innerHTML = '<div id="content"></div>';
        await expect(viewerJS.overlay.open(document.getElementById('content'))).rejects.toMatch(/No overlay/);
    });

    test('rejects when the overlay is already active', async () => {
        setupOverlayDom();
        document.querySelector('.overlay').classList.add('active');
        await expect(viewerJS.overlay.open(document.getElementById('content'))).rejects.toMatch(/already active/);
    });

    test('appends the node, marks the overlay active, and locks <html> scroll', async () => {
        setupOverlayDom();
        const overlay = await viewerJS.overlay.open(document.getElementById('content'), true, false);

        expect(document.querySelector('.overlay.active')).not.toBeNull();
        expect(document.documentElement.classList.contains('no-overflow')).toBe(true);
        // The node was moved into the overlay.
        expect(document.querySelector('.overlay #content')).not.toBeNull();
        // close() restores both invariants.
        overlay.close();
        expect(document.querySelector('.overlay.active')).toBeNull();
        expect(document.documentElement.classList.contains('no-overflow')).toBe(false);
    });

    test('shows dismiss buttons when closable=true and hides them when closable=false', async () => {
        setupOverlayDom();
        let overlay = await viewerJS.overlay.open(document.getElementById('content'), true, false);
        expect(document.querySelector('[data-overlay-action="dismiss"]').style.display).not.toBe('none');
        overlay.close();

        document.body.innerHTML = '';
        setupOverlayDom();
        overlay = await viewerJS.overlay.open(document.getElementById('content'), false, false);
        expect(document.querySelector('[data-overlay-action="dismiss"]').style.display).toBe('none');
        overlay.close();
    });

    test('adds the overlay-fullscreen class when fullscreen=true', async () => {
        setupOverlayDom();
        const overlay = await viewerJS.overlay.open(document.getElementById('content'), true, true);
        expect(document.querySelector('.overlay').classList.contains('overlay-fullscreen')).toBe(true);
        overlay.close();
        // close() also strips the fullscreen marker.
        expect(document.querySelector('.overlay').classList.contains('overlay-fullscreen')).toBe(false);
    });
});
