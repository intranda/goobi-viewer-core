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

describe('viewerJS.cardCatalog stage ownership', function () {
    test('should not open a stale image after leaving the stage', async function () {
        document.body.innerHTML = '<div id="ccStageViewer"></div>';
        cardCatalog.config = { osdScriptUrl: '/osd.js', msg: {} };
        cardCatalog.$content = jQuery('<div>');
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

describe('viewerJS.cardCatalog full view presentation', function () {
    const fullImage = 'https://example.org/iiif/card/full/!1200,1200/0/default.jpg';
    const boundaryItems = [1, 34, 67, 100].map((position) => ({ position, label: 'Card ' + position, imageUrl: fullImage, thumbnailUrl: '/thumb.jpg' }));

    beforeEach(function () {
        cardCatalog.info = {
            drawers: [{ id: 'drawer-a', label: 'A', entryCount: 100 }],
            totalEntries: 100,
            config: { fullViewThreshold: 16, listPageSize: 16, zoomFactor: 8 },
        };
        cardCatalog.config = {
            apiUrl: '/api/v1',
            pi: 'catalog',
            msg: {
                archives__card_catalog_back: 'Eine Zoomstufe zurück',
                archives__card_catalog_card: 'Zettel',
                archives__card_catalog_cards: 'Zettel',
                archives__card_catalog_of: 'von',
                archives__card_catalog_drawer: 'Schublade',
                archives__card_catalog_complete_cards: 'Ganze Karten',
                archives__card_catalog_show_full_cards: 'Ganze Karten anzeigen',
                archives__card_catalog_zoom_help: 'Zoom-Hilfe',
                archives__card_catalog_zoom_hint: 'Bereich zwischen zwei Zetteln auswählen.',
                archives__card_catalog_path_hint: 'Frühere Zoomstufe auswählen, um zurückzuspringen.',
                archives__card_catalog_feedback: 'Frage zu diesem Zettel',
                archives__card_catalog_feedback_message: 'Zettel „{0}“ (Schublade {1}, Zettel {2})\n{3}\n\n',
                previous: 'Zurück',
                fullscreen_enter: 'Vollbild',
                fullscreen_exit: 'Vollbild verlassen',
            },
        };
        cardCatalog.state = { drawer: 'drawer-a', from: null, to: null, card: null, chain: [] };
        cardCatalog.showFullPreviews = false;
        cardCatalog.overviewPositions = new Map();
        cardCatalog.$content = jQuery('<div><div></div></div>').children();
        cardCatalog.$drawers = jQuery('<div>');
        cardCatalog.$register = jQuery('<div>');
        cardCatalog.$orientation = jQuery('<div>');
        cardCatalog.$guide = jQuery('<div>');
        jest.spyOn(cardCatalog, 'fetchJson').mockResolvedValue({ from: 1, to: 100, items: boundaryItems });
        jest.spyOn(cardCatalog, 'syncUrl').mockImplementation(() => {});
        jest.spyOn(cardCatalog, 'showStageImage').mockImplementation(() => {});
    });

    afterEach(function () {
        cardCatalog.cancelRender();
        cardCatalog.resetContent();
        jQuery(document).off('fullscreenchange.cardCatalog');
        jQuery(document).off('keydown.cardCatalogFullscreen');
        cardCatalog.info = null;
        cardCatalog.config = null;
        cardCatalog.state = null;
        cardCatalog.showFullPreviews = false;
        cardCatalog.overviewPositions = null;
        jest.restoreAllMocks();
    });

    async function renderOverview() {
        cardCatalog.renderZoomView(cardCatalog.currentDrawer(), 1, 100);
        await Promise.resolve();
    }

    test('should show the digitized record hint in the guide only while a linked card is displayed', async function () {
        cardCatalog.renderGuide();
        cardCatalog.state.card = 1;
        cardCatalog.fetchJson.mockResolvedValue({
            from: 1,
            to: 16,
            items: [
                { position: 1, label: 'Linked', recordPi: 'PPN1' },
                { position: 2, label: 'Plain' },
            ],
        });

        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();
        expect(cardCatalog.$guide.find('[data-cc-guide-record]').prop('hidden')).toBe(false);

        cardCatalog.selectCard(2);
        expect(cardCatalog.$guide.find('[data-cc-guide-record]').prop('hidden')).toBe(true);
    });

    test('should load the next strip page on wheel while the strip cannot scroll', async function () {
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 4, card: 1, chain: [{ from: 1, to: 100 }] };
        cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 4, items: [1, 2, 3, 4].map((position) => ({ position, label: 'Card ' + position })) });

        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 4);
        await Promise.resolve();
        expect(cardCatalog.fetchJson).toHaveBeenCalledTimes(1);

        cardCatalog.fetchJson.mockResolvedValue({ items: [] });
        const $strip = cardCatalog.$content.find('[data-cc-strip]');
        $strip.trigger(jQuery.Event('wheel', { originalEvent: { deltaY: 100, deltaX: 0, deltaMode: 0 } }));
        expect(cardCatalog.fetchJson).toHaveBeenLastCalledWith(expect.stringContaining('entries?from=5&to=20'), 'entries', expect.anything());
        await Promise.resolve();

        $strip.trigger(jQuery.Event('wheel', { originalEvent: { deltaY: -100, deltaX: 0, deltaMode: 0 } }));
        expect(cardCatalog.fetchJson).toHaveBeenCalledTimes(2);
    });

    test('should show the selected drawer and card position without relying on the thumbnail strip', async function () {
        cardCatalog.state = { drawer: 'drawer-a', from: 85, to: 100, card: 100, chain: [{ from: 1, to: 100 }] };
        cardCatalog.fetchJson.mockResolvedValue({ from: 85, to: 100, items: [{ position: 100, label: 'Last card' }] });

        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 85, 100);
        await Promise.resolve();

        expect(cardCatalog.$content.find('.archives-cardcatalog__location').text()).toContain('A');
        expect(cardCatalog.$content.find('[data-cc-location]').text()).toBe('Zettel 100 von 100');
        expect(cardCatalog.$content.find('[data-cc-location]').attr('aria-live')).toBe('polite');
    });

    test('should put the thumbnail strip before the main stage in reading order', async function () {
        cardCatalog.state.card = 1;
        cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 1, label: 'First card' }] });

        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();

        const $row = cardCatalog.$content.find('.archives-cardcatalog__stage-row');
        expect($row.children().first().is('[data-cc-strip]')).toBe(true);
        expect($row.children().last().hasClass('archives-cardcatalog__stage')).toBe(true);
    });

    test('should split the stage toolbar below the image into tools and card navigation', async function () {
        cardCatalog.state.card = 1;
        cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 1, label: 'First card' }] });

        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();

        const $fullview = cardCatalog.$content.find('.archives-cardcatalog__fullview');
        expect($fullview.children().last().hasClass('archives-cardcatalog__stage-footer')).toBe(true);
        expect($fullview.find('.archives-cardcatalog__stage-footer [data-cc-caption] .archives-cardcatalog__stage-position').text()).toBe('Zettel 1 von 100');
        const $groups = $fullview.find('.archives-cardcatalog__stage-footer .archives-cardcatalog__stage-actions').children('.archives-cardcatalog__stage-group');
        expect($groups).toHaveLength(2);
        expect(
            $groups
                .first()
                .children()
                .map((_, button) => button.getAttribute('data-cc-fullscreen'))
                .get()
        ).toEqual(['true']);
        expect(
            $groups
                .last()
                .children()
                .map((_, button) => button.getAttribute('data-shift'))
                .get()
        ).toEqual(['-1', '1']);
        expect($groups.last().hasClass('archives-cardcatalog__stage-group--navigation')).toBe(true);
        expect($groups.find('button').filter('.archives-cardcatalog__stage-action')).toHaveLength(3);
        const $fullscreen = $groups.find('[data-cc-fullscreen]');
        expect($fullscreen.hasClass('archives-cardcatalog__stage-action--icon')).toBe(true);
        expect($fullscreen.attr('aria-label')).toBe('Vollbild');
        expect($fullscreen.attr('title')).toBe('Vollbild');
        expect($fullview.find('.archives-cardcatalog__content-header [data-cc-fullscreen]')).toHaveLength(0);
    });

    test('should offer a contact action between fullscreen and card navigation only when the contact modal exists', async function () {
        cardCatalog.state.card = 1;
        cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 1, label: 'First card' }] });

        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();
        expect(cardCatalog.$content.find('[data-cc-feedback]')).toHaveLength(0);

        const $modal = jQuery('<div id="feedbackModal"><textarea id="feedbackModal:feedback-modal__feedbackMessage"></textarea></div>').appendTo(document.body);
        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();

        const $tools = cardCatalog.$content.find('.archives-cardcatalog__stage-footer .archives-cardcatalog__stage-group').first();
        const $button = $tools.children('button[data-cc-feedback]');
        expect($button).toHaveLength(1);
        expect($button.hasClass('archives-cardcatalog__stage-action')).toBe(true);
        expect($button.hasClass('archives-cardcatalog__stage-action--icon')).toBe(true);
        expect($button.prev().is('[data-cc-fullscreen]')).toBe(true);
        expect($button.next()).toHaveLength(0);
        expect($button.text()).toBe('');
        expect($button.attr('aria-label')).toBe('Frage zu diesem Zettel');
        expect($button.attr('title')).toBe('Frage zu diesem Zettel');
        expect($button.find('svg use').attr('href')).toContain('/message-question.svg#icon');
        $modal.remove();
    });

    test('should prefill the contact message with the displayed card and its deep link before opening the modal', async function () {
        const $modal = jQuery('<div id="feedbackModal"><textarea id="feedbackModal:feedback-modal__feedbackMessage"></textarea></div>').appendTo(document.body);
        const changed = jest.fn();
        $modal.find('textarea').on('change', changed);
        jQuery.fn.modal = jest.fn();
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 16, card: 1, chain: [{ from: 1, to: 100 }] };
        cardCatalog.fetchJson.mockResolvedValue({
            from: 1,
            to: 16,
            items: [
                { position: 1, label: 'First card' },
                { position: 2, label: 'Second <card> & more' },
            ],
        });

        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();
        cardCatalog.$content.find('[data-cc-feedback]').trigger('click');

        expect($modal.find('textarea').val()).toBe('Zettel „First card“ (Schublade A, Zettel 1)\nhttp://localhost/?drawer=drawer-a&from=1&to=16&card=1\n\n');
        expect(changed).toHaveBeenCalledTimes(1);
        expect(jQuery.fn.modal).toHaveBeenCalledWith('show');
        expect(jQuery.fn.modal.mock.contexts[0].is('#feedbackModal')).toBe(true);

        cardCatalog.selectCard(2);
        cardCatalog.$content.find('[data-cc-feedback]').trigger('click');

        expect($modal.find('textarea').val()).toBe('Zettel „Second <card> & more“ (Schublade A, Zettel 2)\nhttp://localhost/?drawer=drawer-a&from=1&to=16&card=2\n\n');
        delete jQuery.fn.modal;
        $modal.remove();
    });

    test('should leave fullscreen before opening the contact modal', async function () {
        const $modal = jQuery('<div id="feedbackModal"><textarea id="feedbackModal:feedback-modal__feedbackMessage"></textarea></div>').appendTo(document.body);
        jQuery.fn.modal = jest.fn();
        cardCatalog.$content = jQuery('<div class="archives-cardcatalog__wrapper"><div></div></div>').children();
        cardCatalog.state.card = 1;
        cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 1, label: 'First card' }] });
        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();
        const toggle = jest.spyOn(cardCatalog, 'toggleFullscreen');
        const $wrapper = cardCatalog.$content.closest('.archives-cardcatalog__wrapper');

        cardCatalog.$content.find('[data-cc-feedback]').trigger('click');
        expect(toggle).not.toHaveBeenCalled();

        cardCatalog.$content.find('[data-cc-fullscreen]').trigger('click');
        expect(toggle).toHaveBeenCalledTimes(1);
        expect($wrapper.hasClass('archives-cardcatalog__wrapper--fullscreen')).toBe(true);
        cardCatalog.$content.find('[data-cc-feedback]').trigger('click');

        expect(toggle).toHaveBeenCalledTimes(2);
        expect($wrapper.hasClass('archives-cardcatalog__wrapper--fullscreen')).toBe(false);
        expect(jQuery.fn.modal).toHaveBeenCalledTimes(2);
        delete jQuery.fn.modal;
        $modal.remove();
    });

    test('should return from fullscreen to the same card and zoom range', async function () {
        const requestDescriptor = Object.getOwnPropertyDescriptor(Element.prototype, 'requestFullscreen');
        const enabledDescriptor = Object.getOwnPropertyDescriptor(document, 'fullscreenEnabled');
        const elementDescriptor = Object.getOwnPropertyDescriptor(document, 'fullscreenElement');
        const exitDescriptor = Object.getOwnPropertyDescriptor(document, 'exitFullscreen');
        const restore = (target, name, descriptor) => {
            if (descriptor) {
                Object.defineProperty(target, name, descriptor);
            } else {
                delete target[name];
            }
        };
        const requestFullscreen = jest.fn(function () {
            Object.defineProperty(document, 'fullscreenElement', { configurable: true, value: this });
            document.dispatchEvent(new Event('fullscreenchange'));
            return Promise.resolve();
        });
        const exitFullscreen = jest.fn(function () {
            Object.defineProperty(document, 'fullscreenElement', { configurable: true, value: null });
            document.dispatchEvent(new Event('fullscreenchange'));
            return Promise.resolve();
        });

        try {
            Object.defineProperty(Element.prototype, 'requestFullscreen', { configurable: true, value: requestFullscreen });
            Object.defineProperty(document, 'fullscreenEnabled', { configurable: true, value: true });
            Object.defineProperty(document, 'exitFullscreen', { configurable: true, value: exitFullscreen });
            cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 16, card: 13, chain: [{ from: 1, to: 100 }] };
            cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 13, label: 'Card 13' }] });
            cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
            await Promise.resolve();

            const state = JSON.parse(JSON.stringify(cardCatalog.state));
            const $button = cardCatalog.$content.find('[data-cc-fullscreen]');
            const fullview = cardCatalog.$content.find('.archives-cardcatalog__fullview')[0];
            expect($button.attr('aria-label')).toBe('Vollbild');

            $button.trigger('click');
            expect(requestFullscreen.mock.instances[0]).toBe(fullview);
            expect($button.attr('aria-pressed')).toBe('true');
            expect($button.attr('aria-label')).toBe('Vollbild verlassen');

            $button.trigger('click');
            expect(exitFullscreen).toHaveBeenCalledTimes(1);
            expect($button.attr('aria-pressed')).toBe('false');
            expect($button.attr('aria-label')).toBe('Vollbild');

            $button.trigger('click');
            Object.defineProperty(document, 'fullscreenElement', { configurable: true, value: null });
            document.dispatchEvent(new Event('fullscreenchange'));
            expect($button.attr('aria-pressed')).toBe('false');
            expect(cardCatalog.state).toEqual(state);
            expect(cardCatalog.$content.find('[data-cc-location]').text()).toBe('Zettel 13 von 100');
        } finally {
            restore(Element.prototype, 'requestFullscreen', requestDescriptor);
            restore(document, 'fullscreenEnabled', enabledDescriptor);
            restore(document, 'fullscreenElement', elementDescriptor);
            restore(document, 'exitFullscreen', exitDescriptor);
        }
    });

    test('should leave the viewport fullscreen fallback on Escape', async function () {
        const enabledDescriptor = Object.getOwnPropertyDescriptor(document, 'fullscreenEnabled');
        Object.defineProperty(document, 'fullscreenEnabled', { configurable: true, value: false });

        try {
            const $wrapper = jQuery('<div class="archives-cardcatalog__wrapper"><div data-cc="content"></div></div>');
            cardCatalog.$content = $wrapper.find('[data-cc="content"]');
            cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 16, card: 13, chain: [{ from: 1, to: 100 }] };
            cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 13, label: 'Card 13' }] });
            cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
            await Promise.resolve();

            const state = JSON.parse(JSON.stringify(cardCatalog.state));
            const $button = cardCatalog.$content.find('[data-cc-fullscreen]');
            $button.trigger('click');
            expect($wrapper.hasClass('archives-cardcatalog__wrapper--fullscreen')).toBe(true);
            expect($button.attr('aria-pressed')).toBe('true');

            jQuery(document).trigger(jQuery.Event('keydown', { key: 'Escape' }));
            expect($wrapper.hasClass('archives-cardcatalog__wrapper--fullscreen')).toBe(false);
            expect($button.attr('aria-pressed')).toBe('false');
            expect(cardCatalog.state).toEqual(state);
        } finally {
            if (enabledDescriptor) {
                Object.defineProperty(document, 'fullscreenEnabled', enabledDescriptor);
            } else {
                delete document.fullscreenEnabled;
            }
        }
    });

    test('should update the visible position and current zoom level when selecting a card outside the current URL window', async function () {
        cardCatalog.state = { drawer: 'drawer-a', from: 1, to: 16, card: 16, chain: [{ from: 1, to: 100 }] };
        cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 16, label: 'Card 16' }] });
        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();
        cardCatalog.section.items.push({ position: 17, label: 'Card 17' });
        cardCatalog.section.to = 17;

        cardCatalog.selectCard(17);

        expect(cardCatalog.$content.find('[data-cc-location]').text()).toBe('Zettel 17 von 100');
        expect(cardCatalog.$orientation.find('[aria-current="location"] .archives-cardcatalog__path-range').text()).toBe('Zettel 9 – 24');
        expect(cardCatalog.state.from).toBe(9);
        expect(cardCatalog.state.to).toBe(24);
    });

    test('should keep the return actions working after the full-view zoom path is refreshed', async function () {
        cardCatalog.state = {
            drawer: 'drawer-a',
            from: 1,
            to: 16,
            card: 16,
            chain: [
                { from: 1, to: 100 },
                { from: 1, to: 34 },
            ],
        };
        cardCatalog.fetchJson.mockResolvedValue({ from: 1, to: 16, items: [{ position: 16, label: 'Card 16' }] });
        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();
        cardCatalog.section.items.push({ position: 17, label: 'Card 17' });
        cardCatalog.section.to = 17;
        cardCatalog.selectCard(17);
        const chainJump = jest.spyOn(cardCatalog, 'chainJump').mockImplementation(() => {});

        cardCatalog.$orientation.find('.archives-cardcatalog__path-link[data-chain="-1"]').trigger('click');

        expect(chainJump).toHaveBeenCalledTimes(1);
        expect(chainJump).toHaveBeenCalledWith(-1);

        chainJump.mockClear();
        cardCatalog.$content.find('.archives-cardcatalog__back[data-chain="1"]').trigger('click');
        expect(chainJump).toHaveBeenCalledTimes(1);
        expect(chainJump).toHaveBeenCalledWith(1);
    });

    test.each([15, 17])('should preserve the live-position element when selecting card %i inside or outside the current window', async function (position) {
        cardCatalog.state = {
            drawer: 'drawer-a',
            from: 1,
            to: 16,
            card: 16,
            chain: [
                {
                    from: 1,
                    to: 100,
                },
            ],
        };
        cardCatalog.fetchJson.mockResolvedValue({
            from: 1,
            to: 16,
            items: [
                {
                    position: 15,
                },
                {
                    position: 16,
                },
            ],
        });
        cardCatalog.renderFullView(cardCatalog.currentDrawer(), 1, 16);
        await Promise.resolve();
        cardCatalog.section.items.push({
            position: 17,
        });
        cardCatalog.section.to = 17;
        const header = cardCatalog.$content.find('.archives-cardcatalog__content-header')[0];
        const livePosition = cardCatalog.$content.find('[data-cc-location]')[0];
        const back = cardCatalog.$content.find('.archives-cardcatalog__back')[0];
        cardCatalog.selectCard(position);
        expect(cardCatalog.$content.find('.archives-cardcatalog__content-header')[0]).toBe(header);
        expect(cardCatalog.$content.find('[data-cc-location]')[0]).toBe(livePosition);
        expect(livePosition.textContent).toBe('Zettel ' + position + ' von 100');
        expect(livePosition.getAttribute('role')).toBe('status');
        expect(livePosition.getAttribute('aria-live')).toBe('polite');
        expect(cardCatalog.$content.find('.archives-cardcatalog__back')[0]).toBe(back);
    });
});
