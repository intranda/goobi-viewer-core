/**
 * Unit tests for viewerJS.combinedFacets.
 *
 * The module wires the section toggle chevrons of the combined facets
 * container: clicking a toggle collapses or expands its section, updates
 * aria-expanded and persists the collapsed sections in sessionStorage so
 * the state survives the page reloads caused by facet clicks.
 */
const viewerJS = require('../viewerJS.combinedFacets.js');

function sectionMarkup(field) {
    return (
        '<div class="widget">' +
        '<div class="widget__topbar">' +
        '<h2>' +
        field +
        '</h2>' +
        '<button data-section-toggle="' +
        field +
        '" aria-expanded="true"></button>' +
        '</div>' +
        '<div class="widget__body">values</div>' +
        '</div>'
    );
}

function setupDom() {
    document.body.innerHTML = '<div class="combined-facets">' + sectionMarkup('FIELD_A') + sectionMarkup('FIELD_B') + '</div>';
}

function getSection(field) {
    return document.querySelector('[data-section-toggle="' + field + '"]').closest('.widget');
}

function getToggle(field) {
    return document.querySelector('[data-section-toggle="' + field + '"]');
}

describe('viewerJS.combinedFacets', function () {
    afterEach(function () {
        document.body.innerHTML = '';
        window.sessionStorage.clear();
    });

    test('should collapse the section and update aria-expanded on click', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        getToggle('FIELD_A').click();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(true);
        expect(getToggle('FIELD_A').getAttribute('aria-expanded')).toBe('false');
        expect(getSection('FIELD_B').classList.contains('-section-collapsed')).toBe(false);
    });

    test('should expand a collapsed section again on second click', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        getToggle('FIELD_A').click();
        getToggle('FIELD_A').click();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(false);
        expect(getToggle('FIELD_A').getAttribute('aria-expanded')).toBe('true');
    });

    test('should restore persisted collapsed sections on init', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        getToggle('FIELD_B').click();

        setupDom();
        viewerJS.combinedFacets.init();
        expect(getSection('FIELD_B').classList.contains('-section-collapsed')).toBe(true);
        expect(getToggle('FIELD_B').getAttribute('aria-expanded')).toBe('false');
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(false);
    });

    test('should dispatch a resize event when expanding so maps and sliders relayout', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        const resizeListener = jest.fn();
        window.addEventListener('resize', resizeListener);
        getToggle('FIELD_A').click();
        expect(resizeListener).not.toHaveBeenCalled();
        getToggle('FIELD_A').click();
        expect(resizeListener).toHaveBeenCalledTimes(1);
        window.removeEventListener('resize', resizeListener);
    });
});
