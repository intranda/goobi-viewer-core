/*
 * Mode wiring of the compiled token layer: the six OS × toggle states, the sealed admin
 * scope and the public token names. No colour or size value is asserted here; the source
 * rules live in gulp/tokenCheck.js and rendered colours are judged in the browser.
 */
const fs = require('fs');
const path = require('path');
const less = require('less');
const postcss = require('postcss');
const { LESS_ROOT, THEME_LESS_ROOT } = require('../tokenCheck');

const moduleRoot = path.resolve(__dirname, '../..');
const coreRoot = path.resolve(moduleRoot, LESS_ROOT);
const themeRoot = path.resolve(moduleRoot, THEME_LESS_ROOT);
const variants = new Map();

async function compile(root, entry, suffix = '') {
    const filename = path.join(root, entry);
    return (
        await less.render(fs.readFileSync(filename, 'utf8') + suffix, {
            filename,
            paths: [root, path.join(root, 'crowdsourcing')],
        })
    ).css;
}

beforeAll(async () => {
    const core = await compile(coreRoot, 'constructor.less');
    variants.set('core', postcss.parse(core));
    if (!fs.existsSync(themeRoot)) return;
    for (const [name, entry, suffix] of [
        ['reference', 'constructor.less', ''],
        ['legacyFlag', 'constructor.less', '\n@colorModes: false;'],
        ['subtheme1', 'subthemes/subtheme1/subThemeConstructor.less', ''],
        ['subtheme2', 'subthemes/subtheme2/subThemeConstructor.less', ''],
    ])
        variants.set(name, postcss.parse(core + (await compile(themeRoot, entry, suffix))));
}, 30000);

// Evaluate only the documented root/mode wiring, in stylesheet order.
// scope 'admin' adds the admin binding, which outranks every root and mode selector.
const ADMIN_SELECTOR = ':root:has(#admin)';
function state(css, os, toggle, scope = 'page') {
    const tokens = new Map();
    const admin = new Map();
    css.walkRules((rule) => {
        const isAdmin = scope === 'admin' && rule.selector === ADMIN_SELECTOR;
        if (rule.selector !== ':root' && rule.selector !== `.color-theme-${toggle}` && !isAdmin) return;
        for (let parent = rule.parent; parent.type !== 'root'; parent = parent.parent) {
            if (parent.type !== 'atrule' || parent.name !== 'media' || parent.params.replace(/\s/g, '') !== `(prefers-color-scheme:${os})`) return;
        }
        rule.nodes.filter((node) => node.type === 'decl').forEach((d) => (isAdmin ? admin : tokens).set(d.prop, d.value));
    });
    for (const [prop, value] of admin) tokens.set(prop, value);
    function resolve(name, seen = new Set()) {
        if (seen.has(name)) throw new Error(`Token cycle: ${name}`);
        if (!tokens.has(name)) throw new Error(`Missing token: ${name}`);
        return tokens.get(name).replace(/var\((--[\w-]+)\)/g, (_, next) => resolve(next, new Set([...seen, name])));
    }
    return resolve;
}

/* MODE WIRING — OS preference and explicit toggle resolve to one colour scheme */
it.each(['light', 'dark'])('resolves the six mode states to the effective colour scheme under OS %s', (os) => {
    for (const [name, css] of variants)
        for (const toggle of ['none', 'light', 'dark']) {
            const effective = toggle === 'none' ? os : toggle;
            expect({ name, toggle, scheme: state(css, os, toggle)('color-scheme') }).toEqual({ name, toggle, scheme: effective });
        }
});

it('restores the saved mode in the crowdsourcing editor template', () => {
    const template = path.resolve(themeRoot, '../../templateCrowdsourcing.html');
    if (!fs.existsSync(template)) return;
    const html = fs.readFileSync(template, 'utf8').match(/<html\b[^>]*>/)[0];
    expect(html).toContain(`class="#{sessionBean.get('colorTheme')}"`);
});

/* ADMIN SCOPE — fixed light binding, sealed against OS mode, toggle and theme */
function adminTokens(css) {
    const names = [];
    css.walkRules((rule) => {
        if (rule.selector === ADMIN_SELECTOR) rule.walkDecls(/^--/, (d) => names.push(d.prop));
    });
    return names;
}

it('binds the admin scope once at the top level of the core token layer', () => {
    const rules = [];
    variants.get('core').walkRules((rule) => {
        if (rule.selector === ADMIN_SELECTOR) rules.push(rule);
    });
    expect(rules.map((rule) => rule.parent.type)).toEqual(['root']);
    const publicTokens = new Set();
    variants.get('core').walkRules((rule) => {
        if (rule.selector === ':root' && rule.parent.type === 'root') rule.walkDecls(/^--/, (d) => publicTokens.add(d.prop));
    });
    expect(adminTokens(variants.get('core'))).toEqual(expect.arrayContaining([...publicTokens]));
});

it.each(['light', 'dark'])('keeps the admin scope identical for every variant and toggle under OS %s', (os) => {
    const core = variants.get('core');
    const reference = state(core, 'light', 'none', 'admin');
    const names = adminTokens(core);
    for (const [name, css] of variants)
        for (const toggle of ['none', 'light', 'dark']) {
            const get = state(css, os, toggle, 'admin');
            expect({ name, toggle, scheme: get('color-scheme') }).toEqual({ name, toggle, scheme: 'light' });
            for (const token of names) expect({ name, toggle, token, value: get(token) }).toEqual({ name, toggle, token, value: reference(token) });
        }
});

it('keeps a later root override out of the admin scope', () => {
    const core = variants.get('core');
    const reference = state(core, 'light', 'none', 'admin');
    const overridden = postcss.parse(core.toString() + ':root { --bg-subtle: red; --radius-button: 9px; } .color-theme-dark { --fg-link: red; }');
    const get = state(overridden, 'dark', 'dark', 'admin');
    for (const token of ['--bg-subtle', '--radius-button', '--fg-link']) expect({ token, value: get(token) }).toEqual({ token, value: reference(token) });
});

/* PUBLIC TOKEN NAMES — a rename or removal is a deliberate decision with a changelog entry */
it('keeps the public token names of the core stable', () => {
    const expected = JSON.parse(fs.readFileSync(path.resolve(moduleRoot, 'gulp/tokenCheck.baseline.json'), 'utf8')).publicTokens;
    const emitted = new Set();
    variants.get('core').walkRules((rule) => {
        if (rule.selector === ':root' && rule.parent.type === 'root') rule.walkDecls(/^--/, (d) => emitted.add(d.prop));
    });
    const added = [...emitted].filter((n) => !expected.includes(n)).sort();
    const removed = expected.filter((n) => !emitted.has(n));
    expect({ added, removed, note: 'update publicTokens in gulp/tokenCheck.baseline.json, RENAMED_TOKENS in gulp/tokenCheck.js and the theme changelog' }).toEqual({
        added: [],
        removed: [],
        note: expect.any(String),
    });
});
