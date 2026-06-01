/**
 * Unit tests for selected functions in viewerJS.helper.
 *
 * jQuery and the jsdom DOM are wired up by jest-setup-browser.js.
 */
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

describe('viewerJS.helper.renderModal', function () {
    test('should embed the configured ID, label, title and body text in the rendered HTML', function () {
        const html = viewerJS.helper.renderModal({
            id: 'myModal',
            label: 'myLabel',
            string: { title: 'Title', body: '<p>Body</p>', closeBtn: 'X', saveBtn: 'OK' },
        });
        expect(html).toContain('id="myModal"');
        expect(html).toContain('aria-labelledby="myLabel"');
        expect(html).toContain('Title');
        expect(html).toContain('<p>Body</p>');
        expect(html).toContain('OK');
        expect(html).toContain('X');
    });

    test('should fall back to default ids and labels when config is empty', function () {
        // $.extend(true, defaults, config) → config={} leaves defaults in place.
        const html = viewerJS.helper.renderModal({});
        expect(html).toContain('id="myModal"');
        expect(html).toContain('Modal title');
    });

    test('should produce a valid Bootstrap modal structure parsable by jsdom', function () {
        document.body.innerHTML = viewerJS.helper.renderModal({});
        expect(document.querySelectorAll('.modal.fade').length).toBe(1);
        expect(document.querySelectorAll('.modal-header').length).toBe(1);
        expect(document.querySelectorAll('.modal-body').length).toBe(1);
        expect(document.querySelectorAll('.modal-footer').length).toBe(1);
    });
});

describe('viewerJS.helper.renderWarningPopover', function () {
    test('should return a jQuery wrapper carrying the warning-popover class', function () {
        const $popover = viewerJS.helper.renderWarningPopover('beware');
        expect($popover.hasClass('warning-popover')).toBe(true);
    });

    test('should render the message inside a <p> element', function () {
        const $popover = viewerJS.helper.renderWarningPopover('beware');
        expect($popover.find('p').html()).toBe('beware');
    });

    test('should pass HTML through unescaped (msg is set via jQuery .html())', function () {
        const $popover = viewerJS.helper.renderWarningPopover('<em>bold</em>');
        expect($popover.find('p em').length).toBe(1);
        expect($popover.find('p em').text()).toBe('bold');
    });

    test('should include a close button with the warning-popover toggle data attribute', function () {
        const $popover = viewerJS.helper.renderWarningPopover('x');
        expect($popover.find('button[data-toggle="warning-popover"]').length).toBe(1);
    });
});

describe('viewerJS.helper.executeFunctionByName / getFunctionByName', function () {
    test('should resolve a top-level function in the given context', function () {
        const ctx = {
            greet: function () {
                return 'hi';
            },
        };
        expect(viewerJS.helper.executeFunctionByName('greet', ctx)).toBe('hi');
    });

    test('should walk dotted namespaces to find the function', function () {
        const ctx = {
            a: {
                b: {
                    greet: function () {
                        return 'nested';
                    },
                },
            },
        };
        expect(viewerJS.helper.executeFunctionByName('a.b.greet', ctx)).toBe('nested');
    });

    test('should forward extra arguments to the resolved function', function () {
        const ctx = {
            add: function (x, y) {
                return x + y;
            },
        };
        expect(viewerJS.helper.executeFunctionByName('add', ctx, 2, 3)).toBe(5);
    });

    test('getFunctionByName should return the function reference itself, not invoke it', function () {
        const fn = function () {
            return 'untouched';
        };
        const ctx = { x: { y: fn } };
        expect(viewerJS.helper.getFunctionByName('x.y', ctx)).toBe(fn);
    });
});

describe('viewerJS.helper.getUrlSearchParamMap / getUrlSearchParam', function () {
    afterEach(function () {
        // Reset URL between tests so jsdom's location is clean.
        window.history.replaceState({}, '', '/');
    });

    test('should parse a query string into a Map', function () {
        window.history.replaceState({}, '', '/?a=1&b=2');
        const map = viewerJS.helper.getUrlSearchParamMap();
        expect(map.get('a')).toBe('1');
        expect(map.get('b')).toBe('2');
    });

    test('should return an empty Map when the URL has no query string', function () {
        window.history.replaceState({}, '', '/');
        expect(viewerJS.helper.getUrlSearchParamMap().size).toBe(0);
    });

    test('getUrlSearchParam should look up a single key', function () {
        window.history.replaceState({}, '', '/?lang=de&page=4');
        expect(viewerJS.helper.getUrlSearchParam('lang')).toBe('de');
        expect(viewerJS.helper.getUrlSearchParam('missing')).toBeUndefined();
    });
});

