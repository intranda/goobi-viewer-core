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

describe('viewerJS.cardCatalog.headCropUrl', function () {
    test('should derive the upper fifteen percent of a full image by default', function () {
        expect(cardCatalog.headCropUrl('https://example.org/viewer/api/v1/records/94_584/representative/full/!1200,1200/0/default.jpg')).toBe(
            'https://example.org/viewer/api/v1/records/94_584/representative/pct:0,0,100,15/!1200,480/0/default.jpg'
        );
    });

    test('should return null for missing input', function () {
        expect(cardCatalog.headCropUrl(null)).toBeNull();
    });

    test('should return null instead of an uncropped URL when the size pattern does not match', function () {
        expect(cardCatalog.headCropUrl('https://example.org/viewer/api/v1/records/94_584/representative/full/max/0/default.jpg')).toBeNull();
    });
});

describe('viewerJS.cardCatalog overview presentation', function () {
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

    test('should keep the sticky header and complete zoom list in the same overview container', async function () {
        await renderOverview();
        const $overview = cardCatalog.$content.children('.archives-cardcatalog__overview');
        const $header = cardCatalog.$content.find('.archives-cardcatalog__content-header');
        const $list = cardCatalog.$content.find('.archives-cardcatalog__zoomlist');

        expect($overview).toHaveLength(1);
        expect($header.parent()[0]).toBe($overview[0]);
        expect($list.parent()[0]).toBe($overview[0]);
        expect($overview.children().first()[0]).toBe($header[0]);
        expect($overview.children().last()[0]).toBe($list[0]);
        expect($list.children('.archives-cardcatalog__zoomcard')).toHaveLength(boundaryItems.length);
    });

    test('should provide a short visible preview action and its complete accessible label', async function () {
        await renderOverview();
        const $toggle = cardCatalog.$content.find('[data-preview-toggle]');

        expect($toggle.text().trim()).toBe('Ganze Karten');
        expect($toggle.attr('aria-label')).toBe('Ganze Karten anzeigen');
    });

    test('should offer no way back in the content header at the top zoom level', async function () {
        await renderOverview();

        expect(cardCatalog.$content.find('.archives-cardcatalog__back')).toHaveLength(0);
        expect(cardCatalog.$content.find('[data-chain]')).toHaveLength(0);
    });

    test('should keep every range between its two boundary cards in DOM reading order', async function () {
        await renderOverview();

        const $children = cardCatalog.$content.find('.archives-cardcatalog__zoomlist').children();
        expect($children.map((_, child) => jQuery(child).hasClass('archives-cardcatalog__gap')).get()).toEqual([false, true, false, true, false, true, false]);
        $children.filter('.archives-cardcatalog__gap').each((_, gap) => {
            expect(jQuery(gap).attr('data-from')).toBe(jQuery(gap).prev().attr('data-open'));
            expect(jQuery(gap).attr('data-to')).toBe(jQuery(gap).next().attr('data-open'));
        });
    });

    test('should preserve complete-preview preference when rendering another range', async function () {
        await renderOverview();
        cardCatalog.$content.find('[data-preview-toggle]').trigger('click');

        cardCatalog.renderZoomView(cardCatalog.currentDrawer(), 1, 34);
        await Promise.resolve();

        expect(cardCatalog.$content.find('[data-preview-toggle]').attr('aria-pressed')).toBe('true');
        expect(cardCatalog.$content.find('.archives-cardcatalog__zoomcard img').first().attr('src')).toBe(fullImage);
    });

    test('should go back exactly one zoom level from the content header', async function () {
        cardCatalog.state = {
            drawer: 'drawer-a',
            from: 10,
            to: 40,
            card: null,
            chain: [
                { from: 1, to: 100 },
                { from: 1, to: 67 },
            ],
        };
        cardCatalog.renderZoomView(cardCatalog.currentDrawer(), 10, 40);
        await Promise.resolve();

        const $back = cardCatalog.$content.find('.archives-cardcatalog__back');
        expect($back.text().trim()).toBe('Eine Zoomstufe zurück');
        expect($back.attr('data-chain')).toBe('1');
        expect($back.find('svg').attr('aria-hidden')).toBe('true');
        expect(cardCatalog.$content.find('.archives-cardcatalog__navigation').children().first()[0]).toBe($back[0]);
        expect(cardCatalog.$content.find('[data-chain]')).toHaveLength(1);

        $back.trigger('click');
        expect(cardCatalog.state).toEqual({ drawer: 'drawer-a', from: 1, to: 67, card: null, chain: [{ from: 1, to: 100 }] });
        expect(cardCatalog.syncUrl).toHaveBeenCalledWith(true);
        await Promise.resolve();
    });

    test('should restore the overview scroll position after returning from a card', async function () {
        await renderOverview();
        cardCatalog.$content.parent().scrollTop(245);
        cardCatalog.fetchJson.mockResolvedValueOnce({ from: 59, to: 74, items: [{ position: 67, label: 'Card 67' }] });

        cardCatalog.$content.find('[data-open="67"]').trigger('click');
        await Promise.resolve();
        cardCatalog.$content.parent().scrollTop(0);
        cardCatalog.$content.find('.archives-cardcatalog__back[data-chain="-1"]').trigger('click');
        await Promise.resolve();

        expect(cardCatalog.$content.parent().scrollTop()).toBe(245);
        expect(cardCatalog.state).toEqual({ drawer: 'drawer-a', from: null, to: null, card: null, chain: [] });
    });

    test('should bound remembered overview positions while retaining the most recently visited range', function () {
        for (let position = 1; position <= 34; position++) {
            cardCatalog.$content.html('<div data-overview></div>');
            cardCatalog.$content.find('[data-overview]').attr('data-overview', JSON.stringify(['drawer-a', position, 100]));
            cardCatalog.rememberOverviewPosition();
        }

        expect(cardCatalog.overviewPositions.size).toBe(32);
        expect(cardCatalog.overviewPositions.has(JSON.stringify(['drawer-a', 1, 100]))).toBe(false);
        expect(cardCatalog.overviewPositions.has(JSON.stringify(['drawer-a', 34, 100]))).toBe(true);
    });

    test.each([false, true])('should stack a title row and a navigation row when full view is %s', function (fullView) {
        const drawerLabel = 'A & "B" <C> – vollständig lesbarer Schubladentitel';
        cardCatalog.currentDrawer().label = drawerLabel;
        cardCatalog.state.card = fullView ? 1 : null;
        const $header = jQuery(cardCatalog.renderContentHeader(cardCatalog.currentDrawer(), 1, 100, fullView));
        const $main = $header.children('.archives-cardcatalog__header-main');
        const $navigation = $header.children('.archives-cardcatalog__navigation');
        const $location = $main.children('.archives-cardcatalog__location');
        const $actions = $navigation.children('.archives-cardcatalog__header-actions');
        const $drawerTitle = $location.children('strong');
        expect(
            $header
                .children()
                .map((_, child) => child.className)
                .get()
        ).toEqual(['archives-cardcatalog__header-main', 'archives-cardcatalog__navigation']);
        expect(
            $main
                .children()
                .map((_, child) => child.className)
                .get()
        ).toEqual(['archives-cardcatalog__location']);
        expect($location.children().first()[0]).toBe($drawerTitle[0]);
        expect($drawerTitle.attr('title')).toBe(drawerLabel);
        expect($drawerTitle.text()).toBe(drawerLabel);
        expect($drawerTitle.children()).toHaveLength(0);
        expect($location.children('[data-cc-location]')).toHaveLength(1);
        expect($actions).toHaveLength(fullView ? 0 : 1);
        expect($actions.find('[data-preview-toggle]')).toHaveLength(fullView ? 0 : 1);
        expect($header.find('details')).toHaveLength(0);
        expect($navigation.children('.archives-cardcatalog__back')).toHaveLength(0);
        expect(
            $header.find(
                '.archives-cardcatalog__toolbar, .archives-cardcatalog__navigation-hint, .archives-cardcatalog__mode-label, .archives-cardcatalog__breadcrumb-label, .archives-cardcatalog__group-label'
            )
        ).toHaveLength(0);
    });

    test('should render head crops and request the configured boundary count', async function () {
        await renderOverview();
        cardCatalog.$content.find('.archives-cardcatalog__zoomcard img').each((_, image) => {
            expect(image.getAttribute('src')).toContain('/pct:0,0,100,15/');
        });
        const request = new URL(cardCatalog.fetchJson.mock.calls[0][0], 'https://example.org');
        expect(request.searchParams.has('count')).toBe(false);
    });

    test('should toggle complete previews without changing navigation and restore the crop', async function () {
        await renderOverview();
        const originalState = JSON.parse(JSON.stringify(cardCatalog.state));
        const $toggle = cardCatalog.$content.find('[data-preview-toggle]');
        const $image = cardCatalog.$content.find('.archives-cardcatalog__zoomcard img').first();
        expect($toggle.attr('aria-pressed')).toBe('false');
        expect($image.attr('src')).toBe($image.attr('data-preview-crop'));
        expect($image.attr('src')).toContain('/pct:0,0,100,15/');
        $toggle.trigger('click');
        expect($toggle.attr('aria-pressed')).toBe('true');
        expect($image.attr('src')).toBe(fullImage);
        expect($image.attr('data-preview-full')).toBe(fullImage);
        expect(cardCatalog.$content.find('.archives-cardcatalog__zoomlist--full')).toHaveLength(1);
        $toggle.trigger('click');
        expect($toggle.attr('aria-pressed')).toBe('false');
        expect($image.attr('src')).toBe($image.attr('data-preview-crop'));
        expect($image.attr('src')).toContain('/pct:0,0,100,15/');
        expect(cardCatalog.$content.find('.archives-cardcatalog__zoomlist--full')).toHaveLength(0);
        expect(cardCatalog.fetchJson).toHaveBeenCalledTimes(1);
        expect(cardCatalog.syncUrl).not.toHaveBeenCalled();
        expect(cardCatalog.state).toEqual(originalState);
    });

    test('should keep non-IIIF previews usable in either preview mode', async function () {
        cardCatalog.fetchJson.mockResolvedValue({
            items: [
                {
                    position: 1,
                    label: 'Plain image',
                    imageUrl: '/plain.jpg',
                    thumbnailUrl: '/thumb.jpg',
                },
            ],
        });
        await renderOverview();
        const $image = cardCatalog.$content.find('.archives-cardcatalog__zoomcard img');
        expect($image.attr('src')).toBeTruthy();
        cardCatalog.$content.find('[data-preview-toggle]').trigger('click');
        expect($image.attr('src')).toBe('/plain.jpg');
        cardCatalog.$content.find('[data-preview-toggle]').trigger('click');
        expect($image.attr('src')).toBe('/plain.jpg');
    });
});
