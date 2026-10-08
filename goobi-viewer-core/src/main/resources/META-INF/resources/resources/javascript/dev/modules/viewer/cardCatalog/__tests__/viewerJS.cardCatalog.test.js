/**
 * Unit tests for the pure helper functions of viewerJS.cardCatalog:
 * URL state (de)serialization and IIIF info URL derivation.
 * jQuery + jsdom are wired up by jest-setup-browser.js.
 */
const viewerJS = require('../viewerJS.cardCatalog.js');
require('../viewerJS.cardCatalog.state.js');
require('../viewerJS.cardCatalog.stage.js');
require('../viewerJS.cardCatalog.strip.js');

const cardCatalog = viewerJS.cardCatalog;

const info = {
    drawers: [
        { id: 'drawer-a', label: 'A', entryCount: 1022 },
        { id: 'drawer-ze', label: 'Ze', entryCount: 816 },
    ],
};

describe('viewerJS.cardCatalog.stateFromParams', function () {
    test('should return empty state without parameters', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams(''), info);
        expect(state).toEqual({ drawer: null, from: null, to: null, card: null, chain: [] });
    });

    test('should read drawer, interval and card from parameters', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17&to=129&card=20'), info);
        expect(state.drawer).toBe('drawer-a');
        expect(state.from).toBe(17);
        expect(state.to).toBe(129);
        expect(state.card).toBe(20);
    });

    test('should synthesize a breadcrumb chain for interval deep links (reload regression)', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17&to=129'), info);
        expect(state.chain).toEqual([{ from: 1, to: 1022 }]);
    });

    test('should not synthesize a chain without an interval', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a'), info);
        expect(state.chain).toEqual([]);
    });

    test('should ignore unknown drawers', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=unknown&from=1&to=5'), info);
        expect(state.drawer).toBeNull();
        expect(state.from).toBeNull();
    });

    test('should ignore invalid intervals', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=100&to=5'), info);
        expect(state.drawer).toBe('drawer-a');
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
    });

    test('should reject an interval with a missing bound but keep the card', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17&card=3'), info);
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
        expect(state.card).toBe(3);
    });

    test('should return an empty state without info', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1&to=5'), null);
        expect(state).toEqual({ drawer: null, from: null, to: null, card: null, chain: [] });
    });

    test('should keep the card without an interval (small drawer deep link)', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&card=7'), info);
        expect(state.drawer).toBe('drawer-a');
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
        expect(state.card).toBe(7);
    });

    test('should reject non-numeric interval values', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=abc&to=50&card=x'), info);
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
        expect(state.card).toBeNull();
    });

    test('should clamp hand-edited deep link values to the drawer size', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1000&to=999999&card=999999'), info);
        expect(state.to).toBe(1022);
        expect(state.card).toBeNull();
        // from beyond the drawer size: the whole interval is rejected
        const beyond = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=2000&to=999999'), info);
        expect(beyond.from).toBeNull();
        expect(beyond.to).toBeNull();
    });

    test('should reject partially numeric positions and open a card deep link in a bounded window', function () {
        const catalogInfo = { drawers: info.drawers, config: { fullViewThreshold: 16 } };
        expect(cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17foo&to=50'), catalogInfo).from).toBeNull();
        expect(cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&card=20foo'), catalogInfo).card).toBeNull();
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&card=100'), catalogInfo);
        expect(state).toEqual({ drawer: 'drawer-a', from: 92, to: 107, card: 100, chain: [{ from: 1, to: 1022 }] });
    });

    test('should show a card deep link even when its explicit interval is wider than the full view', function () {
        const catalogInfo = { drawers: info.drawers, config: { fullViewThreshold: 16 } };
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1&to=1000&card=500'), catalogInfo);
        expect(state).toEqual({ drawer: 'drawer-a', from: 492, to: 507, card: 500, chain: [{ from: 1, to: 1022 }] });
    });

    test('should open a card outside a narrow URL interval instead of selecting another card', function () {
        const catalogInfo = { drawers: info.drawers, config: { fullViewThreshold: 16 } };
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1&to=4&card=500'), catalogInfo);
        expect(state).toEqual({ drawer: 'drawer-a', from: 492, to: 507, card: 500, chain: [{ from: 1, to: 1022 }] });
    });
});

