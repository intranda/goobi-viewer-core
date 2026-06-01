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

// initWebsocket reads `viewerJS.WebSocket` and `viewerJS.WebSocket.PATH_CONFIG_EDITOR_SOCKET`.
// We seed both before evaluating the source so the loaded module sees them.
const sentMessages = [];
global.viewerJS = global.viewerJS || {};
global.viewerJS.WebSocket = function (host, contextPath, socketPath) {
    this.host = host;
    this.contextPath = contextPath;
    this.socketPath = socketPath;
    // onOpen is observed by initWebsocket. We capture the subscriber so
    // each test can synthesize the open event by calling _fireOpen().
    this.onOpen = {
        _subs: [],
        subscribe: function (handler) {
            this._subs.push(handler);
        },
    };
    this._fireOpen = function () {
        this.onOpen._subs.forEach((h) => h());
    };
    this.sendMessage = function (msg) {
        sentMessages.push(msg);
    };
};
global.viewerJS.WebSocket.PATH_CONFIG_EDITOR_SOCKET = '/admin/config/edit.socket';

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

describe('adminJS.configEditor.initWebsocket', () => {
    beforeEach(() => {
        sentMessages.length = 0;
        // window.currentPath is a global the source reads as the
        // websocket's context path. jsdom's window has no such property
        // by default — set it before each test so the call is reproducible.
        window.currentPath = '/viewer';
    });

    test('opens a WebSocket on PATH_CONFIG_EDITOR_SOCKET with the current host and path', () => {
        configEditor.config = { currentFilePath: '/etc/cfg.xml' };
        configEditor.initWebsocket();
        expect(configEditor.socket).toBeDefined();
        expect(configEditor.socket.host).toBe(window.location.host);
        expect(configEditor.socket.contextPath).toBe('/viewer');
        expect(configEditor.socket.socketPath).toBe('/admin/config/edit.socket');
    });

    test('sends a JSON {fileToLock} message when the socket opens', () => {
        configEditor.config = { currentFilePath: '/etc/cfg.xml' };
        configEditor.initWebsocket();

        configEditor.socket._fireOpen();

        expect(sentMessages).toHaveLength(1);
        expect(JSON.parse(sentMessages[0])).toEqual({ fileToLock: '/etc/cfg.xml' });
    });

    test('reads currentFilePath at the moment the socket opens, not when initWebsocket runs', () => {
        // The arrow callback captures `this` (the configEditor) so a
        // later config change is reflected in the message. Pin that.
        configEditor.config = { currentFilePath: '/old.xml' };
        configEditor.initWebsocket();
        configEditor.config.currentFilePath = '/new.xml';

        configEditor.socket._fireOpen();

        expect(JSON.parse(sentMessages[0]).fileToLock).toBe('/new.xml');
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
