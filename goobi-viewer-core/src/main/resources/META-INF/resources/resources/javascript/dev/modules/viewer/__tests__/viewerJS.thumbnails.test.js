/**
 * Unit tests for viewerJS.loadThumbnails.
 *
 * The constructor subscribes to viewer.jsfAjax.success and immediately
 * runs loadAll. We seed a Subject + jsfAjax stub before requiring the
 * source so the IIFE finds them.
 *
 * loadImage uses jQuery's $.ajax and exposes .done/.fail; we replace
 * $.ajax with a stub that returns a deferred-like object whose
 * resolution and rejection branches we drive from each test.
 */

// Minimal rxjs Subject — we only need next() / subscribe().
function makeSubject() {
    const subs = [];
    return {
        next: (v) => subs.forEach((h) => h(v)),
        subscribe: (h) => subs.push(h),
        _subs: subs,
    };
}
global.rxjs = {
    Subject: function () {
        return makeSubject();
    },
};

const viewerJS = require('../viewerJS.thumbnails.js');
viewerJS.jsfAjax = { success: makeSubject() };

// Each test creates a fresh loadThumbnails; the previous instance's
// subscription would otherwise still fire on later jsfAjax.success
// emissions. Replacing the Subject between tests cuts that link.
beforeEach(() => {
    viewerJS.jsfAjax.success = makeSubject();
});

/**
 * Stub-replacement for $.ajax. Returns an object that records
 * .done/.fail handlers and exposes resolve/reject helpers so each
 * test can drive the response branch synchronously.
 */
function stubAjax() {
    const handlers = { done: [], fail: [] };
    const chain = {
        done(fn) {
            handlers.done.push(fn);
            return chain;
        },
        fail(fn) {
            handlers.fail.push(fn);
            return chain;
        },
        _resolve(blob) {
            handlers.done.forEach((h) => h(blob));
        },
        _reject(err) {
            handlers.fail.forEach((h) => h(err));
        },
    };
    return chain;
}

let lastAjaxOpts;
let lastChain;
beforeEach(() => {
    document.body.innerHTML = '';
    lastAjaxOpts = null;
    lastChain = null;
    jest.spyOn($, 'ajax').mockImplementation((opts) => {
        lastAjaxOpts = opts;
        lastChain = stubAjax();
        return lastChain;
    });
    // jsdom URL.createObjectURL needs a stub.
    if (!URL.createObjectURL) URL.createObjectURL = jest.fn(() => 'blob:fake');
    else jest.spyOn(URL, 'createObjectURL').mockImplementation(() => 'blob:fake');
});

afterEach(() => {
    if ($.ajax.mockRestore) $.ajax.mockRestore();
    if (URL.createObjectURL.mockRestore) URL.createObjectURL.mockRestore();
});

describe('viewer.loadThumbnails constructor', () => {
    test('stores notFound / accessDenied images and exposes the thumbnailImageLoaded subject', () => {
        const t = new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        expect(t.notFound).toBe('/notfound.png');
        expect(t.accessDenied).toBe('/denied.png');
        expect(typeof t.thumbnailImageLoaded.next).toBe('function');
    });

    test('runs loadAll once on construction and again whenever jsfAjax.success fires', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" data-src="/img.jpg" />';
        const t = new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        expect($.ajax).toHaveBeenCalledTimes(1);
        viewerJS.jsfAjax.success.next();
        expect($.ajax).toHaveBeenCalledTimes(2);
    });
});

describe('load() branches', () => {
    test('a thumbnail with data-src and no src triggers loadImage on the data-src URL', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" data-src="/img.jpg" />';
        new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        // dataSource path: the source passes the dataset attribute through verbatim.
        expect(lastAjaxOpts.url).toBe('/img.jpg');
    });

    test('a thumbnail with src and a non-zero naturalWidth only attaches an error handler', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" src="/img.jpg" />';
        const img = document.querySelector('img');
        // Simulate "loaded successfully": complete && naturalWidth > 0.
        Object.defineProperty(img, 'complete', { value: true, configurable: true });
        Object.defineProperty(img, 'naturalWidth', { value: 200, configurable: true });

        new viewerJS.loadThumbnails('/notfound.png', '/denied.png');

        expect($.ajax).not.toHaveBeenCalled();
    });

    test('a thumbnail that completed loading with naturalWidth=0 triggers a reload', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" src="/img.jpg" />';
        const img = document.querySelector('img');
        Object.defineProperty(img, 'complete', { value: true, configurable: true });
        Object.defineProperty(img, 'naturalWidth', { value: 0, configurable: true });

        new viewerJS.loadThumbnails('/notfound.png', '/denied.png');

        expect($.ajax).toHaveBeenCalledTimes(1);
        // src path: the source reads element.src (absolute URL property),
        // so the request URL ends up resolved to the document origin.
        expect(lastAjaxOpts.url).toMatch(/\/img\.jpg$/);
    });
});

describe('loadImage status branches', () => {
    test('200 success path sets src to a blob URL and emits onLoaded', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" data-src="/img.jpg" />';
        const t = new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        const seen = [];
        t.thumbnailImageLoaded.subscribe((el) => seen.push(el));

        lastChain._resolve(new Blob(['x']));

        const img = document.querySelector('img');
        expect(img.src).toBe('blob:fake');
        expect(seen).toHaveLength(1);
    });

    test('403 → src is set to the access-denied URL', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" data-src="/img.jpg" />';
        new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        lastChain._reject({ status: 403 });
        const img = document.querySelector('img');
        expect(img.getAttribute('src')).toBe('/denied.png');
    });

    test('404 → src is set to the not-found URL', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" data-src="/img.jpg" />';
        new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        lastChain._reject({ status: 404 });
        expect(document.querySelector('img').getAttribute('src')).toBe('/notfound.png');
    });

    test('500 → also falls back to the not-found URL', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" data-src="/img.jpg" />';
        new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        lastChain._reject({ status: 500 });
        expect(document.querySelector('img').getAttribute('src')).toBe('/notfound.png');
    });

    test('any other status → falls back to the original requested source', () => {
        document.body.innerHTML = '<img data-viewer-thumbnail="thumbnail" data-src="/img.jpg" />';
        new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        lastChain._reject({ status: 502 });
        expect(document.querySelector('img').getAttribute('src')).toBe('/img.jpg');
    });
});

describe('getAccessDeniedUrl', () => {
    test('uses data-viewer-access-denied-url when set on the element', () => {
        const t = new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        document.body.innerHTML = '<img data-viewer-access-denied-url="/custom-denied.png" />';
        expect(t.getAccessDeniedUrl(document.querySelector('img'))).toBe('/custom-denied.png');
    });

    test('falls back to the constructor-supplied accessDenied url', () => {
        const t = new viewerJS.loadThumbnails('/notfound.png', '/denied.png');
        document.body.innerHTML = '<img />';
        expect(t.getAccessDeniedUrl(document.querySelector('img'))).toBe('/denied.png');
    });
});
