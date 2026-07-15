const viewerJS = require('../viewerJS.activeFacetsBadges.js');

function setupDom(badgeOffsets, moreLabel) {
    document.body.innerHTML =
        '<div data-badges="activeFacets" data-more-label="' +
        (moreLabel || '+{0} weitere') +
        '">' +
        badgeOffsets.map(() => '<a data-badge="facet" class="badge">chip</a>').join('') +
        '</div>';
    const badges = document.querySelectorAll('[data-badge="facet"]');
    // jsdom reports offsetTop as 0: mock it per badge to simulate row wrapping
    badges.forEach((badge, i) => {
        Object.defineProperty(badge, 'offsetTop', { value: badgeOffsets[i], configurable: true });
    });
    return document.querySelector('[data-badges="activeFacets"]');
}

describe('viewerJS.activeFacetsBadges', function () {
    afterEach(function () {
        document.body.innerHTML = '';
    });

    test('should not touch badges that fit into the row limit', function () {
        const wrapper = setupDom([0, 0, 24, 24]);
        viewerJS.activeFacetsBadges.init();
        expect(wrapper.querySelectorAll('.-overflow').length).toBe(0);
        expect(wrapper.querySelector('.active-facets-badges__more')).toBeNull();
    });

    test('should hide badges beyond the row limit and show a counting more-button', function () {
        const wrapper = setupDom([0, 0, 24, 48, 48, 72]);
        viewerJS.activeFacetsBadges.init();
        const hidden = wrapper.querySelectorAll('.-overflow');
        expect(hidden.length).toBe(3);
        const moreButton = wrapper.querySelector('.active-facets-badges__more');
        expect(moreButton).not.toBeNull();
        expect(moreButton.textContent).toBe('+3 weitere');
    });

    test('should reveal all badges and remove the button on click', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init();
        const moreButton = wrapper.querySelector('.active-facets-badges__more');
        moreButton.click();
        expect(wrapper.querySelectorAll('.-overflow').length).toBe(0);
        expect(wrapper.querySelector('.active-facets-badges__more')).toBeNull();
    });

    test('should respect a custom row limit from the config', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init({ maxRows: 1 });
        expect(wrapper.querySelectorAll('.-overflow').length).toBe(2);
        expect(wrapper.querySelector('.active-facets-badges__more').textContent).toBe('+2 weitere');
    });

    test('should not add a second button when init runs twice', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init();
        viewerJS.activeFacetsBadges.init();
        expect(wrapper.querySelectorAll('.active-facets-badges__more').length).toBe(1);
    });
});
