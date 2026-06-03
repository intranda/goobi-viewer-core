/**
 * Unit tests for viewerJS.geoMap.featureGroup.
 *
 * Strategy: load the source via geomap-loader (indirect eval), then
 * exercise prototype methods on stub instances created with
 * Object.create() — that way we never invoke the real constructor and
 * never need a live Leaflet map. Each test seeds only the `this.*`
 * state the method under test actually reads.
 */
const viewerJS = require('./geomap-loader')(['viewerJS.geoMap.featureGroup.js']);

const featureGroupProto = viewerJS.GeoMap.featureGroup.prototype;

/** Build a barebones featureGroup-like object with the given overrides. */
function makeFeatureGroup(overrides) {
    const fg = Object.create(featureGroupProto);
    fg.markers = [];
    fg.areas = [];
    fg.highlighted = [];
    fg.markerIdCounter = 1;
    fg.config = { style: {}, markerIcon: {} };
    fg.geoMap = { map: { hasLayer: () => false, addLayer: () => {}, removeLayer: () => {} } };
    fg.layer = { addLayer: () => {}, clearLayers: () => {} };
    return Object.assign(fg, overrides || {});
}

describe('viewerJS.GeoMap.featureGroup loading', () => {
    test('attaches the featureGroup constructor onto viewer.GeoMap', () => {
        expect(typeof viewerJS.GeoMap.featureGroup).toBe('function');
        expect(typeof featureGroupProto.isEmpty).toBe('function');
    });
});

describe('isEmpty', () => {
    test('returns true when there are no markers and no areas', () => {
        const fg = makeFeatureGroup();
        expect(fg.isEmpty()).toBe(true);
    });

    test('returns false when at least one marker is present', () => {
        const fg = makeFeatureGroup({ markers: [{}] });
        expect(fg.isEmpty()).toBe(false);
    });

    test('returns false when at least one area is present', () => {
        const fg = makeFeatureGroup({ areas: [{}] });
        expect(fg.isEmpty()).toBe(false);
    });
});

describe('getCount', () => {
    test('returns the number of visible entities when properties.entities is present', () => {
        const fg = makeFeatureGroup();
        const properties = {
            entities: [{ visible: true }, { visible: false }, {}],
        };
        // {visible:false} is filtered out; the bare {} keeps default "visible !== false".
        expect(fg.getCount(properties)).toBe(2);
    });

    test('returns properties.count when entities is missing', () => {
        const fg = makeFeatureGroup();
        expect(fg.getCount({ count: 7 })).toBe(7);
    });

    test('falls back to 1 when neither entities nor count is set', () => {
        const fg = makeFeatureGroup();
        expect(fg.getCount({})).toBe(1);
    });
});

describe('compareFeatures', () => {
    test('returns getSize(f2) - getSize(f1) so larger features sort first', () => {
        const fg = makeFeatureGroup();
        // Stub getSize so we don't need a real Leaflet polygon.
        fg.getSize = (feature) => feature.size;
        const result = fg.compareFeatures({ size: 10 }, { size: 30 });
        expect(result).toBe(20);
    });
});

describe('getMarker / getFeatures / getMarkerCount', () => {
    const m1 = { getId: () => 1, feature: { id: 1, name: 'a' } };
    const m2 = { getId: () => 2, feature: { id: 2, name: 'b' } };

    test('getMarker finds a marker by id', () => {
        const fg = makeFeatureGroup({ markers: [m1, m2] });
        expect(fg.getMarker(2)).toBe(m2);
    });

    test('getMarker returns undefined when no marker has the given id', () => {
        const fg = makeFeatureGroup({ markers: [m1, m2] });
        expect(fg.getMarker(99)).toBeUndefined();
    });

    test('getFeatures maps every marker to its feature property', () => {
        const fg = makeFeatureGroup({ markers: [m1, m2] });
        expect(fg.getFeatures()).toEqual([m1.feature, m2.feature]);
    });

    test('getMarkerCount returns the markers array length', () => {
        const fg = makeFeatureGroup({ markers: [m1, m2] });
        expect(fg.getMarkerCount()).toBe(2);
    });
});

describe('removeMarker', () => {
    test('calls marker.remove() and drops the marker from the array', () => {
        const removed = jest.fn();
        const m1 = { getId: () => 1, remove: removed };
        const m2 = { getId: () => 2, remove: jest.fn() };
        const fg = makeFeatureGroup({ markers: [m1, m2] });

        fg.removeMarker({ id: 1 });

        expect(removed).toHaveBeenCalledTimes(1);
        expect(fg.markers).toEqual([m2]);
    });
});

describe('createGeoJson', () => {
    test('builds a GeoJSON Feature from a Leaflet-style latlng + zoom', () => {
        const fg = makeFeatureGroup();
        const result = fg.createGeoJson({ lng: 10.5, lat: 51.2 }, 8);
        expect(result.type).toBe('Feature');
        expect(result.geometry).toEqual({ type: 'Point', coordinates: [10.5, 51.2] });
        expect(result.view).toEqual({ zoom: 8, center: [10.5, 51.2] });
        expect(result.id).toBe(1);
    });

    test('increments markerIdCounter so successive features get unique ids', () => {
        const fg = makeFeatureGroup();
        const a = fg.createGeoJson({ lng: 0, lat: 0 }, 1);
        const b = fg.createGeoJson({ lng: 0, lat: 0 }, 1);
        expect(a.id).toBe(1);
        expect(b.id).toBe(2);
    });
});

