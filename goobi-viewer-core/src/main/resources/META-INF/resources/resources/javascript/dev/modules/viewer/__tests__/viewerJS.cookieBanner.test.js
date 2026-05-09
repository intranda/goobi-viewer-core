/**
 * Unit tests for viewerJS.cookieBanner.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.cookieBanner.js');
const $ = global.$;

function setupDom() {
    document.body.innerHTML =
        '<div id="cookieBanner" style="display:none">' +
        '  <div class="cookie-banner__info">info</div>' +
        '  <div class="cookie-banner__icon-wrapper"></div>' +
        '  <button class="cookie-banner__accept-button" data-set="cookie-banner-accept">Accept</button>' +
        '  <button data-set="cookie-banner-decline">Decline</button>' +
        '</div>';
}

describe('viewerJS.cookieBanner.init', function () {
    beforeEach(function () {
        localStorage.clear();
        setupDom();
    });

    afterEach(function () {
        document.body.innerHTML = '';
        localStorage.clear();
        $('body').off();
    });

    test('should hide the banner when config.active is false', function () {
        $('#cookieBanner').show();
        viewerJS.cookieBanner.init({ active: false });
        expect($('#cookieBanner').css('display')).toBe('none');
    });

    test('should show the banner and seed status=true on first visit', function () {
        viewerJS.cookieBanner.init({ active: true, lastEditedHash: 12345 });
        expect($('#cookieBanner').css('display')).not.toBe('none');
        expect(viewerJS.cookieBanner.getStoredBannerStatus()).toBe(true);
        expect(viewerJS.cookieBanner.getStoredLastEditedHash()).toBe(12345);
    });

    test('should hide the banner when status=false and the hash matches', function () {
        viewerJS.cookieBanner.storeBannerStatus(false);
        viewerJS.cookieBanner.storeLastEditedHash(42);

        viewerJS.cookieBanner.init({ active: true, lastEditedHash: 42 });

        expect($('#cookieBanner').css('display')).toBe('none');
    });

    test('should re-show the banner when the lastEditedHash has changed since last dismissal', function () {
        viewerJS.cookieBanner.storeBannerStatus(false);
        viewerJS.cookieBanner.storeLastEditedHash(1);

        viewerJS.cookieBanner.init({ active: true, lastEditedHash: 2 });

        expect($('#cookieBanner').css('display')).not.toBe('none');
        // Status reset to true (banner shown again).
        expect(viewerJS.cookieBanner.getStoredBannerStatus()).toBe(true);
    });
});

describe('viewerJS.cookieBanner.hideBanner', function () {
    beforeEach(function () {
        localStorage.clear();
        setupDom();
        // Tame async slideUp/fadeOut so assertions are synchronous.
        jest.spyOn($.fn, 'slideUp').mockImplementation(function (cb) {
            this.hide();
            if (typeof cb === 'function') cb.call(this);
            return this;
        });
        jest.spyOn($.fn, 'fadeOut').mockImplementation(function () {
            this.hide();
            return this;
        });
        // Initialise into the "first visit" branch so config + bindings exist.
        viewerJS.cookieBanner.init({ active: true, lastEditedHash: 1 });
    });

    afterEach(function () {
        document.body.innerHTML = '';
        localStorage.clear();
        $('body').off();
        jest.restoreAllMocks();
    });

    test('clicking accept should hide the banner and store cookiesAccepted=true + status=false', function () {
        $('[data-set="cookie-banner-accept"]').trigger('click');
        expect(localStorage.getItem('cookiesAccepted')).toBe('true');
        expect(viewerJS.cookieBanner.getStoredBannerStatus()).toBe(false);
        expect($('#cookieBanner').css('display')).toBe('none');
    });

    test('clicking decline should hide the banner and store cookiesAccepted=false', function () {
        $('[data-set="cookie-banner-decline"]').trigger('click');
        expect(localStorage.getItem('cookiesAccepted')).toBe('false');
        expect(viewerJS.cookieBanner.getStoredBannerStatus()).toBe(false);
    });
});
