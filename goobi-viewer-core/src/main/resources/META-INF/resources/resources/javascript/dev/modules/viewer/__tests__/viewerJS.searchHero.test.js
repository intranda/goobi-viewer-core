const searchHero = require('../viewerJS.searchHero.js');
const API = '/viewer/api/v1/';
const QUERIES = ['DC:maps', 'DC:books', 'DC:prints'];
const failedImages = new Set();
let pools;
let imageRequests;
let pendingImages;
let deferImages;

class FakeImage {
    set src(value) {
        this._src = value;
        const pi = /\/records\/([^/]+)\//.exec(value)[1];
        imageRequests.push({ pi, src: value });
        const load = () => {
            if (failedImages.has(pi)) this.onerror(new Error('image unavailable'));
            else this.onload();
        };
        if (deferImages) pendingImages.push(load);
        else queueMicrotask(load);
    }
    get src() {
        return this._src;
    }
}

function item(pi, label = pi, options = {}) {
    return Object.assign(
        {
            '@id': `https://x/api/v1/records/${pi}/manifest/`,
            label,
            thumbnail: { '@id': `https://x/api/v1/records/${pi}/files/images/00000001.tif/full/!400,400/0/default.jpg` },
            rendering: [{ '@id': `https://x/image/${pi}/`, format: 'text/html' }],
        },
        options
    );
}

function queryOf(url) {
    return new URL(url, 'https://x').searchParams.get('query');
}

function responseFor(url) {
    return { orderedItems: pools.get(queryOf(url)) || [] };
}

function markup(queries) {
    return `<div data-search-hero data-search-hero-api="${API}" data-search-hero-open-label="Werk öffnen">
        <div class="cms-search-hero__gallery">
            ${queries
                .map(
                    (query, i) => `<a class="cms-search-hero__tile" data-search-hero-slot data-search-hero-query="${query}"
                data-search-hero-area="${String.fromCharCode(97 + i)}" data-search-hero-size="${i ? 900 : 1200}" href="/viewer/browse/">
                <img class="cms-search-hero__tile-img" alt="" /><img class="cms-search-hero__tile-img" alt="" />
                <span data-search-hero-caption></span>
            </a>`
                )
                .join('')}
        </div>
    </div>`;
}

function buildPage(queries = QUERIES, count = 1) {
    document.body.innerHTML = Array.from({ length: count }, () => markup(queries)).join('');
    document.querySelectorAll('.cms-search-hero__gallery').forEach((gallery) => {
        jest.spyOn(gallery, 'getBoundingClientRect').mockReturnValue({ top: 10, bottom: 410, left: 10, right: 510 });
    });
}

const hero = (index = 0) => document.querySelectorAll('[data-search-hero]')[index];
const slots = (index = 0) => Array.from(hero(index).querySelectorAll('[data-search-hero-slot]'));
const shown = (index = 0) => slots(index).map((slot) => slot.getAttribute('data-search-hero-pi'));
const gallery = (index = 0) => hero(index).querySelector('.cms-search-hero__gallery');
const flush = async () => {
    for (let i = 0; i < 6; i++) await new Promise((resolve) => jest.requireActual('timers').setImmediate(resolve));
};
const advance = async (milliseconds = 10000) => {
    jest.advanceTimersByTime(milliseconds);
    await flush();
};

beforeEach(() => {
    jest.useFakeTimers({ doNotFake: ['queueMicrotask', 'nextTick', 'setImmediate'] });
    global.Image = FakeImage;
    window.matchMedia = jest.fn(() => ({ matches: false }));
    jest.spyOn(document, 'hidden', 'get').mockReturnValue(false);
    failedImages.clear();
    pendingImages = [];
    imageRequests = [];
    deferImages = false;
    pools = new Map(QUERIES.map((query, i) => [`ISWORK:true AND (${query})`, [1, 2, 3].map((number) => item(`${String.fromCharCode(97 + i)}${number}`))]));
    searchHero.shuffle = (records) => records;
    searchHero.request = jest.fn((url) => Promise.resolve(responseFor(url)));
});

afterEach(() => {
    searchHero.destroy();
    jest.restoreAllMocks();
    jest.useRealTimers();
});

