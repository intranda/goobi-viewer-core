/**
 * Unit tests for viewerJS.apiDocs.
 *
 * jQuery is wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.apiDocs.js');
const apiDocs = viewerJS.apiDocs;

/** Mirrors the requestBuilder shape of @scalar/api-reference: path is an object and
 *  query is a URLSearchParams-like Map the bundle iterates with `for...of ...entries()`. */
function builderFor(rawPath) {
    return { baseUrl: 'https://viewer.example.org/viewer', path: { variables: {}, raw: rawPath }, query: new URLSearchParams() };
}

describe('viewerJS.apiDocs.withTrailingSlash', function () {
    test('should append a slash to a path without one', function () {
        expect(apiDocs.withTrailingSlash('/api/v1/index/query')).toBe('/api/v1/index/query/');
    });

    test('should leave a path that already ends with a slash untouched', function () {
        expect(apiDocs.withTrailingSlash('/api/v1/index/query/')).toBe('/api/v1/index/query/');
    });

    test('should return an empty string unchanged', function () {
        expect(apiDocs.withTrailingSlash('')).toBe('');
    });

    test('should return a non-string argument unchanged', function () {
        expect(apiDocs.withTrailingSlash(undefined)).toBeUndefined();
        expect(apiDocs.withTrailingSlash(null)).toBeNull();
    });
});

describe('viewerJS.apiDocs.buildConfig', function () {
    test('should offer both API versions as sources with v1 as default', function () {
        const config = apiDocs.buildConfig('/viewer', 'de');
        expect(config.sources).toHaveLength(2);
        expect(config.sources[0]).toMatchObject({
            title: 'API v1',
            slug: 'v1',
            url: '/viewer/api/v1/openapi.json',
            default: true,
        });
        expect(config.sources[1]).toMatchObject({
            title: 'API v2',
            slug: 'v2',
            url: '/viewer/api/v2/openapi.json',
        });
        expect(config.sources[1].default).toBeUndefined();
    });

    test('should build absolute paths when the context path is empty', function () {
        expect(apiDocs.buildConfig('', 'de').sources[0].url).toBe('/api/v1/openapi.json');
    });

    test('should treat a missing context path like an empty one', function () {
        expect(apiDocs.buildConfig().sources[0].url).toBe('/api/v1/openapi.json');
    });

    test('should use the modern layout', function () {
        expect(apiDocs.buildConfig('/viewer', 'de').layout).toBe('modern');
    });

    test('should pass the viewer locale to scalar', function () {
        expect(apiDocs.buildConfig('/viewer', 'de').localization).toEqual({ locale: 'de' });
    });

    test('should map the iw locale to he because Scalar only knows the he RTL code', function () {
        // The viewer ships both messages_he.properties and messages_iw.properties for
        // Hebrew, but Scalar's RTL locale set is {ar, fa, he, ur}. Without the mapping
        // 'iw' would render Scalar LTR and in English.
        expect(apiDocs.buildConfig('/viewer', 'iw').localization).toEqual({ locale: 'he' });
    });

    test('should not set a proxyUrl since all requests are same-origin', function () {
        expect(apiDocs.buildConfig('/viewer', 'de').proxyUrl).toBeUndefined();
    });

    test('should disable the bundled fonts so no external host is contacted', function () {
        expect(apiDocs.buildConfig('/viewer', 'de').withDefaultFonts).toBe(false);
    });

    test('should hide the client button that would link to client.scalar.com', function () {
        expect(apiDocs.buildConfig('/viewer', 'de').hideClientButton).toBe(true);
    });

    test('should disable the developer tools that are active on localhost by default', function () {
        expect(apiDocs.buildConfig('/viewer', 'de').showDeveloperTools).toBe('never');
    });

    // There used to be a test here asserting that JSON.stringify(buildConfig(...))
    // does not match /scalar\.com/. That assertion could never fail: none of the
    // literal string values in this config (booleans, 'never', URLs under the same
    // origin) were ever going to contain that substring, and JSON.stringify silently
    // drops functions - including onBeforeRequest - so it did not even see the one
    // property whose *behavior*, not literal text, is what actually matters here.
    // The three flags that keep Scalar from calling out to a *.scalar.com host
    // (withDefaultFonts, hideClientButton, showDeveloperTools) are each verified
    // individually above. Whether the rendered page ends up making zero requests to
    // any *.scalar.com host cannot be verified from this config object at all - that
    // requires opening the page and checking the browser Network tab by hand,
    // in particular after every @scalar/api-reference version bump.
});

