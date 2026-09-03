/** Unit tests for the search panel wiring: result rendering, stale responses, error state. */
import { jest } from '@jest/globals';
import { setupFulltextSearch } from '../ivSearchPanel.mjs';
import { Emitter } from '../ivViewer.mjs';

function fakeViewer() {
    return {
        getCurrentPages: () => [0],
        goToPage: jest.fn(),
        setHighlights: jest.fn(),
        clearHighlights: jest.fn(),
        onPageChange: new Emitter(),
    };
}

function mountMarkup() {
    document.body.innerHTML = `
        <aside id="immersivePanelSearch" class="immersive__panel immersive__panel-left">
            <form>
                <div class="input-group">
                    <input type="text" />
                    <div class="input-group-addon"><button type="submit"></button></div>
                </div>
            </form>
            <div id="immersiveSearchResults" data-label-empty="Keine Treffer" data-label-reset="Suche zurücksetzen" data-label-error="Suche fehlgeschlagen" hidden>
                <span id="immersiveHitCounter"></span>
                <button data-immersive-hit="prev"></button>
                <button data-immersive-hit="next"></button>
                <ul id="immersiveResultsList"></ul>
            </div>
        </aside>`;
    const viewer = fakeViewer();
    setupFulltextSearch(viewer, 'PI', '/api/v1');
    return {
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

function submit(form, input, term) {
    input.value = term;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
}

describe('setupFulltextSearch', () => {
    const realFetch = global.fetch;
    afterEach(() => {
        global.fetch = realFetch;
        document.body.innerHTML = '';
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
        expect(viewer.clearHighlights).toHaveBeenCalled();
        consoleError.mockRestore();
    });
});
