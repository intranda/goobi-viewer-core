/** Unit tests for the search panel wiring: result rendering, stale responses, error state. */
import { jest } from '@jest/globals';
import { setupSearchPanel } from '../ui/ivSearchPanel.mjs';
import { Emitter } from '../engine/ivViewer.mjs';
import { _clearCache } from '../data/ivLayoutTags.mjs';

function fakeViewer() {
    return {
        getCurrentPages: () => [0],
        goToPage: jest.fn(),
        setHighlights: jest.fn(),
        clearHighlights: jest.fn(),
        onPageChange: new Emitter(),
    };
}

function mountMarkup({ hitFilter = false } = {}) {
    const filter = hitFilter
        ? `<div id="immersiveHitFilter" hidden>
               <button type="button" data-immersive-hit-filter="all" aria-pressed="true">Alle <span class="immersive__results-filter-count"></span></button>
               <button type="button" data-immersive-hit-filter="annotations" aria-pressed="false">Annotationen <span class="immersive__results-filter-count"></span></button>
           </div>`
        : '';
    document.body.innerHTML = `
        <aside id="immersivePanelSearch" class="immersive__panel immersive__panel-left">
            <form>
                <div class="input-group">
                    <input type="text" />
                    <div class="input-group-addon"><button type="submit"></button></div>
                </div>
            </form>
            <div id="immersiveSearchResults" data-label-empty="Keine Treffer" data-label-reset="Suche zurücksetzen" data-label-error="Suche fehlgeschlagen" data-label-annotation="Annotation" hidden>
                ${filter}
                <span id="immersiveHitCounter"></span>
                <button data-immersive-hit="prev"></button>
                <button data-immersive-hit="next"></button>
                <ul id="immersiveResultsList"></ul>
            </div>
        </aside>`;
    const viewer = fakeViewer();
    const panel = setupSearchPanel(viewer, 'PI', '/api/v1');
    return {
        panel,
        viewer,
        input: document.querySelector('#immersivePanelSearch input'),
        form: document.querySelector('#immersivePanelSearch form'),
        counter: document.getElementById('immersiveHitCounter'),
        list: document.getElementById('immersiveResultsList'),
    };
}

const hitList = (n) => ({
    resources: Array.from({ length: n }, (_, i) => ({ '@id': `a${i}`, on: `/records/PI/pages/${i + 3}/canvas#xywh=1,2,3,4`, resource: { value: `Treffer ${i}` } })),
});
const response = (list) => ({ ok: true, text: async () => JSON.stringify(list) });
const deferred = () => {
    let resolve, reject;
    const promise = new Promise((res, rej) => ((resolve = res), (reject = rej)));
    return { promise, resolve, reject };
};
const flush = () => new Promise((r) => setTimeout(r, 0));
const annotationHits = {
    resources: [
        { '@id': 'a0', on: '/records/PI/pages/1/canvas#xywh=1,2,3,4', resource: { chars: 'x' }, layoutTag: 'gr_jacob_grimm', styleClass: 'alto-tag-color-1' },
        { '@id': 'a1', on: '/records/PI/pages/1/canvas#xywh=5,6,7,8', resource: { chars: 'x' }, layoutTag: 'gr_andere_linie' },
        { '@id': 'a2', on: '/records/PI/pages/1/canvas#xywh=9,9,9,9', resource: { chars: 'x' } },
    ],
};
const tagIndex = (tags) => ({ ok: true, json: async () => ({ pi: 'PI', pageCount: 1, tags }) });
const searchWithTagIndex = (indexResponse) => jest.fn(async (url) => (url.includes('/layout/tags/index') ? indexResponse : response(annotationHits)));
const tagOf = (item) => item.querySelector('.immersive__results-tag');
const filterBox = () => document.getElementById('immersiveHitFilter');
const filterBtn = (value) => document.querySelector(`[data-immersive-hit-filter="${value}"]`);
const filterCount = (value) => filterBtn(value).querySelector('.immersive__results-filter-count').textContent;

function submit(form, input, term) {
    input.value = term;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
}

