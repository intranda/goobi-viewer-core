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
function state(css, os, toggle) {
    const tokens = new Map();
    css.walkRules((rule) => {
        if (rule.selector !== ':root' && rule.selector !== `.color-theme-${toggle}`) return;
        for (let parent = rule.parent; parent.type !== 'root'; parent = parent.parent) {
            if (parent.type !== 'atrule' || parent.name !== 'media' || parent.params.replace(/\s/g, '') !== `(prefers-color-scheme:${os})`) return;
        }
        rule.nodes.filter((node) => node.type === 'decl').forEach((d) => tokens.set(d.prop, d.value));
    });
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
                expect(get('--accent-color')).toBe(effective === 'dark' ? '#feae1a' : '#f59f00');
                expect(get('--facet-control-radius')).toBe(get('--container-small-border-radius'));
                expect(get('--active-facets-badge-shadow-hover')).toBe('none');
            }
            for (const [fg, bg, base] of [
                ['--quick-filter-label-color', '--quick-filter-fields-bg', '--bg-content-box'],
                ['--quick-filter-label-color', '--quick-filter-bg', '--bg-content-box'],
                ['--datepicker-day-hasdata-fg', '--datepicker-day-hasdata-bg', '--datepicker-bg'],
                ['--datepicker-day-hasdata-hover-fg', '--datepicker-day-hasdata-hover-bg', '--datepicker-bg'],
                ['--wc-popover-item-hover-color', '--wc-popover-item-hover-bg', '--wc-popover-bg'],
                ['--active-facets-badge-color', '--active-facets-badge-background-hover', '--bg-content-box'],
            ]) {
                const ratio = contrast(color(get(fg)), over(color(get(bg)), color(get(base))));
                if (ratio < 4.5) throw new Error(`${name}/${os}/${toggle}: ${fg} on ${bg} = ${ratio}`);
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

const foundationPairs = [
    ['--print-fg', '--bg-document'],
    ['--print-link', '--bg-document'],
    ['--fg-document', '--bg-document'],
    ['--mei-note-active-color', '--bg-document'],
    ['--fg-inverse', '--archives-hero-bg'],
    ['--fg-inverse', '--archives-hero-gradient-bg'],
    ['--fg-secondary', '--bg-subtle'],
    ['--fg-secondary', '--bg-hover'],
    ['--fg-secondary', '--bg-danger'],
    ['--fg-inverse', '--ci-color1'],
    ['--fg-inverse', '--ci-hovercolor1'],
    ['--fg-inverse', '--bg-inverse'],
    ['--fg-inverse', '--bg-inverse-hover'],
    ['--fg-on-warning', '--bg-warning'],
    ['--fg-on-warning-hover', '--bg-warning'],
    ['--fg-selected', '--bg-selected'],
    ['--fg-popover', '--bg-popover'],
    ['--map-marker-primary-fg', '--map-marker-primary-bg'],
    ['--map-marker-secondary-fg', '--map-marker-secondary-bg'],
    ['--rss-badge-fg', '--rss-badge-bg'],
    ['--image-controls-region-active-color', '--image-controls-background'],
    ['--image-controls-region-active-color', '--image-controls-action-background'],
    ...['success', 'danger'].flatMap((status) => [
        [`--fg-on-${status}`, `--bg-${status}`],
        [`--fg-on-${status}`, `--bg-${status}-hover`],
    ]),
    ...['success', 'warning', 'danger', 'info'].map((status) => [`--fg-on-${status}-strong`, `--bg-${status}-strong`]),
    ...[
        '--fg-content-box',
        '--fg-secondary',
        '--fg-muted',
        '--placeholder',
        '--fg-danger',
        '--fg-success',
        '--fg-warning',
        '--ner-person-color',
        '--ner-location-color',
        '--ner-institution-color',
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
    const resolve = (value) => value.replace(/var\((--[\w-]+)\)/g, (_, name) => get(name));
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
        expect([':root', '.color-theme-dark', '.color-theme-light']).toContain(d.parent.selector);
    });
    const overridden = postcss.parse(css.toString() + ':root { --facet-control-radius: 7px; --active-facets-badge-shadow-hover: 0 0 1px; }');
    expect(state(overridden, 'dark', 'none')('--facet-control-radius')).toBe('7px');
    expect(state(overridden, 'dark', 'none')('--active-facets-badge-shadow-hover')).toBe('0 0 1px');
});

it.each(['light', 'dark'])('keeps chart and entity navigation marks distinguishable in %s', (mode) => {
    for (const [name, css] of variants) {
        const get = state(css, mode, 'none');
        for (const fg of ['--chronology-chart-color', '--ner-scale-handle-bg'])
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

function componentCascade(css, html, os, toggle, pseudo = '') {
    const dom = new JSDOM(`<html class="${toggle === 'none' ? '' : `color-theme-${toggle}`}"><body>${html}</body></html>`);
    const element = dom.window.document.querySelector('[data-test]');
    element.setAttribute('data-pseudo', pseudo);
    if (!modeStateCache.has(css)) modeStateCache.set(css, new Map());
    const states = modeStateCache.get(css);
    const stateKey = `${os}/${toggle}`;
    if (!states.has(stateKey)) states.set(stateKey, state(css, os, toggle));
    const get = states.get(stateKey);
    const fixtureKey = `${html}/${toggle}/${pseudo}`;
    if (!selectorMatchCache.has(fixtureKey)) selectorMatchCache.set(fixtureKey, new Map());
    const matchesCache = selectorMatchCache.get(fixtureKey);
    const winners = new Map();
    css.walkRules((rule) => {
        const declarations = rule.nodes.filter((node) => node.type === 'decl' && ['color', 'background', 'background-color', 'display'].includes(node.prop));
        if (!declarations.length) return;
        for (let p = rule.parent; p.type !== 'root'; p = p.parent) {
            if (p.type !== 'atrule' || p.name !== 'media') return;
            const condition = p.params.replace(/\s/g, '');
            if (condition !== 'screen' && condition !== `(prefers-color-scheme:${os})`) return;
        }
        for (const selector of rule.selectors) {
            if (selector.includes('::')) continue;
            const matchSelector = selector.replace(/:(focus-visible|focus-within|hover|focus|active)\b/g, '[data-pseudo~="$1"]');
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
    const resolve = (value) => value.replace(/var\((--[\w-]+)\)/g, (_, name) => get(name));
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
                expect(theme(`--entity-${group}-color`)).toBe(expected);
            }
        }
    }
});