describe('query pools', () => {
    it('uses each slot query independently, requests 18 works and sizes only its image', async () => {
        buildPage();
        searchHero.init();
        await flush();

        expect(searchHero.request).toHaveBeenCalledTimes(3);
        expect(searchHero.request.mock.calls.map(([url]) => queryOf(url))).toEqual(QUERIES.map((query) => `ISWORK:true AND (${query})`));
        searchHero.request.mock.calls.forEach(([url]) => {
            const parameters = new URL(url, 'https://x').searchParams;
            expect(parameters.get('rows')).toBe('18');
            expect(parameters.get('sort')).toMatch(/^random_\d+$/);
        });
        expect(shown()).toEqual(['a1', 'b1', 'c1']);
        expect(imageRequests[0].src).toContain('/full/!1200,1200/');
        expect(imageRequests[1].src).toContain('/full/!900,900/');
        expect(slots()[0].getAttribute('href')).toBe('https://x/image/a1/');
        expect(slots()[0].getAttribute('aria-label')).toBe('a1 – Werk öffnen');
        expect(slots()[0].querySelector('[data-search-hero-caption]').textContent).toBe('a1');
    });

    it('deduplicates identical requests and reserves distinct works even while their images are loading', async () => {
        pools.set(
            'ISWORK:true',
            [1, 2, 3, 4].map((i) => item(`shared${i}`))
        );
        deferImages = true;
        buildPage(['', ' ', '']);
        searchHero.init();
        await flush();

        expect(searchHero.request).toHaveBeenCalledTimes(1);
        expect(queryOf(searchHero.request.mock.calls[0][0])).toBe('ISWORK:true');
        expect(imageRequests.map((request) => request.pi)).toEqual(['shared1', 'shared2', 'shared3']);
        pendingImages.splice(0).forEach((load) => load());
        await flush();
        expect(new Set(shown()).size).toBe(3);
        deferImages = false;
        await advance();
        expect(shown()).toEqual(['shared4', 'shared2', 'shared3']);
    });

    it('reserves a delayed precise query before a broad pool and then loads all images in parallel', async () => {
        const queries = ['TYPE:any', 'PI:shared', 'TYPE:prints'];
        pools.set('ISWORK:true AND (TYPE:any)', [item('shared'), item('broad-other'), item('broad-third')]);
        pools.set('ISWORK:true AND (PI:shared)', [item('shared')]);
        pools.set('ISWORK:true AND (TYPE:prints)', [item('print1'), item('print2')]);
        let resolveSpecific;
        searchHero.request = jest.fn((url) => {
            if (queryOf(url).includes('PI:shared')) {
                return new Promise((resolve) => {
                    resolveSpecific = () => resolve(responseFor(url));
                });
            }
            return Promise.resolve(responseFor(url));
        });
        deferImages = true;
        buildPage(queries);
        searchHero.init();
        await flush();

        expect(imageRequests).toHaveLength(0);
        resolveSpecific();
        await flush();
        expect(imageRequests.map((request) => request.pi)).toEqual(['shared', 'print1', 'broad-other']);
        expect(pendingImages).toHaveLength(3);
        pendingImages.splice(0).forEach((load) => load());
        await flush();

        expect(shown()).toEqual(['broad-other', 'shared', 'print1']);
        slots().forEach((slot) => expect(slot.hidden).toBe(false));
    });

    it('normalizes supported IIIF labels and keeps separate issues from the same series', () => {
        const records = searchHero.normalize(
            [
                item('series_1', [{ '@value': ' First ' }], { rendering: undefined }),
                item('series_2', { value: 'Second' }),
                item('series_3', 'Third'),
                item('series_1', 'Duplicate'),
                item('blank', ' '),
                item('no-image', 'No image', { thumbnail: undefined }),
            ],
            '/viewer/'
        );

        expect(records.map((record) => record.pi)).toEqual(['series_1', 'series_2', 'series_3']);
        expect(records[0].label).toBe('First');
        expect(records[0].url).toBe('/viewer/image/series_1/1/');
        expect(records[1].label).toBe('Second');
    });

    it('retries unsupported random sorting with the same query', async () => {
        searchHero.request = jest.fn((url) => (url.includes('&sort=') ? Promise.reject(new Error('unknown random sort')) : Promise.resolve(responseFor(url))));
        buildPage();
        searchHero.init();
        await flush();

        expect(searchHero.request).toHaveBeenCalledTimes(6);
        QUERIES.forEach((query) => {
            const urls = searchHero.request.mock.calls.map(([url]) => url).filter((url) => queryOf(url) === `ISWORK:true AND (${query})`);
            expect(urls).toHaveLength(2);
            expect(urls[1]).not.toContain('&sort=');
        });
        expect(shown()).toEqual(['a1', 'b1', 'c1']);
    });
});

