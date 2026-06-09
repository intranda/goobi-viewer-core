/**
 * Unit tests for viewerJS.disclaimerModal.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js. The module uses
 * SweetAlert (Swal.fire) to render the modal — we stub Swal so the
 * tests focus on the storage/expiry decision logic.
 */
// SweetAlert stub — captures fire() calls and lets us drive its `then`.
let swalCalls;
let pendingResolve;
global.Swal = {
    fire: function (opts) {
        swalCalls.push(opts);
        return new Promise(function (resolve) {
            pendingResolve = resolve;
        });
    },
};

const viewerJS = require('../viewerJS.disclaimerModal.js');

function clearStorages() {
    localStorage.clear();
    sessionStorage.clear();
}

describe('viewerJS.disclaimerModal.init', function () {
    beforeEach(function () {
        clearStorages();
        swalCalls = [];
        pendingResolve = null;
    });

    afterEach(clearStorages);

    test('should be a no-op when config.active is false', function () {
        viewerJS.disclaimerModal.init({ active: false });
        expect(swalCalls).toEqual([]);
    });

    test('should show the disclaimer on first visit (no stored settings)', function () {
        viewerJS.disclaimerModal.init({
            active: true,
            disclaimerText: '<p>Hi</p>',
            lastEdited: '2024-01-01',
            daysToLive: 14,
            sessionId: 'sid-A',
        });
        expect(swalCalls.length).toBe(1);
        expect(swalCalls[0].html).toBe('<p>Hi</p>');
    });

    test('should NOT show the disclaimer when last accepted is recent and hash unchanged', function () {
        viewerJS.disclaimerModal.setStoredSettings({
            lastAccepted: Date.now(),
            sessionId: 'sid-A',
        });
        viewerJS.disclaimerModal.init({
            active: true,
            lastEdited: '2024-01-01',
            daysToLive: 14,
        });
        expect(swalCalls).toEqual([]);
    });

    test('should re-show the disclaimer when lastAccepted is older than the disclaimer text', function () {
        viewerJS.disclaimerModal.setStoredSettings({
            lastAccepted: '2023-01-01',
            sessionId: 'sid-A',
        });
        viewerJS.disclaimerModal.init({
            active: true,
            lastEdited: '2024-06-01',
            daysToLive: 14,
        });
        expect(swalCalls.length).toBe(1);
    });

    test('should re-show the disclaimer when daysToLive has elapsed since last acceptance', function () {
        const longAgo = new Date();
        longAgo.setDate(longAgo.getDate() - 30);
        viewerJS.disclaimerModal.setStoredSettings({
            lastAccepted: longAgo.toISOString(),
            sessionId: 'sid-A',
        });
        viewerJS.disclaimerModal.init({
            active: true,
            // lastEdited far in the past, so the "outdated" branch isn't the trigger.
            lastEdited: '2000-01-01',
            daysToLive: 14,
        });
        expect(swalCalls.length).toBe(1);
    });

    test('with storage="session" should re-show when the stored sessionId differs', function () {
        sessionStorage.setItem('goobi.viewer.disclaimer.settings', JSON.stringify({ lastAccepted: Date.now(), sessionId: 'sid-OLD' }));
        viewerJS.disclaimerModal.init({
            active: true,
            lastEdited: '2000-01-01',
            daysToLive: 14,
            storage: 'session',
            sessionId: 'sid-NEW',
        });
        expect(swalCalls.length).toBe(1);
    });

    test('after the user accepts the modal, settings should be persisted', async function () {
        viewerJS.disclaimerModal.init({
            active: true,
            disclaimerText: '<p>Hi</p>',
            lastEdited: '2024-01-01',
            daysToLive: 14,
            sessionId: 'sid-A',
        });
        // User clicks confirm → Swal's promise resolves. Flushing two
        // microtasks is enough for the chained .then() handler to run.
        pendingResolve();
        await Promise.resolve();
        await Promise.resolve();
        const stored = JSON.parse(localStorage.getItem('goobi.viewer.disclaimer.settings'));
        expect(stored.sessionId).toBe('sid-A');
        expect(typeof stored.lastAccepted).toBe('number');
    });
});

describe('viewerJS.disclaimerModal storage helpers', function () {
    beforeEach(clearStorages);
    afterEach(clearStorages);

    test('getStoredSettings should return {} when nothing is stored', function () {
        expect(viewerJS.disclaimerModal.getStoredSettings()).toEqual({});
        expect(viewerJS.disclaimerModal.getStoredSettings('session')).toEqual({});
    });

    test('getStoredSettings should return {} for malformed JSON, not throw', function () {
        const errorSpy = jest.spyOn(console, 'error').mockImplementation(function () {});
        try {
            localStorage.setItem('goobi.viewer.disclaimer.settings', 'not-json');
            expect(viewerJS.disclaimerModal.getStoredSettings()).toEqual({});
        } finally {
            errorSpy.mockRestore();
        }
    });

    test('setStoredSettings/getStoredSettings should round-trip via localStorage by default', function () {
        viewerJS.disclaimerModal.setStoredSettings({ lastAccepted: 123, sessionId: 'sid' });
        expect(viewerJS.disclaimerModal.getStoredSettings()).toEqual({ lastAccepted: 123, sessionId: 'sid' });
    });

    test('setStoredSettings/getStoredSettings should round-trip via sessionStorage when location="session"', function () {
        viewerJS.disclaimerModal.setStoredSettings({ x: 1 }, 'session');
        // Default getter (localStorage) doesn't see it.
        expect(viewerJS.disclaimerModal.getStoredSettings()).toEqual({});
        expect(viewerJS.disclaimerModal.getStoredSettings('session')).toEqual({ x: 1 });
    });
});
