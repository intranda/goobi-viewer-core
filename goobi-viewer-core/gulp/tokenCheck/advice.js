/**
 * Advice for the complete token migration of a customer theme: which role replaces a colour or a fixed
 * text size in a component, and which values the theme binds in its token layer so that the look stays.
 * A suggestion is the nearest role by value (colours compared in OKLab); the visual check decides.
 */

const less = require('less');

/**
 * @param {string} text a colour as written in LESS: #hex, rgb()/rgba(), a CSS colour name or transparent
 * @returns {{r: number, g: number, b: number, a: number}|null}
 */
function parseColour(text) {
    const value = text.trim().toLowerCase();
    if (value === 'transparent') {
        return { r: 0, g: 0, b: 0, a: 0 };
    }
    if (less.data.colors[value]) {
        return parseColour(less.data.colors[value]);
    }
    const hex = value.match(/^#([0-9a-f]{3,8})$/);
    if (hex) {
        const digits = hex[1].length <= 4 ? [...hex[1]].map((c) => c + c).join('') : hex[1];
        if (digits.length !== 6 && digits.length !== 8) {
            return null;
        }
        const channel = (i) => parseInt(digits.slice(i, i + 2), 16);
        return { r: channel(0), g: channel(2), b: channel(4), a: digits.length === 8 ? channel(6) / 255 : 1 };
    }
    const rgb = value.match(/^rgba?\(\s*([\d.]+)\s*,?\s*([\d.]+)\s*,?\s*([\d.]+)\s*(?:[,/]\s*([\d.]+%?))?\s*\)$/);
    if (rgb) {
        const alpha = rgb[4] === undefined ? 1 : rgb[4].endsWith('%') ? parseFloat(rgb[4]) / 100 : parseFloat(rgb[4]);
        return { r: +rgb[1], g: +rgb[2], b: +rgb[3], a: alpha };
    }
    return lessFunctionColour(value);
}

const LESS_COLOUR_FUNCTION = /^(darken|lighten|fade|fadein|fadeout|saturate|desaturate|spin|mix|tint|shade)\(([\s\S]*)\)$/;

const splitArguments = (text) => {
    const args = [''];
    let depth = 0;
    for (const c of text) {
        depth += c === '(' ? 1 : c === ')' ? -1 : 0;
        if (c === ',' && depth === 0) {
            args.push('');
        } else {
            args[args.length - 1] += c;
        }
    }
    return args.map((a) => a.trim());
};

function lessFunctionColour(value) {
    const call = value.match(LESS_COLOUR_FUNCTION);
    if (!call) {
        return null;
    }
    const args = splitArguments(call[2]).map((arg) => {
        const colour = parseColour(arg);
        const number = arg.match(/^(-?[\d.]+)(%?)$/);
        return colour ? new less.tree.Color([colour.r, colour.g, colour.b], colour.a) : number ? new less.tree.Dimension(parseFloat(number[1]), number[2]) : null;
    });
    if (args.some((a) => !a)) {
        return null;
    }
    try {
        const result = less.functions.functionRegistry.get(call[1]).apply({}, args);
        const [r, g, b] = result.rgb.map(Math.round);
        return { r, g, b, a: result.alpha };
    } catch {
        return null;
    }
}

const toHex = ({ r, g, b }) => `#${[r, g, b].map((v) => Math.round(v).toString(16).padStart(2, '0')).join('')}`;
const toCss = (colour) => (colour.a < 1 ? `rgba(${[colour.r, colour.g, colour.b].map(Math.round).join(', ')}, ${Math.round(colour.a * 1000) / 1000})` : toHex(colour));

function oklab({ r, g, b }) {
    const linear = (v) => {
        const c = v / 255;
        return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
    };
    const [red, green, blue] = [linear(r), linear(g), linear(b)];
    const l = Math.cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue);
    const m = Math.cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue);
    const s = Math.cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue);
    return [0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s, 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s, 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s];
}

const distance = (a, b) => {
    const [x, y] = [oklab(a), oklab(b)];
    return Math.hypot(x[0] - y[0], x[1] - y[1], x[2] - y[2]);
};
const chroma = (colour) => Math.hypot(oklab(colour)[1], oklab(colour)[2]);

const SAME = 0.01;
const CLOSE = 0.06;
const GREY = 0.04;

