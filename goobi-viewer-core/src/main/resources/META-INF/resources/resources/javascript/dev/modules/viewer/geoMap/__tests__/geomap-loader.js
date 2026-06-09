/**
 * Test helper that loads viewerJS.geoMap.* files into the test's global
 * scope via indirect eval and returns the populated viewerJS namespace.
 *
 * The geoMap files use the `var viewerJS = (function (viewer) { ... })
 * (viewerJS || {}, jQuery)` IIFE pattern. Plain require() does NOT work
 * here: in CJS, the module-local `var viewerJS` shadows any previously
 * seeded `global.viewerJS`, so the IIFE always runs against an empty
 * object — and `viewer.GeoMap.featureGroup = ...` in the dependent file
 * blows up because `viewer.GeoMap` is undefined.
 *
 * The trick used here mirrors crowdsourcing-loader.js: indirect eval
 * `(0, eval)(src)` runs the source in the global scope, where the
 * module's `var viewerJS` becomes a global property, and pre-seeded
 * `global.viewerJS.GeoMap` survives the IIFE.
 *
 * Usage:
 *     const viewerJS = require('./geomap-loader')([
 *         'viewerJS.geoMap.featureGroup.js',
 *     ]);
 *
 * geoMap.js (which defines the GeoMap constructor) is always loaded
 * first; pass any extra geoMap-family filenames as the array argument.
 *
 * Note: this filename does NOT end in ".test.js" so Jest's testMatch
 * skips it.
 */
const fs = require('fs');
const path = require('path');

const sourceDir = path.resolve(__dirname, '..');

// viewerJS.geoMap.js short-circuits if viewer.GeoMap already exists, so
// loading it once per test file is correct. featureGroup.js attaches
// methods to viewer.GeoMap.prototype and viewer.GeoMap.featureGroup, so
// the GeoMap constructor must exist before it loads.
const baseFiles = ['viewerJS.geoMap.js'];

module.exports = function loadGeoMap(extraFiles) {
    // Reset between test files: a previous test may have left viewerJS
    // populated, which would trip geoMap.js's `if (viewer.GeoMap) return`
    // guard and leave us with `var viewerJS = undefined`.
    global.viewerJS = {};

    // rxjs is used inside constructors (Subject) and inside init() (fromEvent).
    // We only test prototype methods directly, so a no-op stub is enough to
    // keep the IIFE from throwing if anything references rxjs at load time.
    if (typeof global.rxjs === 'undefined') {
        global.rxjs = {
            Subject: function () {
                return { next: function () {}, complete: function () {}, subscribe: function () {} };
            },
            fromEvent: function () {
                return {
                    pipe: function () {
                        return { subscribe: function () {} };
                    },
                };
            },
            operators: {
                map: function (fn) {
                    return fn;
                },
            },
        };
    }

    const files = baseFiles.concat(extraFiles || []);
    files.forEach(function (file) {
        (0, eval)(fs.readFileSync(path.join(sourceDir, file), 'utf8'));
    });
    return global.viewerJS;
};
