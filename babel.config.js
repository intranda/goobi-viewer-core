// Babel configuration used only by Jest (gulp build pipelines do not consume Babel).
// Targets the current Node version because Jest runs in Node, not the browser. This lets us
// write ES module imports/exports in test fixtures and the few ESM-style source modules
// (e.g. statistics/charts/indexCharts.js) without altering the browser-side build.
//
// Jest's rootDir is the parent of goobi-viewer-core/, so Babel resolves preset names from
// here. The preset itself is installed in goobi-viewer-core/node_modules — resolve by
// absolute path to bridge that gap.
const path = require('path');

module.exports = {
    presets: [
        [path.resolve(__dirname, 'goobi-viewer-core/node_modules/@babel/preset-env'), { targets: { node: 'current' } }]
    ]
};
