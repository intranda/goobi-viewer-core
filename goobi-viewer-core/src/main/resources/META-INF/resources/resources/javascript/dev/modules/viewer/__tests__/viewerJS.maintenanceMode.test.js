/**
 * Unit tests for viewerJS.maintenanceMode.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.maintenanceMode.js');
const $ = global.$;

describe('viewerJS.maintenanceMode.init', function () {
    beforeEach(function () {
        sessionStorage.clear();
        document.body.innerHTML = '<div id="maintenanceModeBanner" style="display:none">Maintenance!</div>' + '<button data-trigger="closeMaintenanceInfo">Close</button>';
    });

    afterEach(function () {
        document.body.innerHTML = '';
        sessionStorage.clear();
        $('body').off();
    });

    test('should not show the banner when config.active is false', function () {
        viewerJS.maintenanceMode.init({ active: false });
        expect($('#maintenanceModeBanner').css('display')).toBe('none');
    });

    test('should show the banner when active and the user has not dismissed it this session', function () {
        viewerJS.maintenanceMode.init({ active: true });
        expect($('#maintenanceModeBanner').css('display')).not.toBe('none');
    });

    test('should NOT show the banner when sessionStorage flags it as dismissed', function () {
        sessionStorage.setItem('hideMaintenanceBanner', 'true');
        viewerJS.maintenanceMode.init({ active: true });
        expect($('#maintenanceModeBanner').css('display')).toBe('none');
    });

    test('should hide the banner and remember the dismissal in sessionStorage on close-button click', function () {
        // slideUp is async; intercept it so the test is deterministic.
        const slideUpSpy = jest.spyOn($.fn, 'slideUp').mockImplementation(function (cb) {
            this.hide();
            if (typeof cb === 'function') cb.call(this);
            return this;
        });
        try {
            viewerJS.maintenanceMode.init({ active: true });
            $('[data-trigger="closeMaintenanceInfo"]').trigger('click');
            expect(slideUpSpy).toHaveBeenCalled();
            expect(sessionStorage.getItem('hideMaintenanceBanner')).toBe('true');
            expect($('#maintenanceModeBanner').css('display')).toBe('none');
        } finally {
            slideUpSpy.mockRestore();
        }
    });
});
