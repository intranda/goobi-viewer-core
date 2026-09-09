/**
 * Unit tests for Crowdsourcing.Item#getCurrentPageOrder.
 *
 * The crowdsourcing status endpoint (PUT .../crowdsourcing/campaigns/{id}/{pi}/{page}/) and
 * the annotation-persisting endpoint (PUT .../{pi}/annotations/) both ultimately key
 * annotations by PersistentAnnotation.targetPageOrder — the server-assigned physical page
 * order. That order is embedded in each canvas's own id
 * (".../records/{pi}/pages/{pageNo}/canvas/") and does not, in general, equal
 * currentCanvasIndex + 1: it only does so when the record's page order happens to start at 1
 * without gaps, which does not always hold (e.g. a volume with page numbering that continues
 * from a previous volume). getCurrentPageOrder() must read the real order out of the canvas
 * id instead of assuming currentCanvasIndex + 1.
 *
 * Loaded directly via the crowdsourcing-loader (indirect eval) and invoked via
 * `.call(fakeThis)` on the prototype method, rather than through `new Crowdsourcing.Item(...)`,
 * to avoid needing a real rxjs global (the constructor unconditionally creates several
 * rxjs.Subject instances that are irrelevant to this pure accessor).
 */
const Crowdsourcing = require('./crowdsourcing-loader')(['Crowdsourcing.Item.js']);

beforeAll(() => {
    global.viewerJS = {
        iiif: {
            getId: (element) => (typeof element === 'string' ? element : element && (element.id || element['@id'])),
        },
    };
});

function fakeItemAt(currentCanvasIndex, canvases) {
    return { currentCanvasIndex, canvases, getCurrentPageId: Crowdsourcing.Item.prototype.getCurrentPageId };
}

describe('getCurrentPageOrder', () => {
    test('reads the real page order out of the canvas id, not currentCanvasIndex + 1', () => {
        // record whose physical page numbering starts at 87 (e.g. continued from a previous
        // volume) - this is the exact scenario reported: firstPageOrder >> 1.
        const canvases = [
            { id: 'https://viewer.example.org/api/v1/records/PPN123/pages/87/canvas/' },
            { id: 'https://viewer.example.org/api/v1/records/PPN123/pages/88/canvas/' },
            { id: 'https://viewer.example.org/api/v1/records/PPN123/pages/89/canvas/' },
        ];
        const item = fakeItemAt(0, canvases);
        expect(Crowdsourcing.Item.prototype.getCurrentPageOrder.call(item)).toBe(87);

        item.currentCanvasIndex = 2;
        expect(Crowdsourcing.Item.prototype.getCurrentPageOrder.call(item)).toBe(89);

        // currentCanvasIndex + 1 would have given 3 here - the old, wrong value.
        expect(Crowdsourcing.Item.prototype.getCurrentPageOrder.call(item)).not.toBe(item.currentCanvasIndex + 1);
    });

    test('agrees with currentCanvasIndex + 1 for the common case where order starts at 1', () => {
        const canvases = [
            { id: 'https://viewer.example.org/api/v1/records/PPN123/pages/1/canvas/' },
            { id: 'https://viewer.example.org/api/v1/records/PPN123/pages/2/canvas/' },
        ];
        const item = fakeItemAt(1, canvases);
        expect(Crowdsourcing.Item.prototype.getCurrentPageOrder.call(item)).toBe(2);
        expect(Crowdsourcing.Item.prototype.getCurrentPageOrder.call(item)).toBe(item.currentCanvasIndex + 1);
    });

    test('falls back to currentCanvasIndex + 1 when there is no current canvas', () => {
        const item = fakeItemAt(0, []);
        expect(Crowdsourcing.Item.prototype.getCurrentPageOrder.call(item)).toBe(1);
    });

    test('falls back to currentCanvasIndex + 1 when the canvas id does not match the expected pattern', () => {
        const item = fakeItemAt(4, [null, null, null, null, { id: 'https://viewer.example.org/something/else/' }]);
        expect(Crowdsourcing.Item.prototype.getCurrentPageOrder.call(item)).toBe(5);
    });
});
