const viewerJS = require('../viewerJS.cardCatalog.js');
require('../viewerJS.cardCatalog.state.js');
require('../viewerJS.cardCatalog.sidebar.js');
require('../viewerJS.cardCatalog.overview.js');
require('../viewerJS.cardCatalog.stage.js');
require('../viewerJS.cardCatalog.strip.js');

const cardCatalog = viewerJS.cardCatalog;

const info = {
    drawers: [
        { id: 'drawer-a', label: 'A', entryCount: 1022 },
        { id: 'drawer-ze', label: 'Ze', entryCount: 816 },
    ],
};

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
        cardCatalog.$orientation = jQuery('<div>');
        cardCatalog.$guide = jQuery('<div>');
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

describe('viewerJS.cardCatalog.msg', function () {
    afterEach(function () {
        cardCatalog.config = null;
    });

    test('should look up the bundle key for a short name and escape the translation', function () {
        cardCatalog.config = { msg: { archives__card_catalog_register: 'Register <b>&</b>' } };
        expect(cardCatalog.msg('register')).toBe('Register &lt;b&gt;&amp;&lt;/b&gt;');
    });

    test('should fall back to the escaped key for unknown keys', function () {
        cardCatalog.config = { msg: {} };
        expect(cardCatalog.msg('unknown_key')).toBe('unknown_key');
    });

    test('should use the page wording of a volume catalog and keep the shared wording', function () {
        const msg = {
            archives__card_catalog_cards: 'Zettel',
            archives__volume_catalog_cards: 'Seiten',
            archives__card_catalog_drawer: 'Bereich',
        };
        cardCatalog.config = { catalogType: 'VOLUME', msg: msg };
        expect(cardCatalog.msg('cards')).toBe('Seiten');
        expect(cardCatalog.msg('drawer')).toBe('Bereich');

        cardCatalog.config = { catalogType: 'CARD', msg: msg };
        expect(cardCatalog.msg('cards')).toBe('Zettel');
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
        cardCatalog.info = { drawers: info.drawers.map((drawer) => ({ ...drawer })), config: { fullViewThreshold: 16 } };
    });

    afterEach(function () {
        cardCatalog.cancelRender();
        cardCatalog.resetContent();
        jest.restoreAllMocks();
        cardCatalog.info = null;
        cardCatalog.config = null;
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

    test('should not stack history when the first card of a small drawer is already displayed', function () {
        cardCatalog.info.drawers[0].entryCount = 2;
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: 1, chain: [] };
        const syncUrl = jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});

        cardCatalog.selectDrawer('drawer-a');

        expect(syncUrl).not.toHaveBeenCalled();
        expect(render).not.toHaveBeenCalled();
        expect(cardCatalog.state.card).toBe(1);
    });

    test('should return to the first card when reselecting a small drawer from a later card', async function () {
        cardCatalog.info.drawers[0].entryCount = 2;
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: 2, chain: [] };
        cardCatalog.$drawers = jQuery('<div>');
        cardCatalog.$orientation = jQuery('<div>');
        cardCatalog.$guide = jQuery('<div>');
        cardCatalog.$register = jQuery('<div>');
        cardCatalog.$content = jQuery('<div>');
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue({ from: 1, to: 2, items: [{ position: 1 }, { position: 2 }] });
        jest.spyOn(cardCatalog, 'showStageImage').mockImplementation(() => {});
        const syncUrl = jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});

        cardCatalog.selectDrawer('drawer-a');
        await Promise.resolve();

        expect(cardCatalog.state).toEqual({ drawer: 'drawer-a', from: null, to: null, card: 1, chain: [] });
        expect(syncUrl.mock.calls.filter(([push]) => push)).toHaveLength(1);
        expect(cardCatalog.$content.find('[data-stage="1"]').attr('aria-current')).toBe('true');
    });

    test('should not stack history when the first card of the selected register range is already displayed', function () {
        cardCatalog.state = { drawer: 'drawer-a', from: 5, to: 10, card: 5, chain: [{ from: 1, to: 1022 }] };
        const syncUrl = jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});

        cardCatalog.selectDrawer('drawer-a', 5, 10);

        expect(syncUrl).not.toHaveBeenCalled();
        expect(render).not.toHaveBeenCalled();
        expect(cardCatalog.state.card).toBe(5);
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

    test('should expose the HTTP status on the rejection', async function () {
        global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 404 });
        await expect(cardCatalog.fetchJson('http://x/', 'test')).rejects.toMatchObject({ status: 404 });
    });
});

