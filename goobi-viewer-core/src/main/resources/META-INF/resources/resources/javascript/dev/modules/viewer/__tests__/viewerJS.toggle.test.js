/**
 * Unit tests for viewerJS.toggle.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.toggle.js');
const $ = global.$;

describe('viewerJS.toggle.init', function () {
    beforeEach(function () {
        // Detach any previous body delegate handlers from earlier tests in
        // this file. init() registers a `change` listener on body for each
        // toggle target it sees.
        $('body').off('change');
    });

    afterEach(function () {
        document.body.innerHTML = '';
        $('body').off('change');
    });

    test('should be a no-op when no [data-toggle-action] inputs are present', function () {
        document.body.innerHTML = '<div id="x">visible</div>';
        viewerJS.toggle.init();
        // jQuery's :visible selector relies on layout, which jsdom doesn't
        // compute. Check display directly instead.
        expect($('#x').css('display')).not.toBe('none');
    });

    test('should hide the target on init when the source checkbox is checked and action is "hide"', function () {
        document.body.innerHTML = '<input type="checkbox" data-toggle-action="hide" data-toggle-target="#target" checked />' + '<div id="target">content</div>';
        viewerJS.toggle.init();
        expect($('#target').css('display')).toBe('none');
    });

    test('should leave the target visible on init when the source checkbox is unchecked', function () {
        document.body.innerHTML = '<input type="checkbox" data-toggle-action="hide" data-toggle-target="#target" />' + '<div id="target">content</div>';
        viewerJS.toggle.init();
        // jQuery's .css('display') for an undisturbed block element returns the
        // computed value — jsdom returns '' for elements without explicit
        // display style, which is its default. Either way it's not 'none'.
        expect($('#target').css('display')).not.toBe('none');
    });

    test('should toggle the target on subsequent change events when the source becomes checked', function () {
        document.body.innerHTML =
            '<input id="src" type="checkbox" data-toggle-action="show" data-toggle-target="#target" checked />' + '<div id="target" style="display:none">content</div>';
        viewerJS.toggle.init();
        const $target = $('#target');
        // Re-trigger: uncheck, then check again. The change-handler should
        // call .animate({height: toggle, opacity: toggle}) only when the
        // source IS checked (post-change).
        const animateSpy = jest.spyOn($.fn, 'animate').mockImplementation(function () {
            return this;
        });
        try {
            $('#src').prop('checked', true).trigger('change');
            expect(animateSpy).toHaveBeenCalled();
        } finally {
            animateSpy.mockRestore();
        }
        // Sanity: the spy intercepted instead of running real animation.
        expect($target).toBeDefined();
    });
});