/**
 * The values a suggestion is measured against: LESS constants, the light roles and palette of the
 * core with the theme's own bindings on top, and the theme's brand colour.
 *
 * @param {{core: Map<string, object>, theme: Map<string, object>}} parsed parseLess results per file
 * @returns {object} context for adviseColour, adviseFontSize and bindings; adviseColour notes the theme tokens it hands out in it
 */
function adviceContext({ core, theme }) {
    const statics = new Map();
    const themeStatics = new Map();
    for (const p of core.values()) {
        p.statics.forEach((s) => statics.set(s.name, s.value));
    }
    for (const [file, p] of theme) {
        if (/^subthemes\//.test(file)) {
            continue;
        }
        p.statics.forEach((s) => {
            statics.set(s.name, s.value);
            themeStatics.set(s.name, s.value);
        });
    }
    const tokens = new Map();
    const themeTokens = new Map();
    const take = (trees, mixin, into) => {
        for (const p of trees.values()) {
            p.declarations.filter((d) => d.stack.some((h) => mixin.test(h))).forEach((d) => into.forEach((map) => map.set(d.name, d.value)));
        }
    };
    take(core, /^\.gv-(?:palette|base|semantic-light)\(\)$/, [tokens]);
    take(theme, /^\.mx-theme-(?:palette|base|semantic-light)\(\)$/, [tokens, themeTokens]);

    const oldTokens = new Map();
    for (const [file, p] of theme) {
        if (/^subthemes\//.test(file)) {
            continue;
        }
        p.declarations.filter((d) => d.stack.some((h) => /^(?::root\b|\.mx-theme-light(?:\(\))?$)/.test(h))).forEach((d) => oldTokens.set(d.name, d.value));
    }
    const resolve = (value, depth = 0, extra = new Map()) => {
        if (depth > 12) {
            return value;
        }
        const next = value
            .replace(/var\(\s*(--[\w-]+)\s*(?:,\s*([^()]*))?\)/g, (match, name, fallback) => tokens.get(name) ?? extra.get(name) ?? fallback ?? match)
            .replace(/@\{?([\w-]+)\}?/g, (match, name) => statics.get(name) ?? match)
            .replace(/^~?['"](.*)['"]$/, '$1')
            .trim();
        return next === value ? next : resolve(next, depth + 1, extra);
    };
    const colourOf = (value) => parseColour(resolve(value));
    const brandOf = (value) => parseColour(resolve(value, 0, oldTokens));

    const brandSources = [themeTokens.get('--color-brand'), themeStatics.has('ci-color1') ? '@ci-color1' : null, oldTokens.get('--ci-color1')].filter(
        (s) => s && brandOf(s)?.a === 1
    );
    const brand = brandSources.map(brandOf);
    const brandHint = brandSources.length ? { value: toHex(brand[0]), source: brandSources[0].startsWith('@') ? brandSources[0] : null } : null;
    if (brandHint && !themeTokens.has('--color-brand')) {
        tokens.set('--color-brand', brandHint.value);
    }

    const roles = { fg: [], bg: [], border: [] };
    for (const [name, value] of tokens) {
        const family = name.match(/^--(fg|bg|border)-/)?.[1];
        if (!family || /print|document|stage|disabled|selected|pressed|hover/.test(name)) {
            continue;
        }
        const colour = colourOf(value);
        if (colour && colour.a === 1) {
            roles[family].push({ name, colour, meaning: /danger|success|warning|info|accent|highlight|entity|brand/.test(name) });
        }
    }
    const textRoles = [];
    for (const [name, value] of tokens) {
        const size = name.startsWith('--font-size-') && !name.endsWith('-prose') ? resolve(value).match(/(\d*\.?\d+)rem/) : null;
        if (size) {
            textRoles.push({ name, rem: parseFloat(size[1]) });
        }
    }
    const ownTokens = new Map();
    return { resolve, colourOf, roles, textRoles, brand, brandHint, themeStatics, ownTokens };
}

const FAMILY = [
    [/^(?:color|fill|stroke|caret-color|text-decoration-color|-webkit-text-fill-color|column-rule-color)$/, 'fg'],
    [/^background/, 'bg'],
    [/^(?:border|outline)/, 'border'],
    [/shadow$/, 'shadow'],
];

/**
 * Which role replaces each colour in one component declaration. A colour no role fits becomes a theme
 * token named after the component, e.g. --header-bg in layout/header.less.
 *
 * @param {string} what the declaration, "property: value"
 * @param {object} context from adviceContext
 * @param {string} [file] the component file, for the name of a theme token
 * @returns {{text: string, replacements: Array<{from: string, role: string, value: string, source: string|null, label: string|null, constants: string[], theme?: boolean}>}}
 */
function adviseColour(what, context, file = '') {
    const [, property = '', value = ''] = what.match(/^([\w-]+):\s*([\s\S]*)$/) ?? [];
    const family = FAMILY.find(([test]) => test.test(property))?.[1] ?? null;
    const component =
        file
            .replace(/^.*\//, '')
            .replace(/\.less$/, '')
            .replace(/^\d+_/, '')
            .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
            .toLowerCase()
            .replace(/[^\w-]+/g, '-')
            .replace(/^-+|-+$/g, '') || 'component';
    const functions = LESS_COLOUR_FUNCTION.source.slice(2, LESS_COLOUR_FUNCTION.source.indexOf(')'));
    const pattern = new RegExp(`\\b(?:${functions})\\((?:[^()]|\\([^()]*\\))*\\)|@[\\w-]+|#[0-9a-f]{3,8}\\b|rgba?\\([^()]*\\)|\\b[a-z]+\\b`, 'gi');
    const parts = [...value.matchAll(pattern)].map((m) => m[0]).filter((p) => context.colourOf(p));
    const replacements = [];
    const said = [];
    for (const part of [...new Set(parts)]) {
        const colour = context.colourOf(part);
        const source = part.startsWith('@') || LESS_COLOUR_FUNCTION.test(part) ? part : null;
        const constants = [...part.matchAll(/@([\w-]+)/g)].map((m) => m[1]);
        const applied = source?.match(/^(\w+)\(@([\w-]+)(?:,\s*(-?[\d.]+)%?)?.*$/);
        const label = applied ? [applied[2], applied[1], applied[3]].filter(Boolean).join('-') : source?.startsWith('@') ? source.slice(1) : null;
        const literal = toCss(colour);
        if (colour.a === 0) {
            continue;
        }
        if (family === 'shadow') {
            said.push(`${part} → var(--shadow-surface) or var(--shadow-elevated), or a theme token`);
            continue;
        }
        const own = `--${component}-${family ?? 'colour'}`;
        const replace = (role, extra = {}) => replacements.push({ from: part, role, value: literal, source, label, constants, ...extra });
        const ownToken = () => {
            const taken = context.ownTokens.get(own) ?? new Map();
            context.ownTokens.set(own, taken);
            if (!taken.has(literal)) {
                const suffix = label ?? `${toHex(colour).slice(1)}${colour.a < 1 ? `-a${Math.round(colour.a * 100)}` : ''}`;
                taken.set(literal, taken.size ? `${own}-${suffix.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()}` : own);
            }
            replace(taken.get(literal), { theme: true });
            said.push(`${part} → var(${taken.get(literal)}), a theme token`);
        };
        if (!family || colour.a < 1) {
            ownToken();
            continue;
        }
        if (context.brand.some((b) => distance(b, colour) < SAME)) {
            replace(`--${family}-brand`);
            said.push(`${part} → var(--${family}-brand)`);
            continue;
        }
        const exact = context.roles[family].find((role) => distance(role.colour, colour) < SAME);
        const grey = chroma(colour) < GREY;
        const candidates = context.roles[family].filter((role) => !role.meaning && (!grey || chroma(role.colour) < GREY));
        const nearest = exact ?? candidates.reduce((best, role) => (!best || distance(role.colour, colour) < distance(best.colour, colour) ? role : best), null);
        if (!nearest || (!grey && distance(nearest.colour, colour) > CLOSE)) {
            ownToken();
            continue;
        }
        replace(nearest.name);
        said.push(`${part} → var(${nearest.name})`);
    }
    return { text: said.join(', ') || 'keep it (no colour value)', replacements };
}

/**
 * Which text role replaces a fixed text size. The themes set 1rem to 10px (font-size: 62.5% on html).
 *
 * @param {string} what the declaration, "property: value"
 * @param {object} context from adviceContext
 * @returns {{text: string, from?: string, to?: string, note?: string|null}} from: the part of the value to replace, to: what replaces it, note: how close the role is
 */
function adviseFontSize(what, context) {
    const written = what.replace(/^[\w-]+:\s*/, '');
    const value = context.resolve(written);
    const size = value.match(/(\d*\.?\d+)(px|rem|pt)\b/);
    if (!size || !context.textRoles.length) {
        return { text: 'relative size: keep it, or use a text role such as var(--font-size-body)' };
    }
    if (/\b(?:clamp|min|max)\(/.test(value) && (value.match(/\d(?:px|rem|pt)\b/g) ?? []).length > 1) {
        return { text: 'fluid size: keep it, or replace the whole value by a text role such as var(--font-size-heading-large)' };
    }
    const unit = { rem: 1, px: 0.1, pt: 0.4 / 3 }[size[2]];
    const rem = parseFloat(size[1]) * unit;
    const nearest = context.textRoles.reduce((best, role) => (Math.abs(role.rem - rem) < Math.abs(best.rem - rem) ? role : best));
    const exact = Math.abs(nearest.rem - rem) < 0.05;
    const from = written.match(/@[\w-]+/)?.[0] ?? written.match(/(\d*\.?\d+)(px|rem|pt)\b/)?.[0];
    const note = exact ? null : `${nearest.rem}rem instead of ${Math.round(rem * 100) / 100}rem`;
    return { text: `var(${nearest.name})${note ? `  (${note})` : ''}`, from, to: `var(${nearest.name})`, note };
}

/**
 * What the theme binds in its token layer so that the roles carry its own colours: one palette line
 * per colour, one role line per role. A role asked to carry several colours is a choice for the theme.
 *
 * @param {Array<{from: string, role: string, value: string, source: string|null, theme?: boolean}>} replacements all of them
 * @param {object} context from adviceContext: a role already carrying the colour needs no line
 * @returns {{palette: Array<{name: string, value: string, source: string|null}>, roles: Array<{role: string, palette: string, uses: number, theme: boolean, others: Array<{value: string, source: string|null, uses: number}>}>}}
 */
function bindings(replacements, context) {
    const named = new Map();
    for (const r of [...replacements].sort((a, b) => Boolean(b.label) - Boolean(a.label))) {
        if (!named.has(r.value)) {
            const colour = parseColour(r.value);
            const plain = colour
                ? `${toHex(colour).slice(1)}${colour.a < 1 ? `-a${Math.round(colour.a * 100)}` : ''}`
                : r.value
                      .replace(/^#/, '')
                      .replace(/[^\w.]+/g, '-')
                      .replace(/\./g, '');
            const name = r.role.endsWith('-brand') && !r.theme ? 'brand' : (r.label ?? plain);
            named.set(
                r.value,
                name === 'brand'
                    ? '--color-brand'
                    : `--color-theme-${name
                          .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
                          .toLowerCase()
                          .replace(/^-+|-+$/g, '')}`
            );
        }
    }
    const paletteName = (r) => named.get(r.value);
    const byRole = new Map();
    for (const r of replacements) {
        const colours = byRole.get(r.role) ?? new Map();
        const entry = colours.get(r.value) ?? { value: r.value, source: r.source, palette: paletteName(r), uses: 0, theme: Boolean(r.theme) };
        entry.uses += 1;
        colours.set(r.value, entry);
        byRole.set(r.role, colours);
    }
    const palette = new Map();
    const roles = [];
    for (const [role, colours] of byRole) {
        const [main, ...others] = [...colours.values()].sort((a, b) => b.uses - a.uses);
        const current = main.theme ? null : context.colourOf(`var(${role})`);
        const wanted = parseColour(main.value);
        if (current && wanted && distance(current, wanted) < SAME && !others.length) {
            continue;
        }
        [main, ...others].forEach((c) => palette.set(c.palette, { name: c.palette, value: c.value, source: c.source }));
        roles.push({ role, palette: main.palette, uses: main.uses, theme: main.theme, others });
    }
    return { palette: [...palette.values()].sort((a, b) => a.name.localeCompare(b.name)), roles: roles.sort((a, b) => a.theme - b.theme || a.role.localeCompare(b.role)) };
}

module.exports = { parseColour, adviceContext, adviseColour, adviseFontSize, bindings };
