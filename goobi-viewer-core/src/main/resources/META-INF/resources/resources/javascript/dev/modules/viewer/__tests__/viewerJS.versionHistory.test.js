/**
 * Unit tests for viewerJS.versionHistory.
 *
 * The init() reads JSON from each `widgetInputs` element's `value`,
 * then renders one <li> per version into the `widgetList` container.
 * The current version (matching imgPi) gets a non-link span; all
 * others get an anchor.
 *
 * NOTE: the source mutates a module-level `_defaults` object, so
 * state from one init() call leaks into the next. We work around this
 * by using `jest.isolateModules` per test so each gets a fresh copy.
 */

beforeEach(() => {
    document.body.innerHTML = `
        <div>
            <input class="version-input" value='{"id":"PPN1","label":"Edition 1","year":"1900"}'/>
            <input class="version-input" value='{"id":"PPN2","label":"Edition 2","year":"1910"}'/>
            <input class="version-input" value='{"id":"PPN3","year":"1920"}'/>
            <ul class="version-list"></ul>
        </div>`;
});

function loadFresh() {
    let mod;
    jest.isolateModules(() => {
        mod = require('../viewerJS.versionHistory.js');
    });
    return mod;
}

describe('viewerJS.versionHistory.init', () => {
    test('renders one list item per widget input', () => {
        const viewerJS = loadFresh();
        viewerJS.versionHistory.init({
            imgUrl: '/viewer/image',
            imgPi: 'PPNX',
            widgetInputs: '.version-input',
            widgetList: '.version-list',
        });
        expect(document.querySelectorAll('.version-list li').length).toBe(3);
    });

    test('renders the current version (imgPi match) as a span (no link)', () => {
        const viewerJS = loadFresh();
        viewerJS.versionHistory.init({
            imgUrl: '/viewer/image',
            imgPi: 'PPN2',
            widgetInputs: '.version-input',
            widgetList: '.version-list',
        });
        // The matching item is the second one — labelled "Edition 2".
        const items = document.querySelectorAll('.version-list li');
        const current = Array.from(items).find((li) => li.querySelector('span'));
        expect(current).toBeDefined();
        expect(current.querySelector('span').textContent).toBe('Edition 2');
        expect(current.querySelector('a')).toBeNull();
    });

    test('renders other versions as links pointing to imgUrl/<id>/1/', () => {
        const viewerJS = loadFresh();
        viewerJS.versionHistory.init({
            imgUrl: '/viewer/image',
            imgPi: 'PPN2',
            widgetInputs: '.version-input',
            widgetList: '.version-list',
        });
        const links = document.querySelectorAll('.version-list a');
        const hrefs = Array.from(links).map((a) => a.getAttribute('href'));
        expect(hrefs).toContain('/viewer/image/PPN1/1/');
        expect(hrefs).toContain('/viewer/image/PPN3/1/');
    });

    test('falls back to "<id> (<year>)" when no label is present', () => {
        const viewerJS = loadFresh();
        viewerJS.versionHistory.init({
            imgUrl: '/viewer/image',
            imgPi: 'PPN2',
            widgetInputs: '.version-input',
            widgetList: '.version-list',
        });
        // PPN3 has no label, so its rendered text is "PPN3 (1920)".
        const ppn3Link = document.querySelector('a[href="/viewer/image/PPN3/1/"]');
        expect(ppn3Link.textContent.trim()).toBe('PPN3 (1920)');
    });
});