describe('viewerJS.cardCatalog url helpers', function () {
    beforeEach(function () {
        cardCatalog.config = { apiUrl: 'http://localhost:8080/viewer/api/v2', pi: 'A B' };
    });

    afterEach(function () {
        cardCatalog.config = null;
    });

    test('should encode the identifier in the catalog url', function () {
        expect(cardCatalog.catalogUrl()).toBe('http://localhost:8080/viewer/api/v2/archives/A%20B/cardcatalog/');
    });

    test('should strip the api suffix for the viewer root url', function () {
        expect(cardCatalog.rootUrl()).toBe('http://localhost:8080/viewer');
        cardCatalog.config.apiUrl = 'http://localhost:8080/viewer/api/v1';
        expect(cardCatalog.rootUrl()).toBe('http://localhost:8080/viewer');
        cardCatalog.config.rootUrl = '/viewer';
        expect(cardCatalog.rootUrl()).toBe('/viewer');
    });
});

describe('viewerJS.cardCatalog async view ownership', function () {
    let resolveBoundaries;

    beforeEach(function () {
        cardCatalog.info = { drawers: [{ id: 'drawer-a', label: 'A', entryCount: 100 }], totalEntries: 100, config: { fullViewThreshold: 16 } };
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: null, chain: [] };
        cardCatalog.config = { apiUrl: '/viewer/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.$content = jQuery('<div>');
        cardCatalog.$drawers = jQuery('<div>');
        cardCatalog.$register = jQuery('<div>');
        cardCatalog.$counts = jQuery('<div>');
        jest.spyOn(cardCatalog, 'fetchJson').mockImplementation(
            () =>
                new Promise((resolve) => {
                    resolveBoundaries = resolve;
                })
        );
    });

    afterEach(function () {
        cardCatalog.cancelRender();
        cardCatalog.resetContent();
        cardCatalog.info = null;
        cardCatalog.state = null;
        cardCatalog.config = null;
        jest.restoreAllMocks();
    });

    test('should not restore an old zoom view after navigating to the empty state', async function () {
        cardCatalog.render();
        cardCatalog.state = { drawer: null, from: null, to: null, card: null, chain: [] };
        cardCatalog.render();
        resolveBoundaries({ items: [{ position: 1, label: 'Old result' }] });
        await Promise.resolve();
        expect(cardCatalog.$content.text()).toContain('archives__card_catalog_choose_drawer');
        expect(cardCatalog.$content.text()).not.toContain('Old result');
    });

    test('should ignore an old drawer response after switching drawers', async function () {
        const responses = [];
        cardCatalog.info.drawers.push({ id: 'drawer-b', label: 'B', entryCount: 100 });
        cardCatalog.fetchJson.mockImplementation(() => new Promise((resolve) => responses.push(resolve)));
        cardCatalog.render();
        cardCatalog.state = { drawer: 'drawer-b', from: null, to: null, card: null, chain: [] };
        cardCatalog.render();
        responses[0]({ items: [{ position: 1, label: 'Old drawer' }] });
        await Promise.resolve();
        expect(cardCatalog.$content.text()).not.toContain('Old drawer');
        responses[1]({ items: [{ position: 1, label: 'New drawer' }] });
        await Promise.resolve();
        expect(cardCatalog.$content.text()).toContain('New drawer');
    });
});

describe('viewerJS.cardCatalog stage ownership', function () {
    test('should not open a stale image after leaving the stage', async function () {
        document.body.innerHTML = '<div id="ccStageViewer"></div>';
        cardCatalog.config = { osdScriptUrl: '/osd.js', msg: {} };
        const original = window.OpenSeadragon;
        window.OpenSeadragon = jest.fn();
        let finishLoading;
        jest.spyOn(cardCatalog, 'loadOpenSeadragon').mockImplementation(
            () =>
                new Promise((resolve) => {
                    finishLoading = resolve;
                })
        );
        cardCatalog.showStageImage({ imageUrl: '/iiif/full/!400,400/0/default.jpg', label: 'Old image' });
        cardCatalog.resetContent();
        finishLoading();
        await Promise.resolve();
        expect(window.OpenSeadragon).not.toHaveBeenCalled();
        window.OpenSeadragon = original;
        cardCatalog.config = null;
        document.body.innerHTML = '';
        jest.restoreAllMocks();
    });
});

