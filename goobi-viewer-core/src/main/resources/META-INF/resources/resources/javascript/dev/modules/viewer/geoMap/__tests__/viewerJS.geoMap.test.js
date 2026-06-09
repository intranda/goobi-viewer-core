/**
 * Unit tests for viewerJS.GeoMap.prototype methods.
 *
 * Loaded via geomap-loader (indirect eval). Most methods read either
 * `this.map.<X>()` (Leaflet map facade) or call into Leaflet's `L.*`
 * factories. We stub `global.L` with the minimum surface needed by the
 * methods under test, and pass mock map objects through `Object.create`
 * instances.
 */

// Minimal Leaflet stub. Only what setView / getViewAroundFeatures touch.
global.L = {
    latLng: (lat, lng) => ({ lat, lng, _isLatLng: true }),
    latLngBounds: () => {
        const extents = [];
        return {
            extend: (b) => extents.push(b),
            getCenter: () => ({ lat: 0, lng: 0 }),
            pad: function () {
                return this;
            },
            isValid: () => extents.length > 0,
        };
    },
    geoJson: (f) => ({ getBounds: () => ({ feature: f }) }),
};

const viewerJS = require('./geomap-loader')(['viewerJS.geoMap.featureGroup.js']);
const geoMapProto = viewerJS.GeoMap.prototype;

/** Build a barebones GeoMap-like object with a stubbed Leaflet map. */
function makeGeoMap(overrides) {
    const gm = Object.create(geoMapProto);
    gm.config = { initialView: { zoom: 5, center: [0, 0] } };
    gm.layers = [];
    gm.map = {
        setView: jest.fn(),
        panTo: jest.fn(),
        setZoom: jest.fn(),
        getZoom: jest.fn(() => 5),
        getCenter: jest.fn(() => ({ lat: 51, lng: 10 })),
        getBoundsZoom: jest.fn(() => 8),
        distance: jest.fn(() => 0),
        remove: jest.fn(),
    };
    return Object.assign(gm, overrides || {});
}

describe('setView', () => {
    test('returns early when view is undefined and stores it on the instance', () => {
        const gm = makeGeoMap();
        gm.setView(undefined);
        expect(gm.view).toBeUndefined();
        expect(gm.map.setView).not.toHaveBeenCalled();
    });

    test('parses a JSON string view and applies it', () => {
        const gm = makeGeoMap();
        gm.setView(JSON.stringify({ zoom: 7, center: [10, 51] }));
        expect(gm.map.setView).toHaveBeenCalledWith(expect.objectContaining({ lat: 51, lng: 10 }), 7, { animate: undefined });
    });

    test('clamps zoom to a minimum of 1 when given a smaller value', () => {
        const gm = makeGeoMap();
        gm.setView({ zoom: 0, center: [10, 51] });
        expect(gm.map.setView).toHaveBeenCalledWith(expect.anything(), 1, expect.anything());
    });

    test('replaces NaN zoom with 1', () => {
        const gm = makeGeoMap();
        gm.setView({ zoom: NaN, center: [10, 51] });
        expect(gm.map.setView).toHaveBeenCalledWith(expect.anything(), 1, expect.anything());
    });

    test('uses setZoom when only zoom is given', () => {
        const gm = makeGeoMap();
        gm.setView({ zoom: 9 });
        expect(gm.map.setZoom).toHaveBeenCalledWith(9);
        expect(gm.map.setView).not.toHaveBeenCalled();
    });

    test('passes the panning flag through as the animate option', () => {
        const gm = makeGeoMap();
        gm.setView({ zoom: 4, center: [10, 51] }, true);
        expect(gm.map.setView).toHaveBeenCalledWith(expect.anything(), 4, { animate: true });
    });
});

describe('getView', () => {
    test('returns the current zoom and center as [lng, lat]', () => {
        const gm = makeGeoMap();
        gm.map.getZoom.mockReturnValue(6);
        gm.map.getCenter.mockReturnValue({ lat: 52.5, lng: 13.4 });
        expect(gm.getView()).toEqual({ zoom: 6, center: [13.4, 52.5] });
    });
});

