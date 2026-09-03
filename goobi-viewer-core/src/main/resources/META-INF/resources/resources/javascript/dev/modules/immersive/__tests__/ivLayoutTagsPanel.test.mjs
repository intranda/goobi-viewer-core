/**
 * DOM tests for the layout-tag panel: lazy index load, pick menu, page range,
 * page-grouped hit list with visibility-driven detail batches, navigation and
 * image highlights.
 */
import { jest } from '@jest/globals';
import { setupLayoutTagsPanel } from '../ui/layoutTags/ivLayoutTagsPanel.mjs';
import { _clearCache } from '../data/ivLayoutTags.mjs';
import { createKeyDispatcher } from '../shared/ivKeys.mjs';
import { Emitter } from '../engine/ivViewer.mjs';

const INDEX = {
    pi: 'PI',
    pageCount: 626,
    tags: [
        { label: 'gr_unterstrichen', name: 'Unterstreichungen', pages: [7, 8, 25, 49, 300] },
        { label: 'gr_jacob_grimm', name: 'Jacob Grimm', styleClass: 'alto-annotation--jacob-grimm', pages: [8, 49] },
    ],
};

/** Page details as the range endpoint delivers them: page → label → rects. */
const DETAILS = {
    7: { gr_unterstrichen: ['10,10,50,20'] },
    8: { gr_unterstrichen: ['10,10,50,20', '10,30,50,40'], gr_jacob_grimm: ['100,100,300,200'] },
    49: { gr_jacob_grimm: ['0,0,0,0'] },
};

function detailsResponse(start, end) {
    const pages = [];
    for (let p = start; p <= end; p++) {
        const tags = Object.entries(DETAILS[p] || {}).map(([value, boxes]) => ({
            value,
            references: boxes.map((coordinates, i) => ({ id: `${p}-${i}`, coordinates, content: 'x', page: p })),
        }));
        pages.push({ pageOrder: p, tags });
    }
    return { pi: 'PI', pages };
}

function installFetch({ failIndex = false } = {}) {
    const calls = [];
    global.fetch = jest.fn((url) => {
        calls.push(url);
        if (url.includes('/layout/tags/index')) {
            return failIndex ? Promise.resolve({ ok: false, status: 500 }) : Promise.resolve({ ok: true, json: () => Promise.resolve(INDEX) });
        }
        const m = url.match(/start=(\d+)&end=(\d+)/);
        return Promise.resolve({ ok: true, json: () => Promise.resolve(detailsResponse(Number(m[1]), Number(m[2]))) });
    });
    return calls;
}

function mountMarkup() {
    document.body.innerHTML = `
        <div class="immersive">
            <aside id="immersivePanelLayoutTags" class="immersive__panel immersive__panel-left"
                data-label-page="Seite"
                data-label-pick="Layout-Tags wählen"
                data-label-pick-more="{0} und {1} weitere"
                data-label-pick-selected="Layout-Tags wählen, {0} gewählt"
                data-label-filter-none="Kein Treffer"
                data-label-none-selected="Oben wählen."
                data-label-empty="Keine Fundstellen."
                data-label-empty-range="Keine Fundstellen im Bereich."
                data-label-error="Fehler."
                data-label-occurrences="{0} Stellen"
                data-label-progress-loading="{0} von {1} Seiten geladen"
                data-label-progress-done="{0} Seiten geladen"
                data-label-progress-done-one="1 Seite geladen">
                <div class="immersive__layout-tags-sticky" data-immersive-layout-tags="sticky">
                    <div class="immersive__layout-tags-range">
                        <input type="number" data-immersive-layout-tags="from" />
                        <input type="number" data-immersive-layout-tags="to" />
                        <button type="button" data-immersive-layout-tags="apply" hidden>OK</button>
                        <button type="button" data-immersive-layout-tags="reset" hidden>x</button>
                    </div>
                    <div class="immersive__layout-tags-pick">
                        <button type="button" data-immersive-layout-tags="pick-trigger" aria-expanded="false">
                            <span data-immersive-layout-tags="pick-dots"></span>
                            <span data-immersive-layout-tags="pick-text">Layout-Tags wählen</span>
                        </button>
                        <div data-immersive-layout-tags="pick-menu" hidden>
                            <div data-immersive-layout-tags="filter" hidden><input type="text" /></div>
                            <div data-immersive-layout-tags="pick-list"></div>
                        </div>
                    </div>
                </div>
                <div data-immersive-layout-tags="loader" hidden></div>
                <div data-immersive-layout-tags="hits"></div>
                <p data-immersive-layout-tags="progress"></p>
            </aside>
        </div>`;
    return document.getElementById('immersivePanelLayoutTags');
}