describe('viewerJS.cardCatalog.paramsFromState', function () {
    test('should serialize state and preserve unrelated parameters', function () {
        const params = cardCatalog.paramsFromState({ drawer: 'drawer-ze', from: 1, to: 50, card: 3, chain: [] }, new URLSearchParams('foo=bar&card=99'));
        expect(params.get('foo')).toBe('bar');
        expect(params.get('drawer')).toBe('drawer-ze');
        expect(params.get('from')).toBe('1');
        expect(params.get('to')).toBe('50');
        expect(params.get('card')).toBe('3');
    });

    test('should remove all card catalog parameters without a drawer', function () {
        const params = cardCatalog.paramsFromState(
            { drawer: null, from: null, to: null, card: null, chain: [] },
            new URLSearchParams('drawer=drawer-a&from=1&to=5&card=2&selected=xyz')
        );
        expect(params.get('drawer')).toBeNull();
        expect(params.get('from')).toBeNull();
        expect(params.get('selected')).toBe('xyz');
    });

    test('should not serialize a half interval', function () {
        const params = cardCatalog.paramsFromState({ drawer: 'drawer-a', from: 5, to: null, card: null, chain: [] }, new URLSearchParams(''));
        expect(params.get('drawer')).toBe('drawer-a');
        expect(params.get('from')).toBeNull();
        expect(params.get('to')).toBeNull();
    });

    test('should round-trip through stateFromParams', function () {
        const state = { drawer: 'drawer-a', from: 17, to: 129, card: 20, chain: [] };
        const params = cardCatalog.paramsFromState(state, new URLSearchParams(''));
        const restored = cardCatalog.stateFromParams(params, info);
        expect(restored.drawer).toBe(state.drawer);
        expect(restored.from).toBe(state.from);
        expect(restored.to).toBe(state.to);
        expect(restored.card).toBe(state.card);
    });
});

describe('viewerJS.cardCatalog.imageInfoUrl', function () {
    test('should derive info.json from a scaled IIIF image URL', function () {
        expect(cardCatalog.imageInfoUrl('https://example.org/viewer/api/v1/records/94_584/representative/full/!400,400/0/default.jpg')).toBe(
            'https://example.org/viewer/api/v1/records/94_584/representative/info.json'
        );
    });

    test('should return null for unusable input', function () {
        expect(cardCatalog.imageInfoUrl(null)).toBeNull();
        expect(cardCatalog.imageInfoUrl('https://example.org/no-iiif.jpg')).toBeNull();
    });

    test('should use the last /full/ occurrence when the identifier contains one itself', function () {
        expect(cardCatalog.imageInfoUrl('https://example.org/iiif/images/full/scan/full/!400,400/0/default.jpg')).toBe('https://example.org/iiif/images/full/scan/info.json');
    });
});

describe('viewerJS.cardCatalog.escape', function () {
    test('should escape HTML special characters', function () {
        expect(cardCatalog.escape('<b>"Zeri" & \'Co\'</b>')).toBe('&lt;b&gt;&quot;Zeri&quot; &amp; &#39;Co&#39;&lt;/b&gt;');
        expect(cardCatalog.escape(null)).toBe('');
    });
});

