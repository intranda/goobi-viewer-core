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
                        <div data-cc="orientation"></div><div data-cc="drawers"></div>
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
        cardCatalog.$orientation = jQuery('[data-cc="orientation"]');
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

    test('should head the letters and, below them, the ranges of a grouped catalog', function () {
        cardCatalog.config.msg = { archives__card_catalog_letters: 'Buchstaben', archives__card_catalog_drawer: 'Bereich' };
        cardCatalog.info.drawers = Array.from({ length: 30 }, (_, index) => ({ id: 'drawer-' + index, label: 'A ' + index, entryCount: 100 }));
        cardCatalog.state.drawer = 'drawer-0';

        cardCatalog.renderSidebar();

        const parts = cardCatalog.$drawers
            .children()
            .map((_, element) => element.tagName.toLowerCase() + (element.matches('h2, h3') ? ':' + element.textContent : ''))
            .get();
        expect(parts).toEqual(['h2:Buchstaben', 'div', 'h3:Bereich', 'div']);
        expect(cardCatalog.$drawers.find('.archives-cardcatalog__drawer-group [data-drawer]')).toHaveLength(30);
    });

    test('should show a small catalog whose drawers are letters as the letter navigation without a range list', function () {
        cardCatalog.config.msg = { archives__card_catalog_letters: 'Buchstaben', archives__card_catalog_drawer: 'Bereich' };
        cardCatalog.info.drawers = [
            { id: 'drawer-a', label: 'A', entryCount: 1022 },
            { id: 'drawer-ij', label: 'I/J', entryCount: 816 },
        ];

        cardCatalog.renderSidebar();

        const parts = cardCatalog.$drawers
            .children()
            .map((_, element) => element.tagName.toLowerCase() + (element.matches('h2, h3') ? ':' + element.textContent : ''))
            .get();
        expect(parts).toEqual(['h2:Buchstaben', 'div']);
        const $letters = cardCatalog.$drawers.find('.archives-cardcatalog__letters .archives-cardcatalog__letter');
        expect($letters.map((_, letter) => letter.textContent).get()).toEqual(['A', 'I/J']);
        expect($letters.first().hasClass('-active')).toBe(true);
        expect($letters.first().attr('aria-current')).toBe('true');
        expect(cardCatalog.$drawers.find('.archives-cardcatalog__drawer-group')).toHaveLength(0);

        const selectDrawer = jest.spyOn(cardCatalog, 'selectDrawer').mockImplementation(() => {});
        $letters.last().trigger('click');
        expect(selectDrawer).toHaveBeenCalledWith('drawer-ij');
    });

    test('should head the ranges of a small catalog with named ranges without a letters heading', function () {
        cardCatalog.config.msg = { archives__card_catalog_letters: 'Buchstaben', archives__card_catalog_drawer: 'Bereich' };
        cardCatalog.info.drawers = [
            { id: 'drawer-a', label: 'Maier - Mayer', entryCount: 1022 },
            { id: 'drawer-ze', label: 'Ze', entryCount: 816 },
        ];

        cardCatalog.renderSidebar();

        const parts = cardCatalog.$drawers
            .children()
            .map((_, element) => element.tagName.toLowerCase() + (element.matches('h2, h3') ? ':' + element.textContent : ''))
            .get();
        expect(parts).toEqual(['h2:Bereich', 'div']);
        expect(cardCatalog.$drawers.find('.archives-cardcatalog__drawer-group [data-drawer]')).toHaveLength(2);
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
        archives__card_catalog_cards: 'Zettel',
        archives__card_catalog_drawer: 'Schublade',
        archives__card_catalog_location: 'Sie sind hier',
        archives__card_catalog_path_more: '{0} weitere Stufen anzeigen',
        archives__card_catalog_path_level: 'Stufe {0}',
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
        cardCatalog.$orientation = jQuery('<div>');
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

    test('should end with the steps', function () {
        cardCatalog.renderGuide();

        expect(steps()).toHaveLength(3);
        expect(cardCatalog.$guide.children().last().is('ol')).toBe(true);
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

    test('should stay empty without a drawer and then show the chosen drawer as the current location', function () {
        cardCatalog.renderOrientation();
        expect(cardCatalog.$orientation.children()).toHaveLength(0);

        cardCatalog.info.drawers[3].label = 'M <b>3</b>';
        cardCatalog.state = { drawer: 'd3', from: null, to: null, card: null, chain: [] };
        cardCatalog.renderOrientation();

        const $path = cardCatalog.$orientation.find('.archives-cardcatalog__path');
        expect($path.find('h2').text()).toBe('Sie sind hier');
        expect($path.attr('aria-labelledby')).toBe($path.find('h2').attr('id'));
        expect(pathSteps()).toEqual(['M <b>3</b> Zettel 1 – 195']);
        expect($path.find('b')).toHaveLength(0);
        expect($path.find('[aria-current="location"] .archives-cardcatalog__path-label').text()).toBe('M <b>3</b>');
        expect($path.find('[data-chain]')).toHaveLength(0);
        expect(cardCatalog.$orientation.find('dl')).toHaveLength(0);
    });

    const pathSteps = () =>
        cardCatalog.$orientation
            .find('.archives-cardcatalog__path-step')
            .map((_, step) => (step.hidden ? 'hidden:' : '') + step.textContent.trim())
            .get();

    test('should list the zoom levels below the chosen drawer with the current level last and jump back to earlier levels', function () {
        cardCatalog.state = {
            drawer: 'd3',
            from: 10,
            to: 20,
            card: null,
            chain: [
                { from: 1, to: 195 },
                { from: 1, to: 100 },
            ],
        };
        const chainJump = jest.spyOn(cardCatalog, 'chainJump').mockImplementation(() => {});

        cardCatalog.renderOrientation();

        const $path = cardCatalog.$orientation.find('.archives-cardcatalog__path');
        expect(pathSteps()).toEqual(['M3 Zettel 1 – 195', 'Stufe 1 Zettel 1 – 100', 'Stufe 2 Zettel 10 – 20']);
        $path.find('[data-chain], [aria-current="location"]').each((_, row) => {
            expect(
                jQuery(row)
                    .children()
                    .map((_, child) => child.className)
                    .get()
            ).toEqual(['archives-cardcatalog__path-text']);
            expect(
                jQuery(row)
                    .children()
                    .children()
                    .map((_, child) => child.className)
                    .get()
            ).toEqual(['archives-cardcatalog__path-label', 'archives-cardcatalog__path-range']);
        });
        expect(
            $path
                .find('[data-chain]')
                .map((_, link) => link.getAttribute('data-chain'))
                .get()
        ).toEqual(['-1', '1']);
        expect($path.find('[aria-current="location"] .archives-cardcatalog__path-label').text()).toBe('Stufe 2');
        expect(
            $path
                .find('.archives-cardcatalog__path-range .sr-only')
                .map((_, unit) => unit.textContent)
                .get()
        ).toEqual(['Zettel ', 'Zettel ', 'Zettel ']);
        expect($path.find('svg')).toHaveLength(0);

        $path.find('[data-chain="1"]').trigger('click');
        expect(chainJump).toHaveBeenCalledWith(1);
        jest.restoreAllMocks();
    });

    test('should fold the middle levels of a deep zoom path and unfold them on request', function () {
        const chain = [
            { from: 1, to: 195 },
            { from: 1, to: 100 },
            { from: 1, to: 50 },
            { from: 1, to: 25 },
            { from: 1, to: 12 },
            { from: 1, to: 6 },
        ];
        cardCatalog.state = { drawer: 'd3', from: 1, to: 3, card: null, chain: chain };

        cardCatalog.renderOrientation();

        expect(pathSteps()).toEqual([
            'M3 Zettel 1 – 195',
            'Stufe 1 Zettel 1 – 100',
            '3 weitere Stufen anzeigen',
            'hidden:Stufe 2 Zettel 1 – 50',
            'hidden:Stufe 3 Zettel 1 – 25',
            'hidden:Stufe 4 Zettel 1 – 12',
            'Stufe 5 Zettel 1 – 6',
            'Stufe 6 Zettel 1 – 3',
        ]);

        document.body.appendChild(cardCatalog.$orientation[0]);
        cardCatalog.$orientation.find('[data-path-more]').trigger('click');

        expect(pathSteps()).toEqual([
            'M3 Zettel 1 – 195',
            'Stufe 1 Zettel 1 – 100',
            'Stufe 2 Zettel 1 – 50',
            'Stufe 3 Zettel 1 – 25',
            'Stufe 4 Zettel 1 – 12',
            'Stufe 5 Zettel 1 – 6',
            'Stufe 6 Zettel 1 – 3',
        ]);
        expect(document.activeElement).toBe(cardCatalog.$orientation.find('[data-chain="2"]')[0]);
    });
});

describe('viewerJS.cardCatalog note variables', function () {
    const imageUrl = 'https://viewer.example/api/v1/records/CARD1/representative/full/!1200,1200/0/default.jpg';
    const card = { recordPi: 'CARD 1', imageUrl: imageUrl };

    beforeEach(function () {
        document.body.innerHTML =
            '<div data-cc="notes"><div class="widget-record-notes__body"><p>Bild: {IMAGE_URL} ' +
            '<a href="https://order.example/form.do?id=12&amp;card_link={IMAGE_URL}">Bestellen</a> ' +
            '<a href="%7BPAGE_URL%7D">Ansehen</a> <a href="https://viewer.example/hilfe/">Hilfe</a></p></div></div>';
        cardCatalog.config = { rootUrl: '/viewer', msg: {} };
        cardCatalog.initNotes();
    });

    afterEach(function () {
        cardCatalog.config = null;
        cardCatalog.$notes = null;
        cardCatalog.noteTemplates = null;
        document.body.innerHTML = '';
    });

    const links = () => Array.from(document.querySelectorAll('[data-cc="notes"] a'));

    test('should resolve the variables of the work notes for the card on the stage', function () {
        cardCatalog.renderNoteVariables(card);

        expect(links().map((link) => link.getAttribute('href'))).toEqual([
            'https://order.example/form.do?id=12&card_link=' + encodeURIComponent(imageUrl),
            window.location.origin + '/viewer/object/CARD%201/1/',
            'https://viewer.example/hilfe/',
        ]);
        expect(document.querySelector('[data-cc="notes"] p').textContent).toContain('Bild: ' + imageUrl + ' ');
    });

    test('should drop punctuation that would stand alone below a link row and keep real text after a link', function () {
        document.body.innerHTML =
            '<div data-cc="notes"><div class="widget-record-notes__body">' +
            '<p>Bestellen: <a href="https://order.example/">Bestellformular</a>. </p>' +
            '<p><a href="https://help.example/">Hilfe</a> zum Katalog</p></div></div>';
        cardCatalog.initNotes();

        const paragraphs = document.querySelectorAll('[data-cc="notes"] p');
        expect(paragraphs[0].lastChild.nodeName).toBe('A');
        expect(paragraphs[0].textContent).toBe('Bestellen: Bestellformular');
        expect(paragraphs[1].textContent).toBe('Hilfe zum Katalog');

        cardCatalog.renderNoteVariables(card);
        expect(document.querySelectorAll('[data-cc="notes"] p')[0].textContent).toBe('Bestellen: Bestellformular');
    });

    test('should show a work note with variables only while all of its values are known', function () {
        document.body.innerHTML =
            '<div data-cc="notes">' +
            '<div class="widget-record-notes"><div class="widget-record-notes__body"><a href="https://order.example/?card_link={IMAGE_URL}">Bestellen</a></div></div>' +
            '<div class="widget-record-notes"><div class="widget-record-notes__body"><a href="https://help.example/">Hilfe</a></div></div></div>';
        cardCatalog.initNotes();
        const notes = () => Array.from(document.querySelectorAll('.widget-record-notes')).map((note) => note.hidden);

        expect(notes()).toEqual([true, false]);

        cardCatalog.renderNoteVariables({ recordPi: 'CARD 1', imageUrl: null });
        expect(notes()).toEqual([true, false]);

        cardCatalog.renderNoteVariables(card);
        expect(notes()).toEqual([false, false]);
        expect(links()[0].getAttribute('href')).toBe('https://order.example/?card_link=' + encodeURIComponent(imageUrl));

        cardCatalog.renderNoteVariables(null);
        expect(notes()).toEqual([true, false]);
    });
});
