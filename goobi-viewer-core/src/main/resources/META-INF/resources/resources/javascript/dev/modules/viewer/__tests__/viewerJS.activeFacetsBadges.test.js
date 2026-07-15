const viewerJS = require('../viewerJS.activeFacetsBadges.js');

function setupDom(badgeOffsets, moreLabel) {
    document.body.innerHTML =
        '<ul data-badges="activeFacets" data-more-label="' +
        (moreLabel || '+{0} weitere') +
        '">' +
        badgeOffsets.map(() => '<li class="active-facets-badges__list-item"><a href="#" data-badge="facet" class="badge">chip</a></li>').join('') +
        '</ul>';
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

    test('should move focus to the first revealed badge when the more-button is clicked', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init();
        wrapper.querySelector('.active-facets-badges__more').click();
        const firstRevealed = wrapper.querySelectorAll('[data-badge="facet"]')[2];
        expect(document.activeElement).toBe(firstRevealed);
    });

    test('should respect a custom row limit from the config', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init({ maxRows: 1 });
        expect(wrapper.querySelectorAll('.-overflow').length).toBe(2);
        expect(wrapper.querySelector('.active-facets-badges__more').textContent).toBe('+2 weitere');
    });

    test('should hide the surrounding list item instead of the bare badge', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init();
        const hiddenItem = wrapper.querySelectorAll('.active-facets-badges__list-item')[2];
        expect(hiddenItem.classList.contains('-overflow')).toBe(true);
        expect(hiddenItem.querySelector('[data-badge="facet"]').classList.contains('-overflow')).toBe(false);
    });

    test('should mount the more-button inside a list item to keep the list valid', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init();
        const moreButton = wrapper.querySelector('.active-facets-badges__more');
        expect(moreButton.parentElement.tagName).toBe('LI');
        moreButton.click();
        expect(wrapper.querySelector('.active-facets-badges__more')).toBeNull();
        expect(wrapper.querySelectorAll('li:empty').length).toBe(0);
    });

    test('should not add a second button when init runs twice', function () {
        const wrapper = setupDom([0, 24, 48]);
        viewerJS.activeFacetsBadges.init();
        viewerJS.activeFacetsBadges.init();
        expect(wrapper.querySelectorAll('.active-facets-badges__more').length).toBe(1);
    });
});
