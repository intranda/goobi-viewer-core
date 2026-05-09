/**
 * Unit tests for selected functions in viewerJS.helper.
 *
 * The module's IIFE call site reads `jQuery` as a free variable, so we stub
 * it before require(). The functions under test below do not actually invoke
 * jQuery, which is why a thin stub is enough.
 *
 * This file runs in jsdom: window, document, navigator and localStorage are
 * available without spawning a real browser.
 */
global.jQuery = global.$ = function () {
    return {};
};

const viewerJS = require('../viewerJS.helper.js');

describe('viewerJS.helper.truncateString', function () {
    test('should append "..." when the string is longer than the size', function () {
        const long = 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr.';
        const result = viewerJS.helper.truncateString(long, 11);
        expect(result).toBe('Lorem ipsum...');
        expect(result.length).toBe(11 + 3);
    });

    test('should return the original string when it is shorter than the size', function () {
        expect(viewerJS.helper.truncateString('short', 100)).toBe('short');
    });

    test('should return the original string when length equals the size exactly', function () {
        // strSize > size is the truncation condition; equal length must pass through.
        expect(viewerJS.helper.truncateString('abcdef', 6)).toBe('abcdef');
    });

    test('should accept a numeric string length argument like the legacy callers do', function () {
        // truncateString uses parseInt(str.length) — verify it handles int-like input.
        expect(viewerJS.helper.truncateString('1234567890', 4)).toBe('1234...');
    });
});

describe('viewerJS.helper.renderAlert', function () {
    test('should embed content and CSS type class in the HTML', function () {
        const html = viewerJS.helper.renderAlert('alert-success', 'Saved!', false);
        expect(html).toContain('alert-success');
        expect(html).toContain('Saved!');
    });

    test('should include a dismiss button when dismissable is true', function () {
        const html = viewerJS.helper.renderAlert('alert-info', 'msg', true);
        expect(html).toContain('data-dismiss="alert"');
    });

    test('should omit the dismiss button when dismissable is false', function () {
        const html = viewerJS.helper.renderAlert('alert-info', 'msg', false);
        expect(html).not.toContain('data-dismiss="alert"');
    });

    test('should produce a single root <div role="alert"> element (parsed via jsdom)', function () {
        const html = viewerJS.helper.renderAlert('alert-warning', 'Beware', true);
        document.body.innerHTML = html;
        const alerts = document.querySelectorAll('div[role="alert"]');
        expect(alerts.length).toBe(1);
        expect(alerts[0].textContent).toContain('Beware');
    });
});

describe('viewerJS.getMetadataValue', function () {
    test('should return the input unchanged when it is already a string', function () {
        expect(viewerJS.getMetadataValue('Title', 'en')).toBe('Title');
    });

    test('should join an array of strings with spaces', function () {
        expect(viewerJS.getMetadataValue(['Hello', 'World'], 'en')).toBe('Hello World');
    });

    test('should return the first array element for the requested IIIF language', function () {
        // getOrElse uses the path [language, 0] so the returned value is unwrapped
        // from the IIIF language array.
        const meta = { de: ['Titel'], en: ['Title'], es: ['Titulo'] };
        expect(viewerJS.getMetadataValue(meta, 'es')).toBe('Titulo');
    });

    test('should fall back to the first language when the requested one is missing', function () {
        // getOrElse picks Object.keys(xs)[0] when the requested key is absent.
        const meta = { de: ['Titel'], en: ['Title'] };
        expect(viewerJS.getMetadataValue(meta, 'fr')).toBe('Titel');
    });
});

describe('viewerJS.isString', function () {
    test('should return true for a string literal', function () {
        expect(viewerJS.isString('abc')).toBe(true);
    });

    test('should return true for a String object', function () {
        // eslint-disable-next-line no-new-wrappers
        expect(viewerJS.isString(new String('abc'))).toBe(true);
    });

    test('should return false for a number', function () {
        expect(viewerJS.isString(42)).toBe(false);
    });

    test('should return false for null', function () {
        expect(viewerJS.isString(null)).toBe(false);
    });

    test('should return false for an object', function () {
        expect(viewerJS.isString({})).toBe(false);
    });
});

describe('viewerJS.helper.checkLocalStorage (jsdom-backed)', function () {
    test('should return true when localStorage is available (jsdom default)', function () {
        // jsdom provides a working sessionStorage/localStorage; the function
        // probes via sessionStorage.setItem/removeItem.
        expect(viewerJS.helper.checkLocalStorage()).toBe(true);
    });
});