describe('loading and failures', () => {
    it('keeps loading frames out of keyboard navigation and waits for all initial images before rotating', async () => {
        deferImages = true;
        buildPage();
        searchHero.init();
        await flush();

        slots().forEach((slot) => {
            expect(slot.hasAttribute('href')).toBe(false);
            expect(slot.tabIndex).toBe(-1);
            expect(slot.getAttribute('aria-hidden')).toBe('true');
        });
        pendingImages.shift()();
        await flush();
        expect(slots()[0].hasAttribute('tabindex')).toBe(false);
        expect(slots()[0].hasAttribute('aria-hidden')).toBe(false);
        expect(slots()[0].classList.contains('-loaded')).toBe(true);
        await advance(60000);
        expect(imageRequests).toHaveLength(3);
        pendingImages.splice(0).forEach((load) => load());
        await flush();
        await advance(9999);
        expect(imageRequests).toHaveLength(3);
        await advance(1);
        expect(imageRequests).toHaveLength(4);
    });

    it('isolates a rejected slot query instead of borrowing works from another query', async () => {
        searchHero.request = jest.fn((url) => (queryOf(url).includes('DC:maps') ? Promise.reject(new Error('down')) : Promise.resolve(responseFor(url))));
        buildPage();
        searchHero.init();
        await flush();

        expect(shown()).toEqual([null, 'b1', 'c1']);
        expect(slots()[0].hidden).toBe(true);
        expect(slots()[0].classList.contains('-unavailable')).toBe(true);
        expect(hero().classList.contains('-gallery-empty')).toBe(false);
        await advance();
        expect(shown()).toEqual([null, 'b2', 'c1']);
    });

    it('retries broken images only within their slot pool', async () => {
        failedImages.add('a1');
        failedImages.add('a2');
        buildPage();
        searchHero.init();
        await flush();

        expect(shown()).toEqual(['a3', 'b1', 'c1']);
        expect(imageRequests.filter((request) => request.pi[0] === 'a').map((request) => request.pi)).toEqual(['a1', 'a2', 'a3']);
    });

    it.each(['empty pools', 'broken images'])('settles %s without an endless shimmer or timer', async (condition) => {
        if (condition === 'empty pools') pools.clear();
        else pools.forEach((pool) => pool.forEach((record) => failedImages.add(/\/records\/([^/]+)\//.exec(record['@id'])[1])));
        buildPage();
        searchHero.init();
        await flush();

        expect(hero().classList.contains('-gallery-empty')).toBe(true);
        slots().forEach((slot) => {
            expect(slot.hidden).toBe(true);
            expect(slot.classList.contains('-unavailable')).toBe(true);
            expect(slot.hasAttribute('href')).toBe(false);
        });
        expect(jest.getTimerCount()).toBe(0);
    });

    it('retains the current work when all later alternatives fail', async () => {
        buildPage();
        searchHero.init();
        await flush();
        failedImages.add('a2');
        failedImages.add('a3');

        await advance();

        expect(shown()).toEqual(['a1', 'b1', 'c1']);
        expect(slots()[0].hidden).toBe(false);
        expect(slots()[0].getAttribute('href')).toBe('https://x/image/a1/');
    });
});

describe('quiet rotation', () => {
    it('changes exactly one slot every ten seconds in order and stays within each query', async () => {
        buildPage();
        searchHero.init();
        await flush();

        await advance(9999);
        expect(shown()).toEqual(['a1', 'b1', 'c1']);
        await advance(1);
        expect(shown()).toEqual(['a2', 'b1', 'c1']);
        await advance();
        expect(shown()).toEqual(['a2', 'b2', 'c1']);
        await advance();
        expect(shown()).toEqual(['a2', 'b2', 'c2']);
        await advance();
        expect(shown()).toEqual(['a3', 'b2', 'c2']);
    });

    it('does not overlap slow image loads and waits ten seconds after completion', async () => {
        buildPage();
        searchHero.init();
        await flush();
        deferImages = true;

        await advance();
        expect(pendingImages).toHaveLength(1);
        await advance(60000);
        expect(imageRequests).toHaveLength(4);
        pendingImages.shift()();
        await flush();
        expect(shown()).toEqual(['a2', 'b1', 'c1']);
        await advance(9999);
        expect(imageRequests).toHaveLength(4);
        await advance(1);
        expect(imageRequests).toHaveLength(5);
    });

    it.each(['hidden', 'offscreen'])('skips image changes while %s and resumes when visible', async (reason) => {
        buildPage();
        searchHero.init();
        await flush();
        let resume;
        if (reason === 'hidden') {
            const hidden = jest.spyOn(document, 'hidden', 'get').mockReturnValue(true);
            resume = () => hidden.mockReturnValue(false);
        } else {
            gallery().getBoundingClientRect.mockReturnValue({ top: -500, bottom: -100, left: 10, right: 510 });
            resume = () => gallery().getBoundingClientRect.mockReturnValue({ top: 10, bottom: 410, left: 10, right: 510 });
        }

        await advance();
        expect(shown()).toEqual(['a1', 'b1', 'c1']);
        expect(imageRequests).toHaveLength(3);
        resume();
        await advance();
        expect(shown()).toEqual(['a2', 'b1', 'c1']);
    });

    it.each(['hover', 'focus'])('continues changing works during %s', async (interaction) => {
        buildPage();
        searchHero.init();
        await flush();
        if (interaction === 'hover') jest.spyOn(gallery(), 'matches').mockReturnValue(true);
        else slots()[1].focus();

        await advance();
        expect(shown()).toEqual(['a2', 'b1', 'c1']);
        await advance();
        expect(shown()).toEqual(['a2', 'b2', 'c1']);
    });

    it('completes an image change when focus enters during loading', async () => {
        buildPage();
        searchHero.init();
        await flush();
        deferImages = true;
        await advance();
        slots()[2].focus();

        pendingImages.shift()();
        await flush();

        expect(shown()).toEqual(['a2', 'b1', 'c1']);
        expect(slots()[0].getAttribute('href')).toBe('https://x/image/a2/');
    });

    it('does not schedule changes without a distinct available alternative', async () => {
        pools.set('ISWORK:true', [item('only')]);
        buildPage(['', '', '']);
        searchHero.init();
        await flush();

        expect(shown()).toEqual(['only', null, null]);
        expect(jest.getTimerCount()).toBe(0);
        await advance(60000);
        expect(imageRequests).toHaveLength(1);
    });

    it('keeps cycling with reduced motion; CSS suppresses the transitions', async () => {
        window.matchMedia.mockReturnValue({ matches: true });
        buildPage();
        searchHero.init();
        await flush();

        await advance();
        expect(shown()).toEqual(['a2', 'b1', 'c1']);
    });
});

describe('lifecycle', () => {
    it('supports multiple heroes independently while deduplicating their matching requests', async () => {
        buildPage(QUERIES, 2);
        searchHero.init();
        await flush();
        expect(searchHero.request).toHaveBeenCalledTimes(3);
        expect(shown(0)).toEqual(['a1', 'b1', 'c1']);
        expect(shown(1)).toEqual(['a1', 'b1', 'c1']);
        gallery(0).getBoundingClientRect.mockReturnValue({ top: -500, bottom: -100, left: 10, right: 510 });

        await advance();

        expect(shown(0)).toEqual(['a1', 'b1', 'c1']);
        expect(shown(1)).toEqual(['a2', 'b1', 'c1']);
    });

    it('does not refetch or duplicate timers when the same hero roots initialize again', async () => {
        buildPage(QUERIES, 2);
        searchHero.init();
        await flush();
        searchHero.init();
        await flush();
        expect(searchHero.request).toHaveBeenCalledTimes(3);
        expect(imageRequests).toHaveLength(6);
        searchHero.init();

        expect(jest.getTimerCount()).toBe(2);
        await advance();
        expect(shown(0)).toEqual(['a2', 'b1', 'c1']);
        expect(shown(1)).toEqual(['a2', 'b1', 'c1']);
        expect(imageRequests).toHaveLength(8);
    });

    it('reinitializes replaced hero roots and ignores the detached roots pending images', async () => {
        deferImages = true;
        buildPage();
        searchHero.init();
        await flush();
        const detachedHero = hero();
        const staleLoads = pendingImages.splice(0);
        pools = new Map(QUERIES.map((query, i) => [`ISWORK:true AND (${query})`, [item(`replacement${i}`)]]));
        buildPage();
        searchHero.init();
        await flush();
        pendingImages.splice(0).forEach((load) => load());
        staleLoads.forEach((load) => load());
        await flush();

        expect(searchHero.request).toHaveBeenCalledTimes(6);
        expect(shown()).toEqual(['replacement0', 'replacement1', 'replacement2']);
        expect(detachedHero.querySelectorAll('.-loaded')).toHaveLength(0);
    });

    it('ignores old image completions after destroy and reinitialization', async () => {
        deferImages = true;
        buildPage();
        searchHero.init();
        await flush();
        const staleLoads = pendingImages.splice(0);
        searchHero.destroy();
        pools = new Map(QUERIES.map((query, i) => [`ISWORK:true AND (${query})`, [item(`new${i}`)]]));
        searchHero.init();
        await flush();
        pendingImages.splice(0).forEach((load) => load());
        await flush();
        staleLoads.forEach((load) => load());
        await flush();

        expect(shown()).toEqual(['new0', 'new1', 'new2']);
        expect(jest.getTimerCount()).toBe(0);
    });

    it('does nothing on a page without heroes', () => {
        document.body.innerHTML = '<main></main>';

        expect(() => searchHero.init()).not.toThrow();
        expect(searchHero.request).not.toHaveBeenCalled();
        expect(jest.getTimerCount()).toBe(0);
    });
});
