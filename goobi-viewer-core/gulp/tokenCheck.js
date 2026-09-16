/*
Token check — six sentences the LESS tree keeps, one line per finding.

  1  Values stay out of components: no colour, no static @-colour, no fixed text size. Read a role.
  2  What a component reads exists in the token layer.
  3  Tokens and mode branches live only in the token layer.
  4  Light and dark declare the same tokens.
  5  Colour literals only in the palette; the palette is read by the roles only.
  6  The admin binds exactly what admin pages read.

Core findings fail the build, the watcher and `npm test`. Theme findings are printed as
information only: a theme may lag behind the core contract. The check reads the LESS sources
as written; gulp/publicTokens.json freezes the public token names (see publicTokens.test.js).
*/

const fs = require('fs');
const path = require('path');
const less = require('less');

const MODULE_ROOT = path.resolve(__dirname, '..');
const LESS_ROOT = 'src/main/resources/META-INF/resources/resources/css/less';
const THEME_ROOT = '../goobi-viewer-theme-reference/WebContent/resources/themes/reference';
const THEME_LESS_ROOT = `${THEME_ROOT}/css/less`;

/**
 * Which theme is checked: GV_THEME_LESS_ROOT (a theme's css/less directory) or the reference
 * submodule. GV_THEMECHECK=0 skips the theme.
 *
 * @returns {{lessRoot: string, root: string, enabled: boolean}}
 */
function themePaths() {
    const lessRoot = process.env.GV_THEME_LESS_ROOT || THEME_LESS_ROOT;
    return { lessRoot, root: lessRoot.replace(/[\\/]css[\\/]less[\\/]?$/, ''), enabled: process.env.GV_THEMECHECK !== '0' };
}

/* ---------- where things live ---------- */

const PALETTE_FILE = 'misc/palette.less';
const TOKENS_FILE = 'misc/tokens.less';
const ADMIN_PALETTE_FILE = 'misc/paletteAdmin.less';
const ADMIN_TOKENS_FILE = 'misc/tokensAdmin.less';
const VARIABLES_FILE = 'misc/variables.less';
const TOKEN_LAYER = [PALETTE_FILE, TOKENS_FILE, ADMIN_PALETTE_FILE, ADMIN_TOKENS_FILE];
const PALETTE_FILES = [PALETTE_FILE, ADMIN_PALETTE_FILE];
const ADMIN_FILES = [ADMIN_PALETTE_FILE, ADMIN_TOKENS_FILE];
const ROOT_SELECTORS = [':root', '.color-theme-dark', '.color-theme-light', ':root:has(#admin)'];
const TOKEN_SELECTORS = new Set([...ROOT_SELECTORS, '@media (prefers-color-scheme: dark)', '@media (prefers-color-scheme: light)']);
// Backend trees keep their static colours by decision (the admin never follows a theme).
const STATIC_COLOUR_TREES = /^(?:admin\/|cms\/views\/|cms\/includes\/mediaSelect\.less$|crowdsourcing\/admin\/)/;
// Trees that never render on an admin page; everything else is admin-relevant for sentence 6.
const FRONTEND_ONLY = /^(?:views\/|archives\/|cms\/(?:templates|contentComponents|components|tags)\/|crowdsourcing\/(?!admin\/)|widgets\/(?!widgets\.less$))/;
const adminRelevant = (file) => !FRONTEND_ONLY.test(file) && file !== PALETTE_FILE && file !== TOKENS_FILE;
// A theme keeps its token layer in these files; literals belong in its palette mixins.
const THEME_LAYER = /^(?:misc\/(?:mixins|palette|tokens|variables)\.less$|subthemes\/[^/]+\/subThemeConstructor\.less$)/;
const THEME_PALETTE_MIXIN = /^\.mx-theme-[\w-]*palette\(\)$/;
const SUBTHEME_CONSTRUCTOR = /^subthemes\/[^/]+\/subThemeConstructor\.less$/;

/* ---------- syntax ---------- */

