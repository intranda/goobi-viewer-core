/**
 * Unit tests for the pure URL-rewriting logic in PageAreas.setBrowserLocation.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js. Most of pageAreas.mjs
 * is openseadragon/rxjs-coupled and would need heavy stubbing to instantiate;
 * setBrowserLocation is the testable bit because it does not touch `this`.
 *
 * This file is also the "smoke test" for the Jest ESM project (media-mjs).
 */
import PageAreas from '../pageAreas.mjs';

const { setBrowserLocation } = PageAreas.prototype;

function withLocation(href, fn) {
    window.history.replaceState({}, '', href);
    fn();
    window.history.replaceState({}, '', '/');
}

describe('PageAreas.setBrowserLocation', function () {
    test('should be importable as a default export from an ES module', function () {
        expect(typeof PageAreas).toBe('function');
        expect(typeof setBrowserLocation).toBe('function');
    });

    test('should replace the existing page-number segment of the path', function () {
        withLocation('http://localhost/viewer/PI-foo/3/', function () {
            setBrowserLocation('PI-foo', '7');
            expect(window.location.pathname).toBe('/viewer/PI-foo/7/');
        });
    });

    test('should walk the path right-to-left and rewrite the first numeric segment that is not the PI', function () {
        withLocation('http://localhost/viewer/PI-foo/12/extra', function () {
            setBrowserLocation('PI-foo', '99');
            // The trailing 'extra' segment is non-numeric; the next-to-last
            // segment '12' is numeric and not the PI → rewritten.
            expect(window.location.pathname).toBe('/viewer/PI-foo/99/extra');
        });
    });

    test('should append the page number when the path has no numeric segment', function () {
        withLocation('http://localhost/viewer/PI-foo', function () {
            setBrowserLocation('PI-foo', '5');
            expect(window.location.pathname).toBe('/viewer/PI-foo/5');
        });
    });

    test('should append after dropping a trailing empty segment when no numeric page exists', function () {
        withLocation('http://localhost/viewer/PI-foo/', function () {
            setBrowserLocation('PI-foo', '5');
            // Trailing '' is popped before pushing the page number.
            expect(window.location.pathname).toBe('/viewer/PI-foo/5');
        });
    });

    test('should NOT rewrite a numeric segment that equals the PI value', function () {
        // PI is itself numeric in this case ('123'); the page segment is '7'.
        withLocation('http://localhost/viewer/123/7/', function () {
            setBrowserLocation('123', '8');
            // Right-to-left scan finds '7' first (not equal to PI '123') → rewritten.
            expect(window.location.pathname).toBe('/viewer/123/8/');
        });
    });
});