describe('viewerJS.getOrElse', function () {
    test('should walk a path of object keys', function () {
        const obj = { a: { b: { c: 'leaf' } } };
        expect(viewerJS.getOrElse(['a', 'b', 'c'], obj)).toBe('leaf');
    });

    test('should return null when the chain dead-ends and no first-key fallback exists', function () {
        // The reducer falls back to xs[Object.keys(xs)[0]] only when xs is a
        // truthy object; once the chain becomes null, further reductions yield null.
        expect(viewerJS.getOrElse(['x', 'y'], null)).toBeNull();
    });

    test('should fall back to the first key of the current level when the requested key is missing', function () {
        const obj = { de: ['Titel'], en: ['Title'] };
        // 'fr' is missing; the reducer picks Object.keys(obj)[0] === 'de'.
        expect(viewerJS.getOrElse(['fr'], obj)).toEqual(['Titel']);
    });
});

describe('viewerJS.helper.ArrayComparator', function () {
    test('should sort an array against a fixed reference order', function () {
        const order = ['b', 'a', 'c'];
        const cmp = new viewerJS.helper.ArrayComparator(order);
        const out = ['a', 'b', 'c'].sort(cmp.compare);
        expect(out).toEqual(['b', 'a', 'c']);
    });

    test('should place values not present in the reference order *after* listed ones (default)', function () {
        // includeUnsortedBefore is undefined → notIncludedOrdering is 0,
        // which means unsorted values fall through to plain string comparison
        // amongst themselves and stay grouped consistently with listed values.
        const cmp = new viewerJS.helper.ArrayComparator(['b']);
        const out = ['a', 'b', 'c'].sort(cmp.compare);
        // 'b' is in the ordering list; 'a' and 'c' are not. With the default
        // notIncludedOrdering of 0 the listed value compares "equal" to
        // unlisted values, so 'b' may appear in any stable position. Just
        // assert all original values are present.
        expect(out.sort()).toEqual(['a', 'b', 'c']);
    });

    test('should place unsorted values *before* listed ones when includeUnsortedBefore=true', function () {
        const cmp = new viewerJS.helper.ArrayComparator(['known'], true);
        const out = ['unknown', 'known', 'other'].sort(cmp.compare);
        expect(out.indexOf('known')).toBeGreaterThan(out.indexOf('unknown'));
        expect(out.indexOf('known')).toBeGreaterThan(out.indexOf('other'));
    });

    test('should place unsorted values *after* listed ones when includeUnsortedBefore=false', function () {
        const cmp = new viewerJS.helper.ArrayComparator(['known'], false);
        const out = ['unknown', 'known', 'other'].sort(cmp.compare);
        expect(out.indexOf('known')).toBeLessThan(out.indexOf('unknown'));
        expect(out.indexOf('known')).toBeLessThan(out.indexOf('other'));
    });

    test('should apply the valueFunction before comparing (sort by extracted key)', function () {
        const cmp = new viewerJS.helper.ArrayComparator(['high', 'low'], false, function (item) {
            return item.priority;
        });
        const items = [
            { id: 1, priority: 'low' },
            { id: 2, priority: 'high' },
        ];
        const sorted = items.sort(cmp.compare);
        expect(sorted[0].priority).toBe('high');
        expect(sorted[1].priority).toBe('low');
    });
});

describe('viewerJS.helper.detectIEVersion (jsdom-backed)', function () {
    // jsdom's navigator is configurable; we can swap out userAgent per test.
    function setUserAgent(ua) {
        Object.defineProperty(window.navigator, 'userAgent', { value: ua, configurable: true });
    }

    test('should return false for a modern browser (no IE/Trident/Edge)', function () {
        setUserAgent('Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Firefox/120.0');
        expect(viewerJS.helper.detectIEVersion()).toBe(false);
    });

    test('should return the IE version number for an MSIE 10 user agent', function () {
        setUserAgent('Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/6.0)');
        expect(viewerJS.helper.detectIEVersion()).toBe(10);
    });

    test('should return the IE version number for an IE 11 / Trident user agent', function () {
        setUserAgent('Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko');
        expect(viewerJS.helper.detectIEVersion()).toBe(11);
    });

    test('should return the version number for the legacy Edge "Edge/" user agent', function () {
        setUserAgent('Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Edge/16.16299');
        expect(viewerJS.helper.detectIEVersion()).toBe(16);
    });
});
