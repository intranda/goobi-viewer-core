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

// The token layer uses hex, relative rgb and premultiplied sRGB mixing.
// Keep the evaluator strict: unfamiliar color syntax must fail the test.
function color(value) {
    if (value === 'transparent') return [0, 0, 0, 0];
    if (/^#[0-9a-f]{3,8}$/i.test(value)) {
        let hex = value.slice(1);
        if (hex.length === 3) hex = [...hex].map((c) => c + c).join('');
        return [0, 2, 4].map((i) => parseInt(hex.slice(i, i + 2), 16)).concat(hex.length === 8 ? parseInt(hex.slice(6), 16) / 255 : 1);
    }
    let m = value.match(/^rgb\(from (.*) r g b\s*\/\s*([\d.]+)(%)?\)$/);
    if (m)
        return color(m[1])
            .slice(0, 3)
            .concat(Number(m[2]) / (m[3] ? 100 : 1));
    m = value.match(/^color-mix\(in srgb,\s*(.*?)\s+([\d.]+)%,\s*(.*?)\)$/);
    if (m) {
        const a = color(m[1]),
            b = color(m[3]),
            weight = Number(m[2]) / 100;
        const alpha = a[3] * weight + b[3] * (1 - weight);
        return a
            .slice(0, 3)
            .map((v, i) => (v * a[3] * weight + b[i] * b[3] * (1 - weight)) / alpha)
            .concat(alpha);
    }
    throw new Error(`Unsupported color: ${value}`);
}
const over = (a, b) =>
    a
        .slice(0, 3)
        .map((v, i) => v * a[3] + b[i] * (1 - a[3]))
        .concat(1);
const luminance = (c) =>
    c
        .slice(0, 3)
        .map((v) => v / 255)
        .map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4))
        .reduce((sum, v, i) => sum + v * [0.2126, 0.7152, 0.0722][i], 0);
function contrast(fg, bg) {
    const a = luminance(over(fg, bg)),
        b = luminance(bg);
    return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
}

it('checks the contrast evaluator against known opaque and translucent pairs', () => {
    expect(contrast(color('#000'), color('#fff'))).toBe(21);
    expect(over(color('rgb(from #000 r g b / 50%)'), color('#fff'))).toEqual([127.5, 127.5, 127.5, 1]);
    expect(contrast(color('#1565c0'), color('#5b6473'))).toBeLessThan(1.1);
});

it.each(['light', 'dark'])('keeps all six mode states consistent under OS %s', (os) => {
    for (const [name, css] of variants)
        for (const toggle of ['none', 'light', 'dark']) {
            const get = state(css, os, toggle),
                effective = toggle === 'none' ? os : toggle;
            expect({ name, toggle, scheme: get('color-scheme') }).toEqual({ name, toggle, scheme: effective });
            if (name !== 'core') {
                expect(get('--accent')).toBe(effective === 'dark' ? '#feae1a' : '#f59f00');
                expect(get('--facet-control-radius')).toBe(get('--radius-container-small'));
                expect(get('--active-facets-badge-shadow-hover')).toBe('none');
            }
            for (const [fg, bg, base] of [
                ['--quick-filter-label-fg', '--quick-filter-fields-bg', '--bg-content-box'],
                ['--quick-filter-label-fg', '--quick-filter-bg', '--bg-content-box'],
                ['--datepicker-day-fg-has-data', '--datepicker-day-bg-has-data', '--datepicker-bg'],
                ['--datepicker-day-fg-has-data-hover', '--datepicker-day-bg-has-data-hover', '--datepicker-bg'],
                ['--widget-calendar-popover-item-fg-hover', '--widget-calendar-popover-item-bg-hover', '--widget-calendar-popover-bg'],
                ['--active-facets-badge-fg', '--active-facets-badge-bg-hover', '--bg-content-box'],
            ]) {
                const ratio = contrast(color(get(fg)), over(color(get(bg)), color(get(base))));
                if (ratio < 4.5) throw new Error(`${name}/${os}/${toggle}: ${fg} on ${bg} = ${ratio}`);
            }
        }
});