describe('isVisible / setVisible', () => {
    test('isVisible delegates to map.hasLayer(this.layer)', () => {
        const hasLayer = jest.fn(() => true);
        const fg = makeFeatureGroup({
            geoMap: { map: { hasLayer, addLayer: () => {}, removeLayer: () => {} } },
        });
        expect(fg.isVisible()).toBe(true);
        expect(hasLayer).toHaveBeenCalledWith(fg.layer);
    });

    test('setVisible(true) adds the layer when not already visible', () => {
        const addLayer = jest.fn();
        const fg = makeFeatureGroup({
            geoMap: { map: { hasLayer: () => false, addLayer, removeLayer: () => {} } },
        });
        fg.setVisible(true);
        expect(addLayer).toHaveBeenCalledWith(fg.layer);
    });

    test('setVisible(true) is a no-op when the layer is already visible', () => {
        const addLayer = jest.fn();
        const fg = makeFeatureGroup({
            geoMap: { map: { hasLayer: () => true, addLayer, removeLayer: () => {} } },
        });
        fg.setVisible(true);
        expect(addLayer).not.toHaveBeenCalled();
    });

    test('setVisible(false) removes the layer regardless of current state', () => {
        const removeLayer = jest.fn();
        const fg = makeFeatureGroup({
            geoMap: { map: { hasLayer: () => false, addLayer: () => {}, removeLayer } },
        });
        fg.setVisible(false);
        expect(removeLayer).toHaveBeenCalledWith(fg.layer);
    });
});

describe('removePolygon', () => {
    test('clears shownLayer and polygon and asks the map to remove the polygon', () => {
        const removeLayer = jest.fn();
        const setOpacity = jest.fn();
        const fg = makeFeatureGroup({
            geoMap: { map: { hasLayer: () => false, addLayer: () => {}, removeLayer } },
            shownLayer: { setOpacity },
            polygon: { id: 'p1' },
        });

        fg.removePolygon();

        expect(setOpacity).toHaveBeenCalledWith(1);
        expect(removeLayer).toHaveBeenCalledWith({ id: 'p1' });
        expect(fg.shownLayer).toBeNull();
        expect(fg.polygon).toBeNull();
    });

    test('is a safe no-op when no polygon is currently shown', () => {
        const removeLayer = jest.fn();
        const fg = makeFeatureGroup({
            geoMap: { map: { hasLayer: () => false, addLayer: () => {}, removeLayer } },
        });
        expect(() => fg.removePolygon()).not.toThrow();
        expect(removeLayer).not.toHaveBeenCalled();
    });
});

describe('updateMarker', () => {
    test('opens the popup of the marker with the matching id', () => {
        const openPopup = jest.fn();
        const m1 = { getId: () => 1, openPopup };
        const fg = makeFeatureGroup({ markers: [m1] });

        fg.updateMarker(1);

        expect(openPopup).toHaveBeenCalledTimes(1);
    });

    test('is a no-op when no marker matches the id', () => {
        const fg = makeFeatureGroup({ markers: [] });
        expect(() => fg.updateMarker(42)).not.toThrow();
    });
});

describe('resetMarkers', () => {
    test('removes every marker, empties the array, and resets the id counter', () => {
        const r1 = jest.fn();
        const r2 = jest.fn();
        const fg = makeFeatureGroup({
            markers: [{ remove: r1 }, { remove: r2 }],
            markerIdCounter: 7,
        });

        fg.resetMarkers();

        expect(r1).toHaveBeenCalledTimes(1);
        expect(r2).toHaveBeenCalledTimes(1);
        expect(fg.markers).toEqual([]);
        expect(fg.markerIdCounter).toBe(1);
    });
});

describe('getClusterCount', () => {
    test('sums getCount(properties) across all child markers of a cluster', () => {
        const fg = makeFeatureGroup();
        const cluster = {
            getAllChildMarkers: () => [
                { feature: { properties: { count: 3 } } },
                { feature: { properties: { count: 5 } } },
                { feature: { properties: {} } }, // falls back to 1
            ],
        };
        expect(fg.getClusterCount(cluster)).toBe(9);
    });
});

describe('hideMarkers', () => {
    test('clears the layer and re-adds the cluster when one is configured', () => {
        const clearLayers = jest.fn();
        const addLayer = jest.fn();
        const clusterClear = jest.fn();
        const cluster = { clearLayers: clusterClear };
        const fg = makeFeatureGroup({
            layer: { clearLayers, addLayer },
            cluster,
        });

        fg.hideMarkers();

        expect(clearLayers).toHaveBeenCalled();
        expect(clusterClear).toHaveBeenCalled();
        expect(addLayer).toHaveBeenCalledWith(cluster);
    });

    test('only clears the layer when no cluster is configured', () => {
        const clearLayers = jest.fn();
        const addLayer = jest.fn();
        const fg = makeFeatureGroup({ layer: { clearLayers, addLayer } });

        fg.hideMarkers();

        expect(clearLayers).toHaveBeenCalled();
        expect(addLayer).not.toHaveBeenCalled();
    });
});

// Pinning the *current* (buggy) behavior of getArea: it computes `area`
// but never returns it (see source file, function getArea). Once the bug
// is fixed and `area` is returned, this assertion will fail and the test
// must be updated to expect the numeric result. See ticket #27937.
describe('getArea (BUG: missing return)', () => {
    test('currently returns undefined even with valid bounds — pinned for #27937', () => {
        const fg = makeFeatureGroup({
            geoMap: { map: { distance: () => 100 } },
        });
        const bounds = {
            isValid: () => true,
            getSouthWest: () => ({}),
            getNorthWest: () => ({}),
            getSouthEast: () => ({}),
        };
        expect(fg.getArea(bounds, {})).toBeUndefined();
    });

    test('returns 0 for invalid bounds', () => {
        const fg = makeFeatureGroup({ geoMap: { map: { distance: () => 0 } } });
        expect(fg.getArea({ isValid: () => false }, {})).toBe(0);
    });
});
