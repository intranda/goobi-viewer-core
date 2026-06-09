/**
 * Unit tests for viewerJS.GeoMapCms.
 *
 * The constructor wires up a viewerJS.GeoMap instance; we replace
 * that constructor with a stub that just stores the config so we
 * can assert on the highlight/visibility transformations the source
 * applies BEFORE handing the layers to GeoMap.
 *
 * createFilterQuery is a pure URL-template helper and is tested
 * directly via the prototype.
 */
const viewerJS = require('../viewerJS.geoMapCms.js');

// Stub the GeoMap constructor before each test so we can capture the
// config it was called with (the source mutates layer features in
// place during construction).
let lastGeoMapConfig;
beforeEach(() => {
    lastGeoMapConfig = null;
    viewerJS.GeoMap = function (config) {
        lastGeoMapConfig = config;
        this.layers = [];
    };
});

describe('createFilterQuery', () => {
    test('substitutes {lng} / {lat} from feature.geometry.coordinates and URI-encodes the rest', () => {
        const query = viewerJS.GeoMapCms.prototype.createFilterQuery('WKT_COORDS:"Intersects(POINT({lng} {lat}))"', { geometry: { coordinates: [10.5, 51.2] } });
        // The template replacement happens; URI-encoding adds the
        // standard `filterQuery=` prefix.
        expect(query.startsWith('filterQuery=')).toBe(true);
        expect(query).toContain('10.5');
        expect(query).toContain('51.2');
        // Reserved-but-encoded characters: : ( ) " stay as %xx.
        expect(query).toContain('WKT_COORDS');
    });

    test('encodes "+" via the _PLUS_ → %2B sentinel so encodeURI does not pass it through unchanged', () => {
        const query = viewerJS.GeoMapCms.prototype.createFilterQuery('distErrPct=0.0+x', { geometry: { coordinates: [0, 0] } });
        // The "+" must come out as %2B, not raw +.
        expect(query).toContain('%2B');
        expect(query.endsWith('+x')).toBe(false);
    });
});

describe('constructor: config merging', () => {
    test('deep-merges the caller config onto the defaults', () => {
        const cfg = {
            mapType: 'SOLR_QUERY',
            map: { mapId: 'myMap', layers: [] },
        };
        const cms = new viewerJS.GeoMapCms(cfg);
        expect(cms.config.mapType).toBe('SOLR_QUERY');
        expect(cms.config.map.mapId).toBe('myMap');
        // Default fields not overridden survive: e.g. iconPath default.
        expect(cms.config.map.iconPath).toBe('/resources/images/map');
    });
});

describe('constructor: highlight + visibility transformations', () => {
    function makeFeature(properties) {
        return { properties: properties };
    }

    // The constructor calls $.extend(true, {}, _defaults, config) — a
    // deep copy — and mutates the COPY. We therefore assert on the
    // cloned features inside cms.config, not on the originals.
    test('flags the matching documentId with highlighted=true on every layer', () => {
        const cfg = {
            documentIdToHighlight: 'X',
            map: {
                layers: [{ features: [makeFeature({ documentId: 'X' }), makeFeature({ documentId: 'Y' })] }],
            },
        };
        const cms = new viewerJS.GeoMapCms(cfg);
        const features = cms.config.map.layers[0].features;
        expect(features[0].properties.highlighted).toBe(true);
        expect(features[1].properties.highlighted).toBeUndefined();
    });

    test('does nothing when documentIdToHighlight is undefined', () => {
        const cfg = {
            map: { layers: [{ features: [makeFeature({ documentId: 'X' })] }] },
        };
        const cms = new viewerJS.GeoMapCms(cfg);
        expect(cms.config.map.layers[0].features[0].properties.highlighted).toBeUndefined();
    });

    test('hides features whose page is not in pagesToShow when their page is defined', () => {
        const cfg = {
            pagesToShow: [1, 3],
            map: {
                layers: [
                    {
                        features: [
                            makeFeature({ page: 1 }),
                            makeFeature({ page: 2 }),
                            makeFeature({ page: 3 }),
                            // features without a `page` are intentionally ignored
                            makeFeature({ name: 'no page' }),
                        ],
                    },
                ],
            },
        };
        const cms = new viewerJS.GeoMapCms(cfg);
        const features = cms.config.map.layers[0].features;
        expect(features[0].properties.visible).toBeUndefined(); // page 1 — in list
        expect(features[1].properties.visible).toBe(false); // page 2 — not in list
        expect(features[2].properties.visible).toBeUndefined(); // page 3 — in list
        expect(features[3].properties.visible).toBeUndefined(); // no page — ignored
    });
});

describe('constructor: GeoMap instantiation', () => {
    test('hands the merged map config to viewerJS.GeoMap', () => {
        const cfg = { map: { mapId: 'X', layers: [] } };
        new viewerJS.GeoMapCms(cfg);
        expect(lastGeoMapConfig).toBeDefined();
        expect(lastGeoMapConfig.mapId).toBe('X');
    });
});