it.each(['light', 'dark'])('keeps base defaults and Reference differences independent of OS %s', (os) => {
    for (const [name, css] of variants) {
        for (const toggle of ['none', 'light', 'dark']) {
            const get = state(css, os, toggle);
            const core = name === 'core';
            const scale = state(css, 'light', 'none')('--font-scale');
            if (core) expect(scale).toBe('1');
            expect(Number(scale)).toBeGreaterThan(0);
            expect(get('--font-scale')).toBe(scale);
            expect(get('--font-size-small')).toBe(`calc(1.2rem * ${scale})`);
            expect(get('--font-size-medium')).toBe(`calc(1.4rem * ${scale})`);
            expect(get('--font-size-normal')).toBe(`calc(1.6rem * ${scale})`);
            expect(get('--font-size-body')).toBe(`calc(1.6rem * ${scale})`);
            expect(get('--radius-button')).toBe(core ? '0' : '6px');
            expect(get('--radius-container')).toBe(core ? '0' : '8px');
            expect(get('--radius-container-small')).toBe(core ? '0.25rem' : '4px');
            expect(get('--search-toolbar-button-radius')).toBe(get('--radius-button'));
            expect(get('--active-facets-badge-radius')).toBe(get('--radius-button'));
            expect(get('--font-family').startsWith("'Inter'")).toBe(!core);
        }
    }
});

it('keeps legacy LESS font overrides as inputs to the core token defaults', async () => {
    const css = postcss.parse(await compile(coreRoot, 'constructor.less', '\n@fontFamily: serif; @fontSmall: 1.7rem; @fontNormal: 1.9rem;'));
    const get = state(css, 'light', 'none');
    expect(get('--font-family')).toBe('serif');
    expect(get('--font-size-small')).toBe('calc(1.7rem * 1)');
    expect(get('--font-size-body')).toBe('calc(1.9rem * 1)');
});

it('applies runtime base overrides to inner controls and inherited frontend typography', () => {
    for (const name of ['core', 'reference']) {
        if (!variants.has(name)) continue;
        const css = postcss.parse(
            variants.get(name).toString() + ':root { --radius-button: 17px; --radius-container-small: 13px; --font-size-small: 2rem; --font-size-medium: 2.4rem; }'
        );
        for (const os of ['light', 'dark']) {
            for (const toggle of ['none', 'light', 'dark']) {
                for (const [html, property, expected] of [
                    ['<button data-test class="btn search-list__option-button">View</button>', 'border-radius', '17px'],
                    ['<div class="custom-control custom-control--select"><form><select data-test><option>Sort</option></select></form></div>', 'border-radius', '13px'],
                    ['<div class="search-list__excel-export"><span data-test class="excel-export-loader">Loading</span></div>', 'font-size', '2rem'],
                    ['<div data-test class="search-list__options">View options</div>', 'font-size', '2.4rem'],
                ]) {
                    expect(componentCascade(css, html, os, toggle)(property)).toBe(expected);
                }
            }
        }
    }
});

it('exposes each subtheme brand to core consumers in both modes', () => {
    for (const [name, light, dark] of [
        ['subtheme1', '#8a0e13', '#f28b90'],
        ['subtheme2', '#095d42', '#6bc9a5'],
    ]) {
        if (!variants.has(name)) continue;
        expect(state(variants.get(name), 'light', 'none')('--brand')).toBe(light);
        expect(state(variants.get(name), 'dark', 'none')('--brand')).toBe(dark);
        expect(state(variants.get(name), 'dark', 'none')('--datepicker-header-bg')).toBe(light);
    }
});

it('scales every text role once across themes and color modes while leaving layout units alone', () => {
    const sizes = {
        body: '1.6rem',
        'body-small': '1.4rem',
        'body-large': '1.8rem',
        caption: '1.2rem',
        prose: '1.8rem',
        'heading-small': '2.1rem',
        'heading-medium': '2.4rem',
        'heading-large': '3.2rem',
    };
    for (const css of variants.values()) {
        const scaled = postcss.parse(css.toString() + ':root { --font-scale: 1.25; }');
        for (const os of ['light', 'dark'])
            for (const toggle of ['none', 'light', 'dark']) {
                const get = state(scaled, os, toggle);
                for (const [role, size] of Object.entries(sizes)) expect(get(`--font-size-${role}`)).toBe(`calc(${size} * 1.25)`);
                expect(get('--line-height-body')).toBe('1.5');
                expect(get('--line-height-prose')).toBe('1.65');
                const admin = state(scaled, os, toggle, 'admin');
                expect(admin('--font-scale')).toBe('1');
                for (const [role, size] of Object.entries(sizes)) expect(admin(`--font-size-${role}`)).toBe(`calc(${size} * 1)`);
            }
        const rootFontSizes = [];
        scaled.walkDecls('font-size', (d) => {
            if (d.parent.selector === 'html') rootFontSizes.push(d.value);
        });
        expect(rootFontSizes.length).toBeGreaterThan(0);
        expect(rootFontSizes.every((value) => value === '62.5%')).toBe(true);
    }
});

