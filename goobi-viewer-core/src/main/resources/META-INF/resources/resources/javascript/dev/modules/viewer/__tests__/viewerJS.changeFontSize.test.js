/**
 * Unit tests for viewerJS.changeFontSize.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js. The module reads
 * `viewer.localStoragePossible` to gate its behaviour; we set it to true
 * after require()-ing the module.
 */
const viewerJS = require('../viewerJS.changeFontSize.js');
const $ = global.$;

// Module gate: short-circuits init() if storage isn't available.
viewerJS.localStoragePossible = true;

function setupDom(initialHtmlFontSizePx) {
    document.body.innerHTML = '<button id="fontSizeDown">A-</button>' + '<button id="fontSizeUp">A+</button>';
    if (initialHtmlFontSizePx) {
        document.documentElement.style.fontSize = initialHtmlFontSizePx;
    }
}

describe('viewerJS.changeFontSize', function () {
    beforeEach(function () {
        sessionStorage.clear();
        setupDom('14px');
    });

    afterEach(function () {
        document.body.innerHTML = '';
        document.documentElement.style.fontSize = '';
        sessionStorage.clear();
    });

    test('should seed sessionStorage with the configured base font size on first init', function () {
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            baseFontSize: '14px',
            minFontSize: 12,
            maxFontSize: 18,
        });
        expect(sessionStorage.getItem('currentFontSize')).toBe('14px');
    });

    test('should restore the html font-size from sessionStorage if one is already stored', function () {
        sessionStorage.setItem('currentFontSize', '16px');
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            baseFontSize: '14px',
        });
        expect(document.documentElement.style.fontSize).toBe('16px');
    });

    test('should disable the font-down button at the minimum font size', function () {
        sessionStorage.setItem('currentFontSize', '12px');
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            minFontSize: 12,
            maxFontSize: 18,
        });
        expect($('#fontSizeDown').prop('disabled')).toBe(true);
        expect($('#fontSizeUp').prop('disabled')).toBe(false);
    });

    test('should disable the font-up button at the maximum font size', function () {
        sessionStorage.setItem('currentFontSize', '18px');
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            minFontSize: 12,
            maxFontSize: 18,
        });
        expect($('#fontSizeUp').prop('disabled')).toBe(true);
        expect($('#fontSizeDown').prop('disabled')).toBe(false);
    });

    test('should raise the html font-size by 1px on each click of the font-up button', function () {
        sessionStorage.setItem('currentFontSize', '14px');
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            minFontSize: 12,
            maxFontSize: 18,
        });
        $('#fontSizeUp').trigger('click');
        expect(document.documentElement.style.fontSize).toBe('15px');
        expect(sessionStorage.getItem('currentFontSize')).toBe('15px');
    });

    test('should NOT raise above the configured maximum', function () {
        sessionStorage.setItem('currentFontSize', '18px');
        document.documentElement.style.fontSize = '18px';
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            minFontSize: 12,
            maxFontSize: 18,
        });
        $('#fontSizeUp').trigger('click');
        // Stays at 18.
        expect(document.documentElement.style.fontSize).toBe('18px');
        // Up button gets disabled at the cap.
        expect($('#fontSizeUp').prop('disabled')).toBe(true);
    });

    test('should lower the html font-size by 1px on each click of the font-down button', function () {
        sessionStorage.setItem('currentFontSize', '14px');
        document.documentElement.style.fontSize = '14px';
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            minFontSize: 12,
            maxFontSize: 18,
        });
        $('#fontSizeDown').trigger('click');
        expect(document.documentElement.style.fontSize).toBe('13px');
    });

    test('should NOT lower below the configured minimum', function () {
        sessionStorage.setItem('currentFontSize', '12px');
        document.documentElement.style.fontSize = '12px';
        viewerJS.changeFontSize.init({
            fontDownBtn: '#fontSizeDown',
            fontUpBtn: '#fontSizeUp',
            minFontSize: 12,
            maxFontSize: 18,
        });
        $('#fontSizeDown').trigger('click');
        // Stays at 12.
        expect(document.documentElement.style.fontSize).toBe('12px');
        // Down button gets disabled at the floor.
        expect($('#fontSizeDown').prop('disabled')).toBe(true);
    });
});