describe('getViewAroundFeatures', () => {
    test('returns undefined for an empty/missing feature list', () => {
        const gm = makeGeoMap();
        expect(gm.getViewAroundFeatures([])).toBeUndefined();
        expect(gm.getViewAroundFeatures(undefined)).toBeUndefined();
    });

    test('falls back to defaultZoom when bounds have zero diameter', () => {
        const gm = makeGeoMap();
        // L.latLngBounds() in our stub starts empty; isValid() reports
        // false for the diameter call, so getDiameter returns 0.
        gm.getDiameter = () => 0;
        const result = gm.getViewAroundFeatures([{ type: 'Feature' }], 4);
        expect(result.zoom).toBe(4);
        expect(result.center).toEqual([0, 0]);
    });

    test('uses map.getBoundsZoom when there is a non-zero diameter', () => {
        const gm = makeGeoMap();
        gm.getDiameter = () => 1000;
        gm.map.getBoundsZoom.mockReturnValue(11);
        const result = gm.getViewAroundFeatures([{ type: 'Feature' }, { type: 'Feature' }]);
        expect(result.zoom).toBe(11);
    });
});

describe('setViewToFeatures', () => {
    test('skips work entirely when no features are provided', () => {
        const gm = makeGeoMap();
        const setView = jest.spyOn(gm, 'setView');
        gm.setViewToFeatures([]);
        expect(setView).not.toHaveBeenCalled();
    });

    test('zooms to highlighted features when setViewToHighlighted is true', () => {
        const gm = makeGeoMap();
        const around = jest.spyOn(gm, 'getViewAroundFeatures').mockReturnValue({ zoom: 9, center: [0, 0] });
        const setView = jest.spyOn(gm, 'setView').mockImplementation(() => {});
        const features = [
            { properties: { highlighted: true }, id: 1 },
            { properties: {}, id: 2 },
        ];
        gm.setViewToFeatures(features, true);
        // First arg of getViewAroundFeatures is the *highlighted* subset.
        expect(around.mock.calls[0][0]).toEqual([features[0]]);
        expect(setView).toHaveBeenCalledWith({ zoom: 9, center: [0, 0] });
    });

    test('zooms to all features when nothing is highlighted', () => {
        const gm = makeGeoMap();
        const around = jest.spyOn(gm, 'getViewAroundFeatures').mockReturnValue({ zoom: 3, center: [0, 0] });
        jest.spyOn(gm, 'setView').mockImplementation(() => {});
        const features = [{ properties: {} }, { properties: {} }];
        gm.setViewToFeatures(features, true);
        expect(around.mock.calls[0][0]).toBe(features);
    });
});

describe('getZoom', () => {
    test('delegates to map.getZoom', () => {
        const gm = makeGeoMap();
        gm.map.getZoom.mockReturnValue(12);
        expect(gm.getZoom()).toBe(12);
    });
});

describe('setActiveLayers / getActiveLayers', () => {
    function makeLayer(name) {
        return {
            name,
            active: undefined,
            showMarkers: jest.fn(),
            hideMarkers: jest.fn(),
        };
    }

    test('marks layers in the input set as active and shows their markers', () => {
        const a = makeLayer('a');
        const b = makeLayer('b');
        const gm = makeGeoMap({
            layers: [a, b],
            onActiveLayerChange: { next: jest.fn() },
        });
        gm.setActiveLayers([a]);
        expect(a.active).toBe(true);
        expect(a.showMarkers).toHaveBeenCalled();
        expect(b.active).toBe(false);
        expect(b.hideMarkers).toHaveBeenCalled();
    });

    test('emits the new active-layer set on onActiveLayerChange', () => {
        const a = makeLayer('a');
        const next = jest.fn();
        const gm = makeGeoMap({
            layers: [a],
            onActiveLayerChange: { next },
        });
        gm.setActiveLayers([a]);
        expect(next).toHaveBeenCalledWith([a]);
    });

    test('getActiveLayers returns layers that have not been deactivated', () => {
        const a = { active: true };
        const b = { active: false };
        const c = { active: undefined }; // never explicitly toggled
        const gm = makeGeoMap({ layers: [a, b, c] });
        expect(gm.getActiveLayers()).toEqual([a, c]);
    });
});

describe('close', () => {
    test('completes the click/move subjects, closes layers, and removes the map', () => {
        const layerClose = jest.fn();
        const clickComplete = jest.fn();
        const moveComplete = jest.fn();
        const remove = jest.fn();
        const gm = makeGeoMap({
            layers: [{ close: layerClose }, { close: layerClose }],
            onMapClick: { complete: clickComplete },
            onMapMove: { complete: moveComplete },
        });
        gm.map.remove = remove;

        gm.close();

        expect(clickComplete).toHaveBeenCalled();
        expect(moveComplete).toHaveBeenCalled();
        expect(layerClose).toHaveBeenCalledTimes(2);
        expect(remove).toHaveBeenCalled();
    });
});