describe('viewerJS.cardCatalog accessible markup', function () {
    beforeEach(function () {
        cardCatalog.config = { msg: {} };
    });

    afterEach(function () {
        cardCatalog.config = null;
    });

    test('should keep zoom card artwork decorative because the caption names the button', function () {
        const html = cardCatalog.renderZoomCard({ position: 5, label: 'A & B', thumbnailUrl: '/image.jpg' });
        expect(html).toContain('alt=""');
        expect(html).toContain('A &amp; B');
    });

    test('should expose the selected strip card to assistive technology', function () {
        const current = cardCatalog.renderStripThumb({ position: 5, label: 'A', thumbnailUrl: '/image.jpg' }, 5);
        const other = cardCatalog.renderStripThumb({ position: 6, label: 'B', thumbnailUrl: '/image.jpg' }, 5);
        expect(current).toContain('aria-current="true"');
        expect(other).not.toContain('aria-current');
    });

    test('should use unescaped plain text for the dynamic stage image description', function () {
        cardCatalog.info = { drawers: [{ id: 'drawer-a', entryCount: 1 }] };
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 1, card: 1, chain: [] };
        cardCatalog.section = { from: 1, to: 1, items: [{ position: 1, label: 'A & B' }] };
        cardCatalog.$content = jQuery('<div><div id="ccStageViewer"></div><div data-cc-caption></div><div data-cc-strip></div></div>');
        jest.spyOn(cardCatalog, 'showStageImage').mockImplementation(() => {});

        cardCatalog.updateStage(cardCatalog.section.items[0]);

        expect(cardCatalog.$content.find('#ccStageViewer').attr('aria-label')).toBe('archives__card_catalog_card 1: A & B');
        cardCatalog.info = null;
        cardCatalog.state = null;
        cardCatalog.section = null;
        jest.restoreAllMocks();
    });
});

describe('viewerJS.cardCatalog.headCropUrl', function () {
    test('should derive the head strip region from a full image URL', function () {
        expect(cardCatalog.headCropUrl('https://example.org/viewer/api/v1/records/94_584/representative/full/!1200,1200/0/default.jpg')).toBe(
            'https://example.org/viewer/api/v1/records/94_584/representative/pct:0,0,100,40/!1200,480/0/default.jpg'
        );
    });

    test('should return null for missing input', function () {
        expect(cardCatalog.headCropUrl(null)).toBeNull();
    });

    test('should return null instead of an uncropped URL when the size pattern does not match', function () {
        expect(cardCatalog.headCropUrl('https://example.org/viewer/api/v1/records/94_584/representative/full/max/0/default.jpg')).toBeNull();
    });
});

describe('viewerJS.cardCatalog.windowAround', function () {
    beforeEach(function () {
        cardCatalog.info = { config: { fullViewThreshold: 16 } };
    });

    afterEach(function () {
        cardCatalog.info = null;
    });

    test('should center the window around the position', function () {
        expect(cardCatalog.windowAround(100, { entryCount: 1000 })).toEqual({ from: 92, to: 107 });
    });

    test('should clamp the window at the start of the drawer', function () {
        expect(cardCatalog.windowAround(1, { entryCount: 1000 })).toEqual({ from: 1, to: 16 });
    });

    test('should clamp the window at the end of the drawer', function () {
        expect(cardCatalog.windowAround(998, { entryCount: 1000 })).toEqual({ from: 985, to: 1000 });
    });

    test('should span the whole drawer when it is smaller than the window', function () {
        expect(cardCatalog.windowAround(3, { entryCount: 10 })).toEqual({ from: 1, to: 10 });
    });
});

describe('viewerJS.cardCatalog.msg', function () {
    afterEach(function () {
        cardCatalog.config = null;
    });

    test('should look up the bundle key for a short name and escape the translation', function () {
        cardCatalog.config = { msg: { archives__card_catalog_total: 'Total <b>&</b>' } };
        expect(cardCatalog.msg('total')).toBe('Total &lt;b&gt;&amp;&lt;/b&gt;');
    });

    test('should fall back to the escaped key for unknown keys', function () {
        cardCatalog.config = { msg: {} };
        expect(cardCatalog.msg('unknown_key')).toBe('unknown_key');
    });
});

describe('viewerJS.cardCatalog.clampExtension', function () {
    const section = { from: 17, to: 32 };

    test('should clamp the request to the drawer bounds', function () {
        expect(cardCatalog.clampExtension(section, -5, 10, 1000)).toEqual({ from: 1, to: 10 });
        expect(cardCatalog.clampExtension(section, 990, 1010, 1000)).toEqual({ from: 990, to: 1000 });
    });

    test('should trim the already loaded part on either end', function () {
        expect(cardCatalog.clampExtension(section, 30, 40, 1000)).toEqual({ from: 33, to: 40 });
        expect(cardCatalog.clampExtension(section, 10, 20, 1000)).toEqual({ from: 10, to: 16 });
    });

    test('should return null when nothing is left to load', function () {
        expect(cardCatalog.clampExtension(section, 17, 32, 1000)).toBeNull();
        expect(cardCatalog.clampExtension({ from: 1, to: 16 }, -15, 0, 1000)).toBeNull();
    });
});

