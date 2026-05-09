/**
 * Unit tests for viewerJS.mobileToggles.
 *
 * The init() method binds direct (non-delegated) click handlers to the
 * elements that match the four data-toggle attributes at call time. We
 * therefore set up the DOM fixture and call init() inside each test so
 * the handlers attach to fresh elements.
 */

// jQuery animations are async under jsdom and never resolve. Force
// them to commit synchronously so we can assert on .style.display.
$.fx.off = true;

const viewerJS = require('../viewerJS.mobileToggles.js');

beforeEach(() => {
    document.body.innerHTML = `
        <div class="row-offcanvas">
            <button data-toggle="offcanvas" id="offcanvas">A</button>
            <button class="btn-toggle search in" data-toggle="search">S</button>
            <button class="btn-toggle language" data-toggle="language">L</button>
            <button data-toggle="mobilenav" id="mobilenav">N</button>
            <button data-toggle="mobile-image-controls" id="imgctrl">I</button>
            <div class="header-actions__search" style="display:block">SBox</div>
            <div id="mobileNav" style="display:none">Nav</div>
            <div class="image-controls" style="display:none">IC</div>
            <div id="changeLocal" style="display:none">Lang</div>
        </div>`;
    viewerJS.mobileToggles.init();
});

describe('mobileToggles offcanvas', () => {
    test('clicking the offcanvas toggle adds .active to .row-offcanvas and .in to the toggle', () => {
        $('#offcanvas').trigger('click');
        expect($('.row-offcanvas').hasClass('active')).toBe(true);
        expect($('#offcanvas').hasClass('in')).toBe(true);
    });

    test('clicking the offcanvas toggle a second time removes the classes again', () => {
        $('#offcanvas').trigger('click').trigger('click');
        expect($('.row-offcanvas').hasClass('active')).toBe(false);
        expect($('#offcanvas').hasClass('in')).toBe(false);
    });
});

describe('mobileToggles mobilenav', () => {
    test('clicking the nav toggle hides search and language and reveals #mobileNav', () => {
        $('#mobilenav').trigger('click');
        expect($('.btn-toggle.search').hasClass('in')).toBe(false);
        expect($('.btn-toggle.language').hasClass('in')).toBe(false);
        expect(document.querySelector('.header-actions__search').style.display).toBe('none');
        expect(document.getElementById('mobileNav').style.display).not.toBe('none');
    });
});

describe('mobileToggles mobile-image-controls', () => {
    test('clicking the image-controls toggle reveals .image-controls', () => {
        $('#imgctrl').trigger('click');
        expect(document.querySelector('.image-controls').style.display).not.toBe('none');
    });
});

describe('mobileToggles language', () => {
    test('clicking the language toggle hides search and toggles language visibility', () => {
        $('[data-toggle="language"]').trigger('click');
        expect($('.btn-toggle.search').hasClass('in')).toBe(false);
        expect(document.querySelector('.header-actions__search').style.display).toBe('none');
        expect($('[data-toggle="language"]').hasClass('in')).toBe(true);
        // #changeLocal starts hidden; fadeToggle (with fx.off) flips it.
        expect(document.getElementById('changeLocal').style.display).not.toBe('none');
    });
});

describe('mobileToggles search', () => {
    test('clicking the search toggle hides language and toggles search visibility', () => {
        $('[data-toggle="search"]').trigger('click');
        expect($('.btn-toggle.language').hasClass('in')).toBe(false);
        expect(document.getElementById('changeLocal').style.display).toBe('none');
        // The search button started with .in (visible state) → now toggled off.
        expect($('[data-toggle="search"]').hasClass('in')).toBe(false);
    });
});
