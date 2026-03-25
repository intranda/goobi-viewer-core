// flatpickr stub — datePicker.js checks for its existence
global.flatpickr = function () {
    return { destroy: function () {} };
};

var viewerJS = require('../viewerJS.datePicker.js');
var dp = viewerJS.datePicker;

describe('_toISODate', function () {
    test('formats a normal date', function () {
        expect(dp._toISODate(new Date(2024, 0, 15))).toBe('2024-01-15');
    });

    test('zero-pads single-digit month', function () {
        expect(dp._toISODate(new Date(2024, 2, 5))).toBe('2024-03-05');
    });

    test('handles Dec 31', function () {
        expect(dp._toISODate(new Date(2024, 11, 31))).toBe('2024-12-31');
    });

    test('handles Jan 1', function () {
        expect(dp._toISODate(new Date(2024, 0, 1))).toBe('2024-01-01');
    });

    test('handles historical dates', function () {
        expect(dp._toISODate(new Date(1823, 2, 1))).toBe('1823-03-01');
    });
});

describe('_parseISODate', function () {
    test('parses a normal ISO date', function () {
        var d = dp._parseISODate('2024-03-15');
        expect(d.getFullYear()).toBe(2024);
        expect(d.getMonth()).toBe(2);
        expect(d.getDate()).toBe(15);
    });

    test('parses Jan 1', function () {
        var d = dp._parseISODate('2024-01-01');
        expect(d.getMonth()).toBe(0);
        expect(d.getDate()).toBe(1);
    });

    test('parses historical date', function () {
        var d = dp._parseISODate('1823-06-18');
        expect(d.getFullYear()).toBe(1823);
        expect(d.getMonth()).toBe(5);
        expect(d.getDate()).toBe(18);
    });

    test('is inverse of _toISODate', function () {
        var original = new Date(1893, 2, 1);
        var roundtrip = dp._parseISODate(dp._toISODate(original));
        expect(roundtrip.getFullYear()).toBe(1893);
        expect(roundtrip.getMonth()).toBe(2);
        expect(roundtrip.getDate()).toBe(1);
    });
});

describe('_escapeRegex', function () {
    test('escapes special regex characters', function () {
        expect(dp._escapeRegex('a.b+c')).toBe('a\\.b\\+c');
    });

    test('passes through normal strings', function () {
        expect(dp._escapeRegex('yyyy')).toBe('yyyy');
    });
});

describe('_convertByMap (Java to Flatpickr)', function () {
    test('converts full Java date format', function () {
        expect(dp._convertByMap('dd.MM.yyyy', dp._javaToFp)).toBe('d.m.Y');
    });

    test('converts date+time format', function () {
        expect(dp._convertByMap('dd.MM.yyyy HH:mm', dp._javaToFp)).toBe('d.m.Y H:i');
    });

    test('passes through unknown tokens', function () {
        expect(dp._convertByMap('dd/MM/yyyy', dp._javaToFp)).toBe('d/m/Y');
    });

    test('converts month name format MMMM', function () {
        expect(dp._convertByMap('MMMM yyyy', dp._javaToFp)).toBe('F Y');
    });
});

describe('_convertByMap (Flatpickr to Luxon)', function () {
    test('converts d.m.Y to dd.MM.yyyy', function () {
        expect(dp._convertByMap('d.m.Y', dp._fpToLuxon)).toBe('dd.MM.yyyy');
    });

    test('converts m/d/Y to MM/dd/yyyy', function () {
        expect(dp._convertByMap('m/d/Y', dp._fpToLuxon)).toBe('MM/dd/yyyy');
    });

    test('single-char keys do not collide in single-pass replace', function () {
        expect(dp._convertByMap('H:i', dp._fpToLuxon)).toBe('HH:mm');
    });
});

describe('_resolveLocaleFromConfig', function () {
    test('data-attribute has highest priority', function () {
        var el = { dataset: { datepickerLocale: 'en' } };
        var config = { locale: 'de' };
        var result = dp._resolveLocaleFromConfig(el, config);
        expect(result).toBe(dp.locales.en);
    });

    test('config string is second priority', function () {
        var el = { dataset: {} };
        var config = { locale: 'en' };
        var result = dp._resolveLocaleFromConfig(el, config);
        expect(result).toBe(dp.locales.en);
    });

    test('config object is used directly', function () {
        var customLocale = { dateFormat: 'Y-m-d', firstDay: 0 };
        var el = { dataset: {} };
        var config = { locale: customLocale };
        var result = dp._resolveLocaleFromConfig(el, config);
        expect(result).toBe(customLocale);
    });

    test('falls back to de when nothing provided', function () {
        var el = { dataset: {} };
        var config = {};
        var result = dp._resolveLocaleFromConfig(el, config);
        expect(result).toBe(dp.locales.de);
    });

    test('unknown locale string falls back to en', function () {
        var el = { dataset: {} };
        var config = { locale: 'xx' };
        var result = dp._resolveLocaleFromConfig(el, config);
        expect(result).toBe(dp.locales.en);
    });
});

describe('_wrapCallback', function () {
    test('returns extra when existing is null', function () {
        var extra = function () {};
        expect(dp._wrapCallback(null, extra)).toBe(extra);
    });

    test('returns extra when existing is undefined', function () {
        var extra = function () {};
        expect(dp._wrapCallback(undefined, extra)).toBe(extra);
    });

    test('calls both functions in order', function () {
        var calls = [];
        var existing = function () {
            calls.push('existing');
        };
        var extra = function () {
            calls.push('extra');
        };
        var wrapped = dp._wrapCallback(existing, extra);
        wrapped();
        expect(calls).toEqual(['existing', 'extra']);
    });

    test('passes arguments to both functions', function () {
        var existingArgs, extraArgs;
        var existing = function (a, b) {
            existingArgs = [a, b];
        };
        var extra = function (a, b) {
            extraArgs = [a, b];
        };
        var wrapped = dp._wrapCallback(existing, extra);
        wrapped('foo', 'bar');
        expect(existingArgs).toEqual(['foo', 'bar']);
        expect(extraArgs).toEqual(['foo', 'bar']);
    });

    test('handles array of functions (flatpickr convention)', function () {
        var fn1 = function () {};
        var fn2 = function () {};
        var extra = function () {};
        var result = dp._wrapCallback([fn1, fn2], extra);
        expect(Array.isArray(result)).toBe(true);
        expect(result).toEqual([fn1, fn2, extra]);
    });
});