describe('setupSearchPanel', () => {
    const realFetch = global.fetch;
    afterEach(() => {
        global.fetch = realFetch;
        document.body.innerHTML = '';
        _clearCache();
    });

    test('renders hits and jumps to the first one', async () => {
        global.fetch = jest.fn(async () => response(hitList(2)));
        const { viewer, form, input, counter, list } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        expect(counter.textContent).toBe('1 / 2');
        expect(list.querySelectorAll('.immersive__results-item')).toHaveLength(2);
        expect(viewer.goToPage).toHaveBeenCalledWith(2);
    });

    test('hits on the open page are drawn under the search owner', async () => {
        global.fetch = jest.fn(async () => response({ resources: [{ '@id': 'a0', on: '/records/PI/pages/1/canvas#xywh=1,2,3,4', resource: { value: 'x' } }] }));
        const { viewer, form, input } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        expect(viewer.setHighlights).toHaveBeenLastCalledWith([expect.objectContaining({ x: 1, y: 2, w: 3, h: 4, order: 0, active: true })], 'search');
    });

    test('hits in annotation blocks are drawn on the scan exactly like printed hits', async () => {
        global.fetch = searchWithTagIndex(tagIndex([]));
        const { viewer, form, input } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        const rects = viewer.setHighlights.mock.calls.at(-1)[0];
        expect(rects).toHaveLength(3);
        rects.forEach((r) => expect(r).not.toHaveProperty('className'));
    });

    test('the hit list names the tag of hits in annotation blocks, with a colour dot only for a configured style class', async () => {
        global.fetch = searchWithTagIndex(
            tagIndex([
                { label: 'gr_jacob_grimm', name: 'Jacob Grimm', styleClass: 'alto-tag-color-1', pages: [1] },
                { label: 'gr_andere_linie', name: 'Andere Linie', pages: [1] },
            ])
        );
        const { form, input, list } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        const [jacob, line, printed] = list.querySelectorAll('.immersive__results-item');
        expect(tagOf(jacob).textContent).toBe('Annotation Jacob Grimm');
        expect(tagOf(jacob).querySelector('.sr-only').textContent).toBe('Annotation ');
        expect(tagOf(jacob).querySelector('.immersive__layout-tags-dot.alto-tag-color-1')).not.toBeNull();
        expect(tagOf(line).textContent).toBe('Annotation Andere Linie');
        expect(tagOf(line).querySelector('.immersive__layout-tags-dot')).toBeNull();
        expect(tagOf(printed)).toBeNull();
    });

    test('without the tag index, hits in annotation blocks still read as annotations in the hit list', async () => {
        global.fetch = searchWithTagIndex({ ok: false, status: 500 });
        const { form, input, list } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        const [jacob, , printed] = list.querySelectorAll('.immersive__results-item');
        expect(tagOf(jacob).textContent).toBe('Annotation');
        expect(tagOf(jacob).querySelector('.sr-only')).toBeNull();
        expect(tagOf(printed)).toBeNull();
    });

    test('a search without hits in annotation blocks does not load the tag index', async () => {
        global.fetch = jest.fn(async () => response(hitList(2)));
        const { form, input } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        expect(global.fetch.mock.calls.some(([url]) => url.includes('/layout/tags/'))).toBe(false);
    });

    test('a hit standing alone on its line shows its matched word', async () => {
        global.fetch = jest.fn(async () =>
            response({
                resources: [{ '@id': 'a0', on: '/records/PI/pages/390/canvas#xywh=1,2,3,4', resource: { chars: 'Gotha.' } }],
                hits: [{ '@type': 'search:Hit', annotations: ['a0'], match: 'Gotha.' }],
            })
        );
        const { form, input, list } = mountMarkup();
        submit(form, input, 'Gotha');
        await flush();
        const snippet = list.querySelector('.immersive__results-snippet');
        expect(snippet.textContent).toBe('Gotha.');
        expect(snippet.querySelector('.immersive__results-match').textContent).toBe('Gotha.');
    });

    test('a slow older search never overwrites a newer one', async () => {
        const first = deferred();
        const second = deferred();
        global.fetch = jest.fn((url) => (url.includes('q=alt') ? first.promise : second.promise));
        const { viewer, form, input, counter, list } = mountMarkup();
        submit(form, input, 'alt');
        submit(form, input, 'neu');
        second.resolve(response(hitList(0)));
        await flush();
        first.resolve(response(hitList(1)));
        await flush();
        expect(counter.textContent).toBe('Keine Treffer');
        expect(list.querySelectorAll('.immersive__results-item')).toHaveLength(0);
        expect(viewer.goToPage).not.toHaveBeenCalled();
    });

    test('a failed request shows the error label instead of "no hits"', async () => {
        global.fetch = jest.fn(async () => {
            throw new Error('offline');
        });
        const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
        const { viewer, form, input, counter, list } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        expect(counter.textContent).toBe('Suche fehlgeschlagen');
        expect(list.textContent).toContain('Suche fehlgeschlagen');
        expect(list.textContent).not.toContain('Keine Treffer');
        expect(viewer.clearHighlights).toHaveBeenCalledWith('search');
        consoleError.mockRestore();
    });

    test('an HTTP error shows the error label, not "no hits"', async () => {
        global.fetch = jest.fn(async () => ({ ok: false, status: 500 }));
        const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
        const { viewer, form, input, counter, list } = mountMarkup();
        submit(form, input, 'wort');
        await flush();
        expect(counter.textContent).toBe('Suche fehlgeschlagen');
        expect(list.textContent).not.toContain('Keine Treffer');
        expect(viewer.clearHighlights).toHaveBeenCalledWith('search');
        consoleError.mockRestore();
    });

    test('the hit filter counts all hits and those in annotation blocks', async () => {
        global.fetch = searchWithTagIndex(tagIndex([]));
        const { form, input } = mountMarkup({ hitFilter: true });
        expect(filterBox().hidden).toBe(true);
        submit(form, input, 'wort');
        await flush();
        expect(filterBox().hidden).toBe(false);
        expect([filterCount('all'), filterCount('annotations')]).toEqual(['3', '2']);
        expect(filterBtn('all').getAttribute('aria-pressed')).toBe('true');
    });

    test('a search without hits in annotation blocks keeps the hit filter hidden', async () => {
        global.fetch = jest.fn(async () => response(hitList(2)));
        const { form, input } = mountMarkup({ hitFilter: true });
        submit(form, input, 'wort');
        await flush();
        expect(filterBox().hidden).toBe(true);
    });

    test('the annotations filter narrows list, counter and scan highlights without a new request', async () => {
        global.fetch = searchWithTagIndex(tagIndex([]));
        const { viewer, form, input, counter, list } = mountMarkup({ hitFilter: true });
        submit(form, input, 'wort');
        await flush();
        const requests = global.fetch.mock.calls.length;
        filterBtn('annotations').click();
        await flush();
        expect(global.fetch.mock.calls).toHaveLength(requests);
        expect(list.querySelectorAll('.immersive__results-item')).toHaveLength(2);
        expect(counter.textContent).toBe('1 / 2');
        expect(viewer.setHighlights.mock.calls.at(-1)[0]).toHaveLength(2);
        expect(filterBtn('annotations').getAttribute('aria-pressed')).toBe('true');
        expect(filterBtn('all').getAttribute('aria-pressed')).toBe('false');
    });

    test('switching the hit filter keeps the active hit when it is still listed', async () => {
        global.fetch = searchWithTagIndex(tagIndex([]));
        const { form, input, counter } = mountMarkup({ hitFilter: true });
        submit(form, input, 'wort');
        await flush();
        filterBtn('annotations').click();
        document.querySelector('[data-immersive-hit="next"]').click();
        expect(counter.textContent).toBe('2 / 2');
        filterBtn('all').click();
        expect(counter.textContent).toBe('2 / 3');
    });

    test('switching to annotations while a printed hit is active jumps to the first annotation hit', async () => {
        global.fetch = searchWithTagIndex(tagIndex([]));
        const { form, input, counter, list } = mountMarkup({ hitFilter: true });
        submit(form, input, 'wort');
        await flush();
        list.querySelectorAll('.immersive__results-item')[2].click();
        expect(counter.textContent).toBe('3 / 3');
        filterBtn('annotations').click();
        expect(counter.textContent).toBe('1 / 2');
        expect(list.querySelector('.immersive__results-item.-active .immersive__results-tag')).not.toBeNull();
    });

    test('a new search starts again on all hits, so a printed-only result never reads as "no hits"', async () => {
        global.fetch = jest.fn(async (url) => {
            if (url.includes('/layout/tags/index')) return tagIndex([]);
            return response(url.includes('q=leer') ? hitList(2) : annotationHits);
        });
        const { form, input, counter } = mountMarkup({ hitFilter: true });
        submit(form, input, 'wort');
        await flush();
        filterBtn('annotations').click();
        submit(form, input, 'leer');
        await flush();
        expect(counter.textContent).toBe('1 / 2');
        expect(filterBox().hidden).toBe(true);
        expect(filterBtn('all').getAttribute('aria-pressed')).toBe('true');
    });

    test('destroy detaches the form and removes the injected clear button', async () => {
        global.fetch = jest.fn(async () => response(hitList(1)));
        const { panel, form, input } = mountMarkup();
        expect(document.querySelector('.immersive__search-clear')).not.toBeNull();
        panel.destroy();
        expect(document.querySelector('.immersive__search-clear')).toBeNull();
        submit(form, input, 'wort');
        await flush();
        expect(global.fetch).not.toHaveBeenCalled();
    });
});