describe('viewerJS.cardCatalog.extendStrip', function () {
    test('should not retry forever when a pending strip load made no progress', async function () {
        cardCatalog.section = { from: 1, to: 16, items: [] };
        cardCatalog.stripLoading = true;
        cardCatalog.stripPromise = Promise.resolve();
        const fetchJson = jest.spyOn(cardCatalog, 'fetchJson');
        await cardCatalog.extendStrip(17, 32);
        expect(fetchJson).not.toHaveBeenCalled();
        cardCatalog.section = null;
        cardCatalog.stripLoading = false;
        cardCatalog.stripPromise = null;
        jest.restoreAllMocks();
    });

    test('should bound the loaded strip and preserve a way back to an evicted selected card', async function () {
        const items = Array.from({ length: 100 }, (_, index) => ({ position: index + 1, label: String(index + 1) }));
        cardCatalog.info = { drawers: [{ id: 'drawer-a', entryCount: 1000 }], config: { fullViewThreshold: 100, listPageSize: 50 } };
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 100, card: 1, chain: [] };
        cardCatalog.section = { from: 1, to: 100, items: items };
        cardCatalog.$content = jQuery('<div><div class="archives-cardcatalog__stage-caption"></div><div data-cc-strip></div></div>');
        cardCatalog.$content.find('[data-cc-strip]').html(items.map((item) => cardCatalog.renderStripThumb(item, 1)).join(''));
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue({ items: Array.from({ length: 100 }, (_, index) => ({ position: index + 101, label: String(index + 101) })) });

        await cardCatalog.extendStrip(101, 200);

        expect(cardCatalog.section.from).toBe(51);
        expect(cardCatalog.section.to).toBe(200);
        expect(cardCatalog.section.items).toHaveLength(150);
        expect(cardCatalog.$content.find('[data-stage]')).toHaveLength(150);
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        cardCatalog.shiftCard(1);
        expect(cardCatalog.state.card).toBe(2);
        expect(cardCatalog.state.from).toBe(1);
        expect(render).toHaveBeenCalled();

        cardCatalog.resetContent();
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        jest.restoreAllMocks();
    });

    test('should show a retry action after a failed strip request', async function () {
        const items = Array.from({ length: 16 }, (_, index) => ({ position: index + 1, label: String(index + 1) }));
        cardCatalog.info = { drawers: [{ id: 'drawer-a', entryCount: 100 }], config: { fullViewThreshold: 16, listPageSize: 16 } };
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 16, card: 16, chain: [] };
        cardCatalog.section = { from: 1, to: 16, items: items };
        cardCatalog.$content = jQuery('<div><div class="archives-cardcatalog__stage-caption"></div><div data-cc-strip></div></div>');
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'fetchJson')
            .mockRejectedValueOnce(new Error('network'))
            .mockResolvedValueOnce({ items: Array.from({ length: 16 }, (_, index) => ({ position: index + 17, label: String(index + 17) })) });

        await cardCatalog.extendStrip(17, 32);
        expect(cardCatalog.stripErrorRange).toEqual({ from: 17, to: 32 });
        expect(cardCatalog.$content.find('[data-strip-retry]')).toHaveLength(1);
        expect(cardCatalog.$content.find('.archives-cardcatalog__stage-caption [data-strip-error]')).toHaveLength(1);
        await cardCatalog.retryStrip();
        expect(cardCatalog.$content.find('[data-strip-error]')).toHaveLength(0);
        expect(cardCatalog.section.items.some((item) => item.position === 17)).toBe(true);

        cardCatalog.resetContent();
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        jest.restoreAllMocks();
    });
});

