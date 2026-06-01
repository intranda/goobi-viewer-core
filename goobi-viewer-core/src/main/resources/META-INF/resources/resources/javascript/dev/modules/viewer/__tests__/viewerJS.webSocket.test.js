/**
 * Unit tests for viewerJS.WebSocket.
 *
 * The source IIFE asserts `if (!rxjs) throw 'Missing dependencies for
 * WebSocket'` at load time, so we must seed `global.rxjs` BEFORE the
 * require. We also stub `global.WebSocket` with a fake constructor that
 * records the URL it was called with and exposes the `OPEN` constant
 * the prototype's `isOpen()` reads.
 */

// rxjs.Subject — minimal: each handler asserts via `next()`.
global.rxjs = {
    Subject: function () {
        const handlers = [];
        return {
            next: function (value) {
                handlers.forEach((h) => h(value));
            },
            subscribe: function (handler) {
                handlers.push(handler);
            },
            _handlers: handlers,
        };
    },
};

// FakeWebSocket records what was constructed and offers send/close spies.
const constructed = [];
class FakeWebSocket {
    constructor(url) {
        this.url = url;
        this.readyState = FakeWebSocket.CONNECTING;
        this.send = jest.fn();
        this.close = jest.fn();
        // The four browser-WebSocket event handlers are assigned by the
        // viewerJS constructor; we capture them here to fire from tests.
        this.onopen = null;
        this.onmessage = null;
        this.onerror = null;
        this.onclose = null;
        constructed.push(this);
    }
}
FakeWebSocket.CONNECTING = 0;
FakeWebSocket.OPEN = 1;
FakeWebSocket.CLOSING = 2;
FakeWebSocket.CLOSED = 3;
global.WebSocket = FakeWebSocket;

const viewerJS = require('../viewerJS.webSocket.js');

beforeEach(() => {
    constructed.length = 0;
});

describe('module shape', () => {
    test('exposes the WebSocket constructor on the viewerJS namespace', () => {
        expect(typeof viewerJS.WebSocket).toBe('function');
    });

    test('exposes the five socket-path constants', () => {
        expect(viewerJS.WebSocket.PATH_SESSION_SOCKET).toBe('/session.socket');
        expect(viewerJS.WebSocket.PATH_CAMPAIGN_SOCKET).toBe('/crowdsourcing/campaign.socket');
        expect(viewerJS.WebSocket.PATH_CONFIG_EDITOR_SOCKET).toBe('/admin/config/edit.socket');
        expect(viewerJS.WebSocket.PATH_DOWNLOAD_TASK).toBe('/tasks/download/monitor.socket');
        expect(viewerJS.WebSocket.PATH_SEARCH_AUTOCOMPLETE).toBe('/search/autocomplete.socket');
    });
});

describe('constructor URL building', () => {
    test('uses ws:// when window.location.protocol is http', () => {
        // jsdom's default location is http://localhost. setupFiles in
        // jest-setup-browser.js does not override it.
        new viewerJS.WebSocket('host.example', '/viewer', '/session.socket');
        expect(constructed).toHaveLength(1);
        expect(constructed[0].url).toBe('ws://host.example/viewer/session.socket');
    });

    // The wss:// branch is exercised in viewerJS.webSocket.https.test.js,
    // which lives in its own file because jsdom's window.location.protocol
    // is non-configurable per-test — switching protocols requires the
    // per-file `@jest-environment-options` pragma.

    test('concatenates host + contextPath + socketPath verbatim (no extra slashes)', () => {
        new viewerJS.WebSocket('h', '', '/session.socket');
        expect(constructed[0].url).toBe('ws://h/session.socket');
    });
});

describe('event wiring', () => {
    test('socket.onopen fires the onOpen subject', () => {
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        const seen = [];
        ws.onOpen.subscribe((event) => seen.push(event));
        constructed[0].onopen({ type: 'open', tag: 'A' });
        expect(seen).toEqual([{ type: 'open', tag: 'A' }]);
    });

    test('socket.onmessage fires the onMessage subject', () => {
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        const seen = [];
        ws.onMessage.subscribe((event) => seen.push(event));
        constructed[0].onmessage({ data: 'hello' });
        expect(seen).toEqual([{ data: 'hello' }]);
    });

    test('socket.onerror fires the onError subject', () => {
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        const seen = [];
        ws.onError.subscribe((event) => seen.push(event));
        constructed[0].onerror({ kind: 'err' });
        expect(seen).toEqual([{ kind: 'err' }]);
    });

    test('socket.onclose fires the onClose subject', () => {
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        const seen = [];
        ws.onClose.subscribe((event) => seen.push(event));
        constructed[0].onclose({ code: 1000 });
        expect(seen).toEqual([{ code: 1000 }]);
    });
});

describe('prototype methods', () => {
    test('sendMessage delegates to socket.send', () => {
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        ws.sendMessage('payload');
        expect(constructed[0].send).toHaveBeenCalledWith('payload');
    });

    test('close delegates to socket.close with statusCode and reason swapped', () => {
        // The source signature is close(reason, statusCode) but it calls
        // socket.close(statusCode, reason) — pin that behavior.
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        ws.close('bye', 1001);
        expect(constructed[0].close).toHaveBeenCalledWith(1001, 'bye');
    });

    test('isOpen returns true when socket.readyState equals WebSocket.OPEN', () => {
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        constructed[0].readyState = FakeWebSocket.OPEN;
        expect(ws.isOpen()).toBe(true);
    });

    test('isOpen returns false in any other readyState', () => {
        const ws = new viewerJS.WebSocket('h', '', '/s.socket');
        constructed[0].readyState = FakeWebSocket.CONNECTING;
        expect(ws.isOpen()).toBe(false);
        constructed[0].readyState = FakeWebSocket.CLOSING;
        expect(ws.isOpen()).toBe(false);
        constructed[0].readyState = FakeWebSocket.CLOSED;
        expect(ws.isOpen()).toBe(false);
    });
});
