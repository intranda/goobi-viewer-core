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

describe('viewerJS.cardCatalog sidebar navigation', function () {
    let drawerBody;

    beforeEach(function () {
        document.body.innerHTML = `
            <div class="archives-cardcatalog__wrapper">
                <aside class="archives-cardcatalog__sidebar">
                    <div class="archives-cardcatalog__sidebar-body">
                        <div data-cc="counts"></div><div data-cc="drawers"></div>
                    </div>
                </aside>
                <main><div data-cc="content"></div></main>
                <aside class="archives-cardcatalog__sidebar archives-cardcatalog__sidebar--right">
                    <div class="archives-cardcatalog__sidebar-body"><div data-cc="register"></div></div>
                </aside>
            </div>`;
        drawerBody = document.querySelector('.archives-cardcatalog__sidebar-body');
        cardCatalog.$content = jQuery('[data-cc="content"]');
        cardCatalog.$drawers = jQuery('[data-cc="drawers"]');
        cardCatalog.$register = jQuery('[data-cc="register"]');
        cardCatalog.$counts = jQuery('[data-cc="counts"]');
        cardCatalog.$guide = jQuery('<div>');
        cardCatalog.config = { apiUrl: '/viewer/api/v1', pi: 'catalog', msg: { archives__card_catalog_letters: 'Anfangsbuchstaben' } };
        cardCatalog.info = { drawers: info.drawers, totalEntries: 1838, config: { fullViewThreshold: 16 } };
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: null, chain: [] };
        cardCatalog.renderSidebar();
    });

    afterEach(function () {
        cardCatalog.info = null;
        cardCatalog.state = null;
        cardCatalog.config = null;
        document.body.innerHTML = '';
        jest.restoreAllMocks();
    });

    test('should scroll the newly active drawer into view when it is outside the sidebar', function () {
        const active = cardCatalog.$drawers.find('[aria-current="true"]')[0];
        jest.spyOn(drawerBody, 'getBoundingClientRect').mockReturnValue({ top: 20 });
        Object.defineProperty(drawerBody, 'clientHeight', { configurable: true, value: 100 });
        jest.spyOn(active, 'getBoundingClientRect').mockReturnValue({ top: 300 });
        Object.defineProperty(active, 'offsetHeight', { configurable: true, value: 20 });

        cardCatalog.showActiveDrawer();

        expect(drawerBody.scrollTop).toBe(240);
    });

    test('should leave the scroll position alone when the active drawer is already visible', function () {
        const active = cardCatalog.$drawers.find('[aria-current="true"]')[0];
        jest.spyOn(drawerBody, 'getBoundingClientRect').mockReturnValue({ top: 20 });
        Object.defineProperty(drawerBody, 'clientHeight', { configurable: true, value: 100 });
        jest.spyOn(active, 'getBoundingClientRect').mockReturnValue({ top: 50 });
        Object.defineProperty(active, 'offsetHeight', { configurable: true, value: 20 });
        drawerBody.scrollTop = 70;

        cardCatalog.showActiveDrawer();

        expect(drawerBody.scrollTop).toBe(70);
    });

    test('should show only the selected letter group and jump to its first drawer', function () {
        cardCatalog.info.mode = 'REGISTER';
        cardCatalog.info.drawers = [
            { id: 'drawer-a1', label: 'A - Abels', entryCount: 100 },
            { id: 'drawer-a2', label: 'Abels - Abreu', entryCount: 100 },
            { id: 'drawer-b', label: 'Bach - Bauer', entryCount: 100 },
            { id: 'drawer-c', label: 'Cahn - Carl', entryCount: 100 },
            ...Array.from({ length: 24 }, (_, index) => ({ id: 'drawer-extra-' + index, label: 'A ' + index, entryCount: 100 })),
        ];
        cardCatalog.state.drawer = 'drawer-c';

        cardCatalog.renderSidebar();

        expect(
            cardCatalog.$drawers
                .find('[data-letter]')
                .map((_, element) => element.textContent)
                .get()
        ).toEqual(['A', 'B', 'C']);
        expect(cardCatalog.$drawers.find('.archives-cardcatalog__letters').attr('aria-label')).toBe('Anfangsbuchstaben');
        expect(cardCatalog.$drawers.find('[data-letter="C"]').attr('aria-pressed')).toBe('true');
        expect(
            cardCatalog.$drawers
                .find('[data-drawer]')
                .map((_, element) => element.getAttribute('data-drawer'))
                .get()
        ).toEqual(['drawer-c']);

        const selectDrawer = jest.spyOn(cardCatalog, 'selectDrawer').mockImplementation(() => {});
        cardCatalog.$drawers.find('[data-letter="A"]').trigger('click');
        expect(selectDrawer).toHaveBeenCalledWith('drawer-a1');
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

describe('viewerJS.cardCatalog right column', function () {
    const messages = {
        archives__card_catalog_catalog: 'Katalog',
        archives__card_catalog_cards: 'Zettel',
        archives__card_catalog_drawers: 'Schubladen',
        archives__card_catalog_index_date: 'Stand des Index: {0}',
        archives__card_catalog_selection: 'Aktuelle Auswahl',
        archives__card_catalog_drawer: 'Schublade',
        archives__card_catalog_interval: 'Intervall',
        archives__card_catalog_guide: 'So funktioniert es',
        archives__card_catalog_guide_drawer: 'Schublade wählen',
        archives__card_catalog_guide_drawer_text: 'Von „{0}“ bis „{1}“.',
        archives__card_catalog_guide_letters: 'Links zuerst den Anfangsbuchstaben wählen.',
        archives__card_catalog_guide_zoom: 'Bereich eingrenzen',
        archives__card_catalog_guide_zoom_text: 'Zwischen zwei Zetteln hineinzoomen, über den Zoompfad zurück.',
        archives__card_catalog_guide_zoom_steps: 'In höchstens {0} Stufen bei einzelnen Zetteln.',
        archives__card_catalog_guide_zoom_steps_one: 'Nach einer Stufe bei einzelnen Zetteln.',
        archives__card_catalog_guide_register: 'Oder oben im Register einen Bereich wählen.',
        archives__card_catalog_guide_read: 'Zettel lesen',
        archives__card_catalog_guide_read_text: 'Mit Zurück und Weiter blättern.',
        archives__card_catalog_guide_record: 'Der Titel führt zum Digitalisat.',
        archives__card_catalog_guide_feedback_text: 'Fragen zu einem Zettel? Über das Sprechblasen-Symbol.',
    };
    const drawers = (count, size) =>
        Array.from({ length: count }, (_, index) => ({
            id: 'd' + index,
            label: index === 0 ? 'A - Abels' : index === count - 1 ? 'Zverina - Zywoty' : 'M' + index,
            entryCount: size,
        }));

    beforeEach(function () {
        cardCatalog.config = { apiUrl: '/api/v1', pi: 'catalog', msg: messages };
        cardCatalog.info = {
            mode: 'LETTERS',
            totalEntries: 900,
            dateIndexed: Date.UTC(2026, 8, 29, 12),
            drawers: drawers(30, 195),
            config: { zoomFactor: 8, fullViewThreshold: 16 },
        };
        cardCatalog.state = { drawer: null, from: null, to: null, card: null, chain: [] };
        cardCatalog.$counts = jQuery('<div>');
        cardCatalog.$guide = jQuery('<div>');
    });

    afterEach(function () {
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        document.body.innerHTML = '';
    });

    const steps = () =>
        cardCatalog.$guide
            .find('.archives-cardcatalog__guide-step')
            .map((_, step) => step.textContent)
            .get();

    test('should derive drawer span, letter hint and the number of zoom levels from the catalog and its configuration', function () {
        cardCatalog.renderGuide();

        const text = steps();
        expect(text).toHaveLength(3);
        expect(text[0]).toContain('Von „A - Abels“ bis „Zverina - Zywoty“.');
        expect(text[0]).toContain('Links zuerst den Anfangsbuchstaben wählen.');
        expect(text[1]).toContain('Zwischen zwei Zetteln hineinzoomen, über den Zoompfad zurück.');
        expect(text[1]).toContain('In höchstens 2 Stufen bei einzelnen Zetteln.');
        expect(text[1]).not.toContain('Register');
        expect(cardCatalog.$guide.find('ol').attr('role')).toBe('list');
    });

    test('should adapt to small catalogs, a single zoom level and register mode', function () {
        cardCatalog.info.mode = 'REGISTER';
        cardCatalog.info.drawers = drawers(5, 100);
        cardCatalog.renderGuide();
        expect(steps()[0]).not.toContain('Anfangsbuchstaben');
        expect(steps()[1]).toContain('Nach einer Stufe bei einzelnen Zetteln.');
        expect(steps()[1]).toContain('Oder oben im Register einen Bereich wählen.');

        cardCatalog.info.drawers = drawers(5, 16);
        cardCatalog.renderGuide();
        expect(steps()).toHaveLength(2);
        expect(steps()[1]).toContain('Zettel lesen');
    });

    test('should add the question tip below the steps only when the contact modal is on the page', function () {
        cardCatalog.renderGuide();
        expect(cardCatalog.$guide.find('.archives-cardcatalog__guide-tip')).toHaveLength(0);

        document.body.innerHTML = '<div id="feedbackModal"></div>';
        cardCatalog.renderGuide();
        const $tip = cardCatalog.$guide.find('.archives-cardcatalog__guide-tip');
        expect(steps()).toHaveLength(3);
        expect($tip.text()).toContain('Fragen zu einem Zettel?');
        expect($tip.find('svg use').attr('href')).toContain('/message-question.svg#icon');
        expect($tip.find('svg').attr('aria-hidden')).toBe('true');
    });

    test('should mark the step that matches the current view and the steps before it as done', function () {
        const marks = () =>
            cardCatalog.$guide
                .find('.archives-cardcatalog__guide-step')
                .map((_, step) => (step.getAttribute('aria-current') === 'step' ? 'current' : step.classList.contains('-done') ? 'done' : '-'))
                .get();

        cardCatalog.renderGuide();
        expect(marks()).toEqual(['current', '-', '-']);

        cardCatalog.state = { drawer: 'd3', from: null, to: null, card: null, chain: [] };
        cardCatalog.renderGuide();
        expect(marks()).toEqual(['done', 'current', '-']);

        cardCatalog.state = { drawer: 'd3', from: 1, to: 16, card: 1, chain: [{ from: 1, to: 195 }] };
        cardCatalog.renderGuide();
        expect(marks()).toEqual(['done', 'done', 'current']);
    });

    test('should show the catalog figures as labelled values, the index date and the current selection', function () {
        cardCatalog.renderCounts();
        const stats = cardCatalog.$counts
            .find('.archives-cardcatalog__stat')
            .map((_, stat) => jQuery(stat).children('dt').text() + '=' + jQuery(stat).children('dd').text())
            .get();
        expect(stats).toEqual(['Zettel=900', 'Schubladen=30']);
        expect(cardCatalog.$counts.find('.archives-cardcatalog__facts').text()).toContain('2026');
        expect(cardCatalog.$counts.find('.archives-cardcatalog__selection')).toHaveLength(0);

        cardCatalog.state = { drawer: 'd3', from: 17, to: 32, card: 20, chain: [{ from: 1, to: 195 }] };
        cardCatalog.renderCounts();
        const rows = cardCatalog.$counts
            .find('.archives-cardcatalog__selection dt, .archives-cardcatalog__selection dd')
            .map((_, cell) => cell.textContent)
            .get();
        expect(rows).toEqual(['Schublade', 'M3', 'Zettel', '195', 'Intervall', '17 – 32']);
    });
});
