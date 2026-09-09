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
        ['noModes', 'constructor.less', '\n@colorModes: false;'],
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
                expect(get('--accent-color')).toBe(name !== 'noModes' && effective === 'dark' ? '#feae1a' : '#f59f00');
                expect(get('--facet-control-radius')).toBe('0');
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