describe('viewerJS.cardCatalog.openSelectedEntry', function () {
    afterEach(function () {
        cardCatalog.config = null;
        cardCatalog.state = null;
        cardCatalog.initController = null;
        jest.restoreAllMocks();
    });

    test('should treat an unknown selected entry as not found without logging an error', async function () {
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.initController = new AbortController();
        cardCatalog.state = { drawer: null, from: null, to: null, card: null, chain: [] };
        jest.spyOn(cardCatalog, 'fetchJson').mockRejectedValue(Object.assign(new Error('lookup request failed: 404'), { status: 404 }));
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});

        cardCatalog.openSelectedEntry('missing');
        for (let i = 0; i < 5; i++) {
            await Promise.resolve();
        }

        expect(error).not.toHaveBeenCalled();
        expect(render).not.toHaveBeenCalled();
    });
});

describe('viewerJS.cardCatalog default drawer', function () {
    const catalogInfo = {
        drawers: [
            { id: 'empty', label: 'Leer', entryCount: 0 },
            { id: 'drawer-a', label: 'A', entryCount: 1022 },
            { id: 'drawer-b', label: 'B', entryCount: 816 },
        ],
        config: { fullViewThreshold: 16, listPageSize: 16, zoomFactor: 8 },
    };

    beforeEach(function () {
        document.body.innerHTML =
            '<div data-cc="drawers"></div><div data-cc="orientation"></div><div data-cc="guide"></div><div data-cc="register"></div><div data-cc="content"></div>';
        jest.spyOn(cardCatalog, 'renderSidebar').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
    });

    afterEach(function () {
        cardCatalog.initController = null;
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        document.body.innerHTML = '';
        window.history.replaceState(null, '', '/viewer/archives/catalog/');
        jest.restoreAllMocks();
    });

    async function flush() {
        for (let i = 0; i < 8; i++) {
            await Promise.resolve();
        }
    }

    test('should open the first non-empty drawer when the URL names none', async function () {
        window.history.replaceState(null, '', '/viewer/archives/catalog/');
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue(catalogInfo);

        cardCatalog.init({ apiUrl: '/api/v1', pi: 'catalog', msg: {} });
        await flush();

        expect(cardCatalog.state).toEqual({ drawer: 'drawer-a', from: null, to: null, card: null, chain: [] });
        expect(window.location.search).toBe('?drawer=drawer-a');
        expect(window.history.state.cc.drawer).toBe('drawer-a');
        expect(cardCatalog.render).toHaveBeenCalledTimes(1);
    });

    test('should resolve a selected entry before choosing a drawer and fall back to the first drawer when it is unknown', async function () {
        window.history.replaceState(null, '', '/viewer/archives/catalog/?selected=missing');
        jest.spyOn(cardCatalog, 'fetchJson')
            .mockResolvedValueOnce(catalogInfo)
            .mockRejectedValueOnce(Object.assign(new Error('lookup request failed: 404'), { status: 404 }));

        cardCatalog.init({ apiUrl: '/api/v1', pi: 'catalog', msg: {} });
        await flush();

        expect(cardCatalog.fetchJson.mock.calls[1][0]).toContain('lookup?entryId=missing');
        expect(cardCatalog.state.drawer).toBe('drawer-a');
        expect(window.location.search).toBe('?drawer=drawer-a');
        expect(cardCatalog.render).toHaveBeenCalledTimes(2);
    });

    test('should leave the explicit drawer of the URL alone', async function () {
        window.history.replaceState(null, '', '/viewer/archives/catalog/?drawer=drawer-b');
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue(catalogInfo);

        cardCatalog.init({ apiUrl: '/api/v1', pi: 'catalog', msg: {} });
        await flush();

        expect(cardCatalog.state.drawer).toBe('drawer-b');
        expect(window.location.search).toBe('?drawer=drawer-b');
    });

    test('should fall back to the first drawer when the lookup of a selected entry fails', async function () {
        window.history.replaceState(null, '', '/viewer/archives/catalog/?selected=card-1');
        jest.spyOn(cardCatalog, 'fetchJson')
            .mockResolvedValueOnce(catalogInfo)
            .mockRejectedValueOnce(Object.assign(new Error('lookup request failed: 500'), { status: 500 }));
        const error = jest.spyOn(console, 'error').mockImplementation(() => {});

        cardCatalog.init({ apiUrl: '/api/v1', pi: 'catalog', msg: {} });
        await flush();

        expect(error).toHaveBeenCalledTimes(1);
        expect(cardCatalog.state.drawer).toBe('drawer-a');
        expect(window.location.search).toBe('?drawer=drawer-a');
        expect(cardCatalog.render).toHaveBeenCalledTimes(2);
    });

    test('should replace an unknown drawer of a history entry with the first drawer without adding history', async function () {
        window.history.replaceState(null, '', '/viewer/archives/catalog/?drawer=drawer-b');
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue(catalogInfo);
        cardCatalog.init({ apiUrl: '/api/v1', pi: 'catalog', msg: {} });
        await flush();
        const historyLength = window.history.length;

        window.history.replaceState(null, '', '/viewer/archives/catalog/?drawer=unknown');
        window.dispatchEvent(new PopStateEvent('popstate', { state: null }));

        expect(cardCatalog.state.drawer).toBe('drawer-a');
        expect(window.location.search).toBe('?drawer=drawer-a');
        expect(window.history.length).toBe(historyLength);
    });

    test('should stay without a drawer when no drawer has entries', async function () {
        window.history.replaceState(null, '', '/viewer/archives/catalog/');
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue({ ...catalogInfo, drawers: [{ id: 'empty', label: 'Leer', entryCount: 0 }] });

        cardCatalog.init({ apiUrl: '/api/v1', pi: 'catalog', msg: {} });
        await flush();

        expect(cardCatalog.openDefaultDrawer()).toBe(false);
        expect(cardCatalog.state.drawer).toBeNull();
        expect(window.location.search).toBe('');
    });
});

