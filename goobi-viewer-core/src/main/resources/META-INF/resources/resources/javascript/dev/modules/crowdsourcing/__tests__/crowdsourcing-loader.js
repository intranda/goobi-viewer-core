/**
 * Test helper that loads Crowdsourcing's namespace and any requested
 * sub-modules into the test's global scope via indirect eval.
 *
 * The Crowdsourcing files use a `var X = (function(x){...})(X)` IIFE
 * pattern that depends on `X` being a global at parse time. Plain
 * require() in Node breaks that — see the long comment in
 * Crowdsourcing.Annotation.Plaintext.test.js for the gory detail.
 *
 * Usage:
 *     const Crowdsourcing = require('./crowdsourcing-loader')([
 *         'Crowdsourcing.Annotation.Foo.js',
 *     ]);
 *
 * The base files (Crowdsourcing.js + Crowdsourcing.Annotation.js) are
 * always loaded; pass any sub-module filenames as extras.
 *
 * Note: this filename does NOT end in ".test.js" so Jest's testMatch
 * skips it.
 */
const fs = require('fs');
const path = require('path');

const sourceDir = path.resolve(__dirname, '..');

const baseFiles = ['Crowdsourcing.js', 'Crowdsourcing.Annotation.js'];

module.exports = function loadCrowdsourcing(extraFiles) {
    const files = baseFiles.concat(extraFiles || []);
    files.forEach(function (file) {
        (0, eval)(fs.readFileSync(path.join(sourceDir, file), 'utf8'));
    });
    return global.Crowdsourcing;
};