function fakeViewer(current = [6]) {
    return {
        current,
        getPageCount: () => 626,
        getCurrentPages() {
            return this.current;
        },
        goToPage: jest.fn(),
        setHighlights: jest.fn(),
        clearHighlights: jest.fn(),
        onPageChange: new Emitter(),
        onOpen: new Emitter(),
    };
}

function fakePanels() {
    const hooks = [];
    return {
        hooks,
        closePanels: jest.fn(),
        registerCloseHook: (h) => hooks.push(h),
    };
}

const flush = async (n = 4) => {
    for (let i = 0; i < n; i++) await new Promise((r) => setTimeout(r, 0));
};

async function openPanel(panel) {
    panel.classList.add('is-open');
    panel.dispatchEvent(new CustomEvent('immersive:panel-open'));
    await flush();
}

const hits = () => Array.from(document.querySelectorAll('.immersive__layout-tags-hit'));
const checkbox = (label) => document.querySelector(`.immersive__layout-tags-pick-row[data-tag-label="${label}"] input`);

describe('setupLayoutTagsPanel', () => {
    let rectSpy;

    beforeEach(() => {
        _clearCache();
        rectSpy = jest.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({ top: 0, bottom: 500, left: 0, right: 300, width: 300, height: 500 });
    });

    afterEach(() => {
        rectSpy.mockRestore();
        delete global.fetch;
        document.body.innerHTML = '';
    });

    test('loads the index once on the first open and builds the pick menu', async () => {
        const calls = installFetch();
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        await openPanel(panel);

        expect(calls.filter((u) => u.endsWith('/layout/tags/index'))).toHaveLength(1);
        expect(calls[0]).toBe('/api/records/PI/layout/tags/index');
        expect(document.querySelectorAll('.immersive__layout-tags-pick-row')).toHaveLength(2);
        expect(document.querySelector('[data-immersive-layout-tags="filter"]').hidden).toBe(true);
        expect(document.querySelector('[data-immersive-layout-tags="hits"]').textContent).toBe('Oben wählen.');
        expect(document.querySelector('[data-immersive-layout-tags="from"]').value).toBe('1');
        expect(document.querySelector('[data-immersive-layout-tags="to"]').value).toBe('626');
    });

    test('selecting a tag lists its pages, fetches the cursor page first and the visible rows in range batches, then shows counts', async () => {
        const calls = installFetch();
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);

        checkbox('gr_unterstrichen').click();
        expect(hits().map((b) => b.dataset.pageNo)).toEqual(['7', '8', '25', '49', '300']);
        expect(hits().every((b) => b.classList.contains('-pending'))).toBe(true);
        await flush();

        const ranges = calls.filter((u) => u.includes('start='));
        expect(ranges).toEqual([
            '/api/records/PI/layout/tags/?start=7&end=7',
            '/api/records/PI/layout/tags/?start=8&end=8',
            '/api/records/PI/layout/tags/?start=25&end=25',
            '/api/records/PI/layout/tags/?start=49&end=49',
            '/api/records/PI/layout/tags/?start=300&end=300',
        ]);
        const page8 = hits().find((b) => b.dataset.pageNo === '8');
        expect(page8.classList.contains('-pending')).toBe(false);
        expect(page8.querySelector('.immersive__layout-tags-hit-count').textContent).toBe('(2)');
        expect(page8.getAttribute('aria-label')).toBe('Unterstreichungen, Seite 8, 2 Stellen');
        expect(document.querySelector('[data-immersive-layout-tags="progress"]').textContent).toBe('5 Seiten geladen');
        expect(document.querySelector('[data-immersive-layout-tags="pick-text"]').textContent).toBe('Unterstreichungen');
    });

    test('the cursor prefers the open page; a hit on another page navigates, on the open page it highlights all occurrences', async () => {
        installFetch();
        const panel = mountMarkup();
        const viewer = fakeViewer([7]);
        setupLayoutTagsPanel(viewer, 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        checkbox('gr_unterstrichen').click();
        checkbox('gr_jacob_grimm').click();
        await flush();

        const active = document.querySelector('.immersive__layout-tags-hit.-active');
        expect(active.dataset.spotKey).toBe('8#gr_unterstrichen');
        expect(active.getAttribute('aria-current')).toBe('true');
        expect(viewer.goToPage).not.toHaveBeenCalled();
        expect(viewer.setHighlights.mock.lastCall[1]).toBe('layout-tags');
        expect(viewer.setHighlights.mock.lastCall[0]).toEqual([
            { x: 10, y: 10, w: 40, h: 10, order: 7, className: '' },
            { x: 10, y: 30, w: 40, h: 10, order: 7, className: '' },
        ]);

        hits()
            .find((b) => b.dataset.spotKey === '8#gr_jacob_grimm')
            .click();
        expect(viewer.setHighlights.mock.lastCall[0]).toEqual([{ x: 100, y: 100, w: 200, h: 100, order: 7, className: 'alto-annotation--jacob-grimm' }]);
        expect(
            hits()
                .find((b) => b.dataset.spotKey === '8#gr_jacob_grimm')
                .classList.contains('alto-annotation--jacob-grimm')
        ).toBe(true);

        hits()
            .find((b) => b.dataset.spotKey === '49#gr_unterstrichen')
            .click();
        expect(viewer.goToPage).toHaveBeenCalledWith(48);
        viewer.current = [48];
        viewer.onPageChange.emit(48);
        expect(document.querySelector('.immersive__layout-tags-hit.-active').dataset.spotKey).toBe('49#gr_unterstrichen');
        expect(viewer.clearHighlights).toHaveBeenCalledWith('layout-tags');
    });

    test('the page range is applied explicitly, narrows the list and can be reset', async () => {
        installFetch();
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        checkbox('gr_unterstrichen').click();
        const from = document.querySelector('[data-immersive-layout-tags="from"]');
        const to = document.querySelector('[data-immersive-layout-tags="to"]');
        const apply = document.querySelector('[data-immersive-layout-tags="apply"]');
        const reset = document.querySelector('[data-immersive-layout-tags="reset"]');

        from.value = '60';
        from.dispatchEvent(new Event('input'));
        expect(apply.hidden).toBe(false);
        expect(hits()).toHaveLength(5);
        to.value = '40';
        to.dispatchEvent(new Event('input'));
        to.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

        expect(from.value).toBe('40');
        expect(to.value).toBe('60');
        expect(hits().map((b) => b.dataset.pageNo)).toEqual(['49']);
        expect(apply.hidden).toBe(true);
        expect(reset.hidden).toBe(false);
        expect(document.querySelector('.immersive__layout-tags-pick-count').textContent).toBe('1');

        reset.click();
        expect(from.value).toBe('1');
        expect(to.value).toBe('626');
        expect(reset.hidden).toBe(true);
        expect(hits()).toHaveLength(5);
    });

    test('out-of-range tags stay listed but disabled; the range-empty hint appears when nothing is left', async () => {
        installFetch();
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        checkbox('gr_unterstrichen').click();
        const from = document.querySelector('[data-immersive-layout-tags="from"]');
        const to = document.querySelector('[data-immersive-layout-tags="to"]');
        from.value = '100';
        to.value = '200';
        document.querySelector('[data-immersive-layout-tags="apply"]').click();

        expect(document.querySelector('[data-immersive-layout-tags="hits"]').textContent).toBe('Keine Fundstellen im Bereich.');
        expect(checkbox('gr_unterstrichen').disabled).toBe(false);
        expect(checkbox('gr_jacob_grimm').disabled).toBe(true);
        expect(checkbox('gr_jacob_grimm').closest('.immersive__layout-tags-pick-row').classList.contains('-empty')).toBe(true);
    });

    test('Escape closes the open pick menu before the panel; the close hook clears highlights', async () => {
        installFetch();
        const panel = mountMarkup();
        const viewer = fakeViewer();
        const panels = fakePanels();
        const keys = createKeyDispatcher();
        setupLayoutTagsPanel(viewer, 'PI', '/api', panels, keys);
        await openPanel(panel);
        const trigger = document.querySelector('[data-immersive-layout-tags="pick-trigger"]');
        const menu = document.querySelector('[data-immersive-layout-tags="pick-menu"]');

        trigger.click();
        expect(menu.hidden).toBe(false);
        expect(trigger.getAttribute('aria-expanded')).toBe('true');
        expect(document.activeElement).toBe(checkbox('gr_unterstrichen'));
        expect(keys.handleEvent(new KeyboardEvent('keydown', { key: 'Escape' }))).toBe(true);
        expect(menu.hidden).toBe(true);
        expect(document.activeElement).toBe(trigger);
        expect(keys.handleEvent(new KeyboardEvent('keydown', { key: 'Escape' }))).toBe(false);

        panels.hooks.forEach((h) => h());
        expect(viewer.clearHighlights).toHaveBeenCalledWith('layout-tags');
    });

    test('a batch arriving while the user scrolls does not pull the list back to the active hit', async () => {
        const release = [];
        global.fetch = jest.fn((url) => {
            if (url.includes('/layout/tags/index')) return Promise.resolve({ ok: true, json: () => Promise.resolve(INDEX) });
            const m = url.match(/start=(\d+)&end=(\d+)/);
            return new Promise((resolve) => release.push(() => resolve({ ok: true, json: () => Promise.resolve(detailsResponse(Number(m[1]), Number(m[2]))) })));
        });
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        checkbox('gr_unterstrichen').click();
        await flush();

        panel.scrollTop = 500;
        release.splice(0).forEach((r) => r());
        await flush();

        expect(panel.scrollTop).toBe(500);
    });

    test('arriving page details fill the existing rows in place instead of rebuilding the list', async () => {
        const release = [];
        global.fetch = jest.fn((url) => {
            if (url.includes('/layout/tags/index')) return Promise.resolve({ ok: true, json: () => Promise.resolve(INDEX) });
            const m = url.match(/start=(\d+)&end=(\d+)/);
            return new Promise((resolve) => release.push(() => resolve({ ok: true, json: () => Promise.resolve(detailsResponse(Number(m[1]), Number(m[2]))) })));
        });
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        checkbox('gr_unterstrichen').click();
        await flush();

        const rowBefore = hits().find((h) => h.dataset.spotKey === '8#gr_unterstrichen');
        expect(rowBefore.classList.contains('-pending')).toBe(true);
        release.splice(0).forEach((r) => r());
        await flush();

        const rowAfter = hits().find((h) => h.dataset.spotKey === '8#gr_unterstrichen');
        expect(rowAfter).toBe(rowBefore);
        expect(rowAfter.classList.contains('-pending')).toBe(false);
        expect(rowAfter.querySelector('.immersive__layout-tags-hit-count').textContent).toBe('(2)');
    });

    test('activating another hit still brings it into view', async () => {
        installFetch();
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        checkbox('gr_unterstrichen').click();
        await flush();

        panel.scrollTop = 500;
        hits()
            .find((h) => h.dataset.spotKey === '25#gr_unterstrichen')
            .click();

        expect(panel.scrollTop).toBe(0);
    });

    test('a failed index request shows the error text instead of throwing', async () => {
        installFetch({ failIndex: true });
        const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);

        expect(document.querySelector('[data-immersive-layout-tags="hits"]').textContent).toBe('Fehler.');
        expect(document.querySelector('[data-immersive-layout-tags="loader"]').hidden).toBe(true);
        errorSpy.mockRestore();
    });

    test('a failed detail batch marks its rows as errors instead of "no occurrences"', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
        global.fetch = jest.fn((url) => {
            if (url.includes('/layout/tags/index')) return Promise.resolve({ ok: true, json: () => Promise.resolve(INDEX) });
            const m = url.match(/start=(\d+)&end=(\d+)/);
            if (m[1] === '7') return Promise.resolve({ ok: false, status: 500 });
            return Promise.resolve({ ok: true, json: () => Promise.resolve(detailsResponse(Number(m[1]), Number(m[2]))) });
        });
        const panel = mountMarkup();
        setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher());
        await openPanel(panel);
        checkbox('gr_unterstrichen').click();
        await flush();
        const page7 = hits().find((b) => b.dataset.pageNo === '7');
        expect(page7.classList.contains('-error')).toBe(true);
        expect(page7.classList.contains('-pending')).toBe(false);
        expect(page7.querySelector('.immersive__layout-tags-hit-count').textContent).toBe('!');
        expect(page7.getAttribute('aria-label')).toBe('Unterstreichungen, Seite 7, Fehler.');
        expect(document.querySelector('[data-immersive-layout-tags="progress"]').textContent).toBe('Fehler.');
        warn.mockRestore();
    });

    test('bails without the panel markup', () => {
        document.body.innerHTML = '<div class="immersive"></div>';
        expect(() => setupLayoutTagsPanel(fakeViewer(), 'PI', '/api', fakePanels(), createKeyDispatcher())).not.toThrow();
    });
});