describe('viewerJS.cardCatalog drawer boundaries', function () {
    beforeEach(function () {
        cardCatalog.info = {
            drawers: [
                { id: 'empty-first', label: 'Empty first', entryCount: 0 },
                { id: 'drawer-a', label: 'A', entryCount: 100 },
                { id: 'empty-middle', label: 'Empty middle', entryCount: 0 },
                { id: 'drawer-b', label: 'B', entryCount: 50 },
                { id: 'empty-last', label: 'Empty last', entryCount: 0 },
            ],
            totalEntries: 150,
            config: { fullViewThreshold: 16, listPageSize: 16 },
        };
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.state = {
            drawer: 'drawer-a',
            from: 85,
            to: 100,
            card: 100,
            chain: [
                { from: 1, to: 100 },
                { from: 50, to: 100 },
            ],
        };
        cardCatalog.section = { from: 85, to: 100, items: [{ position: 99 }, { position: 100 }] };
        cardCatalog.$drawers = jQuery('<div>');
        cardCatalog.$orientation = jQuery('<div>');
        cardCatalog.$guide = jQuery('<div>');
        cardCatalog.$register = jQuery('<div>');
        cardCatalog.$content = jQuery('<div><div data-cc-caption></div><div data-cc-strip></div><button data-shift="-1"></button><button data-shift="1"></button></div>');
        jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'showStageImage').mockImplementation(() => {});
    });

    afterEach(function () {
        cardCatalog.cancelRender();
        cardCatalog.resetContent();
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        jest.restoreAllMocks();
    });

    test('should find the next nonempty drawer and skip empty drawers', function () {
        expect(cardCatalog.adjacentDrawer(1).id).toBe('drawer-b');
    });

    test('should find the previous nonempty drawer symmetrically', function () {
        cardCatalog.state.drawer = 'drawer-b';
        expect(cardCatalog.adjacentDrawer(-1).id).toBe('drawer-a');
    });

    test('should return no adjacent drawer at either catalog boundary', function () {
        expect(cardCatalog.adjacentDrawer(-1)).toBeNull();
        cardCatalog.state.drawer = 'drawer-b';
        expect(cardCatalog.adjacentDrawer(1)).toBeNull();
    });

    test('should not choose a drawer without a direction or a valid current drawer', function () {
        expect(cardCatalog.adjacentDrawer(0)).toBeNull();
        cardCatalog.state.drawer = 'unknown';
        expect(cardCatalog.adjacentDrawer(1)).toBeNull();
        expect(cardCatalog.adjacentDrawer(-1)).toBeNull();
    });

    test('should enter the next drawer only when advancing beyond the last card', function () {
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});

        cardCatalog.shiftCard(1);

        expect(cardCatalog.state).toEqual({ drawer: 'drawer-b', from: 1, to: 16, card: 1, chain: [{ from: 1, to: 50 }] });
        expect(cardCatalog.syncUrl).toHaveBeenCalledWith(true);
        expect(render).toHaveBeenCalledTimes(1);
    });

    test('should enter the previous drawer at its last card', function () {
        cardCatalog.state = { drawer: 'drawer-b', from: 1, to: 16, card: 1, chain: [{ from: 1, to: 50 }] };
        cardCatalog.section = { from: 1, to: 16, items: [{ position: 1 }] };
        jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});

        cardCatalog.shiftCard(-1);

        expect(cardCatalog.state).toEqual({ drawer: 'drawer-a', from: 85, to: 100, card: 100, chain: [{ from: 1, to: 100 }] });
        expect(cardCatalog.syncUrl).toHaveBeenCalledWith(true);
    });

    test.each([1, 2, 16])('should enter a complete next drawer of %i cards without creating a redundant zoom level', async function (entryCount) {
        cardCatalog.drawerById('drawer-b').entryCount = entryCount;
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue({
            from: 1,
            to: entryCount,
            items: Array.from({ length: entryCount }, (_, index) => ({ position: index + 1 })),
        });

        cardCatalog.shiftCard(1);
        await Promise.resolve();

        expect(cardCatalog.state).toEqual({ drawer: 'drawer-b', from: null, to: null, card: 1, chain: [] });
        expect(cardCatalog.$content.find('.archives-cardcatalog__back')).toHaveLength(0);
        expect(cardCatalog.$content.find('[data-chain]')).toHaveLength(0);
        expect(cardCatalog.$orientation.find('.archives-cardcatalog__path-step')).toHaveLength(1);
    });

    test.each([1, 2])('should enter a complete previous drawer of %i cards at its last card without a redundant zoom level', async function (entryCount) {
        cardCatalog.drawerById('drawer-a').entryCount = entryCount;
        cardCatalog.state = { drawer: 'drawer-b', from: 1, to: 16, card: 1, chain: [{ from: 1, to: 50 }] };
        cardCatalog.section = { from: 1, to: 16, items: [{ position: 1 }] };
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue({
            from: 1,
            to: entryCount,
            items: Array.from({ length: entryCount }, (_, index) => ({ position: index + 1 })),
        });

        cardCatalog.shiftCard(-1);
        await Promise.resolve();

        expect(cardCatalog.state).toEqual({ drawer: 'drawer-a', from: null, to: null, card: entryCount, chain: [] });
        expect(cardCatalog.$content.find('.archives-cardcatalog__back')).toHaveLength(0);
        expect(cardCatalog.$content.find('[data-chain]')).toHaveLength(0);
    });

    test.each([1, 2])('should retain only genuine zoom levels for a next drawer of %i cards with a threshold of one', async function (entryCount) {
        cardCatalog.info.config.fullViewThreshold = 1;
        cardCatalog.drawerById('drawer-b').entryCount = entryCount;
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue({ from: 1, to: 1, items: [{ position: 1 }] });

        cardCatalog.shiftCard(1);
        await Promise.resolve();

        expect(cardCatalog.state).toEqual({
            drawer: 'drawer-b',
            from: entryCount === 1 ? null : 1,
            to: entryCount === 1 ? null : 1,
            card: 1,
            chain: entryCount === 1 ? [] : [{ from: 1, to: entryCount }],
        });
        expect(cardCatalog.$orientation.find('.archives-cardcatalog__path-link')).toHaveLength(entryCount === 1 ? 0 : 1);
        expect(cardCatalog.$content.find('.archives-cardcatalog__back')).toHaveLength(entryCount === 1 ? 0 : 1);
    });

    test('should not leave the drawer when displaying its last card', function () {
        cardCatalog.state.card = 99;
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});

        cardCatalog.shiftCard(1);

        expect(cardCatalog.state.drawer).toBe('drawer-a');
        expect(cardCatalog.state.card).toBe(100);
        expect(render).not.toHaveBeenCalled();
        expect(cardCatalog.$content.find('[data-shift="1"]').prop('disabled')).toBe(false);
    });

    test('should keep previous and next enabled at internal drawer boundaries', function () {
        cardCatalog.updateStage({ position: 100 });
        expect(cardCatalog.$content.find('[data-shift="1"]').prop('disabled')).toBe(false);

        cardCatalog.state.drawer = 'drawer-b';
        cardCatalog.state.card = 1;
        cardCatalog.updateStage({ position: 1 });
        expect(cardCatalog.$content.find('[data-shift="-1"]').prop('disabled')).toBe(false);
    });

    test('should disable navigation only at absolute catalog boundaries', function () {
        cardCatalog.state.card = 1;
        cardCatalog.updateStage({ position: 1 });
        expect(cardCatalog.$content.find('[data-shift="-1"]').prop('disabled')).toBe(true);

        cardCatalog.state.drawer = 'drawer-b';
        cardCatalog.state.card = 50;
        cardCatalog.updateStage({ position: 50 });
        expect(cardCatalog.$content.find('[data-shift="1"]').prop('disabled')).toBe(true);
    });

    test('should not add history entries when moving beyond either end of the catalog', function () {
        const render = jest.spyOn(cardCatalog, 'render').mockImplementation(() => {});
        cardCatalog.state.card = 1;
        cardCatalog.shiftCard(-1);
        cardCatalog.state.drawer = 'drawer-b';
        cardCatalog.state.card = 50;
        cardCatalog.shiftCard(1);

        expect(cardCatalog.syncUrl).not.toHaveBeenCalled();
        expect(render).not.toHaveBeenCalled();
    });

    test('should update the active sidebar drawer and the location after crossing a drawer boundary', function () {
        cardCatalog.renderSidebar();
        jest.spyOn(cardCatalog, 'renderFullView').mockImplementation(() => {});

        cardCatalog.shiftCard(1);

        expect(cardCatalog.$drawers.find('[aria-current="true"]').attr('data-drawer')).toBe('drawer-b');
        expect(cardCatalog.$drawers.find('[data-drawer="drawer-a"]').hasClass('-active')).toBe(false);
        expect(cardCatalog.$orientation.text()).toContain('B');
        expect(cardCatalog.$orientation.text()).toContain('50');
        expect(cardCatalog.$orientation.text()).not.toContain('85');
    });
});

describe('viewerJS.cardCatalog translation bootstrap', function () {
    test('should request from the catalog view exactly the bundle keys the modules use', function () {
        const fs = require('fs');
        const path = require('path');
        const source = fs.readFileSync(path.join(__dirname, '../viewerJS.cardCatalog.js'), 'utf8');
        const view = fs.readFileSync(path.join(__dirname, '../../../../../../includes/archives/archiveCardCatalogView.xhtml'), 'utf8');
        const keyMaps = source.slice(source.indexOf('var _msgKeys'), source.indexOf('viewer.cardCatalog = {'));
        const moduleKeys = keyMaps.match(/'[a-z_]+'/g).map((key) => key.slice(1, -1));
        const requested = view
            .match(/getTranslations\(\s*\[([^\]]*)\]/)[1]
            .match(/"[^"]+"/g)
            .map((key) => key.slice(1, -1));

        expect(new Set(moduleKeys).size).toBe(moduleKeys.length);
        expect(new Set(requested).size).toBe(requested.length);
        expect([...requested].sort()).toEqual([...moduleKeys].sort());
    });
});
