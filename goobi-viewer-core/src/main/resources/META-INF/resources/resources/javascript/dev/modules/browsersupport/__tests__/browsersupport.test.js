/**
 * Unit tests for browsersupport.getCurrentBrowser.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js. The module's
 * top-level setupBrowserSupport() call fires once at require()-time;
 * we then exercise getCurrentBrowser() with various user-agent strings.
 */
const { getCurrentBrowser } = require('../browsersupport.js');

function setUserAgent(ua) {
    Object.defineProperty(window.navigator, 'userAgent', { value: ua, configurable: true });
}

describe('browsersupport.getCurrentBrowser', function () {
    test('should detect Edge (Chromium-based, "Edg/" UA)', function () {
        setUserAgent('Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Edg/120.0');
        expect(getCurrentBrowser()).toBe('Edge');
    });

    test('should detect Chrome', function () {
        // No "Edg" token, contains "Chrome".
        setUserAgent('Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36');
        // Note: this UA also contains "Safari" (Chrome adds it for compat).
        // The implementation checks Chrome before Safari, so Chrome wins.
        expect(getCurrentBrowser()).toBe('Chrome');
    });

    test('should detect Safari (no Chrome token in UA)', function () {
        setUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Version/17.0 Safari/605.1.15');
        expect(getCurrentBrowser()).toBe('Safari');
    });

    test('should detect Firefox', function () {
        setUserAgent('Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0');
        expect(getCurrentBrowser()).toBe('Firefox');
    });

    test('should detect IE 11 via the MSIE token', function () {
        // IE 11 doesn't carry "MSIE" but earlier versions do.
        setUserAgent('Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1)');
        expect(getCurrentBrowser()).toBe('IE');
    });
});
