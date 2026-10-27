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

describe('viewerJS.cardCatalog.stateFromParams', function () {
    test('should return empty state without parameters', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams(''), info);
        expect(state).toEqual({ drawer: null, from: null, to: null, card: null, chain: [] });
    });

    test('should read drawer, interval and card from parameters', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17&to=129&card=20'), info);
        expect(state.drawer).toBe('drawer-a');
        expect(state.from).toBe(17);
        expect(state.to).toBe(129);
        expect(state.card).toBe(20);
    });

    test('should synthesize a breadcrumb chain for interval deep links (reload regression)', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17&to=129'), info);
        expect(state.chain).toEqual([{ from: 1, to: 1022 }]);
    });

    test('should not synthesize a chain without an interval', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a'), info);
        expect(state.chain).toEqual([]);
    });

    test('should ignore unknown drawers', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=unknown&from=1&to=5'), info);
        expect(state.drawer).toBeNull();
        expect(state.from).toBeNull();
    });

    test('should ignore invalid intervals', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=100&to=5'), info);
        expect(state.drawer).toBe('drawer-a');
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
    });

    test('should reject an interval with a missing bound but keep the card', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17&card=3'), info);
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
        expect(state.card).toBe(3);
    });

    test('should return an empty state without info', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1&to=5'), null);
        expect(state).toEqual({ drawer: null, from: null, to: null, card: null, chain: [] });
    });

    test('should keep the card without an interval (small drawer deep link)', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&card=7'), info);
        expect(state.drawer).toBe('drawer-a');
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
        expect(state.card).toBe(7);
    });

    test('should reject non-numeric interval values', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=abc&to=50&card=x'), info);
        expect(state.from).toBeNull();
        expect(state.to).toBeNull();
        expect(state.card).toBeNull();
    });

    test('should clamp hand-edited deep link values to the drawer size', function () {
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1000&to=999999&card=999999'), info);
        expect(state.to).toBe(1022);
        expect(state.card).toBeNull();
        // from beyond the drawer size: the whole interval is rejected
        const beyond = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=2000&to=999999'), info);
        expect(beyond.from).toBeNull();
        expect(beyond.to).toBeNull();
    });

    test('should reject partially numeric positions and open a card deep link in a bounded window', function () {
        const catalogInfo = { drawers: info.drawers, config: { fullViewThreshold: 16 } };
        expect(cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=17foo&to=50'), catalogInfo).from).toBeNull();
        expect(cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&card=20foo'), catalogInfo).card).toBeNull();
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&card=100'), catalogInfo);
        expect(state).toEqual({ drawer: 'drawer-a', from: 92, to: 107, card: 100, chain: [{ from: 1, to: 1022 }] });
    });

    test('should show a card deep link even when its explicit interval is wider than the full view', function () {
        const catalogInfo = { drawers: info.drawers, config: { fullViewThreshold: 16 } };
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1&to=1000&card=500'), catalogInfo);
        expect(state).toEqual({ drawer: 'drawer-a', from: 492, to: 507, card: 500, chain: [{ from: 1, to: 1022 }] });
    });

    test('should open a card outside a narrow URL interval instead of selecting another card', function () {
        const catalogInfo = { drawers: info.drawers, config: { fullViewThreshold: 16 } };
        const state = cardCatalog.stateFromParams(new URLSearchParams('drawer=drawer-a&from=1&to=4&card=500'), catalogInfo);
        expect(state).toEqual({ drawer: 'drawer-a', from: 492, to: 507, card: 500, chain: [{ from: 1, to: 1022 }] });
    });
});

describe('viewerJS.cardCatalog.paramsFromState', function () {
    test('should serialize state and preserve unrelated parameters', function () {
        const params = cardCatalog.paramsFromState({ drawer: 'drawer-ze', from: 1, to: 50, card: 3, chain: [] }, new URLSearchParams('foo=bar&card=99'));
        expect(params.get('foo')).toBe('bar');
        expect(params.get('drawer')).toBe('drawer-ze');
        expect(params.get('from')).toBe('1');
        expect(params.get('to')).toBe('50');
        expect(params.get('card')).toBe('3');
    });

    test('should remove all card catalog parameters without a drawer', function () {
        const params = cardCatalog.paramsFromState(
            { drawer: null, from: null, to: null, card: null, chain: [] },
            new URLSearchParams('drawer=drawer-a&from=1&to=5&card=2&selected=xyz')
        );
        expect(params.get('drawer')).toBeNull();
        expect(params.get('from')).toBeNull();
        expect(params.get('selected')).toBe('xyz');
    });

    test('should not serialize a half interval', function () {
        const params = cardCatalog.paramsFromState({ drawer: 'drawer-a', from: 5, to: null, card: null, chain: [] }, new URLSearchParams(''));
        expect(params.get('drawer')).toBe('drawer-a');
        expect(params.get('from')).toBeNull();
        expect(params.get('to')).toBeNull();
    });

    test('should round-trip through stateFromParams', function () {
        const state = { drawer: 'drawer-a', from: 17, to: 129, card: 20, chain: [] };
        const params = cardCatalog.paramsFromState(state, new URLSearchParams(''));
        const restored = cardCatalog.stateFromParams(params, info);
        expect(restored.drawer).toBe(state.drawer);
        expect(restored.from).toBe(state.from);
        expect(restored.to).toBe(state.to);
        expect(restored.card).toBe(state.card);
    });
});

describe('viewerJS.cardCatalog.windowAround', function () {
    beforeEach(function () {
        cardCatalog.info = { config: { fullViewThreshold: 16 } };
    });

    afterEach(function () {
        cardCatalog.info = null;
    });

    test('should center the window around the position', function () {
        expect(cardCatalog.windowAround(100, { entryCount: 1000 })).toEqual({ from: 92, to: 107 });
    });

    test('should clamp the window at the start of the drawer', function () {
        expect(cardCatalog.windowAround(1, { entryCount: 1000 })).toEqual({ from: 1, to: 16 });
    });

    test('should clamp the window at the end of the drawer', function () {
        expect(cardCatalog.windowAround(998, { entryCount: 1000 })).toEqual({ from: 985, to: 1000 });
    });

    test('should span the whole drawer when it is smaller than the window', function () {
        expect(cardCatalog.windowAround(3, { entryCount: 10 })).toEqual({ from: 1, to: 10 });
    });
});