it('lets themes override one text role without rescaling it or changing the other roles', () => {
    const css = postcss.parse(variants.get('core').toString() + ':root { --font-scale: 2; --font-size-prose: 2rem; }');
    expect(state(css, 'dark', 'light')('--font-size-prose')).toBe('2rem');
    expect(state(css, 'dark', 'light')('--font-size-body')).toBe('calc(1.6rem * 2)');
    expect(state(css, 'dark', 'light', 'admin')('--font-size-prose')).toBe('calc(1.8rem * 1)');
});

it('connects component text to the shared scale instead of theme repair selectors', () => {
    for (const name of ['core', 'reference']) {
        const original = variants.get(name);
        if (!original) continue;
        const css = postcss.parse(original.toString() + ':root { --font-scale: 2; }');
        for (const [html, value] of [
            ['<h1 data-test>Page title</h1>', '3.2rem'],
            ['<div class="widget"><h2 data-test class="widget__title">Widget</h2></div>', '2.1rem'],
            ['<div data-test class="content-component__htmltext">Reading text</div>', '1.8rem'],
            ['<div class="search-list__excel-export"><span data-test class="excel-export-loader">Loading</span></div>', '1.2rem'],
            ['<div data-test class="archives-tree">Archive tree</div>', '1.6rem'],
        ])
            expect(componentCascade(css, html, 'light', 'none')('font-size')).toBe(`calc(${value} * 2)`);
    }
});

const foundationPairs = [
    ['--fg-print', '--bg-document'],
    ['--fg-print-link', '--bg-document'],
    ['--fg-document', '--bg-document'],
    ['--mei-note-fg-active', '--bg-document'],
    ['--fg-inverse', '--archives-hero-bg'],
    ['--fg-inverse', '--archives-hero-gradient-bg'],
    ['--fg-secondary', '--bg-subtle'],
    ['--fg-secondary', '--bg-hover'],
    ['--fg-secondary', '--bg-danger'],
    ['--fg-on-brand', '--bg-brand'],
    ['--fg-on-brand', '--bg-brand-hover'],
    ['--brand', '--bg-general'],
    ['--brand', '--bg-content-box'],
    ['--fg-inverse', '--bg-inverse'],
    ['--fg-inverse', '--bg-inverse-hover'],
    ['--fg-on-warning', '--bg-warning'],
    ['--fg-on-warning-hover', '--bg-warning'],
    ['--fg-selected', '--bg-selected'],
    ['--fg-popover', '--bg-popover'],
    ['--map-marker-primary-fg', '--map-marker-primary-bg'],
    ['--map-marker-secondary-fg', '--map-marker-secondary-bg'],
    ['--rss-badge-fg', '--rss-badge-bg'],
    ['--image-controls-region-fg-active', '--image-controls-bg'],
    ['--image-controls-region-fg-active', '--image-controls-action-bg'],
    ...['success', 'danger'].flatMap((status) => [
        [`--fg-on-${status}`, `--bg-${status}`],
        [`--fg-on-${status}`, `--bg-${status}-hover`],
    ]),
    ...['success', 'warning', 'danger', 'info'].map((status) => [`--fg-on-${status}-strong`, `--bg-${status}-strong`]),
    ...[
        '--fg-content-box',
        '--fg-secondary',
        '--fg-muted',
        '--fg-placeholder',
        '--fg-danger',
        '--fg-success',
        '--fg-warning',
        '--ner-person-fg',
        '--ner-location-fg',
        '--ner-institution-fg',
    ].flatMap((fg) => ['--bg-general', '--bg-content-box', '--bg-subtle', '--bg-popover', '--bg-canvas'].map((bg) => [fg, bg])),
];

function checkFoundationContrast(get, context) {
    for (const [fg, bg] of foundationPairs) {
        const ratio = contrast(color(get(fg)), over(color(get(bg)), color(get('--bg-content-box'))));
        if (ratio < 4.5) throw new Error(`${context}: ${fg} on ${bg} = ${ratio}`);
    }
}

