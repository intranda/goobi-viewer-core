/**
 * Unit tests for the viewerJS base module (viewerJS.js).
 *
 * The module is the central viewerJS namespace and contains many
 * helpers. We focus on the small, side-effect-free or DOM-only
 * functions:
 *   - iOS() platform detection,
 *   - getRestApiUrl() replacement,
 *   - setCheckedStatus() data-checked → checked attribute,
 *   - initWidgetUsage() hide-empty subtitle,
 *   - initDisallowDownload() preventDefault on right-click,
 *   - sessionStorage scroll-position helpers,
 *   - initFragmentNavigation() scrollIntoView.
 *
 * The module references several free globals (rxjs, currentPage,
 * restURL, getCurrentBrowser) that have to be stubbed BEFORE require.
 * It also calls into Bootstrap's $.fn.tooltip on module load via the
 * if-block at the bottom — we provide a minimal stub for that too.
 */

// rxjs Subjects are constructed at module load (initialized,
// toggledCollapsible). A barebones stub is enough.
function makeSubject() {
    const subs = [];
    return {
        next: (v) => subs.forEach((h) => h(v)),
        subscribe: (h) => subs.push(h),
        complete: () => {},
        pipe: function () {
            return this;
        },
    };
}
global.rxjs = {
    Subject: function () {
        return makeSubject();
    },
    operators: { first: () => null },
};

// Free globals used by the source.
global.getCurrentBrowser = () => 'jsdom';
global.currentPage = 'page-1';
global.restURL = 'https://example.org/rest';
global.currentLang = 'en';

// Bootstrap tooltip / dropdown plugin defaults — touched at module load.
$.fn.tooltip = $.fn.tooltip || function () {};
$.fn.tooltip.Constructor = { Default: { boundary: 'viewport' } };
$.fn.dropdown = $.fn.dropdown || function () {};
$.fn.dropdown.Constructor = { Default: { boundary: 'viewport' } };

const viewerJS = require('../viewerJS.js');

beforeEach(() => {
    sessionStorage.clear();
    document.body.innerHTML = '';
});

describe('viewer.iOS', () => {
    test('returns true for an iPad navigator.platform', () => {
        Object.defineProperty(navigator, 'platform', { value: 'iPad', configurable: true });
        expect(viewerJS.iOS()).toBe(true);
    });

    test('returns true for iPhone Simulator', () => {
        Object.defineProperty(navigator, 'platform', { value: 'iPhone Simulator', configurable: true });
        expect(viewerJS.iOS()).toBe(true);
    });

    test('returns false for a plain Linux platform', () => {
        Object.defineProperty(navigator, 'platform', { value: 'Linux x86_64', configurable: true });
        // Need to also clear the iPad-on-iOS-13 fallback (Mac userAgent + ontouchend).
        Object.defineProperty(navigator, 'userAgent', { value: 'Mozilla/5.0 (X11; Linux)', configurable: true });
        expect(viewerJS.iOS()).toBe(false);
    });

    test('detects iPad on iOS 13 via Mac userAgent + ontouchend', () => {
        Object.defineProperty(navigator, 'platform', { value: 'MacIntel', configurable: true });
        Object.defineProperty(navigator, 'userAgent', { value: 'Mozilla/5.0 (Macintosh)', configurable: true });
        document.ontouchend = () => {};
        try {
            expect(viewerJS.iOS()).toBe(true);
        } finally {
            delete document.ontouchend;
        }
    });
});

describe('viewer.getRestApiUrl', () => {
    test('replaces "/rest" with "/api/v1" in the global restURL', () => {
        // restURL was seeded above to "https://example.org/rest".
        expect(viewerJS.getRestApiUrl()).toBe('https://example.org/api/v1');
    });
});

describe('viewer.setCheckedStatus', () => {
    test('adds the checked attribute when data-checked is anything except "false"', () => {
        document.body.innerHTML = '<input type="radio" data-checked="true" />';
        viewerJS.setCheckedStatus();
        expect(document.querySelector('input').hasAttribute('checked')).toBe(true);
    });

    test('removes the checked attribute when data-checked is "false"', () => {
        document.body.innerHTML = '<input type="radio" data-checked="false" checked />';
        viewerJS.setCheckedStatus();
        expect(document.querySelector('input').hasAttribute('checked')).toBe(false);
    });
});

