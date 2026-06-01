/**
 * Unit tests for viewerJS.download.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.download.js');
const $ = global.$;

describe('viewerJS.download', function () {
    afterEach(function () {
        document.body.innerHTML = '';
        $('body').off();
    });

    describe('init', function () {
        test('should disable the download button until the licence checkbox is ticked', function () {
            document.body.innerHTML = '<input type="checkbox" id="agreeLicense"/>' + '<button id="downloadBtn">Download</button>';

            viewerJS.download.init();

            // Download button starts disabled.
            expect($('#downloadBtn').prop('disabled')).toBe(true);

            // Native click toggles `checked` AND fires the click event in
            // one go (jQuery .trigger('click') only does the latter).
            document.getElementById('agreeLicense').click();
            expect($('#downloadBtn').prop('disabled')).toBe(false);

            // Click again — checkbox un-toggles, button disabled again.
            document.getElementById('agreeLicense').click();
            expect($('#downloadBtn').prop('disabled')).toBe(true);
        });
    });

    describe('checkboxValidation', function () {
        beforeEach(function () {
            document.body.innerHTML = '<input type="checkbox" id="agreeLicense"/>' + '<button id="downloadBtn">Download</button>';
            viewerJS.download.init();
        });

        test('should enable the download button when called with a truthy state', function () {
            // Disabled by init().
            expect($('#downloadBtn').prop('disabled')).toBe(true);
            viewerJS.download.checkboxValidation(true);
            expect($('#downloadBtn').prop('disabled')).toBe(false);
        });

        test('should disable the download button when called with a falsy state', function () {
            viewerJS.download.checkboxValidation(true);
            expect($('#downloadBtn').prop('disabled')).toBe(false);
            viewerJS.download.checkboxValidation(false);
            expect($('#downloadBtn').prop('disabled')).toBe(true);
        });

        test('should return false when called with a falsy state (legacy contract)', function () {
            // The function returns false on the disabled branch; some callers
            // may rely on this to short-circuit JSF behaviour.
            expect(viewerJS.download.checkboxValidation(false)).toBe(false);
        });
    });
});