it.each(['light', 'dark'])('keeps foundation text readable under OS %s and every explicit mode', (os) => {
    for (const [name, css] of variants) for (const toggle of ['none', 'light', 'dark']) checkFoundationContrast(state(css, os, toggle), `${name}/${os}/${toggle}`);
});

it('detects a theme override that breaks a foundation foreground/background pair', () => {
    const broken = postcss.parse(variants.get('core').toString() + ':root { --rss-badge-fg: var(--bg-rss); }');
    expect(() => checkFoundationContrast(state(broken, 'light', 'none'), 'broken theme')).toThrow('--rss-badge-fg on --rss-badge-bg = 1');
});

it.each(['light', 'dark'])('keeps status text and surfaces paired in the compiled components in %s', (mode) => {
    const css = variants.get('core'),
        get = state(css, mode, 'none');
    const declarations = (selector) => {
        const result = new Map();
        css.walkRules((rule) => {
            if (rule.parent.type !== 'root' || !rule.selectors.includes(selector)) return;
            rule.walkDecls((d) => result.set(d.prop, d.value));
        });
        return result;
    };
    const resolve = (value) => value.replace(/var\((--[\w-]+)\)/g, (_, name) => (name.startsWith('--_') && winners.has(name) ? resolve(winners.get(name).value) : get(name)));
    for (const status of ['success', 'danger']) {
        const base = declarations(`.btn--${status}`);
        for (const suffix of ['', ':hover', ':focus', ':active']) {
            const values = new Map([...base, ...declarations(`.btn--${status}${suffix}`)]);
            expect(contrast(color(resolve(values.get('color'))), color(resolve(values.get('background-color'))))).toBeGreaterThanOrEqual(4.5);
        }
    }
    for (const status of ['green', 'yellow', 'red']) {
        const values = declarations(`.copyright-indicator__traffic-light-style-circle.-${status}`);
        expect(contrast(color(resolve(values.get('color'))), color(resolve(values.get('background'))))).toBeGreaterThanOrEqual(4.5);
    }
});

it('allows a later root override to reach the faceted-search widgets', () => {
    const css = variants.get('reference');
    if (!css) return;
    css.walkDecls(/^(--facet-control-radius|--active-facets-badge-shadow-hover)$/, (d) => {
        expect([':root', '.color-theme-dark', '.color-theme-light', ADMIN_SELECTOR]).toContain(d.parent.selector);
    });
    const overridden = postcss.parse(css.toString() + ':root { --facet-control-radius: 7px; --active-facets-badge-shadow-hover: 0 0 1px; }');
    expect(state(overridden, 'dark', 'none')('--facet-control-radius')).toBe('7px');
    expect(state(overridden, 'dark', 'none')('--active-facets-badge-shadow-hover')).toBe('0 0 1px');
});

it.each(['light', 'dark'])('keeps chart and entity navigation marks distinguishable in %s', (mode) => {
    for (const [name, css] of variants) {
        const get = state(css, mode, 'none');
        for (const fg of ['--chronology-chart-fg', '--ner-scale-handle-bg'])
            for (const bg of ['--bg-content-box', '--bg-subtle']) {
                const ratio = contrast(color(get(fg)), color(get(bg)));
                if (ratio < 3) throw new Error(`${name}/${mode}: ${fg} on ${bg} = ${ratio}`);
            }
    }
});

it('restores the saved mode in the crowdsourcing editor template', () => {
    const template = path.resolve(themeRoot, '../../templateCrowdsourcing.html');
    if (!fs.existsSync(template)) return;
    const html = fs.readFileSync(template, 'utf8').match(/<html\b[^>]*>/)[0];
    expect(html).toContain(`class="#{sessionBean.get('colorTheme')}"`);
});

