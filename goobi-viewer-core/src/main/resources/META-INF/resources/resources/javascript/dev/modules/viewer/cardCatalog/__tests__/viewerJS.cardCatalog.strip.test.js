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

describe('viewerJS.cardCatalog.fillStrip', function () {
    let strip;
    let scrollHeight;

    beforeEach(function () {
        const items = [1, 2, 3, 4].map((position) => ({ position, label: String(position) }));
        cardCatalog.info = { drawers: [{ id: 'drawer-a', entryCount: 10 }], config: { fullViewThreshold: 4, listPageSize: 4 } };
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: {} };
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 4, card: 1, chain: [] };
        cardCatalog.section = { from: 1, to: 4, items: items };
        cardCatalog.$content = jQuery('<div><div class="archives-cardcatalog__stage-caption"></div><div data-cc-strip></div></div>');
        strip = cardCatalog.$content.find('[data-cc-strip]')[0];
        strip.innerHTML = items.map((item) => cardCatalog.renderStripThumb(item, 1)).join('');
        scrollHeight = 100;
        Object.defineProperty(strip, 'clientHeight', { configurable: true, get: () => 300 });
        Object.defineProperty(strip, 'scrollHeight', { configurable: true, get: () => scrollHeight });
        jest.spyOn(cardCatalog, 'fetchJson').mockImplementation((url) => {
            const params = new URL(url, 'https://example.org').searchParams;
            const from = Number(params.get('from'));
            const to = Number(params.get('to'));
            scrollHeight += 25 * (to - from + 1);
            return Promise.resolve({ items: Array.from({ length: to - from + 1 }, (_, index) => ({ position: from + index, label: String(from + index) })) });
        });
    });

    afterEach(function () {
        cardCatalog.section = null;
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        jest.restoreAllMocks();
    });

    const requestedRanges = () => cardCatalog.fetchJson.mock.calls.map((call) => call[0].replace(/.*from=/, ''));

    test('should load following pages until the strip overflows or the drawer ends', async function () {
        await cardCatalog.fillStrip();

        expect(requestedRanges()).toEqual(['5&to=8', '9&to=10']);
        expect(cardCatalog.section.from).toBe(1);
        expect(cardCatalog.section.to).toBe(10);
        expect(cardCatalog.$content.find('[data-stage]')).toHaveLength(10);
    });

    test('should fill backwards at the end of the drawer', async function () {
        cardCatalog.state.from = 7;
        cardCatalog.state.to = 10;
        cardCatalog.section = { from: 7, to: 10, items: [7, 8, 9, 10].map((position) => ({ position, label: String(position) })) };
        strip.innerHTML = cardCatalog.section.items.map((item) => cardCatalog.renderStripThumb(item, 7)).join('');

        await cardCatalog.fillStrip();

        expect(requestedRanges()).toEqual(['3&to=6', '1&to=2']);
        expect(cardCatalog.section.from).toBe(1);
        expect(cardCatalog.section.to).toBe(10);
    });

    test('should not load anything while the strip already overflows or has no layout', async function () {
        scrollHeight = 400;
        await cardCatalog.fillStrip();
        expect(cardCatalog.fetchJson).not.toHaveBeenCalled();

        scrollHeight = 100;
        Object.defineProperty(strip, 'clientHeight', { configurable: true, get: () => 0 });
        await cardCatalog.fillStrip();
        expect(cardCatalog.fetchJson).not.toHaveBeenCalled();
    });
});

describe('viewerJS.cardCatalog card format', function () {
    const ratio = () => document.querySelector('.archives-cardcatalog__wrapper').style.getPropertyValue('--_archives-cardcatalog-card-ratio');
    const thumb = (width, height) => {
        const image = document.createElement('img');
        Object.defineProperty(image, 'naturalWidth', { value: width });
        Object.defineProperty(image, 'naturalHeight', { value: height });
        const button = document.createElement('button');
        button.className = 'archives-cardcatalog__strip-thumb';
        button.appendChild(image);
        document.querySelector('[data-cc-strip]').appendChild(button);
        return image;
    };

    beforeEach(function () {
        sessionStorage.clear();
        document.body.innerHTML = '<div class="archives-cardcatalog__wrapper"><div data-cc="content"><div data-cc-strip="true"></div></div></div>';
        cardCatalog.config = { pi: 'catalog', msg: {} };
        cardCatalog.$content = jQuery('[data-cc="content"]');
    });

    afterEach(function () {
        cardCatalog.config = null;
        cardCatalog.cardRatio = null;
        sessionStorage.clear();
        document.body.innerHTML = '';
    });

    test('should shape the thumbnails after the first loaded card image and remember it for the session', function () {
        cardCatalog.initCardRatio();
        expect(ratio()).toBe('');

        thumb(400, 234).dispatchEvent(new Event('load'));
        expect(ratio()).toBe('400 / 234');
        expect(sessionStorage.getItem('cardCatalogRatio:catalog')).toBe('400 / 234');

        thumb(2224, 3306).dispatchEvent(new Event('load'));
        expect(ratio()).toBe('400 / 234');
    });

    test('should restore a remembered card format and ignore an invalid one', function () {
        sessionStorage.setItem('cardCatalogRatio:catalog', '2224 / 3306');
        cardCatalog.initCardRatio();
        expect(ratio()).toBe('2224 / 3306');

        cardCatalog.cardRatio = null;
        document.querySelector('.archives-cardcatalog__wrapper').style.removeProperty('--_archives-cardcatalog-card-ratio');
        sessionStorage.setItem('cardCatalogRatio:catalog', 'url(evil)');
        cardCatalog.initCardRatio();
        expect(ratio()).toBe('');
    });
});
