/**
 * Unit tests for viewerJS.userDropdown.
 *
 * jQuery + jsdom are wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.userDropdown.js');
const $ = global.$;

describe('viewerJS.userDropdown.init', function () {
    afterEach(function () {
        document.body.innerHTML = '';
        $('body').off();
    });

    function setupDom() {
        document.body.innerHTML = '<button data-toggle="user-dropdown">User</button>' + '<div class="login-navigation__user-dropdown" style="display:none">menu</div>';
    }

    test('should toggle the user-dropdown on click of [data-toggle="user-dropdown"]', function () {
        setupDom();
        viewerJS.userDropdown.init();
        // fadeToggle is async by default; spy and run synchronously.
        const fadeSpy = jest.spyOn($.fn, 'fadeToggle').mockImplementation(function () {
            this.toggle();
            return this;
        });
        try {
            $('[data-toggle="user-dropdown"]').trigger('click');
            expect(fadeSpy).toHaveBeenCalledTimes(1);
            // After fadeToggle the dropdown is shown.
            expect($('.login-navigation__user-dropdown').css('display')).not.toBe('none');
        } finally {
            fadeSpy.mockRestore();
        }
    });

    test('should hide the dropdown when a click happens outside of it', function () {
        setupDom();
        // Dropdown starts visible.
        $('.login-navigation__user-dropdown').show();
        viewerJS.userDropdown.init();

        // Click on body away from the dropdown.
        const outside = document.createElement('div');
        outside.id = 'outside';
        document.body.appendChild(outside);
        $('#outside').trigger('click');

        expect($('.login-navigation__user-dropdown').css('display')).toBe('none');
    });

    test('should NOT hide the dropdown when the click is inside the dropdown', function () {
        setupDom();
        $('.login-navigation__user-dropdown').append('<a id="inside">link</a>').show();
        viewerJS.userDropdown.init();

        $('#inside').trigger('click');

        // Click was inside, dropdown stays visible.
        expect($('.login-navigation__user-dropdown').css('display')).not.toBe('none');
    });
});