/* COMPOSED CORE AND THEME CASCADE */
const { JSDOM } = require('jsdom');
const cssTree = require('css-tree');
const specificityCache = new Map();
const selectorMatchCache = new Map();
const modeStateCache = new WeakMap();
function specificity(selector) {
    if (specificityCache.has(selector)) return specificityCache.get(selector);
    const score = (node) => {
        if (node.type === 'IdSelector') return 1000000;
        if (node.type === 'ClassSelector' || node.type === 'AttributeSelector') return 1000;
        if (node.type === 'TypeSelector') return node.name === '*' ? 0 : 1;
        if (node.type === 'PseudoElementSelector') return 1;
        if (node.type === 'PseudoClassSelector') {
            if (node.name === 'where') return 0;
            if (['is', 'not', 'has'].includes(node.name)) {
                const args = [];
                node.children.forEach((child) => args.push(cssTree.generate(child)));
                return Math.max(...postcss.list.comma(args.join(',')).map(specificity));
            }
            return 1000;
        }
        return node.children ? node.children.reduce((sum, child) => sum + score(child), 0) : 0;
    };
    const result = score(cssTree.parse(selector, { context: 'selector' }));
    specificityCache.set(selector, result);
    return result;
}

function componentCascade(css, html, os, toggle, pseudo = '', scope = 'page') {
    const dom = new JSDOM(`<html class="${toggle === 'none' ? '' : `color-theme-${toggle}`}"><body${scope === 'admin' ? ' id="admin"' : ''}>${html}</body></html>`);
    const element = dom.window.document.querySelector('[data-test]');
    element.setAttribute('data-pseudo', pseudo);
    if (!modeStateCache.has(css)) modeStateCache.set(css, new Map());
    const states = modeStateCache.get(css);
    const stateKey = `${os}/${toggle}/${scope}`;
    if (!states.has(stateKey)) states.set(stateKey, state(css, os, toggle, scope));
    const get = states.get(stateKey);
    const fixtureKey = `${html}/${toggle}/${pseudo}/${scope}`;
    if (!selectorMatchCache.has(fixtureKey)) selectorMatchCache.set(fixtureKey, new Map());
    const matchesCache = selectorMatchCache.get(fixtureKey);
    const winners = new Map();
    css.walkRules((rule) => {
        const declarations = rule.nodes.filter(
            (node) =>
                node.type === 'decl' &&
                (node.prop.startsWith('--_') ||
                    [
                        'color',
                        'background',
                        'background-color',
                        'display',
                        'font-family',
                        'font-size',
                        'border-radius',
                        'box-shadow',
                        'height',
                        'min-height',
                        'line-height',
                        'width',
                        'min-width',
                    ].includes(node.prop))
        );
        if (!declarations.length) return;
        for (let p = rule.parent; p.type !== 'root'; p = p.parent) {
            if (p.type !== 'atrule' || p.name !== 'media') return;
            const condition = p.params.replace(/\s/g, '');
            if (condition !== 'screen' && condition !== `(prefers-color-scheme:${os})`) return;
        }
        for (const selector of rule.selectors) {
            if (selector.includes('::')) continue;
            // jsdom's selector engine cannot match nested :has() in :where().
            // Evaluate this known frontend guard against the fixture scope;
            // retain the original selector for specificity calculation.
            const matchSelector = selector
                .replace(':where(html:not(:has(#admin)))', scope === 'admin' ? ':not(*)' : ':where(html)')
                .replace(/:(focus-visible|focus-within|hover|focus|active)\b/g, '[data-pseudo~="$1"]');
            if (!matchesCache.has(selector)) {
                try {
                    matchesCache.set(selector, element.matches(matchSelector));
                } catch {
                    matchesCache.set(selector, false);
                }
            }
            if (!matchesCache.get(selector)) continue;
            const weight = specificity(selector);
            for (const d of declarations) {
                const prop = d.prop === 'background' ? 'background-color' : d.prop;
                const rank = weight + (d.important ? 1000000000 : 0);
                if ((winners.get(prop)?.rank ?? -1) <= rank) winners.set(prop, { value: d.value, rank, selector });
            }
        }
    });
    dom.window.close();
    const resolve = (value) => value.replace(/var\((--[\w-]+)\)/g, (_, name) => (name.startsWith('--_') && winners.has(name) ? resolve(winners.get(name).value) : get(name)));
    return (prop) => {
        const value = winners.get(prop)?.value;
        return value === undefined ? undefined : resolve(value);
    };
}