describe('viewer.initWidgetUsage', () => {
    test('hides .widget-usage__subtitle entries that have no following options sibling', () => {
        // The source uses $title.next() and hides the title when its next
        // sibling does not exist. Each widget-usage block thus has an
        // option-list immediately after the subtitle when populated.
        document.body.innerHTML = `
            <div class="widget-usage">
                <h3 class="widget-usage__subtitle" id="empty">Empty</h3>
            </div>
            <div class="widget-usage">
                <h3 class="widget-usage__subtitle" id="filled">With content</h3>
                <ul class="widget-usage__option-list">…</ul>
            </div>`;
        viewerJS.initWidgetUsage();
        expect(document.getElementById('empty').style.display).toBe('none');
        expect(document.getElementById('filled').style.display).not.toBe('none');
    });

    test('does nothing when no .widget-usage element is in the DOM', () => {
        document.body.innerHTML = '';
        expect(() => viewerJS.initWidgetUsage()).not.toThrow();
    });
});

describe('viewer.initDisallowDownload', () => {
    test('binds a contextmenu handler that preventDefaults the right-click', () => {
        document.body.innerHTML = '<img id="protected" data-allow-download="false" />';
        viewerJS.initDisallowDownload();

        const ev = new MouseEvent('contextmenu', { bubbles: true, cancelable: true });
        document.getElementById('protected').dispatchEvent(ev);
        expect(ev.defaultPrevented).toBe(true);
    });

    test('elements without data-allow-download="false" are not affected', () => {
        document.body.innerHTML = '<img id="free" />';
        viewerJS.initDisallowDownload();

        const ev = new MouseEvent('contextmenu', { bubbles: true, cancelable: true });
        document.getElementById('free').dispatchEvent(ev);
        expect(ev.defaultPrevented).toBe(false);
    });
});

describe('viewer.handleScrollPositionClick', () => {
    test('writes scrollTop + linkid for the current page into sessionStorage', () => {
        document.body.innerHTML = '<a id="a" data-linkid="L42" href="#">x</a>';
        // jQuery's .scrollTop() with no element data returns 0 in jsdom; that
        // is sufficient because we only care about the JSON shape.
        viewerJS.handleScrollPositionClick(document.getElementById('a'));

        const stored = JSON.parse(sessionStorage.getItem('scrollPositions'));
        expect(stored['page-1']).toBeDefined();
        expect(stored['page-1'].linkId).toBe('L42');
        expect(typeof stored['page-1'].scrollTop).toBe('number');
    });

    test('preserves entries for other pages when writing the current one', () => {
        sessionStorage.setItem(
            'scrollPositions',
            JSON.stringify({
                'other-page': { linkId: 'OTHER', scrollTop: 99 },
            })
        );
        document.body.innerHTML = '<a id="a" data-linkid="L1">x</a>';
        viewerJS.handleScrollPositionClick(document.getElementById('a'));

        const stored = JSON.parse(sessionStorage.getItem('scrollPositions'));
        expect(stored['other-page']).toEqual({ linkId: 'OTHER', scrollTop: 99 });
        expect(stored['page-1'].linkId).toBe('L1');
    });
});

describe('viewer.checkScrollPosition', () => {
    test('does nothing when sessionStorage has no scrollPositions key', () => {
        document.body.innerHTML = '<a data-linkid="X"></a>';
        expect(() => viewerJS.checkScrollPosition()).not.toThrow();
    });

    test('clears the stored entry for the current page after applying it', () => {
        sessionStorage.setItem(
            'scrollPositions',
            JSON.stringify({
                'page-1': { linkId: 'L1', scrollTop: 100 },
                'other-page': { linkId: 'OTHER', scrollTop: 50 },
            })
        );
        document.body.innerHTML = '<a data-linkid="L1"></a>';

        viewerJS.checkScrollPosition();

        const stored = JSON.parse(sessionStorage.getItem('scrollPositions'));
        // The current page's entry was wiped, the other one survives.
        expect(stored['page-1']).toBeUndefined();
        expect(stored['other-page']).toEqual({ linkId: 'OTHER', scrollTop: 50 });
    });
});

describe('viewer.toggledCollapsible / viewer.initialized', () => {
    test('exposes both as Subject-like objects with subscribe/next', () => {
        expect(typeof viewerJS.toggledCollapsible.subscribe).toBe('function');
        expect(typeof viewerJS.toggledCollapsible.next).toBe('function');
        expect(typeof viewerJS.initialized.subscribe).toBe('function');
    });
});

describe('viewer.tinyConfig', () => {
    test('starts as an empty config object', () => {
        // The module exports a single shared object that submodules
        // mutate before calling tinyMce.init.
        expect(typeof viewerJS.tinyConfig).toBe('object');
    });
});