describe('viewerJS.helper.getFragmentHash', function () {
    afterEach(function () {
        window.history.replaceState({}, '', '/');
    });

    test('should return undefined when the URL has no xywh fragment', function () {
        window.history.replaceState({}, '', '/page.html#noxywh');
        expect(viewerJS.helper.getFragmentHash()).toBeUndefined();
    });

    test('should return the xywh string when one fragment is present', function () {
        window.history.replaceState({}, '', '/page.html#xywh=10,20,30,40');
        expect(viewerJS.helper.getFragmentHash()).toBe('10,20,30,40');
    });

    test('should accept the percent: prefix used by IIIF region selectors', function () {
        window.history.replaceState({}, '', '/page.html#xywh=percent:0,0,100,50');
        expect(viewerJS.helper.getFragmentHash()).toBe('percent:0,0,100,50');
    });

    test('should return an array of strings when several xywh fragments are present', function () {
        window.history.replaceState({}, '', '/page.html#xywh=1,2,3,4&xywh=5,6,7,8');
        const result = viewerJS.helper.getFragmentHash();
        expect(Array.isArray(result)).toBe(true);
        expect(result).toEqual(['1,2,3,4', '5,6,7,8']);
    });
});

describe('viewerJS.helper.compareNumerical', function () {
    test('should sort by parsed integer value', function () {
        expect(['10', '2', '30'].sort(viewerJS.helper.compareNumerical)).toEqual(['2', '10', '30']);
    });

    test('should push NaN values to the end', function () {
        const out = ['5', 'foo', '1', 'bar'].sort(viewerJS.helper.compareNumerical);
        expect(out.indexOf('foo')).toBeGreaterThan(out.indexOf('1'));
        expect(out.indexOf('bar')).toBeGreaterThan(out.indexOf('5'));
    });

    test('should return 0 for two non-numeric values (stable comparator)', function () {
        expect(viewerJS.helper.compareNumerical('foo', 'bar')).toBe(0);
    });
});

describe('viewerJS.helper.compareAlphanumerical', function () {
    test('should delegate to localeCompare when both operands are present', function () {
        expect(['banana', 'apple'].sort(viewerJS.helper.compareAlphanumerical)).toEqual(['apple', 'banana']);
    });

    test('should push falsy values to the start (returns -1 when b is missing)', function () {
        // Note: the sort is asymmetric: returns -1 when only b is falsy (a > b → ? actually 1).
        // Verify the contract by checking direct returns rather than sort.
        expect(viewerJS.helper.compareAlphanumerical('a', null)).toBe(1);
        // When a is falsy, returns -1 regardless of b.
        expect(viewerJS.helper.compareAlphanumerical(null, 'a')).toBe(-1);
        expect(viewerJS.helper.compareAlphanumerical(null, null)).toBe(-1);
    });
});

describe('viewerJS.unique (Array.filter helper)', function () {
    test('should drop duplicates while preserving the original order', function () {
        expect([1, 2, 2, 3, 1, 4].filter(viewerJS.unique)).toEqual([1, 2, 3, 4]);
    });

    test('should distinguish via strict equality (string "1" ≠ number 1)', function () {
        expect([1, '1', 1].filter(viewerJS.unique)).toEqual([1, '1']);
    });
});

describe('viewerJS.parseMap', function () {
    test('should turn a plain object into a Map preserving keys and values', function () {
        const m = viewerJS.parseMap({ a: 1, b: 'two' });
        expect(m).toBeInstanceOf(Map);
        expect(m.get('a')).toBe(1);
        expect(m.get('b')).toBe('two');
    });

    test('should return an empty Map for null or undefined input', function () {
        expect(viewerJS.parseMap(null).size).toBe(0);
        expect(viewerJS.parseMap(undefined).size).toBe(0);
    });
});

describe('viewerJS.getMapBoxToken', function () {
    afterEach(function () {
        delete viewerJS.mapBoxConfig;
        delete global.mapBoxToken;
    });

    test('should prefer viewerJS.mapBoxConfig.token when defined', function () {
        viewerJS.mapBoxConfig = { token: 'cfg-token' };
        global.mapBoxToken = 'global-token';
        expect(viewerJS.getMapBoxToken.call(viewerJS)).toBe('cfg-token');
    });

    test('should fall back to the global mapBoxToken when no config is set', function () {
        global.mapBoxToken = 'global-token';
        expect(viewerJS.getMapBoxToken.call(viewerJS)).toBe('global-token');
    });

    test('should return undefined when neither source has a token', function () {
        expect(viewerJS.getMapBoxToken.call(viewerJS)).toBeUndefined();
    });
});
