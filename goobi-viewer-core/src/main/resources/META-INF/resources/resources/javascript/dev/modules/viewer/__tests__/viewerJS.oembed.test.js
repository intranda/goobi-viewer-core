/**
 * Unit tests for viewerJS.oembed.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js. fetch is mocked
 * per test (same pattern as viewerJS.translator.test.js).
 */
const viewerJS = require('../viewerJS.oembed.js');

function mockFetchOnceWithJson(json, ok) {
    if (ok === undefined) ok = true;
    global.fetch = jest.fn().mockResolvedValueOnce({
        ok: ok,
        errorMessage: 'mock error',
        json: function () {
            return Promise.resolve(json);
        },
    });
}

describe('viewerJS.oembed', function () {
    afterEach(function () {
        delete global.fetch;
    });

    test('should send a GET request with JSON content-type to the given URL', async function () {
        mockFetchOnceWithJson({ type: 'rich', html: '<p>hello</p>' });
        await viewerJS.oembed('https://example.org/oembed?url=foo');
        expect(global.fetch).toHaveBeenCalledTimes(1);
        const [url, options] = global.fetch.mock.calls[0];
        expect(url).toBe('https://example.org/oembed?url=foo');
        expect(options.method).toBe('GET');
        expect(options.headers['Content-Type']).toBe('application/json');
    });

    test('should return a jQuery wrapper around the html for type "rich"', async function () {
        mockFetchOnceWithJson({ type: 'rich', html: '<p class="x">embed</p>' });
        const $result = await viewerJS.oembed('https://example.org/');
        // jQuery's `instanceof jQuery` only works on the same instance — use the
        // duck-type instead so global.$ vs returned-$ differences don't trip us.
        expect($result.length).toBeGreaterThan(0);
        expect($result.hasClass('x')).toBe(true);
        expect($result.text()).toBe('embed');
    });

    test('should return an <img> jQuery wrapper for type "photo"', async function () {
        mockFetchOnceWithJson({ type: 'photo', url: 'https://example.org/cat.jpg' });
        const $result = await viewerJS.oembed('https://example.org/');
        expect($result[0].tagName).toBe('IMG');
        expect($result.attr('src')).toBe('https://example.org/cat.jpg');
    });

    test('should resolve to undefined for a payload type the module does not handle', async function () {
        mockFetchOnceWithJson({ type: 'video', url: 'https://example.org/v.mp4' });
        const result = await viewerJS.oembed('https://example.org/');
        expect(result).toBeUndefined();
    });
});
