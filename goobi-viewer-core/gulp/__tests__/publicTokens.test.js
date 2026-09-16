/*
 * The public token names are the contract with customer themes. A rename or removal is a
 * deliberate change: update gulp/publicTokens.json, RENAMED_TOKENS in gulp/themeTokens.js
 * and the theme changelog.
 */
const fs = require('fs');
const path = require('path');
const less = require('less');
const postcss = require('postcss');
const { LESS_ROOT, MODULE_ROOT } = require('../tokenCheck');

it('keeps the public token names of the core stable', async () => {
    const root = path.resolve(MODULE_ROOT, LESS_ROOT);
    const filename = path.join(root, 'constructor.less');
    const { css } = await less.render(fs.readFileSync(filename, 'utf8'), { filename, paths: [root, path.join(root, 'crowdsourcing')] });
    const emitted = new Set();
    postcss.parse(css).walkRules((rule) => {
        if (rule.selector === ':root' && rule.parent.type === 'root') rule.walkDecls(/^--/, (d) => emitted.add(d.prop));
    });
    const expected = JSON.parse(fs.readFileSync(path.resolve(MODULE_ROOT, 'gulp/publicTokens.json'), 'utf8'));
    const added = [...emitted].filter((n) => !expected.includes(n)).sort();
    const removed = expected.filter((n) => !emitted.has(n));
    expect({ added, removed }).toEqual({ added: [], removed: [] });
}, 30000);
