/**
 * Unit tests for viewerJS.translator — fetch-based REST translation cache.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js.
 *
 * The translator depends on viewerJS.isString and viewerJS.getMetadataValue
 * (both from viewerJS.helper.js). Stubbing them keeps this test focused on
 * translator behaviour and avoids loading the helper module.
 *
 * Establishes the fetch-mock pattern reusable by other REST-coupled modules:
 *   global.fetch = jest.fn(() => Promise.resolve({ json: () => ({...}) }));
 */
const viewerJS = require('../viewerJS.translator.js');

// Minimal helper stubs — translator uses these from viewerJS.helper.js at
// runtime; loading helper.js here would just enlarge the test footprint.
viewerJS.isString = function (v) {
    return typeof v === 'string' || v instanceof String;
};
viewerJS.getMetadataValue = function (object, language) {
    if (typeof object === 'string') return object;
    if (Array.isArray(object) && object.length > 0 && typeof object[0] === 'string') {
        return object.join(' ');
    }
    if (object && object[language]) {
        const v = object[language];
        return Array.isArray(v) ? v[0] : v;
    }
    return undefined;
};

function mockFetchOnceWithJson(json) {
    global.fetch = jest.fn().mockResolvedValueOnce({
        json: function () {
            return Promise.resolve(json);
        },
    });
}

function mockFetchOnceWithError(err) {
    global.fetch = jest.fn().mockRejectedValueOnce(err);
}

describe('viewerJS.translator', function () {
    let t;

    beforeEach(function () {
        t = new viewerJS.translator('https://example.org/api/v1/', 'de');
    });

    afterEach(function () {
        delete global.fetch;
    });

    describe('constructor', function () {
        test('should record the rest base URL and the default language', function () {
            expect(t.restApiUrl).toBe('https://example.org/api/v1/');
            expect(t.language).toBe('de');
            expect(t.keys).toEqual([]);
        });
    });

    describe('addTranslations', function () {
        test('should reject when called with undefined', async function () {
            await expect(t.addTranslations(undefined)).rejects.toBe('No keys given to translate');
        });

        test('should accept a single string key (auto-wraps to array)', async function () {
            mockFetchOnceWithJson({ greeting: { de: 'Hallo', en: 'Hello' } });
            await t.addTranslations('greeting');
            expect(global.fetch).toHaveBeenCalledTimes(1);
            expect(global.fetch.mock.calls[0][0]).toBe('https://example.org/api/v1/localization/translations?keys=greeting');
            expect(t.translations).toEqual({ greeting: { de: 'Hallo', en: 'Hello' } });
            expect(t.keys).toEqual(['greeting']);
        });

        test('should join multiple keys with commas in the request URL', async function () {
            mockFetchOnceWithJson({ a: { de: 'A' }, b: { de: 'B' } });
            await t.addTranslations(['a', 'b']);
            expect(global.fetch.mock.calls[0][0]).toBe('https://example.org/api/v1/localization/translations?keys=a,b');
        });

        test('should drop empty / undefined keys before issuing the request', async function () {
            mockFetchOnceWithJson({ ok: { de: 'OK' } });
            await t.addTranslations(['ok', '', undefined]);
            expect(global.fetch.mock.calls[0][0]).toBe('https://example.org/api/v1/localization/translations?keys=ok');
        });

        test('should not re-fetch keys that are already cached', async function () {
            // One mock that resolves to two different bodies, so call counts
            // accumulate across both addTranslations() invocations.
            global.fetch = jest
                .fn()
                .mockResolvedValueOnce({ json: () => Promise.resolve({ a: { de: 'A' } }) })
                .mockResolvedValueOnce({ json: () => Promise.resolve({ b: { de: 'B' } }) });

            await t.addTranslations(['a']);
            // Second call asks again for `a` plus a new key `b`.
            await t.addTranslations(['a', 'b']);

            expect(global.fetch).toHaveBeenCalledTimes(2);
            // Only the new key is requested in the second URL.
            expect(global.fetch.mock.calls[1][0]).toBe('https://example.org/api/v1/localization/translations?keys=b');
            expect(t.keys.sort()).toEqual(['a', 'b']);
        });

        test('should not fetch at all when every key is already cached', async function () {
            mockFetchOnceWithJson({ a: { de: 'A' } });
            await t.addTranslations(['a']);
            // Fetch should not be invoked again.
            global.fetch = jest.fn();
            await t.addTranslations(['a']);
            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should swallow fetch errors and reset translations to {}', async function () {
            const errorSpy = jest.spyOn(console, 'error').mockImplementation(function () {});
            mockFetchOnceWithError(new Error('network down'));
            await t.addTranslations(['x']);
            expect(t.translations).toEqual({});
            errorSpy.mockRestore();
        });
    });

    describe('init', function () {
        test('should delegate to addTranslations', async function () {
            mockFetchOnceWithJson({ k: { de: 'K' } });
            await t.init(['k']);
            expect(global.fetch).toHaveBeenCalledTimes(1);
            expect(t.translations).toEqual({ k: { de: 'K' } });
        });
    });

    describe('translate', function () {
        beforeEach(function () {
            t.translations = {
                greeting: { de: 'Hallo', en: 'Hello' },
                onlyEnglish: { en: 'EN only' },
            };
        });

        test('should return the translation in the configured default language', function () {
            // language defaults to t.language === 'de'
            expect(t.translate('greeting')).toBe('Hallo');
        });

        test('should accept an explicit language override', function () {
            expect(t.translate('greeting', 'en')).toBe('Hello');
        });

        test('should fall back to the configured fallback language when the requested one is missing', function () {
            t.fallbackLanguage = 'en';
            expect(t.translate('onlyEnglish', 'de')).toBe('EN only');
        });

        test('should return the key itself for an unknown message key', function () {
            const warnSpy = jest.spyOn(console, 'warn').mockImplementation(function () {});
            expect(t.translate('missing')).toBe('missing');
            warnSpy.mockRestore();
        });

        test('should return the key itself when translations were never initialised', function () {
            const fresh = new viewerJS.translator('https://example.org/api/v1/', 'de');
            const warnSpy = jest.spyOn(console, 'warn').mockImplementation(function () {});
            expect(fresh.translate('greeting')).toBe('greeting');
            warnSpy.mockRestore();
        });
    });
});