const componentFixtures = [
    ['success', '<button data-test class="btn btn--success">Save</button>', '--bg-content-box'],
    ['danger', '<button data-test class="btn btn--danger">Delete</button>', '--bg-content-box'],
    ['default', '<button data-test class="btn btn--default">Cancel</button>', '--bg-content-box'],
    ['fulltext', '<div class="view-fulltext__wrapper"><div data-test class="view-fulltext__content">Text</div></div>', '--bg-content-box'],
    ['fulltext link', '<div class="view-fulltext__content"><a data-test href="#">Link</a></div>', '--bg-content-box'],
    ['login input', '<input data-test class="form-control user-login-modal__form-control">', '--bg-content-box'],
    ['disabled input', '<input data-test disabled class="form-control">', '--bg-content-box'],
    ['bookmark counter', '<span data-test class="bookmark-navigation__counter">3</span>', '--bg-content-box'],
    ['bookmark action', '<button data-test class="search-list__hit-options-button search-list__hit-options-button-bookmark">Bookmark</button>', '--bg-content-box'],
    ['calendar day', '<div class="widget-calendar"><span data-test class="flatpickr-day -has-data-">12</span></div>', '--bg-content-box'],
    ['calendar current', '<div class="widget-calendar"><span data-test class="flatpickr-day -has-data- -current-issue-">12</span></div>', '--bg-content-box'],
    ['TOC header link', '<div class="widget-toc__title-expand"><a data-test href="#">Expand</a></div>', '--bg-content-box'],
    ['geo action', '<div class="widget-geofacetting__actions"><button data-test class="btn btn--clean">Reset</button></div>', '--bg-content-box'],
    ...['person', 'location', 'institution'].map((entity) => [`NER ${entity}`, `<span data-test class="ner-${entity}">Name</span>`, '--bg-content-box']),
    ...['work', 'person', 'agent', 'award', 'event'].map((entity) => [
        `entity dropdown ${entity}`,
        `<button data-test class="btn search-advanced__entity-tabs-dropdown-toggle-button -result-group-docstructtype__${entity}">Group</button>`,
        '--bg-content-box',
    ]),
    ...['work', 'person', 'agent', 'award', 'event'].map((entity) => [
        `entity ${entity}`,
        `<button data-test class="btn btn--full search-advanced__action-search-button -result-group-docstructtype__${entity}">Search</button>`,
        '--bg-content-box',
    ]),
];

it.each(['light', 'dark'])(
    'checks final component colors including theme overrides under OS %s',
    (os) => {
        for (const [name, css] of variants) {
            if (name === 'core') continue;
            for (const toggle of ['none', 'light', 'dark']) {
                const get = state(css, os, toggle);
                for (const [label, html, base] of componentFixtures) {
                    for (const pseudo of ['', 'hover', 'focus', 'active']) {
                        const computed = componentCascade(css, html, os, toggle, pseudo);
                        const fg = computed('color');
                        const bg = computed('background-color');
                        if (!fg) throw new Error(`${name}/${os}/${toggle}/${label}: missing foreground`);
                        const background = !bg || bg === 'none' ? color(get(base)) : over(color(bg), color(get(base)));
                        const ratio = contrast(color(fg), background);
                        if (ratio < 4.5) throw new Error(`${name}/${os}/${toggle}/${label}/${pseudo}: ${fg} on ${bg || get(base)} = ${ratio}`);
                    }
                }
            }
        }
    },
    120000
);

it('detects a later component override even when all root token pairs remain valid', () => {
    const css = variants.get('reference');
    if (!css) return;
    const broken = postcss.parse(css.toString() + '.btn--success { color: var(--bg-success); }');
    checkFoundationContrast(state(broken, 'dark', 'none'), 'valid tokens');
    const get = componentCascade(broken, componentFixtures[0][1], 'dark', 'none');
    expect(contrast(color(get('color')), color(get('background-color')))).toBe(1);
});

it.each(['light', 'dark'])('uses central toggle visibility under OS %s', (os) => {
    const css = variants.get('reference');
    if (!css) return;
    for (const toggle of ['none', 'light', 'dark']) {
        const effective = toggle === 'none' ? os : toggle;
        for (const target of ['light', 'dark']) {
            const get = componentCascade(css, `<button data-test class="color-theme-switch color-theme-switch-${target}"></button>`, os, toggle);
            expect(get('display')).toBe(target === effective ? 'none' : 'flex');
        }
    }
});

it('evaluates selector specificity including zero-weight and alternative selectors', () => {
    expect(specificity(':where(#ignored) .item')).toBe(1000);
    expect(specificity('button:is(.item, #selected)')).toBe(1000001);
    expect(specificity('button:not(:disabled)')).toBe(1001);
});

