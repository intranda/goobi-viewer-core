const viewerJS = require('../viewerJS.combinedFacets.js');

// jsfAjax stub: captures the handler so tests can simulate ajax re-renders
let _ajaxHandler = null;
viewerJS.jsfAjax = {
    success: {
        subscribe: jest.fn(function (handler) {
            _ajaxHandler = handler;
            return { unsubscribe: jest.fn() };
        }),
    },
};

function sectionMarkup(field) {
    return (
        '<div class="widget">' +
        '<div class="widget__topbar">' +
        '<h2>' +
        field +
        '</h2>' +
        '<button class="filter-toggle">F</button>' +
        '<button data-section-toggle="' +
        field +
        '" aria-expanded="true"><span class="toggle-icon">v</span></button>' +
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

    test('should toggle the section when clicking anywhere on the topbar, e.g. the heading', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        getSection('FIELD_A').querySelector('h2').click();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(true);
        expect(getToggle('FIELD_A').getAttribute('aria-expanded')).toBe('false');
        getSection('FIELD_A').querySelector('.widget__topbar').click();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(false);
        expect(getToggle('FIELD_A').getAttribute('aria-expanded')).toBe('true');
    });

    test('should toggle the section when clicking the icon inside the toggle button', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        getToggle('FIELD_A')
            .querySelector('.toggle-icon')
            .dispatchEvent(new MouseEvent('click', { bubbles: true }));
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(true);
    });

    test('should push a stickyElements refresh after toggling so hc-sticky recalculates', function () {
        setupDom();
        viewerJS.stickyElements = { refresh: { next: jest.fn() } };
        viewerJS.combinedFacets.init();
        getToggle('FIELD_A').click();
        expect(viewerJS.stickyElements.refresh.next).toHaveBeenCalledTimes(1);
        getSection('FIELD_A').querySelector('h2').click();
        expect(viewerJS.stickyElements.refresh.next).toHaveBeenCalledTimes(2);
        delete viewerJS.stickyElements;
    });

    test('should toggle without error when stickyElements is not present', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        expect(function () {
            getToggle('FIELD_A').click();
        }).not.toThrow();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(true);
    });

    test('should not toggle the section when clicking other controls in the topbar', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        getSection('FIELD_A').querySelector('.filter-toggle').click();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(false);
    });

    test('should keep working when a toggle has no surrounding topbar', function () {
        document.body.innerHTML =
            '<div class="combined-facets"><div class="widget">' +
            '<button data-section-toggle="FIELD_C" aria-expanded="true"></button>' +
            '<div class="widget__body">values</div>' +
            '</div></div>';
        viewerJS.combinedFacets.init();
        getToggle('FIELD_C').click();
        expect(getSection('FIELD_C').classList.contains('-section-collapsed')).toBe(true);
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

    test('should not toggle twice when init runs twice (idempotent binding)', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        viewerJS.combinedFacets.init();
        getToggle('FIELD_A').click();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(true);
    });

    test('should subscribe to the jsfAjax success stream only once', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        viewerJS.combinedFacets.init();
        expect(viewerJS.jsfAjax.success.subscribe).toHaveBeenCalledTimes(1);
    });

    test('should rebind the toggle and restore collapsed state after an ajax section re-render', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        getToggle('FIELD_A').click();

        // simulate the f:ajax re-render of section A
        getSection('FIELD_A').outerHTML = sectionMarkup('FIELD_A');
        const ajaxSource = document.createElement('button');
        ajaxSource.setAttribute('data-collapse-link', 'collapse-link-0');
        _ajaxHandler({ source: ajaxSource });

        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(true);
        expect(getToggle('FIELD_A').getAttribute('aria-expanded')).toBe('false');
        getToggle('FIELD_A').click();
        expect(getSection('FIELD_A').classList.contains('-section-collapsed')).toBe(false);
    });

    test('should move focus to the re-rendered collapse link after an ajax re-render', function () {
        setupDom();
        const section = getSection('FIELD_A');
        section.querySelector('.widget__body').innerHTML = '<button data-collapse-link="collapse-link-0">Alle anzeigen</button>';
        viewerJS.combinedFacets.init();

        // simulate the f:ajax re-render: old button is replaced by a new one with the same marker
        const oldButton = section.querySelector('[data-collapse-link]');
        section.querySelector('.widget__body').innerHTML = '<button data-collapse-link="collapse-link-0">Weniger anzeigen</button>';
        _ajaxHandler({ source: oldButton });

        expect(document.activeElement).toBe(section.querySelector('[data-collapse-link]'));
    });

    test('should ignore ajax events without a collapse link source', function () {
        setupDom();
        viewerJS.combinedFacets.init();
        expect(function () {
            _ajaxHandler({});
            _ajaxHandler({ source: document.createElement('div') });
        }).not.toThrow();
    });
});