describe('viewerJS.cardCatalog.chainJump', function () {
    afterEach(function () {
        jest.restoreAllMocks();
        cardCatalog.state = null;
    });

    test('should be a no-op at the drawer root (dead history entry regression)', function () {
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: null, chain: [] };
        const syncUrl = jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
        cardCatalog.chainJump(-1);
        expect(syncUrl).not.toHaveBeenCalled();
        expect(render).not.toHaveBeenCalled();
    });

    test('should navigate back to the root from a zoomed interval', function () {
        cardCatalog.state = { drawer: 'drawer-a', from: 17, to: 129, card: null, chain: [{ from: 1, to: 1022 }] };
        const syncUrl = jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
        cardCatalog.chainJump(-1);
        expect(syncUrl).toHaveBeenCalledWith(true);
        expect(cardCatalog.state.from).toBeNull();
        expect(cardCatalog.state.chain).toEqual([]);
    });
});

describe('viewerJS.cardCatalog.selectDrawer', function () {
    beforeEach(function () {
        cardCatalog.info = info;
    });

    afterEach(function () {
        jest.restoreAllMocks();
        cardCatalog.info = null;
        cardCatalog.state = null;
    });

    test('should not stack history entries when the target equals the current state', function () {
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: null, chain: [] };
        const syncUrl = jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'renderSidebar').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
        cardCatalog.selectDrawer('drawer-a');
        expect(syncUrl).not.toHaveBeenCalled();
    });

    test('should navigate when a different drawer is selected', function () {
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: null, chain: [] };
        const syncUrl = jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'renderSidebar').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
        cardCatalog.selectDrawer('drawer-ze');
        expect(syncUrl).toHaveBeenCalledWith(true);
        expect(cardCatalog.state.drawer).toBe('drawer-ze');
    });
});

describe('viewerJS.cardCatalog.loadRegister', function () {
    beforeEach(function () {
        cardCatalog.info = { mode: 'REGISTER', drawers: [{ id: 'drawer-a' }, { id: 'drawer-b' }] };
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: null, chain: [] };
        cardCatalog.register = { drawers: [] };
        cardCatalog.registerLoaded = new Set();
        cardCatalog.registerController = null;
        cardCatalog.registerPendingDrawer = null;
        cardCatalog.registerErrorDrawer = null;
        cardCatalog.$register = jQuery('<div>');
        cardCatalog.initSeq = 1;
    });

    afterEach(function () {
        if (cardCatalog.registerController) {
            cardCatalog.registerController.abort();
        }
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        cardCatalog.register = null;
        cardCatalog.registerLoaded = null;
        cardCatalog.registerController = null;
        cardCatalog.registerPendingDrawer = null;
        cardCatalog.registerErrorDrawer = null;
        jest.restoreAllMocks();
    });

    test('should load only the active drawer and cache its register', async function () {
        const fetchJson = jest.spyOn(cardCatalog, 'fetchJson').mockImplementation((url) =>
            Promise.resolve({
                available: true,
                drawers: [{ id: url.includes('drawer-a') ? 'drawer-a' : 'drawer-b', ranges: [] }],
            })
        );

        await cardCatalog.loadRegister();
        expect(fetchJson).toHaveBeenCalledWith('/api/v1/archives/catalog/cardcatalog/drawers/drawer-a/register', 'register', expect.anything());
        expect(cardCatalog.register.drawers.map((drawer) => drawer.id)).toEqual(['drawer-a']);

        cardCatalog.state.drawer = 'drawer-b';
        await cardCatalog.loadRegister();
        expect(cardCatalog.register.drawers.map((drawer) => drawer.id)).toEqual(['drawer-a', 'drawer-b']);
        cardCatalog.state.drawer = 'drawer-a';
        cardCatalog.loadRegister();
        expect(fetchJson).toHaveBeenCalledTimes(2);
    });

    test('should ignore an aborted drawer response after changing drawers', async function () {
        let resolveFirst;
        jest.spyOn(cardCatalog, 'fetchJson')
            .mockImplementationOnce(
                () =>
                    new Promise((resolve) => {
                        resolveFirst = resolve;
                    })
            )
            .mockResolvedValueOnce({ available: true, drawers: [{ id: 'drawer-b', ranges: [] }] });

        const first = cardCatalog.loadRegister();
        cardCatalog.state.drawer = 'drawer-b';
        await cardCatalog.loadRegister();
        resolveFirst({ available: true, drawers: [{ id: 'drawer-a', ranges: [] }] });
        await first;

        expect(cardCatalog.register.drawers.map((drawer) => drawer.id)).toEqual(['drawer-b']);
        expect(cardCatalog.registerLoaded.has('drawer-a')).toBe(false);
    });

    test('should show a retry after a transient register failure', async function () {
        jest.spyOn(console, 'error').mockImplementation(() => {});
        const fetchJson = jest
            .spyOn(cardCatalog, 'fetchJson')
            .mockRejectedValueOnce(new Error('temporary failure'))
            .mockResolvedValueOnce({ available: true, drawers: [{ id: 'drawer-a', ranges: [] }] });

        await cardCatalog.loadRegister();
        expect(cardCatalog.$register.find('[data-register-retry]').length).toBe(1);
        expect(cardCatalog.registerLoaded.has('drawer-a')).toBe(false);

        cardCatalog.$register.find('[data-register-retry]').trigger('click');
        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(fetchJson).toHaveBeenCalledTimes(2);
        expect(cardCatalog.registerLoaded.has('drawer-a')).toBe(true);
        expect(cardCatalog.$register.find('[data-register-retry]').length).toBe(0);
    });
});