/* NER TEXT AND ENTITY GROUP COLORS */
it.each(['light', 'dark'])('keeps reference entity group colors independent of core NER text under OS %s', (os) => {
    for (const [name, css] of variants) {
        if (name === 'core') continue;
        for (const toggle of ['none', 'light', 'dark']) {
            const core = state(variants.get('core'), os, toggle);
            const theme = state(css, os, toggle);
            for (const entity of ['person', 'location', 'institution']) {
                expect(theme(`--fg-entity-${entity}`)).toBe(core(`--fg-entity-${entity}`));
            }
            for (const [group, expected] of Object.entries({ work: '#dc7e2e', person: '#b13b29', agent: '#568fb2', award: '#2e8d38', event: '#8966ca' })) {
                expect(theme(`--entity-${group}-bg`)).toBe(expected);
            }
        }
    }
});

/* ADMIN SCOPE — fixed light binding, sealed against OS mode and theme */
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
    expect(adminTokens(variants.get('core')).length).toBeGreaterThan(300);
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

it('pins the admin scope to the legacy backend values', () => {
    const get = state(variants.get('core'), 'dark', 'none', 'admin');
    expect(get('--fg-link')).toBe('#015999');
    expect(get('--fg-link-hover')).toBe('#134ea0');
    expect(get('--color-brand-hover')).toBe('#3365a9');
    expect(get('--bg-subtle')).toBe('#f7f7f7');
    expect(get('--bg-hover')).toBe('#f2f3f4');
    expect(get('--fg-content-box')).toBe('#1a1a1a');
    expect(get('--bg-inverse')).toBe('#1b2330');
    expect(get('--widget-topbar-icon')).toBe('#fff');
    expect(get('--fg-placeholder')).toBe('rgb(from #000 r g b / 25%)');
    expect(get('--radius-button')).toBe('0');
    expect(get('--font-size-body')).toBe('calc(1.6rem * 1)');
});

it('keeps a later root override out of the admin scope', () => {
    const overridden = postcss.parse(variants.get('core').toString() + ':root { --bg-subtle: red; --radius-button: 9px; } .color-theme-dark { --fg-link: red; }');
    const get = state(overridden, 'dark', 'dark', 'admin');
    expect(get('--bg-subtle')).toBe('#f7f7f7');
    expect(get('--radius-button')).toBe('0');
    expect(get('--fg-link')).toBe('#015999');
});

it.each(['light', 'dark'])('renders admin controls with the legacy colors under OS %s', (os) => {
    const css = variants.get('core');
    for (const toggle of ['none', 'light', 'dark']) {
        const cascade = (html, pseudo = '') => componentCascade(css, html, os, toggle, pseudo, 'admin');
        const button = cascade('<button data-test class="btn btn--default">Cancel</button>');
        expect([toggle, button('background-color'), button('color')]).toEqual([toggle, '#f7f7f7', '#555']);
        expect(cascade('<button data-test class="btn btn--default">Cancel</button>', 'hover')('background-color')).toBe('#f2f3f4');
        expect(cascade('<button data-test class="btn btn--full">Save</button>', 'hover')('background-color')).toBe('#3365a9');
        expect(cascade('<a data-test href="#">Link</a>')('color')).toBe('#015999');
        expect(cascade('<div data-test class="widget widget-admin">Admin</div>')('background-color')).toBe('#1b2330');
    }
});

/* BRAND ROLES — components and features read roles, never the brand primitive */
const WIRING_SELECTORS = new Set([':root', '.color-theme-dark', '.color-theme-light', ADMIN_SELECTOR]);
it('reads the brand only through roles outside the token wiring', () => {
    for (const [name, css] of variants) {
        const offenders = [];
        css.walkDecls((d) => {
            if (WIRING_SELECTORS.has(d.parent.selector)) return;
            if (/var\(--(ci-color1|ci-hovercolor1|ci-color1-dark)\)/.test(d.value)) offenders.push(`${name}: ${d.parent.selector} { ${d.prop}: ${d.value} }`);
        });
        expect(offenders).toEqual([]);
    }
});

it.each(['light', 'dark'])('keeps filled brand surfaces strong and brand accents readable under OS %s', (os) => {
    for (const [name, css] of variants)
        for (const toggle of ['none', 'light', 'dark']) {
            const get = state(css, os, toggle);
            expect({ name, toggle, fill: get('--bg-brand') }).toEqual({ name, toggle, fill: get('--color-brand') });
            expect({ name, toggle, hover: get('--bg-brand-hover') }).toEqual({ name, toggle, hover: get('--color-brand-hover') });
            const effective = toggle === 'none' ? os : toggle;
            expect({ name, toggle, accent: get('--brand') }).toEqual({ name, toggle, accent: get(effective === 'dark' ? '--color-brand-dark' : '--color-brand') });
        }
});

