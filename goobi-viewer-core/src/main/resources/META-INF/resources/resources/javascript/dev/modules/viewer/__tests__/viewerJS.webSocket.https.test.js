/**
 * @jest-environment jsdom
 * @jest-environment-options {"url": "https://localhost/"}
 *
 * Companion file to viewerJS.webSocket.test.js — exercises the wss://
 * branch of the URL builder. It lives in a separate file because
 * jsdom's window.location is non-configurable, so switching protocols
 * mid-test is not possible. The pragma above asks the test environment
 * to boot with an https URL so window.location.protocol === 'https:'.
 */

global.rxjs = {
    Subject: function () {
        return { next: function () {}, subscribe: function () {} };
    },
};

const constructed = [];
class FakeWebSocket {
    constructor(url) {
        this.url = url;
        this.readyState = 0;
        this.send = jest.fn();
        this.close = jest.fn();
        constructed.push(this);
    }
}
FakeWebSocket.OPEN = 1;
global.WebSocket = FakeWebSocket;

const viewerJS = require('../viewerJS.webSocket.js');

describe('viewerJS.WebSocket on https', () => {
    test('builds a wss:// URL when window.location.protocol is https', () => {
        expect(window.location.protocol).toBe('https:');
        new viewerJS.WebSocket('host.example', '/viewer', '/session.socket');
        expect(constructed[0].url).toBe('wss://host.example/viewer/session.socket');
    });
});