describe('viewerJS.cardCatalog.shiftCard', function () {
    beforeEach(function () {
        cardCatalog.info = { drawers: [{ id: 'drawer-a', label: 'A', entryCount: 100 }], config: { listPageSize: 16, fullViewThreshold: 16 } };
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 16, card: 5, chain: [] };
        cardCatalog.section = { from: 1, to: 16, items: [] };
    });

    afterEach(function () {
        jest.restoreAllMocks();
        cardCatalog.info = null;
        cardCatalog.state = null;
        cardCatalog.section = null;
    });

    test('should select directly without loading when the target is loaded', function () {
        const selectCard = jest.spyOn(cardCatalog, 'selectCard').mockImplementation(() => {});
        const extendStrip = jest.spyOn(cardCatalog, 'extendStrip').mockResolvedValue(undefined);
        cardCatalog.shiftCard(1);
        expect(selectCard).toHaveBeenCalledWith(6);
        expect(extendStrip).not.toHaveBeenCalled();
    });

    test('should load the neighbors first when the target is outside the strip', async function () {
        cardCatalog.state.card = 16;
        const selectCard = jest.spyOn(cardCatalog, 'selectCard').mockImplementation(() => {});
        const extendStrip = jest.spyOn(cardCatalog, 'extendStrip').mockImplementation(async () => {
            cardCatalog.section.items.push({ position: 17 });
            cardCatalog.section.to = 32;
        });
        cardCatalog.shiftCard(1);
        expect(extendStrip).toHaveBeenCalledWith(17, 32);
        await Promise.resolve();
        expect(selectCard).toHaveBeenCalledWith(17);
    });

    test('should do nothing at the drawer bounds', function () {
        cardCatalog.state.card = 100;
        cardCatalog.section = { from: 85, to: 100, items: [] };
        const selectCard = jest.spyOn(cardCatalog, 'selectCard').mockImplementation(() => {});
        cardCatalog.shiftCard(1);
        expect(selectCard).not.toHaveBeenCalled();
    });

    test('should drop the deferred selection when the user picked another card meanwhile', async function () {
        cardCatalog.state.card = 16;
        const selectCard = jest.spyOn(cardCatalog, 'selectCard').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'extendStrip').mockResolvedValue(undefined);
        cardCatalog.shiftCard(1);
        cardCatalog.state.card = 9;
        await Promise.resolve();
        expect(selectCard).not.toHaveBeenCalled();
    });
});

describe('viewerJS.cardCatalog.fetchJson', function () {
    const originalFetch = global.fetch;

    afterEach(function () {
        global.fetch = originalFetch;
    });

    test('should reject with the status for non-ok responses', async function () {
        global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 503 });
        await expect(cardCatalog.fetchJson('http://x/', 'test')).rejects.toThrow('test request failed: 503');
    });

    test('should resolve with the parsed body for ok responses', async function () {
        global.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve({ a: 1 }) });
        await expect(cardCatalog.fetchJson('http://x/', 'test')).resolves.toEqual({ a: 1 });
    });
});