const NAMED_COLOURS = Object.keys(less.data.colors).join('|');
const SEMANTIC_MIXIN = /^\.[\w-]*semantic[\w-]*\(\)$/;
const MODE_MIXIN = /^(\.[\w-]+)-(light|dark)\(\)$/;
const MIXIN_DEFINITION = /^\.[\w-]+\([^)]*\)(?: when .+)?$/;
const DETACHED_DEFINITION = /^@[\w-]+:$/;
const DECLARATION = /^(--[\w-]+)\s*:\s*([\s\S]*)$/;
const STATIC = /^@([\w-]+)\s*:/;
const READ = /var\(\s*(--[\w-]+)\s*(,)?/g;
const AT_USE = /@([\w-]+)/g;
const INTERNAL = /^--_/;
const MODE_BRANCH = /prefers-color-scheme|\.color-theme-(dark|light)|@colorModes/;
// Any colour: hex, colour functions (relative `from` syntax reads a token and is fine) and keywords.
const COLOR_LITERAL = new RegExp(`#[0-9a-f]{3,8}\\b|\\b(?:rgba?|hsla?|hwb|lab|lch|oklab|oklch|color)\\((?!\\s*from\\b)|(?:^|[\\s,(])(?:${NAMED_COLOURS})(?=[\\s,)]|$)`, 'i');
// A colour written into a component: hex, numeric rgb()/hsl() or a keyword in value position.
const COMPONENT_COLOR = new RegExp(`#[\\da-f]{3}(?:[\\da-f](?:[\\da-f]{2}(?:[\\da-f]{2})?)?)?\\b|\\b(?:rgba?|hsla?)\\(\\s*[+-]?(?:\\d|\\.\\d)|(?:^|[\\s,(])(?:${NAMED_COLOURS})(?=[\\s,)!]|$)`, 'i');
const FIXED_TYPE_SIZE = /(?:\d*\.)?\d+(?:px|rem|pt|pc|in|cm|mm|q|vw|vh|vmin|vmax)\b/i;
const FIXED_GEOMETRY_NOTE = '// Fixed icon/marker geometry; independent of text scale.';
const FONT_CONSTANT = /^font(?:Small|Medium|Normal|Large|XLarge)$/;
const isTypographyProperty = (name) => name === 'font-size' || name === 'font';

/* ---------- parsing ---------- */

function stripComments(source) {
    let out = '';
    let quote = null;
    let paren = 0;
    for (let i = 0; i < source.length; i++) {
        const c = source[i];
        const d = source[i + 1];
        if (quote) {
            out += c;
            if (c === '\\') {
                out += d === undefined ? '' : d;
                i++;
            } else if (c === quote) {
                quote = null;
            }
            continue;
        }
        if (c === '"' || c === "'") {
            quote = c;
            out += c;
            continue;
        }
        if (c === '/' && d === '*') {
            const end = source.indexOf('*/', i + 2);
            const stop = end === -1 ? source.length : end + 2;
            out += source.slice(i, stop).replace(/[^\n]/g, '');
            i = stop - 1;
            continue;
        }
        if (c === '/' && d === '/' && paren === 0) {
            const end = source.indexOf('\n', i);
            i = (end === -1 ? source.length : end) - 1;
            continue;
        }
        if (c === '(') paren++;
        else if (c === ')') paren = Math.max(0, paren - 1);
        out += c;
    }
    return out;
}

// Ordinary CSS strings are data; LESS escaped strings become CSS syntax.
function withoutStrings(text) {
    return text.replace(/(~?)(["'])(?:\\[\s\S]|(?!\2)[^\\])*?\2/g, (match, escaped) => (escaped ? match.slice(2, -1) : ' '));
}

function colorValue(text) {
    return withoutStrings(text).replace(/\burl\((?:[^()]|\([^()]*\))*\)/gi, ' ');
}

function staticColorNames(parsed) {
    const definitions = parsed?.statics || [];
    const names = new Set(definitions.filter((d) => COLOR_LITERAL.test(colorValue(d.value))).map((d) => d.name));
    let previousSize;
    do {
        previousSize = names.size;
        for (const d of definitions) {
            if ([...colorValue(d.value).matchAll(AT_USE)].some((m) => names.has(m[1]))) names.add(d.name);
        }
    } while (names.size !== previousSize);
    return names;
}

function collectReads(text, line, stack, into) {
    for (const m of withoutStrings(text).matchAll(READ)) {
        into.push({ name: m[1], fallback: m[2] === ',', line, stack });
    }
}

/**
 * Minimal LESS reader: statements, block headers and their nesting.
 *
 * @param {string} source LESS source.
 * @returns {{declarations: Array, properties: Array, reads: Array, statics: Array, blocks: Array}}
 */
function parseLess(source) {
    const text = stripComments(source);
    const result = { declarations: [], properties: [], reads: [], statics: [], blocks: [] };
    const stack = [];
    let buf = '';
    let start = null;
    let line = 1;
    let quote = null;
    let paren = 0;
    let interpolation = false;

    const headers = () => stack.map((b) => b.header);

    const statement = () => {
        const stmt = buf.trim();
        const at = start;
        buf = '';
        start = null;
        if (!stmt) return;
        const context = headers();
        const property = stmt.match(/^((?:--)?[\w@{}-]+)(?:\+_?)?\s*:\s*([\s\S]*)$/);
        if (property && (!property[1].startsWith('@') || property[1].startsWith('@{'))) {
            result.properties.push({ name: property[1], value: property[2].trim(), line: at, stack: context });
        }
        const declaration = stmt.match(DECLARATION);
        if (declaration) {
            result.declarations.push({ name: declaration[1], value: declaration[2].trim(), line: at, stack: context });
            collectReads(declaration[2], at, context, result.reads);
            return;
        }
        const stat = stmt.match(STATIC);
        if (stat && context.length === 0) {
            result.statics.push({ name: stat[1], value: stmt.slice(stmt.indexOf(':') + 1).trim(), line: at });
            collectReads(stmt, at, context, result.reads);
            return;
        }
        collectReads(stmt, at, context, result.reads);
    };

    for (let i = 0; i < text.length; i++) {
        const c = text[i];
        if (c === '\n') line++;
        if (quote) {
            buf += c;
            if (c === quote && text[i - 1] !== '\\') quote = null;
            continue;
        }
        if (c === '"' || c === "'") {
            quote = c;
            if (start === null) start = line;
            buf += c;
            continue;
        }
        if (c === '{' && text[i - 1] === '@') {
            interpolation = true;
            buf += c;
            continue;
        }
        if (c === '}' && interpolation) {
            interpolation = false;
            buf += c;
            continue;
        }
        if (c === '(') paren++;
        else if (c === ')') paren = Math.max(0, paren - 1);
        if (paren === 0 && c === '{') {
            const header = buf.trim().replace(/\s+/g, ' ');
            const block = { header, line: start === null ? line : start, stack: headers() };
            collectReads(header, block.line, block.stack, result.reads);
            result.blocks.push(block);
            stack.push(block);
            buf = '';
            start = null;
            continue;
        }
        if (paren === 0 && c === '}') {
            statement();
            stack.pop();
            continue;
        }
        if (paren === 0 && c === ';') {
            statement();
            continue;
        }
        if (start === null && !/\s/.test(c)) start = line;
        buf += c;
    }
    statement();
    return result;
}

/* ---------- rules ---------- */

const violation = (rule, file, token, line, message) => ({ rule, file, token, line, message });

/** Tokens read via var() or via a used @alias across a parsed tree. */

/* ---------- findings ---------- */

const finding = (file, line, what, fix) => ({ file, line, what, fix });
const flattened = (stack) => stack.join(' ');
const inMixin = (stack) => stack.some((h) => MIXIN_DEFINITION.test(h) || DETACHED_DEFINITION.test(h));
const underRoot = (stack) => {
    const context = stack.filter((h) => !MIXIN_DEFINITION.test(h) && !DETACHED_DEFINITION.test(h) && h !== '&');
    return stack.length > 0 && context.every((h) => TOKEN_SELECTORS.has(h)) && context.filter((h) => !h.startsWith('@')).length <= 1;
};
const parseAll = (files) => new Map([...files].map(([file, source]) => [file, parseLess(source)]));

// Static LESS constants that carry a fixed text size, following alias chains.
function staticTypeSizes(parsedMaps) {
    const definitions = parsedMaps.flatMap((m) => [...m.values()].flatMap((p) => p.statics));
    const names = new Set();
    let previous;
    do {
        previous = names.size;
        for (const d of definitions) {
            const value = withoutStrings(d.value);
            if (FIXED_TYPE_SIZE.test(value) || [...value.matchAll(AT_USE)].some((m) => names.has(m[1]))) names.add(d.name);
        }
    } while (names.size !== previous);
    return names;
}

// Sentence 1 for one component file: colours, static colours and fixed text sizes in property values.
function componentValues(file, p, source, staticColours, typeSizes, out) {
    const lines = source.split('\n');
    for (const d of p.properties) {
        const value = colorValue(d.value);
        const what = `${d.name}: ${d.value.replace(/\s+/g, ' ')}`;
        if (COMPONENT_COLOR.test(value)) out.push(finding(file, d.line, what, 'colour written into the component — read a role, e.g. var(--fg-content-box) or var(--bg-content-box)'));
        else if ([...value.matchAll(AT_USE)].some((m) => staticColours.has(m[1]))) out.push(finding(file, d.line, what, 'static colour constant — read a role instead'));
        if (!isTypographyProperty(d.name)) continue;
        const text = withoutStrings(d.value);
        const fixed = FIXED_TYPE_SIZE.test(text) || [...text.matchAll(AT_USE)].some((m) => typeSizes.has(m[1]) || FONT_CONSTANT.test(m[1]));
        if (fixed && !lines[d.line - 1]?.includes(FIXED_GEOMETRY_NOTE)) out.push(finding(file, d.line, what, 'fixed text size — use a text role such as var(--font-size-body)'));
    }
}

// Sentence 2 for one file.
function reads(file, p, known, out) {
    const internalHere = new Set(p.declarations.filter((d) => INTERNAL.test(d.name)).map((d) => d.name));
    for (const r of p.reads) {
        if (INTERNAL.test(r.name)) {
            if (!r.fallback && !internalHere.has(r.name)) out.push(finding(file, r.line, `var(${r.name})`, 'internal token without a declaration in this file — declare it here or give the read a fallback'));
        } else if (!known.has(r.name)) {
            out.push(finding(file, r.line, `var(${r.name})`, `no token of this name — check the spelling, or declare it in ${TOKENS_FILE}`));
        }
    }
}

// Sentence 4 across the mixins of a layer.
function modeSymmetry(mixins, out) {
    for (const [mixin, declared] of mixins) {
        const mode = mixin.match(MODE_MIXIN);
        if (!mode) continue;
        const counterpart = `${mode[1]}-${mode[2] === 'light' ? 'dark' : 'light'}()`;
        const other = mixins.get(counterpart) || new Map();
        for (const [name, site] of declared) if (!other.has(name)) out.push(finding(site.file, site.line, name, `declared in ${mixin} but not in ${counterpart} — the other mode keeps the previous value`));
    }
}

function mixinTokens(parsed, files) {
    const mixins = new Map();
    for (const file of files) {
        for (const d of parsed.get(file)?.declarations || []) {
            const mixin = d.stack.find((h) => MIXIN_DEFINITION.test(h))?.replace(/ when .+$/, '');
            if (!mixin) continue;
            if (!mixins.has(mixin)) mixins.set(mixin, new Map());
            mixins.get(mixin).set(d.name, { file, line: d.line });
        }
    }
    return mixins;
}

/**
 * Runs the six sentences over the core tree and, when given, the theme tree.
 *
 * @param {Map<string,string>} core relative path → LESS source
 * @param {Map<string,string>|null} theme relative path → LESS source, or null
 * @returns {{core: Array, theme: Array}} findings; theme files are reported as theme/<path>
 */
function checkTree(core, theme = null) {
    const parsed = parseAll(core);
    const out = { core: [], theme: [] };

    const tokens = new Set();
    const palette = new Set();
    for (const file of TOKEN_LAYER) for (const d of parsed.get(file)?.declarations || []) if (!INTERNAL.test(d.name)) tokens.add(d.name);
    for (const file of PALETTE_FILES) for (const d of parsed.get(file)?.declarations || []) palette.add(d.name);
    const staticColours = staticColorNames(parsed.get(VARIABLES_FILE));
    const themeParsed = theme ? parseAll(theme) : new Map();
    const typeSizes = staticTypeSizes([parsed, themeParsed]);

    for (const [file, p] of parsed) {
        const layer = TOKEN_LAYER.includes(file);
        if (layer) {
            for (const d of p.declarations) {
                if (INTERNAL.test(d.name)) out.core.push(finding(file, d.line, d.name, 'internal --_ token in the token layer — public tokens carry no underscore'));
                else if (!underRoot(d.stack) && !inMixin(d.stack)) out.core.push(finding(file, d.line, d.name, `declared under "${flattened(d.stack) || '(top level)'}" — only ${ROOT_SELECTORS.join(', ')} or a mixin`));
                if (!PALETTE_FILES.includes(file) && COLOR_LITERAL.test(withoutStrings(d.value)))
                    out.core.push(finding(file, d.line, `${d.name}: ${d.value}`, `colour literal in the token layer — every colour comes from ${file === ADMIN_TOKENS_FILE ? ADMIN_PALETTE_FILE : PALETTE_FILE}`));
            }
        } else if (file !== VARIABLES_FILE) {
            for (const d of p.declarations) if (!INTERNAL.test(d.name)) out.core.push(finding(file, d.line, d.name, `public token declared outside the token layer — declare it in ${TOKENS_FILE}, or use --_${d.name.slice(2)} for a local value`));
            for (const b of p.blocks) if (MODE_BRANCH.test(b.header)) out.core.push(finding(file, b.line, flattened([...b.stack, b.header]), 'light/dark branch outside the token layer — read a token that differs per mode'));
            if (!STATIC_COLOUR_TREES.test(file)) componentValues(file, p, core.get(file), staticColours, typeSizes, out.core);
        }
        reads(file, p, tokens, out.core);
        for (const r of p.reads) {
            if (!palette.has(r.name) || PALETTE_FILES.includes(file) || (layer && r.stack.some((h) => SEMANTIC_MIXIN.test(h)))) continue;
            out.core.push(finding(file, r.line, `var(${r.name})`, 'palette colour read outside the semantic roles — read a role instead'));
        }
    }
    modeSymmetry(mixinTokens(parsed, TOKEN_LAYER), out.core);

    // Sentence 6: the admin token files bind what admin pages read, and nothing else.
    const bound = new Map();
    for (const file of ADMIN_FILES) for (const d of parsed.get(file)?.declarations || []) if (!INTERNAL.test(d.name)) bound.set(d.name, { file, line: d.line });
    if (bound.size) {
        const adminReads = new Map();
        for (const [file, p] of parsed) if (adminRelevant(file)) for (const r of p.reads) if (tokens.has(r.name) && !adminReads.has(r.name)) adminReads.set(r.name, { file, line: r.line });
        for (const [name, site] of adminReads) if (!bound.has(name)) out.core.push(finding(site.file, site.line, `var(${name})`, `read on admin pages but not bound for the admin — add it to ${name.startsWith('--color-') ? ADMIN_PALETTE_FILE : ADMIN_TOKENS_FILE}`));
        for (const [name, site] of bound) if (!adminReads.has(name)) out.core.push(finding(site.file, site.line, name, 'bound for the admin but no admin page reads it — delete the line'));
    }

    if (theme) {
        const known = new Set(tokens);
        const themePalette = new Set(palette);
        const themeStatics = new Set();
        for (const p of themeParsed.values()) {
            for (const d of p.declarations) {
                if (INTERNAL.test(d.name)) continue;
                known.add(d.name);
                if (d.name.startsWith('--color-') && d.stack.some((h) => THEME_PALETTE_MIXIN.test(h))) themePalette.add(d.name);
            }
            for (const name of staticColorNames(p)) themeStatics.add(name);
        }
        for (const [file, p] of themeParsed) {
            const inLayer = THEME_LAYER.test(file);
            const at = `theme/${file}`;
            reads(at, p, known, out.theme);
            for (const r of p.reads) if (themePalette.has(r.name) && !inLayer) out.theme.push(finding(at, r.line, `var(${r.name})`, 'palette colour read in a component — read a role instead'));
            for (const d of p.declarations) {
                if (INTERNAL.test(d.name)) continue;
                if (!inLayer) out.theme.push(finding(at, d.line, d.name, 'public token declared outside the theme token layer — put it into the root wiring of misc/tokens.less'));
                const paletteSpot = d.stack.some((h) => THEME_PALETTE_MIXIN.test(h)) || (SUBTHEME_CONSTRUCTOR.test(file) && /^--color-brand(-hover|-dark)?$/.test(d.name));
                if (!paletteSpot && COLOR_LITERAL.test(withoutStrings(d.value))) out.theme.push(finding(at, d.line, `${d.name}: ${d.value}`, 'colour literal outside the palette — declare it as --color-* in .mx-theme-palette() and read it here'));
            }
            if (inLayer) continue;
            for (const b of p.blocks) if (MODE_BRANCH.test(b.header)) out.theme.push(finding(at, b.line, flattened([...b.stack, b.header]), 'light/dark branch in a component — read a token that differs per mode'));
            componentValues(at, p, theme.get(file), themeStatics, typeSizes, out.theme);
        }
        modeSymmetry(new Map([...mixinTokens(themeParsed, [...themeParsed.keys()].filter((f) => THEME_LAYER.test(f)))].map(([m, d]) => [m, new Map([...d].map(([n, s]) => [n, { ...s, file: `theme/${s.file}` }]))])), out.theme);
    }

    const byPlace = (a, b) => a.file.localeCompare(b.file) || a.line - b.line;
    out.core.sort(byPlace);
    out.theme.sort(byPlace);
    return out;
}

/* ---------- filesystem, run, report ---------- */

function readLessTree(root) {
    const out = new Map();
    const walk = (dir) => {
        for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) walk(full);
            else if (entry.name.endsWith('.less')) out.set(path.relative(root, full).split(path.sep).join('/'), fs.readFileSync(full, 'utf8'));
        }
    };
    walk(root);
    return out;
}

/**
 * Reads the real trees and runs the check.
 *
 * @param {{moduleRoot?: string, lessRoot?: string, themeLessRoot?: string}} [options]
 * @returns {Promise<{core: Array, theme: {status: string, name: string, path: string, findings: Array}}>}
 */
async function runTokenCheck({ moduleRoot = MODULE_ROOT, lessRoot = LESS_ROOT, themeLessRoot = themePaths().lessRoot } = {}) {
    const core = readLessTree(path.resolve(moduleRoot, lessRoot));
    const themeDir = path.resolve(moduleRoot, themeLessRoot);
    const enabled = themePaths().enabled;
    const found = fs.existsSync(themeDir);
    const theme = enabled && found ? readLessTree(themeDir) : null;
    const result = checkTree(core, theme);
    return {
        core: result.core,
        theme: { status: !enabled ? 'off' : found ? 'checked' : 'missing', name: path.basename(path.resolve(themeDir, '..', '..')), path: themeLessRoot, findings: result.theme },
    };
}

const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`;
const formatFinding = (f) => `    ${f.file}:${f.line}  ${f.what}  →  ${f.fix}`;

/**
 * One line per finding: where, what stands there, what to do instead.
 *
 * @param {{core: Array, theme?: {status: string, name: string, path: string, findings?: Array}}} result
 * @returns {string}
 */
function formatReport({ core, theme }) {
    const lines = [];
    if (core.length) {
        lines.push(`Token check: ${plural(core.length, 'finding')} in the core. Fix and save again.`, '');
        for (const f of core) lines.push(formatFinding(f));
    } else {
        lines.push(`Token check passed.${theme?.status === 'checked' ? ` Theme: ${theme.name}.` : ''}`);
    }
    if (theme?.status === 'missing') lines.push(`No theme at ${theme.path} — the theme was not checked. Set GV_THEME_LESS_ROOT to your theme's css/less directory.`);
    if (theme?.status === 'off') lines.push('Theme check off via GV_THEMECHECK=0.');
    const findings = theme?.findings || [];
    if (findings.length) {
        lines.push('', `Theme ${theme.name}: ${plural(findings.length, 'finding')}, information only. The core check is not affected.`, '');
        for (const f of findings) lines.push(formatFinding(f));
    }
    return lines.join('\n');
}

module.exports = {
    LESS_ROOT,
    THEME_ROOT,
    THEME_LESS_ROOT,
    MODULE_ROOT,
    themePaths,
    parseLess,
    withoutStrings,
    COLOR_LITERAL,
    readLessTree,
    checkTree,
    runTokenCheck,
    formatReport,
};
