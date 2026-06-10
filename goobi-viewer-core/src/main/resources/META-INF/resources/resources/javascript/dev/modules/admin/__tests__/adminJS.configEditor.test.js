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
    this._open = false;
    // Each of onOpen/onClose/onMessage/onError is observed by the lifecycle code.
    // We capture subscribers so a test can synthesize events via the _fire* helpers.
    const subj = () => ({ _subs: [], subscribe(h) { this._subs.push(h); } });
    this.onOpen = subj();
    this.onClose = subj();
    this.onMessage = subj();
    this.onError = subj();
    this._fireOpen = function () { this._open = true; this.onOpen._subs.forEach((h) => h()); };
    this._fireClose = function (evt) { this._open = false; this.onClose._subs.forEach((h) => h(evt || { code: 1006 })); };
    this._fireMessage = function (data) { this.onMessage._subs.forEach((h) => h({ data })); };
    // The real socket throws when sending on a closed connection; mirror that so the
    // best-effort try/catch in the lifecycle code is exercised.
    this.sendMessage = function (msg) { if (!this._open) { throw new Error('not open'); } sentMessages.push(msg); };
    this.isOpen = function () { return this._open; };
    this.close = function () { this._fireClose({ code: 1000 }); };
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
    // Fake timers so the new heartbeat setInterval does not leak as an open handle.
    beforeEach(() => {
        sentMessages.length = 0;
        // window.currentPath is a global the source reads as the
        // websocket's context path. jsdom's window has no such property
        // by default — set it before each test so the call is reproducible.
        window.currentPath = '/viewer';
        jest.useFakeTimers();
    });
    afterEach(() => {
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    test('opens a WebSocket on PATH_CONFIG_EDITOR_SOCKET with the current host and path', () => {
        configEditor.config = { currentFilePath: '/etc/cfg.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        expect(configEditor.socket).toBeDefined();
        expect(configEditor.socket.host).toBe(window.location.host);
        expect(configEditor.socket.contextPath).toBe('/viewer');
        expect(configEditor.socket.socketPath).toBe('/admin/config/edit.socket');
    });

    test('sends a JSON {fileToLock} message when the socket opens', () => {
        configEditor.config = { currentFilePath: '/etc/cfg.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();

        configEditor.socket._fireOpen();

        expect(sentMessages).toHaveLength(1);
        expect(JSON.parse(sentMessages[0])).toEqual({ fileToLock: '/etc/cfg.xml' });
    });

    test('reads currentFilePath at the moment the socket opens, not when initWebsocket runs', () => {
        // The arrow callback captures `this` (the configEditor) so a
        // later config change is reflected in the message. Pin that.
        configEditor.config = { currentFilePath: '/old.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        configEditor.config.currentFilePath = '/new.xml';

        configEditor.socket._fireOpen();

        expect(JSON.parse(sentMessages[0]).fileToLock).toBe('/new.xml');
    });
});

describe('adminJS.configEditor lifecycle', () => {
    // The module under test is a singleton, so socket/lifecycle state survives between
    // tests. Reset it explicitly so each case starts with no live socket.
    beforeEach(() => {
        sentMessages.length = 0;
        window.currentPath = '/viewer';
        configEditor.socket = undefined;
        configEditor._wantConnection = false;
        configEditor._reconnectScheduled = false;
        configEditor._heartbeatTimer = null;
        configEditor._stableTimer = null;
        jest.useFakeTimers();
    });
    afterEach(() => { jest.clearAllTimers(); jest.useRealTimers(); });

    test('does not open a socket for a read-only file', () => {
        configEditor.config = { currentFilePath: '/etc/cfg.xml', currentFileIsReadable: true, currentFileIsWritable: false };
        configEditor.initWebsocket();
        expect(configEditor.socket).toBeUndefined();
    });

    test('does not open a socket when no file is selected', () => {
        configEditor.config = { currentFilePath: '', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        expect(configEditor.socket).toBeUndefined();
    });

    test('sends a heartbeat on the interval while open', () => {
        configEditor.config = { currentFilePath: '/c.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        configEditor.socket._fireOpen();
        sentMessages.length = 0;
        jest.advanceTimersByTime(20000);
        expect(JSON.parse(sentMessages[0])).toEqual({ heartbeat: true });
    });

    test('reconnects once after an unexpected close and re-sends fileToLock', () => {
        configEditor.config = { currentFilePath: '/c.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        const first = configEditor.socket;
        first._fireOpen();
        first._fireClose({ code: 1006 });
        jest.advanceTimersByTime(1000);
        expect(configEditor.socket).not.toBe(first);
        sentMessages.length = 0;
        configEditor.socket._fireOpen();
        expect(JSON.parse(sentMessages[0])).toEqual({ fileToLock: '/c.xml' });
    });

    test('does not reconnect on a policy close (1008)', () => {
        configEditor.config = { currentFilePath: '/c.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        const first = configEditor.socket;
        first._fireOpen();
        first._fireClose({ code: 1008 });
        jest.advanceTimersByTime(60000);
        expect(configEditor.socket).toBe(first);
    });

    test('does not reconnect after teardown', () => {
        configEditor.config = { currentFilePath: '/c.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        const first = configEditor.socket;
        first._fireOpen();
        configEditor.teardownWebsocket();
        jest.advanceTimersByTime(60000);
        // teardownWebsocket() drops the socket (TTL frees the server lease); the key assertion
        // is that no reconnect ran, i.e. no new socket instance replaced the torn-down one.
        expect(configEditor.socket).not.toBe(first);
        expect(configEditor.socket).toBeUndefined();
    });

    test('switches editor to read-only and shows banner on lockStatus lost', () => {
        document.body.innerHTML = '<div id="configEditorLockLost" style="display:none"></div>';
        const setOption = jest.fn();
        configEditor.cmEditor = { setOption };
        configEditor.config = { currentFilePath: '/c.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        configEditor.socket._fireOpen();
        configEditor.socket._fireMessage(JSON.stringify({ lockStatus: 'lost' }));
        expect(setOption).toHaveBeenCalledWith('readOnly', true);
        expect(document.getElementById('configEditorLockLost').style.display).toBe('block');
    });

    test('sends a release frame when the page is left', () => {
        configEditor.config = { currentFilePath: '/c.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        configEditor.socket._fireOpen();
        sentMessages.length = 0;
        configEditor._releaseAndTeardown();
        expect(sentMessages.map((m) => JSON.parse(m))).toContainEqual({ release: true });
    });

    test('does not send a release frame on a plain reconnect teardown', () => {
        configEditor.config = { currentFilePath: '/c.xml', currentFileIsReadable: true, currentFileIsWritable: true };
        configEditor.initWebsocket();
        configEditor.socket._fireOpen();
        sentMessages.length = 0;
        configEditor.teardownWebsocket(); // reconnect/idle path must NOT release
        expect(sentMessages).toHaveLength(0);
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
