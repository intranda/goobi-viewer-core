/**
 * Unit tests for viewerJS.GeoMapSearch.
 *
 * The constructor only stores merged config. setSearchString is a small
 * sessionStorage + DOM helper. init() depends on riot.mount which we
 * stub with jest.fn().
 */
const viewerJS = require('../viewerJS.geoMapSearch.js');

beforeEach(() => {
    sessionStorage.clear();
    document.body.innerHTML = `
        <input id="searchInput" />
        <button id="submitBtn">Submit</button>`;
});

describe('GeoMapSearch constructor', () => {
    test('stores a deep-merged config (defaults + caller overrides)', () => {
        const search = new viewerJS.GeoMapSearch({
            inputSearchSelector: '#searchInput',
            submitSearchSelector: '#submitBtn',
            opts: { search_placeholder: 'Bitte Adresse' },
        });
        expect(search.config.inputSearchSelector).toBe('#searchInput');
        expect(search.config.submitSearchSelector).toBe('#submitBtn');
        // The default heatmap settings (under opts.hitsLayer) survive the merge.
        expect(search.config.opts.hitsLayer.heatmap.enabled).toBe(true);
        // The override is applied.
        expect(search.config.opts.search_placeholder).toBe('Bitte Adresse');
    });
});

describe('GeoMapSearch.setSearchString', () => {
    function makeSearch() {
        return new viewerJS.GeoMapSearch({
            inputSearchSelector: '#searchInput',
            submitSearchSelector: '#submitBtn',
        });
    }

    test('writes the feature into sessionStorage under "geoFacet"', () => {
        const search = makeSearch();
        search.setSearchString('GEOPOLYGON');
        expect(sessionStorage.getItem('geoFacet')).toBe('GEOPOLYGON');
    });

    test('writes the feature into the configured search-input element', () => {
        const search = makeSearch();
        search.setSearchString('GEOPOLYGON');
        expect(document.getElementById('searchInput').value).toBe('GEOPOLYGON');
    });

    test('disables the submit button when given a falsy feature', () => {
        const search = makeSearch();
        search.setSearchString('');
        expect(document.getElementById('submitBtn').hasAttribute('disabled')).toBe(true);
    });

    test('removes the disabled attribute when given a truthy feature', () => {
        const search = makeSearch();
        document.getElementById('submitBtn').setAttribute('disabled', 'disabled');
        search.setSearchString('GEOPOLYGON');
        expect(document.getElementById('submitBtn').hasAttribute('disabled')).toBe(false);
    });
});
