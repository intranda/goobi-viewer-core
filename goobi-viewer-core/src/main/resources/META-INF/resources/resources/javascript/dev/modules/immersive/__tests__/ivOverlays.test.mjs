/** Unit tests for the overlay layers: per-owner highlights, text regions and annotation regions on a fake OSD. */
import { jest } from '@jest/globals';
import { IvOverlays } from '../engine/ivOverlays.mjs';

function setup(item = { imageToViewportRectangle: (x, y, w, h) => ({ x, y, w, h }) }) {
    const osd = { addOverlay: jest.fn(), removeOverlay: jest.fn() };
    const overlays = new IvOverlays({ osd: () => osd, itemFor: () => item, currentItem: () => item });
    return { osd, overlays };
}

test('highlights live per owner; clearing one owner leaves the others', () => {
    const { osd, overlays } = setup();
    overlays.setHighlights([{ x: 1, y: 2, w: 3, h: 4, active: true, className: 'c' }], 'search');
    overlays.setHighlights([{ x: 5, y: 6, w: 7, h: 8 }], 'tags');
    expect(osd.addOverlay).toHaveBeenCalledTimes(2);
    expect(osd.addOverlay.mock.calls[0][0].element.className).toBe('immersive__highlight -active c');
    overlays.clearHighlights('tags');
    expect(osd.removeOverlay).toHaveBeenCalledTimes(1);
    overlays.clearHighlights();
    expect(osd.removeOverlay).toHaveBeenCalledTimes(2);
});

test('text and annotation regions carry their ids and level or modifier classes; regions without rect are skipped', () => {
    const { osd, overlays } = setup();
    const text = overlays.setTextRegions([{ id: 'w1', rect: { x: 1, y: 1, w: 1, h: 1 }, level: 'word' }, { id: 'none' }]);
    expect(text.get('w1').className).toBe('immersive__text-region immersive__text-region-word');
    expect(text.get('w1').dataset.ivRegionId).toBe('w1');
    expect(text.has('none')).toBe(false);
    const anno = overlays.setAnnotationRegions([{ id: 'a1', rect: { x: 1, y: 1, w: 1, h: 1 }, modifier: 'note' }]);
    expect(anno.get('a1').className).toBe('immersive__anno-region immersive__anno-region-note');
    overlays.destroy();
    expect(osd.removeOverlay).toHaveBeenCalledTimes(2);
});

test('without a current item nothing is drawn', () => {
    const { osd, overlays } = setup(null);
    expect(overlays.setTextRegions([{ id: 'x', rect: { x: 0, y: 0, w: 1, h: 1 } }]).size).toBe(0);
    expect(osd.addOverlay).not.toHaveBeenCalled();
});