describe('viewerJS.apiDocs.buildConfig request hook', function () {
    test('should append a trailing slash to the raw request path', function () {
        const requestBuilder = builderFor('/api/v1/index/query');
        apiDocs.buildConfig('/viewer', 'de').onBeforeRequest({ requestBuilder: requestBuilder });
        expect(requestBuilder.path.raw).toBe('/api/v1/index/query/');
    });

    test('should leave a raw path that already ends with a slash untouched', function () {
        const requestBuilder = builderFor('/api/v1/index/query/');
        apiDocs.buildConfig('/viewer', 'de').onBeforeRequest({ requestBuilder: requestBuilder });
        expect(requestBuilder.path.raw).toBe('/api/v1/index/query/');
    });

    test('should append a slash to the path even when query parameters are present', function () {
        // The hook never reads builder.query, so this is behaviorally identical to the
        // first hook test above - it only additionally documents that a populated query
        // does not stop the slash from being appended. Query must be a URLSearchParams
        // (the bundle iterates it with `for (let [t, r] of e.query.entries())`), not a
        // plain object, to mirror the real requestBuilder shape.
        // That the query string itself survives untouched is NOT verified here - Scalar
        // builds the final url from path and query independently, and that composition
        // can only be checked by hand in the browser.
        const requestBuilder = builderFor('/api/v1/records');
        requestBuilder.query = new URLSearchParams({ q: 'test' });
        apiDocs.buildConfig('/viewer', 'de').onBeforeRequest({ requestBuilder: requestBuilder });
        expect(requestBuilder.path.raw).toBe('/api/v1/records/');
    });

    test('should keep path.variables an object so the url builder does not break', function () {
        const requestBuilder = builderFor('/api/v1/index/query');
        apiDocs.buildConfig('/viewer', 'de').onBeforeRequest({ requestBuilder: requestBuilder });
        expect(typeof requestBuilder.path).toBe('object');
        expect(requestBuilder.path.variables).toEqual({});
        expect(requestBuilder.baseUrl).toBe('https://viewer.example.org/viewer');
    });

    test('should tolerate a missing requestBuilder', function () {
        const config = apiDocs.buildConfig('/viewer', 'de');
        expect(function () {
            config.onBeforeRequest({});
        }).not.toThrow();
    });

    test('should tolerate a builder without a path object', function () {
        const config = apiDocs.buildConfig('/viewer', 'de');
        expect(function () {
            config.onBeforeRequest({ requestBuilder: { baseUrl: '/viewer' } });
        }).not.toThrow();
    });
});

describe('viewerJS.apiDocs.init', function () {
    afterEach(function () {
        delete global.Scalar;
    });

    test('should mount the api reference with the built configuration', function () {
        global.Scalar = { createApiReference: jest.fn() };
        apiDocs.init('#scalar-api-reference', '/viewer', 'de');
        expect(global.Scalar.createApiReference).toHaveBeenCalledTimes(1);
        const args = global.Scalar.createApiReference.mock.calls[0];
        expect(args[0]).toBe('#scalar-api-reference');
        expect(args[1].sources[0].url).toBe('/viewer/api/v1/openapi.json');
        expect(args[1].localization).toEqual({ locale: 'de' });
    });

    test('should log an error instead of throwing when the bundle is missing', function () {
        const spy = jest.spyOn(console, 'error').mockImplementation(function () {});
        expect(function () {
            apiDocs.init('#scalar-api-reference', '/viewer', 'de');
        }).not.toThrow();
        expect(spy).toHaveBeenCalled();
        spy.mockRestore();
    });
});