/* BORDER ROLES — one value type per family */
it.each(['light', 'dark'])('keeps every border role a plain color under OS %s', (os) => {
    const names = [];
    variants.get('core').walkRules((rule) => {
        if (rule.selector === ':root' && rule.parent.type === 'root') rule.walkDecls(/^--border/, (d) => names.push(d.prop));
    });
    expect(names).toEqual(expect.arrayContaining(['--border-faint', '--border-input', '--border-default', '--border-strong']));
    expect(names).not.toEqual(expect.arrayContaining(['--border']));
    expect(names).not.toEqual(expect.arrayContaining(['--border-controls']));
    for (const [name, css] of variants)
        for (const toggle of ['none', 'light', 'dark'])
            for (const scope of ['page', 'admin']) {
                const get = state(css, os, toggle, scope);
                for (const token of new Set(names)) expect(() => color(get(token))).not.toThrow();
            }
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

it('overrides Bootstrap sizes for standalone and grouped controls without leaking into admin', () => {
    const bootstrap = fs.readFileSync(path.resolve(coreRoot, '../libs/bs/bootstrap.custom.css'), 'utf8');
    const fixtures = [
        ['<button data-test class="btn">Search</button>', '1.6rem'],
        ['<input data-test class="form-control">', '1.6rem'],
        ['<select data-test class="custom-select"><option>Option</option></select>', '1.6rem'],
        ['<button data-test class="btn btn-sm">Search</button>', '1.4rem'],
        ['<button data-test class="btn btn-lg">Search</button>', '1.8rem'],
    ];
    for (const [variant, size] of [
        ['sm', '1.4rem'],
        ['lg', '1.8rem'],
    ]) {
        fixtures.push([`<div class="input-group input-group-${variant}"><select data-test class="custom-select"><option>Option</option></select></div>`, size]);
        for (const side of ['prepend', 'append'])
            fixtures.push([`<div class="input-group input-group-${variant}"><div class="input-group-${side}"><button data-test class="btn">Search</button></div></div>`, size]);
    }
    for (const name of ['core', 'reference']) {
        if (!variants.has(name)) continue;
        const css = postcss.parse(bootstrap + variants.get(name).toString() + ':root { --font-scale: 2; }');
        for (const [html, size] of fixtures) expect(componentCascade(css, html, 'light', 'none')('font-size')).toBe(`calc(${size} * 2)`);
        // The frontend adapter must not replace the admin Bootstrap form size.
        const adminBaseline = postcss.parse(bootstrap + variants.get(name).toString() + ':root { --font-scale: 1; }');
        const adminSize = (tree) => componentCascade(tree, '<input data-test class="form-control">', 'dark', 'dark', '', 'admin')('font-size');
        expect(adminSize(css)).toBe(adminSize(adminBaseline));
        expect(adminSize(css)).toBe('1.4rem');
    }
});

it('keeps the scaled select inside its image-control box and releases fixed paginator heights', () => {
    for (const name of ['core', 'reference']) {
        if (!variants.has(name)) continue;
        const css = postcss.parse(variants.get(name).toString() + ':root { --font-scale: 2; }');
        for (const outer of ['', 'fullscreen']) {
            const action = componentCascade(css, `<div class="${outer}"><div data-test class="image-controls__action select"></div></div>`, 'light', 'none');
            expect(action('height')).toBe('max(36px, calc(calc(1.4rem * 2) * 1.5 + 0.6rem + 2px))');
            const wrapper = componentCascade(
                css,
                `<div class="${outer}"><div class="image-controls__action select"><div data-test class="custom-control--select"></div></div></div>`,
                'light',
                'none'
            );
            expect(wrapper('height')).toBe('100%');
            expect(wrapper('min-height')).toBe('0');
        }
        const paginator = componentCascade(css, '<div data-test class="inputfield-paginator__navigate-text inputfield-paginator__navigate">Long page label</div>', 'light', 'none');
        expect(paginator('height')).toBe('auto');
        expect(paginator('min-height')).toBe('35px');
    }
});
