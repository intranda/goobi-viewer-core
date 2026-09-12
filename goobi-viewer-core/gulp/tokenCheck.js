/*
Token check for the LESS token layer (core, optionally the reference theme).

Rules
  R1  every var(--x) read in core names a token of the token layer; internal --_ tokens are
      declared in the same file or carry a fallback
  R2  tokens without the --_ prefix are declared only in the token layer, under
      :root / .color-theme-dark / .color-theme-light (specificity 0,1,0), the fixed admin scope
      :root:has(#admin) or inside mixin definitions;
      emitted CSS and public core overrides in the reference theme must follow the same contract
  R4  --_ tokens never live in the token layer; prefers-color-scheme / .color-theme-* never outside it
  R5  (warning) token declared but read nowhere in core or theme
  R6  token in the theme mixins that core does not know and the theme never reads
  R7  light/dark mixins of the token layer require matching counterparts and token sets
  R8  palette tokens are read only by the semantic mixins and never get a LESS alias — features,
      components and themes read roles; the brand primitives --color-brand / --color-brand-hover are
      declared by themes but read only through --brand, --bg-brand and --bg-brand-hover
  R9  outside misc/palette.less the token layer holds no color literal; alphas derive via rgb(from …)
  R10 component property values use tokens, not color literals or static color variables; backend exceptions are explicit
  R11 frontend font sizes use text roles or relative inheritance; fixed icon/marker geometry is explicitly annotated
  R12 (theme) no !important outside the focus helper and the print styles
  R13 (theme) every LESS file is reachable from an entry point
  R14 (theme) every visible color pair reaches its WCAG 2.2 AA threshold in both modes
  R15 (theme) light and dark declare the same token names
  R16 (theme) templates carry no color values and no inline color styles

Known violations are tolerated via tokenCheck.baseline.json; entries that no longer
occur are reported as stale so the baseline can only shrink.
*/

const fs = require('fs');
const path = require('path');
const less = require('less');
const postcss = require('postcss');

const MODULE_ROOT = path.resolve(__dirname, '..');
const LESS_ROOT = 'src/main/resources/META-INF/resources/resources/css/less';
const TOKEN_LAYER = ['misc/palette.less', 'misc/tokens.less'];
const THEME_ROOT = '../goobi-viewer-theme-reference/WebContent/resources/themes/reference';
const THEME_LESS_ROOT = `${THEME_ROOT}/css/less`;

/**
 * Which theme the check and the watcher look at, and whether the theme rules run at all.
 * The core rules are not switchable: they keep a build from going past the token system.
 * The theme rules judge a separate contract, and a customer theme may predate it.
 *
 * GV_THEME_LESS_ROOT  the theme's css/less directory (default: the reference submodule)
 * GV_THEMECHECK=0     run the core rules only
 *
 * @returns {{lessRoot: string, root: string, enabled: boolean}}
 */
function themePaths() {
    const lessRoot = process.env.GV_THEME_LESS_ROOT || THEME_LESS_ROOT;
    return { lessRoot, root: lessRoot.replace(/[\\/]css[\\/]less[\\/]?$/, ''), enabled: process.env.GV_THEMECHECK !== '0' };
}
const THEME_MIXINS = 'misc/mixins.less';
// A theme keeps its token layer either in the legacy mixins file or split like the core.
const THEME_TOKEN_LAYER = [THEME_MIXINS, ...TOKEN_LAYER];
const THEME_MODE_MIXIN = /^\.mx-theme-(?:semantic-)?(?:light|dark)\(\)$/;
const THEME_PALETTE_MIXIN = /^\.mx-theme-[\w-]*palette\(\)$/;
const BASELINE_FILE = path.join(__dirname, 'tokenCheck.baseline.json');
const VARIABLES_FILE = 'misc/variables.less';
const COMPONENT_COLOR = /#[\da-f]{3}(?:[\da-f](?:[\da-f]{2}(?:[\da-f]{2})?)?)?\b|\b(?:rgba?|hsla?)\(\s*[+-]?(?:\d|\.\d)|(?<![\w@#.-])(?:black|white)(?![\w-])/i;

const ADMIN_SCOPE = ':root:has(#admin)';
const ROOT_SELECTORS = [':root', '.color-theme-dark', '.color-theme-light', ADMIN_SCOPE];
const TOKEN_SELECTORS = new Set([...ROOT_SELECTORS, '@media (prefers-color-scheme: dark)', '@media (prefers-color-scheme: light)']);
const PALETTE_FILE = 'misc/palette.less';
const SEMANTIC_MIXIN = /^\.[\w-]*semantic[\w-]*\(\)$/;
const COLOR_LITERAL = new RegExp(
    `#[0-9a-f]{3,8}\\b|\\b(?:rgba?|hsla?|hwb|lab|lch|oklab|oklch|color)\\((?!\\s*from\\b)|(?:^|[\\s,(])(?:${Object.keys(less.data.colors).join('|')})(?=[\\s,)]|$)`,
    'i'
);
const OCCURRENCE_LIMIT = 12;
const IMPORTANT_ALLOWED = ['misc/mixins.less', 'misc/print.less'];
const ENTRY_POINTS = /^(?:constructor\.less|crowdsourcing\/csConstructor\.less|subthemes\/[^/]+\/subThemeConstructor\.less)$/;
const MIXIN_DEFINITION = /^\.[\w-]+\([^)]*\)(?: when .+)?$/;
const DETACHED_DEFINITION = /^@[\w-]+:$/;
const MIXIN_CALL = /^\.[\w-]+(\([^)]*\))?$/;
const DECLARATION = /^(--[\w-]+)\s*:\s*([\s\S]*)$/;
const ALIAS = /^@([\w-]+)\s*:\s*var\(\s*(--[\w-]+)/;
const STATIC = /^@([\w-]+)\s*:/;
const READ = /var\(\s*(--[\w-]+)\s*(,)?/g;
const AT_USE = /@([\w-]+)/g;
const INTERNAL = /^--_/;
const FIXED_TYPE_SIZE = /(?:\d*\.)?\d+(?:px|rem|pt|pc|in|cm|mm|q|vw|vh|vmin|vmax)\b/i;
const FIXED_GEOMETRY_NOTE = '// Fixed icon/marker geometry; independent of text scale.';
const isTypographyProperty = (name) => name === 'font-size' || name === 'font';
const MODE_BRANCH = /prefers-color-scheme|\.color-theme-(dark|light)/;
const AT_KEYWORDS = new Set(['media', 'import', 'supports', 'keyframes', 'font-face', 'layer', 'page', 'charset', 'plugin']);

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

function colorExemption(file, exemptions) {
    return exemptions.find((e) => e.reason?.trim() && (e.path.endsWith('/') ? file.startsWith(e.path) : file === e.path));
}

function collectReads(text, line, stack, into) {
    for (const m of withoutStrings(text).matchAll(READ)) {
        into.push({ name: m[1], fallback: m[2] === ',', line, stack });
    }
}

function collectAtUses(text, into) {
    for (const m of text.matchAll(AT_USE)) {
        if (!AT_KEYWORDS.has(m[1])) into.add(m[1]);
    }
}

/**
 * Minimal LESS reader: statements, block headers and their nesting.
 *
 * @param {string} source LESS source.
 * @returns {{declarations: Array, reads: Array, calls: Array, aliases: Array, blocks: Array, atUses: Set<string>}}
 */
function parseLess(source) {
    const text = stripComments(source);
    const result = { declarations: [], properties: [], reads: [], calls: [], aliases: [], statics: [], imports: [], blocks: [], atUses: new Set() };
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
        const imp = stmt.match(/^@import\s+(?:\([^)]*\)\s*)?['"]([^'"]+)['"]/);
        if (imp) {
            result.imports.push({ target: imp[1], line: at, stack: context });
            return;
        }
        const property = stmt.match(/^((?:--)?[\w@{}-]+)(?:\+_?)?\s*:\s*([\s\S]*)$/);
        if (property && (!property[1].startsWith('@') || property[1].startsWith('@{'))) {
            result.properties.push({ name: property[1], value: property[2].trim(), line: at, stack: context });
        }
        const declaration = stmt.match(DECLARATION);
        if (declaration) {
            result.declarations.push({ name: declaration[1], value: declaration[2].trim(), line: at, stack: context });
            collectReads(declaration[2], at, context, result.reads);
            collectAtUses(declaration[2], result.atUses);
            return;
        }
        const alias = stmt.match(ALIAS);
        if (alias) {
            result.aliases.push({ alias: alias[1], token: alias[2], line: at });
            const reads = [];
            collectReads(stmt, at, context, reads);
            result.reads.push(...reads.map((read) => ({ ...read, alias: alias[1] })));
            return;
        }
        const stat = stmt.match(STATIC);
        if (stat && context.length === 0) {
            result.statics.push({ name: stat[1], value: stmt.slice(stmt.indexOf(':') + 1).trim(), line: at });
            collectReads(stmt, at, context, result.reads);
            collectAtUses(stmt, result.atUses);
            return;
        }
        if (MIXIN_CALL.test(stmt)) {
            result.calls.push({ name: stmt, line: at, stack: context });
            return;
        }
        collectReads(stmt, at, context, result.reads);
        collectAtUses(stmt, result.atUses);
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
            collectAtUses(header, result.atUses);
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
function usedTokens(parsedMap) {
    const used = new Set();
    const aliasByToken = new Map();
    const atUsed = new Set();
    for (const p of parsedMap.values()) {
        for (const r of p.reads) if (!r.alias) used.add(r.name);
        for (const a of p.aliases) aliasByToken.set(a.token, a.alias);
        for (const u of p.atUses) atUsed.add(u);
    }
    for (const [token, alias] of aliasByToken) if (atUsed.has(alias)) used.add(token);
    return used;
}
const flattened = (stack) => stack.join(' ');
const allowedInTokenLayer = (stack) => {
    const context = stack.filter((h) => !MIXIN_DEFINITION.test(h) && !DETACHED_DEFINITION.test(h) && h !== '&');
    return stack.length > 0 && context.every((h) => TOKEN_SELECTORS.has(h)) && context.filter((h) => !h.startsWith('@')).length <= 1;
};

/**
 * Runs all rules over parsed sources.
 *
 * @param {Map<string,string>} core relative path → LESS source (core tree)
 * @param {Map<string,string>|null} theme relative path → LESS source (theme tree), or null to skip R5-theme/R6
 * @param {{tokenLayer: string[], aliasFile: string, themeMixins: string}} options
 * @returns {{violations: Array, warnings: Array, tokens: Set<string>}}
 */
function checkFiles(core, theme, { tokenLayer = TOKEN_LAYER, paletteFile = PALETTE_FILE, themeMixins = THEME_MIXINS, colorExemptions = loadColorExemptions() } = {}) {
    const parsed = new Map();
    for (const [file, source] of core) parsed.set(file, parseLess(source));

    const violations = [];
    const warnings = [];
    const tokens = new Set();
    const isTokenLayer = (file) => tokenLayer.includes(file);
    const staticColors = staticColorNames(parsed.get(VARIABLES_FILE));
    // Follow named LESS constants as well as direct literals. Compilation below
    // additionally catches interpolation, mixin parameters and imported aliases.
    const staticTypeSizes = new Set();
    const typeDefinitions = [...core.values(), ...(theme?.values() || [])].flatMap((source) => parseLess(source).statics);
    let previousTypeSizeCount;
    do {
        previousTypeSizeCount = staticTypeSizes.size;
        for (const definition of typeDefinitions) {
            const value = withoutStrings(definition.value);
            if (FIXED_TYPE_SIZE.test(value) || [...value.matchAll(AT_USE)].some((m) => staticTypeSizes.has(m[1]))) staticTypeSizes.add(definition.name);
        }
    } while (staticTypeSizes.size !== previousTypeSizeCount);
    const checkTypography = (file, parsedSource, source) => {
        const lines = source.split('\n');
        for (const d of parsedSource.properties) {
            if (!isTypographyProperty(d.name)) continue;
            const value = withoutStrings(d.value);
            if (!FIXED_TYPE_SIZE.test(value) && ![...value.matchAll(AT_USE)].some((m) => staticTypeSizes.has(m[1]) || /^font(?:Small|Medium|Normal|Large|XLarge)$/.test(m[1])))
                continue;
            if (lines[d.line - 1].includes(FIXED_GEOMETRY_NOTE)) continue;
            violations.push(violation('R11', file, JSON.stringify([d.stack, d.name, d.value]), d.line, 'fixed text size — use a typography role'));
        }
    };

    for (const file of tokenLayer) {
        const p = parsed.get(file);
        if (!p) continue;
        for (const d of p.declarations) if (!INTERNAL.test(d.name)) tokens.add(d.name);
    }

    for (const [file, p] of parsed) {
        if (!isTokenLayer(file) && file !== VARIABLES_FILE && !colorExemption(file, colorExemptions)) {
            checkTypography(file, p, core.get(file));
            for (const d of p.properties) {
                const value = colorValue(d.value);
                if (!COMPONENT_COLOR.test(value) && ![...value.matchAll(AT_USE)].some((m) => staticColors.has(m[1]))) continue;
                const token = JSON.stringify([d.stack, d.name, d.value.replace(/\s+/g, ' ')]);
                violations.push(violation('R10', file, token, d.line, 'static component color — use a semantic role, feature token or brand'));
            }
        }
        if (isTokenLayer(file)) {
            for (const d of p.declarations) {
                if (INTERNAL.test(d.name)) {
                    violations.push(violation('R4', file, d.name, d.line, 'internal token declared in the token layer'));
                } else if (!allowedInTokenLayer(d.stack)) {
                    violations.push(violation('R2', file, d.name, d.line, `declared under "${flattened(d.stack) || '(top level)'}" — only ${ROOT_SELECTORS.join(', ')}`));
                }
            }
            for (const c of p.calls) {
                if (!allowedInTokenLayer(c.stack)) {
                    violations.push(violation('R2', file, c.name, c.line, `mixin called under "${flattened(c.stack) || '(top level)'}" — only ${ROOT_SELECTORS.join(', ')}`));
                }
            }
            const byMixin = new Map();
            for (const d of p.declarations) {
                const mixin = d.stack.find((h) => MIXIN_DEFINITION.test(h))?.replace(/ when .+$/, '');
                if (!mixin) continue;
                if (!byMixin.has(mixin)) byMixin.set(mixin, new Map());
                byMixin.get(mixin).set(d.name, d.line);
            }
            for (const [mixin, modeTokens] of byMixin) {
                const m = mixin.match(/^(\.[\w-]+)-(light|dark)\(\)(?: when .+)?$/);
                if (!m) continue;
                const counterpart = `${m[1]}-${m[2] === 'light' ? 'dark' : 'light'}()`;
                const otherTokens = byMixin.get(counterpart) || new Map();
                for (const [name, line] of modeTokens)
                    if (!otherTokens.has(name)) violations.push(violation('R7', file, name, line, `declared in ${mixin} but not in ${counterpart}`));
            }
        } else {
            for (const d of p.declarations) {
                if (!INTERNAL.test(d.name)) {
                    violations.push(
                        violation('R2', file, d.name, d.line, `theme-visible token declared on "${flattened(d.stack) || '(top level)'}" — read it, declare --_* instead`)
                    );
                }
            }
            for (const b of p.blocks) {
                if (MODE_BRANCH.test(b.header)) {
                    violations.push(violation('R4', file, flattened([...b.stack, b.header]), b.line, 'mode branching outside the token layer — use a token'));
                }
            }
        }

        const internalHere = new Set(p.declarations.filter((d) => INTERNAL.test(d.name)).map((d) => d.name));
        for (const r of p.reads) {
            if (INTERNAL.test(r.name)) {
                if (!r.fallback && !internalHere.has(r.name))
                    violations.push(violation('R1', file, r.name, r.line, 'internal token read without declaration in this file and without fallback'));
            } else if (!tokens.has(r.name)) {
                violations.push(violation('R1', file, r.name, r.line, renameHint(r.name) || 'read of a token the token layer does not declare'));
            }
        }
    }

    const paletteTokens = new Set();
    const paletteParsed = parsed.get(paletteFile);
    if (paletteParsed) for (const d of paletteParsed.declarations) paletteTokens.add(d.name);
    const restricted = paletteTokens;
    for (const [file, p] of parsed) {
        for (const r of p.reads) {
            if (r.alias || !restricted.has(r.name)) continue;
            if (isTokenLayer(file) && r.stack.some((h) => SEMANTIC_MIXIN.test(h))) continue;
            violations.push(violation('R8', file, r.name, r.line, 'palette token read outside the semantic roles — read a semantic role instead'));
        }
        for (const a of p.aliases) {
            if (restricted.has(a.token)) violations.push(violation('R8', file, a.token, a.line, `@${a.alias} exposes a palette token as a LESS variable`));
        }
        if (paletteParsed && isTokenLayer(file) && file !== paletteFile) {
            for (const d of p.declarations) {
                if (COLOR_LITERAL.test(withoutStrings(d.value)))
                    violations.push(violation('R9', file, d.name, d.line, `color literal "${d.value}" — every color comes from ${paletteFile}`));
            }
        }
    }

    const readByCore = usedTokens(parsed);
    const readAnywhere = new Set(readByCore);

    if (theme) {
        const themeParsed = new Map();
        for (const [file, source] of theme) themeParsed.set(file, parseLess(source));
        const readByTheme = usedTokens(themeParsed);
        for (const t of readByTheme) readAnywhere.add(t);

        const themeTokens = new Set(tokens);
        const themePalette = new Set(restricted);
        const themeStaticColors = new Set();
        for (const p of themeParsed.values()) {
            for (const d of p.declarations) if (!INTERNAL.test(d.name)) themeTokens.add(d.name);
            for (const d of p.declarations) if (d.name.startsWith('--color-') && d.stack.some((h) => THEME_PALETTE_MIXIN.test(h))) themePalette.add(d.name);
            for (const name of staticColorNames(p)) themeStaticColors.add(name);
        }
        const isThemeLayer = (file) =>
            THEME_TOKEN_LAYER.includes(file) || file === VARIABLES_FILE || /^subthemes\/[^/]+\/subThemeConstructor\.less$/.test(file);
        for (const [file, p] of themeParsed) {
            const inLayer = isThemeLayer(file);
            const report = (rule, token, line, message) => violations.push(violation(rule, `theme/${file}`, token, line, message));
            for (const r of p.reads) {
                if (!INTERNAL.test(r.name) && !themeTokens.has(r.name)) report('R1', r.name, r.line, renameHint(r.name) || 'unknown core or theme token');
                const paletteReadAllowed =
                    tokenLayer.includes(file) || (file === themeMixins && r.stack.some((h) => THEME_MODE_MIXIN.test(h)));
                if ((themePalette.has(r.name) || paletteTokens.has(r.name)) && !paletteReadAllowed)
                    report('R8', r.name, r.line, 'theme components and features read roles, not palette primitives');
            }
            for (const d of p.declarations) {
                if (!inLayer && !INTERNAL.test(d.name)) report('R2', d.name, d.line, 'public theme tokens belong in the root token wiring');
                const paletteDeclaration = (file === themeMixins || file === paletteFile) && d.stack.some((h) => THEME_PALETTE_MIXIN.test(h));
                const subthemeBrand = /^subthemes\/[^/]+\/subThemeConstructor\.less$/.test(file) && /^--color-brand(-hover|-dark)?$/.test(d.name);
                if (!paletteDeclaration && !subthemeBrand && COLOR_LITERAL.test(withoutStrings(d.value)))
                    report('R9', d.name, d.line, 'theme color literals belong in .mx-theme-palette()');
                const mode = d.stack.find((h) => THEME_MODE_MIXIN.test(h));
                if (mode) {
                    const other = mode.includes('light') ? mode.replace('light', 'dark') : mode.replace('dark', 'light');
                    if (!p.declarations.some((x) => x.name === d.name && x.stack.includes(other))) report('R7', d.name, d.line, `missing counterpart in ${other}`);
                }
            }
            if (!inLayer) {
                checkTypography(`theme/${file}`, p, theme.get(file));
                if (!IMPORTANT_ALLOWED.includes(file))
                    (theme.get(file) || '').split('\n').forEach((text, index) => {
                        if (/!important/.test(text) && !/^\s*(\/\/|\*|\/\*)/.test(text)) report('R12', text.trim().slice(0, 60), index + 1, 'declaration marked !important');
                    });
                for (const b of p.blocks)
                    if (MODE_BRANCH.test(b.header) || /@colorModes/.test(b.header)) report('R4', b.header, b.line, 'theme mode branching belongs in the token wiring');
                for (const d of p.properties) {
                    const value = colorValue(d.value);
                    if (COLOR_LITERAL.test(value) || [...value.matchAll(AT_USE)].some((m) => themeStaticColors.has(m[1])))
                        report('R10', d.name, d.line, 'theme component colors must read tokens');
                }
            }
        }

        for (const file of unreachableFiles(theme))
            violations.push(violation('R13', `theme/${file}`, file, 1, 'not imported by any entry point'));

        const seen = new Set();
        for (const layerFile of THEME_TOKEN_LAYER) {
            const parsed = themeParsed.get(layerFile);
            if (!parsed) continue;
            for (const d of parsed.declarations) {
                if (INTERNAL.test(d.name) || tokens.has(d.name) || readByTheme.has(d.name) || seen.has(d.name)) continue;
                seen.add(d.name);
                const message = readByCore.has(d.name)
                    ? 'theme token that core reads via fallback only — belongs in the token layer'
                    : 'theme token unknown to core and never read by the theme';
                violations.push(violation('R6', `theme/${layerFile}`, d.name, d.line, message));
            }
        }
    }

    for (const t of tokens) {
        if (!readAnywhere.has(t)) warnings.push({ rule: 'R5', token: t, message: 'declared but read nowhere' });
    }

    return { violations, warnings, tokens };
}

/**
 * Validate emitted CSS as well as source text. LESS expansion is authoritative
 * for interpolation, guarded/detached mixins and reference imports. Source maps
 * retain the declaring file so a root declaration in a component is still R2.
 * Only private --_* properties remain local; public core and theme tokens inherit.
 */
async function checkCompiledTree(root, { entry = 'constructor.less', tokens = new Set(), theme = false, onCss } = {}) {
    const filename = path.join(root, entry);
    const violations = [];
    let output;
    try {
        output = await less.render(fs.readFileSync(filename, 'utf8'), {
            filename,
            paths: [root, path.join(root, 'crowdsourcing')],
            sourceMap: { outputSourceFiles: true },
        });
    } catch (error) {
        return [violation('LESS', error.filename ? path.relative(root, error.filename) : entry, '', error.line || 1, error.message)];
    }
    onCss?.(output.css);
    const css = postcss.parse(output.css, { from: path.join(root, 'token-check.css'), map: { prev: output.map } });
    const location = (node) => {
        const { input, start } = node.source;
        const origin = input.origin(start.line, start.column);
        return { file: origin ? path.relative(root, origin.file).split(path.sep).join('/') : entry, line: origin ? origin.line : start.line };
    };
    const emitted = new Set();
    const backendExemptions = loadColorExemptions();
    const sourceLines = new Map();
    const hasGeometryNote = (file, line) => {
        if (!sourceLines.has(file)) sourceLines.set(file, fs.readFileSync(path.resolve(root, file), 'utf8').split('\n'));
        return sourceLines.get(file)[line - 1]?.includes(FIXED_GEOMETRY_NOTE);
    };
    const internalFiles = new Map();
    const add = (rule, node, token, message) => {
        const { file, line } = location(node);
        const declaration = node.type === 'decl' ? { property: node.prop, value: node.value } : undefined;
        // Repeated mode expansions should not flood the report.
        if (!violations.some((v) => v.rule === rule && v.file === file && v.line === line && v.token === token && v.message === message && v.declaration?.value === declaration?.value)) {
            violations.push({ ...violation(rule, file, token, line, message), ...(declaration && { declaration }) });
        }
    };
    const allowedOutput = (node) => {
        if (node.parent.type !== 'rule' || !ROOT_SELECTORS.includes(node.parent.selector)) return false;
        for (let parent = node.parent.parent; parent && parent.type !== 'root'; parent = parent.parent) {
            if (parent.type !== 'atrule' || !TOKEN_SELECTORS.has(`@${parent.name} ${parent.params}`)) return false;
        }
        return !node.important;
    };
    css.walkDecls((d) => {
        if (!d.prop.startsWith('--')) return;
        const { file } = location(d);
        if (INTERNAL.test(d.prop)) {
            if (!internalFiles.has(file)) internalFiles.set(file, new Set());
            internalFiles.get(file).add(d.prop);
            if (!theme && TOKEN_LAYER.includes(file)) add('R4', d, d.prop, 'internal token emitted by the token layer');
        } else if (!allowedOutput(d) || (!theme && !TOKEN_LAYER.includes(file))) {
            add('R2', d, d.prop, `public token emitted on "${d.parent.selector}" — use the root token wiring`);
        } else {
            emitted.add(d.prop);
        }
        if (!theme && TOKEN_LAYER.includes(file) && file !== PALETTE_FILE && COLOR_LITERAL.test(withoutStrings(d.value))) {
            add('R9', d, d.prop, `emitted color literal "${d.value}" — every color comes from ${PALETTE_FILE}`);
        }
    });
    css.walkDecls((d) => {
        const { file, line } = location(d);
        if (
            isTypographyProperty(d.prop) &&
            FIXED_TYPE_SIZE.test(withoutStrings(d.value)) &&
            !TOKEN_LAYER.includes(file) &&
            !(theme && THEME_TOKEN_LAYER.includes(file)) &&
            (theme || !colorExemption(file, backendExemptions)) &&
            !hasGeometryNote(file, line)
        ) {
            add('R11', d, d.prop, 'emitted fixed text size — use a typography role');
        }
        const reads = [];
        collectReads(d.value, 0, [], reads);
        for (const read of reads) {
            if (INTERNAL.test(read.name)) {
                if (!read.fallback && !internalFiles.get(file)?.has(read.name)) add('R1', d, read.name, 'emitted internal read without a declaration in this file or fallback');
            } else if (!emitted.has(read.name) && !(theme && tokens.has(read.name))) {
                add('R1', d, read.name, 'read of a token not emitted by the root token wiring');
            }
        }
        if (theme && !d.prop.startsWith('--') && COLOR_LITERAL.test(colorValue(d.value))) add('R10', d, d.prop, 'emitted theme component color must read a token');
    });
    css.walk((node) => {
        if (node.type !== 'rule' && node.type !== 'atrule') return;
        const selector = node.type === 'rule' ? node.selector : `@${node.name} ${node.params}`;
        const file = location(node).file;
        const inLayer = theme ? file === VARIABLES_FILE || THEME_TOKEN_LAYER.includes(file) : TOKEN_LAYER.includes(file);
        if (MODE_BRANCH.test(selector) && !inLayer) {
            add('R4', node, selector, 'emitted mode branching outside the token layer');
        }
    });
    return violations;
}

/* ---------- baseline ---------- */

const keyOf = (v) => `${v.rule} ${v.file} ${v.token}`;

/**
 * @param {Array} violations
 * @param {string[]} baseline tolerated keys "RULE file token"
 */
function applyBaseline(violations, baseline) {
    const tolerated = new Set(baseline);
    const remainingColors = new Map();
    for (const key of baseline) if (key.startsWith('R10 ')) remainingColors.set(key, (remainingColors.get(key) || 0) + 1);
    const hit = new Set();
    const errors = [];
    const kept = [];
    for (const v of violations) {
        const key = keyOf(v);
        if (v.rule === 'R10' ? remainingColors.get(key) > 0 : tolerated.has(key)) {
            if (v.rule === 'R10') remainingColors.set(key, remainingColors.get(key) - 1);
            hit.add(key);
            kept.push(v);
        } else {
            errors.push(v);
        }
    }
    const stale = baseline.filter((key) => {
        if (!key.startsWith('R10 ')) return !hit.has(key);
        if (!remainingColors.get(key)) return false;
        remainingColors.set(key, remainingColors.get(key) - 1);
        return true;
    });
    return { errors, tolerated: kept, stale };
}

// Baseline fingerprints remain machine-readable and stable. Decode them only
// for presentation; never use display deduplication to tolerate a violation.
function reportDeclaration(v) {
    if (v.declaration) return v.declaration;
    try {
        const fingerprint = JSON.parse(v.token);
        if (Array.isArray(fingerprint) && fingerprint.length === 3 && Array.isArray(fingerprint[0]) && typeof fingerprint[1] === 'string' && typeof fingerprint[2] === 'string') {
            return { property: fingerprint[1], value: fingerprint[2] };
        }
    } catch {
        // Most diagnostics contain a plain token name, not a fingerprint.
    }
    return null;
}

/**
 * One short explanation and one concrete fix per rule. The watcher prints these,
 * so a developer never has to look a rule code up somewhere else.
 * `hint(violation)` may return a fix tailored to the offending token.
 */
const RULE_HELP = {
    R14: {
        title: 'a color pair is too low in contrast',
        why: 'text or a boundary below the WCAG 2.2 AA threshold is unreadable for part of the users',
        fix: 'darken or lighten one of the two roles in misc/tokens.less, or pick another palette step in misc/palette.less',
        hint: (v) => (/\(dark\)$/.test(v.token) ? 'the dark mode fails here: check the -dark branch of the palette' : null),
    },
    R16: {
        title: 'a color sits in a template instead of a stylesheet',
        why: 'a value in the markup ignores the light/dark switch and cannot be overridden by a customer theme',
        fix: 'move the rule into the matching LESS file and read a role there, for example color: var(--fg-content-box)',
    },
    R15: {
        title: 'a token is declared for one mode only',
        why: 'switching with .color-theme-light / .color-theme-dark leaves the other mode on the previous value',
        fix: 'declare the token in both mode blocks of misc/tokens.less, or move it to the mode-free block if the value never changes',
    },

    R1: {
        title: 'a token is read that nobody declares',
        why: 'the value falls back to nothing, so the property silently keeps its inherited value',
        fix: 'check the spelling, or declare the token in misc/tokens.less under :root',
        hint: (v) => (renameHint(v.token) ? `${v.token} was renamed: ${renameHint(v.token)}` : null),
    },
    R2: {
        title: 'a public token is declared outside the root wiring',
        why: 'tokens declared on a component only apply inside it, which splits the token layer in two',
        fix: 'declare it in misc/tokens.less under :root / .color-theme-light / .color-theme-dark; for a value that is truly local use a --_name',
    },
    R4: {
        title: 'light and dark are branched outside the token layer',
        why: 'every component would need its own mode handling, and the mode toggle stops working',
        fix: 'put the two values into the light and dark mixins of misc/tokens.less and read one token here',
    },
    R5: { title: 'a token is declared but never read', why: 'dead weight in the token layer', fix: 'remove it, or read it where it belongs' },
    R6: {
        title: 'a theme token is unknown to core and unused in the theme',
        why: 'nothing consumes it, so it only looks like a contract',
        fix: 'remove it, or read it from a component',
    },
    R7: {
        title: 'a token exists in only one colour mode',
        why: 'switching the mode keeps the old value, because nothing overrides it',
        fix: 'declare the same token in the other mode mixin as well',
    },
    R8: {
        title: 'a palette colour is read outside the token layer',
        why: 'palette colours are fixed values with no light/dark meaning; reading them skips the mode switch',
        fix: 'read the semantic role instead, for example var(--bg-content-box), var(--fg-content-box), var(--border-default)',
        hint: (v) => `${v.token} is a palette primitive; only the semantic mixins of misc/tokens.less may read it`,
    },
    R9: {
        title: 'a colour value stands outside the palette',
        why: 'a customer palette replaces misc/palette.less; values elsewhere survive the swap and break the theme',
        fix: 'move the value into .mx-theme-palette() in misc/palette.less and read it as a --color-* token',
    },
    R10: {
        title: 'a colour is written into a component',
        why: 'it follows neither the palette nor the dark mode',
        fix: 'read a role, for example color: var(--fg-content-box), background: var(--bg-content-box), border-color: var(--border-default)',
    },
    R11: {
        title: 'a fixed text size bypasses the type scale',
        why: 'the theme scales typography through tokens; fixed sizes stay put',
        fix: 'use a text role such as var(--font-size-body) or var(--font-size-body-large)',
    },
    R12: {
        title: '!important in a component',
        why: 'it removes the only lever a later rule has, so the next override needs !important as well',
        fix: 'raise specificity instead, for example by prefixing the block selector',
    },
    R13: {
        title: 'a LESS file is not reachable from any entry point',
        why: 'it is never compiled, so its rules silently do nothing',
        fix: 'import it in constructor.less, or delete the file',
    },
    LESS: { title: 'the LESS compiler could not read a file', why: 'the check needs the compiled output', fix: 'fix the syntax error shown below' },
};

function formatReport({ errors, stale, tolerated, warnings, theme }, { showWarnings = true } = {}) {
    const lines = [];
    const issues = new Map();
    for (const v of errors) {
        const declaration = reportDeclaration(v);
        // Source and compiled CSS find the same problem twice. Report it once, with the
        // declaration when we have it. R11 keeps distinct emitted values of one source line.
        const identity = JSON.stringify([v.rule, v.file, v.line, v.rule === 'R11' && declaration ? declaration : v.token]);
        const known = issues.get(identity);
        if (!known || (!known.declaration && declaration)) issues.set(identity, { ...v, declaration });
    }
    if (issues.size) {
        const files = new Set([...issues.values()].map((v) => v.file));
        lines.push(`Token check failed: ${issues.size} ${issues.size === 1 ? 'issue' : 'issues'} in ${files.size} ${files.size === 1 ? 'file' : 'files'}.`);
        const byRule = new Map();
        for (const v of issues.values()) {
            if (!byRule.has(v.rule)) byRule.set(v.rule, []);
            byRule.get(v.rule).push(v);
        }
        for (const [rule, found] of [...byRule].sort((a, b) => b[1].length - a[1].length)) {
            const help = RULE_HELP[rule];
            lines.push('', `${rule}  ${help ? help.title : found[0].message}  (${found.length}x)`);
            if (help) {
                lines.push(`    why  ${help.why}`);
                lines.push(`    fix  ${help.fix}`);
                const hints = [...new Set(found.map((v) => help.hint?.(v)).filter(Boolean))];
                for (const hint of hints.slice(0, 3)) lines.push(`         ${hint}`);
            }
            const width = Math.max(...found.slice(0, OCCURRENCE_LIMIT).map((v) => `${v.file}:${v.line}`.length));
            for (const v of found.slice(0, OCCURRENCE_LIMIT)) {
                const where = `${v.file}:${v.line}`.padEnd(width);
                const what = v.declaration ? `${v.declaration.property}: ${v.declaration.value};` : v.token || v.message;
                lines.push(`    ${where}  ${what}`);
            }
            if (found.length > OCCURRENCE_LIMIT) lines.push(`    … and ${found.length - OCCURRENCE_LIMIT} more of the same kind`);
        }
        lines.push('', 'Fix the lines above and save again; the watcher re-runs the check.');
    } else if (!stale.length) {
        lines.push(`Token check passed.${theme?.status === 'checked' ? ` Theme: ${theme.name}.` : ''}`);
    }
    // A green report must never hide a check that never ran.
    if (theme?.status === 'missing') lines.push(`No theme at ${theme.path} — the theme rules did not run. Set GV_THEME_LESS_ROOT to your theme's css/less directory.`);
    if (theme?.status === 'off') lines.push('Theme rules off via GV_THEMECHECK=0. The core rules still apply.');
    if (stale.length) {
        lines.push('', `${stale.length} stale baseline entr${stale.length === 1 ? 'y' : 'ies'}: the violation is gone, so remove the line from gulp/tokenCheck.baseline.json.`);
        for (const k of stale) lines.push(`    ${k}`);
    }
    if (tolerated.length) lines.push('', `${tolerated.length} known violations allowed by the baseline.`);
    if (warnings.length) lines.push('', `${warnings.length} non-blocking warnings.${showWarnings ? '' : ' Set GV_TOKENWARN=1 to show details.'}`);
    if (showWarnings) for (const w of warnings) lines.push(`    ${w.rule} ${w.token} — ${w.message}`);
    return lines.join('\n');
}

const TEMPLATE_FILES = /\.(?:xhtml|html|js|tag)$/;
const TEMPLATE_SKIP = new Set(['node_modules', 'dist', 'target', 'css']);

/**
 * R16: a theme template carries a color value or an inline color style.
 * JSF expressions, HTML entities and anchors are stripped first, so `#{...}`,
 * `&#8211;` and `href="#top"` are not mistaken for hex colors.
 *
 * @param {string} themeDir the theme's css/less directory; templates live two levels above
 * @returns {object[]} violations
 */
function checkThemeTemplates(themeDir) {
    // Only a real theme has templates next to its stylesheets; a bare LESS directory has none.
    if (path.basename(themeDir) !== 'less' || path.basename(path.dirname(themeDir)) !== 'css') return [];
    const templateRoot = path.resolve(themeDir, '..', '..');
    const violations = [];
    const walk = (dir) => {
        for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            if (entry.isDirectory()) {
                if (!TEMPLATE_SKIP.has(entry.name)) walk(path.join(dir, entry.name));
                continue;
            }
            if (!TEMPLATE_FILES.test(entry.name)) continue;
            const file = path.join(dir, entry.name);
            fs.readFileSync(file, 'utf8')
                .split('\n')
                .forEach((line, index) => {
                    if (/^\s*(\/\/|\*|<!--)/.test(line)) return;
                    const cleaned = line.replace(/#\{[^}]*\}/g, '').replace(/&#\d+;/g, '').replace(/href="#[^"]*"/g, '').replace(/id="[^"]*"/g, '');
                    const hasColor = /#[\da-f]{6}\b|#[\da-f]{3}\b(?![\w-])/i.test(cleaned);
                    if (hasColor || /style="[^"]*(?:color|background)/.test(line))
                        violations.push(violation('R16', `theme/${path.relative(templateRoot, file).split(path.sep).join('/')}`, line.trim().slice(0, 70), index + 1, 'color value in a template'));
                });
        }
    };
    if (fs.existsSync(templateRoot)) walk(templateRoot);
    return violations;
}

/**
 * Where a token is declared, per mode, so a palette finding points at an editable line
 * instead of at the compiled bundle.
 *
 * @param {Map<string,string>} theme file path to source text
 * @returns {(token: string, mode: string) => {file: string, line: number}}
 */
function declarationSite(theme) {
    const sites = new Map();
    for (const file of THEME_TOKEN_LAYER) {
        if (!theme.has(file)) continue;
        for (const d of parseLess(theme.get(file)).declarations) {
            const stack = d.stack.join(' ');
            const mode = /dark/.test(stack) ? 'dark' : /light/.test(stack) ? 'light' : 'any';
            if (!sites.has(d.name)) sites.set(d.name, []);
            sites.get(d.name).push({ file, line: d.line, mode });
        }
    }
    return (token, mode) => {
        const found = sites.get(token) || [];
        const hit = found.find((s) => s.mode === mode) || found.find((s) => s.mode === 'any') || found[0];
        return hit ? { file: hit.file, line: hit.line } : { file: 'misc/tokens.less', line: 1 };
    };
}

/**
 * Files that no entry point imports, directly or indirectly. Such a file is never
 * compiled, so its rules never apply.
 *
 * @param {Map<string,string>} tree file path to source text
 * @returns {string[]} unreachable paths, sorted
 */
function unreachableFiles(tree) {
    const entries = [...tree.keys()].filter((file) => ENTRY_POINTS.test(file));
    // A partial tree without an entry point says nothing about reachability.
    if (!entries.length) return [];
    const reached = new Set();
    const visit = (file) => {
        if (reached.has(file) || !tree.has(file)) return;
        reached.add(file);
        const dir = path.posix.dirname(file);
        for (const match of tree.get(file).matchAll(/@import\s+(?:\([^)]*\)\s*)?['"]([^'"]+)['"]/g))
            visit(path.posix.normalize(path.posix.join(dir, match[1])));
    };
    entries.forEach(visit);
    return [...tree.keys()].filter((file) => !reached.has(file)).sort();
}


/**
 * The contrast contract: every pair a user actually sees, with its WCAG 2.2 AA
 * threshold (4.5 for text, 3 for boundaries and focus rings). The theme's palette
 * stress test reads the same list, so both run one contract.
 */
const CONTRAST_PAIRS = [
    ['page text', '--fg-general', '--bg-general', 4.5],
    ['widget text', '--fg-content-box', '--bg-content-box', 4.5],
    ['subtle surface text', '--fg-content-box', '--bg-subtle', 4.5],
    ['popover text', '--fg-popover', '--bg-popover', 4.5],
    ['header text', '--fg-header', '--bg-header', 4.5],
    ['inverse text', '--fg-inverse', '--bg-inverse', 4.5],
    ['field text', '--fg-content-box', '--bg-input', 4.5],
    ['placeholder', '--fg-placeholder', '--bg-input', 4.5],
    ['secondary text', '--fg-secondary', '--bg-content-box', 4.5],
    ['muted text', '--fg-muted', '--bg-content-box', 4.5],
    ['content link', '--fg-link', '--bg-content-box', 4.5],
    ['link hover', '--fg-link-hover', '--bg-content-box', 4.5],
    ['brand as text', '--brand', '--bg-content-box', 4.5],
    ['hover text', '--fg-hover', '--bg-hover', 4.5],
    ['pressed text', '--fg-content-box', '--bg-pressed', 4.5],
    ['brand fill text', '--fg-on-brand', '--bg-brand', 4.5],
    ['brand hover text', '--fg-on-brand', '--bg-brand-hover', 4.5],
    ['brand pressed text', '--fg-on-brand', '--bg-brand-pressed', 4.5],
    ['selected text', '--fg-selected', '--bg-selected', 4.5],
    ['selected hover text', '--fg-selected', '--bg-selected-hover', 4.5],
    ['selected pressed text', '--fg-selected', '--bg-selected-pressed', 4.5],
    ['disabled text', '--fg-disabled', '--bg-disabled', 4.5],
    ['input boundary', '--border-input', '--bg-input', 3],
    ['input focus', '--outline-focus', '--bg-input', 3],
    ['widget focus', '--outline-focus', '--bg-content-box', 3],
    ['selected indicator', '--brand', '--bg-selected', 3],
    ['selected hover indicator', '--brand', '--bg-selected-hover', 3],
    ['slider range against track', '--brand', '--border-default', 3],
    ['selected hover focus', '--outline-focus', '--bg-selected-hover', 3],
    ['selected pressed focus', '--outline-focus', '--bg-selected-pressed', 3],
    ['secondary on subtle', '--fg-secondary', '--bg-subtle', 4.5],
    ['muted on subtle', '--fg-muted', '--bg-subtle', 4.5],
    ['danger on subtle', '--fg-danger', '--bg-subtle', 4.5],
    ['warning on subtle', '--fg-warning', '--bg-subtle', 4.5],
    ['success on subtle', '--fg-success', '--bg-subtle', 4.5],
    ['canvas text', '--fg-general', '--bg-canvas', 4.5],
    ['accent text', '--fg-accent', '--bg-content-box', 4.5],
    ['text on accent', '--fg-on-bright', '--accent', 4.5],
    ['success text on surface', '--fg-on-success', '--bg-success', 4.5],
    ['success icon', '--fg-success', '--bg-content-box', 4.5],
    ['text on success strong', '--fg-on-success-strong', '--bg-success-strong', 4.5],
    ['danger text on surface', '--fg-on-danger', '--bg-danger', 4.5],
    ['danger icon', '--fg-danger', '--bg-content-box', 4.5],
    ['text on danger strong', '--fg-on-danger-strong', '--bg-danger-strong', 4.5],
    ['warning text on surface', '--fg-on-warning', '--bg-warning', 4.5],
    ['warning icon', '--fg-warning', '--bg-content-box', 4.5],
    ['text on warning strong', '--fg-on-warning-strong', '--bg-warning-strong', 4.5],
    ['text on info strong', '--fg-on-info-strong', '--bg-info-strong', 4.5],
    ['inverse hover text', '--fg-inverse', '--bg-inverse-hover', 4.5],
    ['popover link', '--fg-link', '--bg-popover', 4.5],
    ['header link', '--fg-link', '--bg-header', 4.5],
    ['page link', '--fg-link', '--bg-general', 4.5],
    ['subtle link', '--fg-link', '--bg-subtle', 4.5],
    ['footer text', '--fg-inverse', '--footer-topnav-bg', 4.5],
    ['card link', '--fg-link-content-box', '--bg-content-box', 4.5],
    ['card border on page', '--border-container', '--bg-general', 1.1],
];
const CONTRAST_ENTRIES = /^(?:constructor\.less|subthemes\/[^/]+\/subThemeConstructor\.less)$/;

/* ---------- color evaluation ---------- */

/** Splits `a, b(c, d)` on top-level commas only. */
function splitTopLevel(text) {
    const parts = [];
    let depth = 0;
    let current = '';
    for (const char of text) {
        if (char === '(') depth++;
        if (char === ')') depth--;
        if (char === ',' && depth === 0) {
            parts.push(current.trim());
            current = '';
        } else current += char;
    }
    if (current.trim()) parts.push(current.trim());
    return parts;
}

/**
 * sRGB channels of a hex color or a (nested) opaque `color-mix(in srgb, …)`.
 *
 * @returns {number[]|null} three channels in 0..1, or null for an expression we do not evaluate
 */
function channels(value) {
    const text = String(value).trim();
    const short = /^#([\da-f])([\da-f])([\da-f])$/i.exec(text);
    if (short) return channels(`#${short[1]}${short[1]}${short[2]}${short[2]}${short[3]}${short[3]}`);
    const hex = /^#([\da-f]{6})$/i.exec(text);
    if (hex) return [0, 2, 4].map((offset) => parseInt(hex[1].slice(offset, offset + 2), 16) / 255);
    if (text.startsWith('color-mix(') && text.endsWith(')')) {
        const [space, first, second] = splitTopLevel(text.slice('color-mix('.length, -1));
        if (space !== 'in srgb' || !second) return null;
        const part = (entry) => {
            const m = /^(.*?)\s+([\d.]+)%$/.exec(entry);
            return m ? { color: m[1].trim(), weight: Number(m[2]) / 100 } : { color: entry, weight: null };
        };
        const a = part(first);
        const b = part(second);
        if (a.weight === null && b.weight === null) a.weight = b.weight = 0.5;
        else if (a.weight === null) a.weight = 1 - b.weight;
        else if (b.weight === null) b.weight = 1 - a.weight;
        const ca = channels(a.color);
        const cb = channels(b.color);
        return ca && cb ? ca.map((channel, i) => channel * a.weight + cb[i] * b.weight) : null;
    }
    return null;
}

function relativeLuminance(rgb) {
    return rgb
        .map((channel) => (channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4))
        .reduce((sum, channel, i) => sum + channel * [0.2126, 0.7152, 0.0722][i], 0);
}

/** Replaces var() reads by their declared values until a literal remains. */
function resolveVars(value, tokens, seen = []) {
    return String(value).replace(/var\(\s*(--[\w-]+)\s*\)/g, (whole, name) => {
        if (!(name in tokens) || seen.includes(name)) return whole;
        return resolveVars(tokens[name], tokens, [...seen, name]);
    });
}

/** Declarations that apply in one mode, core defaults first, theme on top. */
function modeTokens(css, mode, into = {}) {
    postcss.parse(css).walkRules((rule) => {
        if (![':root', `.color-theme-${mode}`].includes(rule.selector)) return;
        for (let parent = rule.parent; parent; parent = parent.parent)
            if (parent.type === 'atrule' && parent.name === 'media' && !(mode === 'dark' && parent.params === '(prefers-color-scheme: dark)')) return;
        rule.nodes.filter((node) => node.type === 'decl' && node.prop.startsWith('--')).forEach((node) => {
            into[node.prop] = node.value;
        });
    });
    return into;
}

/**
 * R14: every visible foreground/background pair reaches its WCAG threshold, in both modes.
 *
 * @param {string} coreCss compiled core CSS (supplies the defaults a theme does not override)
 * @param {string} themeCss compiled CSS of one theme entry point
 * @param {(token: string, mode: string) => {file: string, line: number}} site where to point the developer
 * @returns {{violations: object[], warnings: object[]}}
 */
function checkContrast(coreCss, themeCss, site) {
    const violations = [];
    const warnings = [];
    for (const mode of ['light', 'dark']) {
        const tokens = modeTokens(themeCss, mode, modeTokens(coreCss, mode, {}));
        for (const [label, fg, bg, threshold] of CONTRAST_PAIRS) {
            const front = channels(resolveVars(`var(${fg})`, tokens));
            const back = channels(resolveVars(`var(${bg})`, tokens));
            if (!front || !back) {
                warnings.push(violation('R14', 'misc/tokens.less', `${fg} on ${bg}`, 1, `${mode}: color expression not evaluated, contrast unchecked`));
                continue;
            }
            const a = relativeLuminance(front);
            const b = relativeLuminance(back);
            const ratio = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
            if (ratio >= threshold) continue;
            const where = site(fg, mode);
            violations.push(violation('R14', where.file, `${label}: ${fg} on ${bg} — ${ratio.toFixed(2)}:1, needs ${threshold}:1 (${mode})`, where.line, 'contrast below the WCAG threshold'));
        }
    }
    return { violations, warnings };
}

/**
 * R15: a token set for one mode only sticks when the mode class overrides the system
 * preference, so light and dark must declare the same names.
 */
function checkModeSymmetry(css, site) {
    const names = { '.color-theme-light': new Set(), '.color-theme-dark': new Set() };
    postcss.parse(css).walkRules((rule) => {
        if (!(rule.selector in names)) return;
        rule.nodes.filter((n) => n.type === 'decl' && n.prop.startsWith('--')).forEach((n) => names[rule.selector].add(n.prop));
    });
    const missing = (from, other, mode) =>
        [...names[from]]
            .filter((name) => !names[other].has(name))
            .map((name) => {
                const where = site(name, mode);
                return violation('R15', where.file, `${name} — declared for ${mode} only`, where.line, 'token declared in one mode only');
            });
    return [...missing('.color-theme-light', '.color-theme-dark', 'light'), ...missing('.color-theme-dark', '.color-theme-light', 'dark')];
}

/* ---------- filesystem ---------- */

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

function loadBaseline(file = BASELINE_FILE) {
    return fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')).tolerated : [];
}

function loadColorExemptions() {
    return fs.existsSync(BASELINE_FILE) ? JSON.parse(fs.readFileSync(BASELINE_FILE, 'utf8')).colorExemptions || [] : [];
}

/**
 * Reads the real LESS trees and applies the baseline.
 *
 * @param {{moduleRoot?: string, lessRoot?: string, themeLessRoot?: string, baseline?: string[]}} [options]
 * @returns {Promise<{errors: Array, stale: string[], tolerated: Array, warnings: Array, violations: Array, themeChecked: boolean}>}
 */
async function runTokenCheck({ moduleRoot = MODULE_ROOT, lessRoot = LESS_ROOT, themeLessRoot = themePaths().lessRoot, baseline = loadBaseline() } = {}) {
    const core = readLessTree(path.resolve(moduleRoot, lessRoot));
    const themeDir = path.resolve(moduleRoot, themeLessRoot);
    const enabled = themePaths().enabled;
    const found = fs.existsSync(themeDir);
    const theme = enabled && found ? readLessTree(themeDir) : null;
    const themeState = { status: !enabled ? 'off' : found ? 'checked' : 'missing', name: path.basename(path.resolve(themeDir, '..', '..')), path: themeLessRoot };
    const { violations, warnings, tokens } = checkFiles(core, theme);
    let coreCss = '';
    violations.push(...(await checkCompiledTree(path.resolve(moduleRoot, lessRoot), { tokens, onCss: (css) => (coreCss = css) })));
    if (theme) {
        const entries = [...theme.keys()].filter((file) => /(?:^constructor\.less$|^crowdsourcing\/csConstructor\.less$|^subthemes\/[^/]+\/subThemeConstructor\.less$)/.test(file));
        const site = declarationSite(theme);
        violations.push(...checkThemeTemplates(themeDir));
        // The same component compiles into several theme bundles; report each finding once.
        const seenCompiled = new Set();
        const seenPalette = new Set();
        for (const entry of entries) {
            let themeCss = '';
            const compiled = await checkCompiledTree(themeDir, { entry, theme: true, tokens, onCss: (css) => (themeCss = css) });
            for (const v of compiled) {
                const key = `${v.rule}|${v.file}|${v.line}|${v.token}|${v.declaration?.property}|${v.declaration?.value}`;
                if (seenCompiled.has(key)) continue;
                seenCompiled.add(key);
                violations.push({ ...v, file: `theme/${v.file}` });
            }
            if (!themeCss || !CONTRAST_ENTRIES.test(entry)) continue;
            const contrast = checkContrast(coreCss, themeCss, site);
            // Subthemes inherit most roles; report a shared pair once, under the entry that hits it first.
            const knownAsymmetric = new Set(violations.filter((v) => v.rule === 'R7').map((v) => v.token));
            const symmetry = checkModeSymmetry(themeCss, site).filter((v) => !knownAsymmetric.has(v.token.split(' ')[0]));
            for (const v of [...contrast.violations, ...symmetry]) {
                const key = `${v.rule}|${v.token}`;
                if (seenPalette.has(key)) continue;
                seenPalette.add(key);
                violations.push({ ...v, file: `theme/${v.file}`, entry });
            }
            warnings.push(...contrast.warnings.filter((w) => !seenPalette.has(`W|${w.token}`) && seenPalette.add(`W|${w.token}`)));
        }
    }
    return { ...applyBaseline(violations, baseline), warnings, violations, theme: themeState, themeChecked: theme !== null };
}

/* ---------- theme token report (customer theme updates) ---------- */

// Renamed and removed public tokens: report hints and R1 messages. Removed = null.
const RENAMED_TOKENS = new Map([
    ['--ci-color1-dark', '--color-brand-dark'],
    ['--ci-hovercolor1', '--color-brand-hover'],
    ['--ci-color1', '--color-brand'],
    ['--container-small-border-radius', '--radius-container-small'],
    ['--container-border-radius', '--radius-container'],
    ['--button-border-radius', '--radius-button'],
    ['--container-box-shadow', '--shadow-container'],
    ['--color-theme-switch-dark-display', '--theme-switch-dark-display'],
    ['--color-theme-switch-light-display', '--theme-switch-light-display'],
    ['--placeholder', '--fg-placeholder'],
    ['--lk-general', '--fg-link'],
    ['--lk-general-hover', '--fg-link-hover'],
    ['--lk-general-active', '--fg-link-active'],
    ['--lk-content-box', '--fg-link-content-box'],
    ['--lh-general', '--fg-heading'],
    ['--lh-content-box', '--fg-heading-content-box'],
    ['--bg-active', '--bg-control-active'],
    ['--fg-controls', '--fg-on-control'],
    ['--border-color', '--border-default'],
    ['--accent-color', '--accent'],
    ['--focus-ring', '--outline-focus'],
    ['--mask-opaque', '--bg-mask'],
    ['--focus-ring-inner', '--outline-focus-inner'],
    ['--print-fg', '--fg-print'],
    ['--print-link', '--fg-print-link'],
    ['--print-border', '--border-print'],
    ['--widget-icon-color-body', '--widget-body-icon'],
    ['--widget-icon-color-topbar', '--widget-topbar-icon'],
    ['--widget-icon-color-expand', '--widget-expand-icon'],
    ['--login-nav-background', '--login-nav-bg'],
    ['--login-nav-background-hover', '--login-nav-bg-hover'],
    ['--login-nav-text-color', '--login-nav-fg'],
    ['--login-nav-text-color-hover', '--login-nav-fg-hover'],
    ['--login-nav-icon-color', '--login-nav-icon'],
    ['--login-nav-icon-color-hover', '--login-nav-icon-hover'],
    ['--login-nav-bookmarks-text-color', '--login-nav-bookmarks-fg'],
    ['--login-nav-bookmarks-text-color-hover', '--login-nav-bookmarks-fg-hover'],
    ['--login-nav-bookmarks-counter-background', '--login-nav-bookmarks-counter-bg'],
    ['--search-toolbar-background-active', '--search-toolbar-bg-active'],
    ['--search-toolbar-text-color-active', '--search-toolbar-fg-active'],
    ['--search-toolbar-icon-color-active', '--search-toolbar-icon-active'],
    ['--search-toolbar-text-color-hover', '--search-toolbar-fg-hover'],
    ['--search-toolbar-focus-outline-color', '--search-toolbar-outline-focus'],
    ['--toc-page-link-color', '--toc-page-link-fg'],
    ['--image-controls-background', '--image-controls-bg'],
    ['--image-controls-container-border-color', '--image-controls-container-border'],
    ['--image-controls-icon-color', '--image-controls-icon'],
    ['--datepicker-day-hover-bg', '--datepicker-day-bg-hover'],
    ['--datepicker-day-selected-bg', '--datepicker-day-bg-selected'],
    ['--datepicker-day-selected-fg', '--datepicker-day-fg-selected'],
    ['--datepicker-day-today-border-color', '--datepicker-day-border-today'],
    ['--datepicker-day-disabled-fg', '--datepicker-day-fg-disabled'],
    ['--datepicker-day-hasdata-bg', '--datepicker-day-bg-has-data'],
    ['--datepicker-day-hasdata-fg', '--datepicker-day-fg-has-data'],
    ['--datepicker-day-hasdata-hover-bg', '--datepicker-day-bg-has-data-hover'],
    ['--datepicker-day-hasdata-hover-fg', '--datepicker-day-fg-has-data-hover'],
    ['--datepicker-day-range-bg', '--datepicker-day-bg-in-range'],
    ['--ns-bg', '--newspaper-search-bg'],
    ['--ns-border-color', '--newspaper-search-border'],
    ['--ns-tab-bg', '--newspaper-search-tab-bg'],
    ['--ns-tab-border-color', '--newspaper-search-tab-border'],
    ['--ns-tab-fg', '--newspaper-search-tab-fg'],
    ['--ns-tab-fg-hover', '--newspaper-search-tab-fg-hover'],
    ['--ns-tab-active-color', '--newspaper-search-tab-fg-active'],
    ['--ns-label-fg', '--newspaper-search-label-fg'],
    ['--quick-filter-text-color', '--quick-filter-fg'],
    ['--quick-filter-dropdown-hover-bg', '--quick-filter-dropdown-bg-hover'],
    ['--quick-filter-invalid-color', '--quick-filter-fg-invalid'],
    ['--quick-filter-accent-color', '--quick-filter-accent'],
    ['--quick-filter-submit-icon-color', '--quick-filter-submit-icon'],
    ['--quick-filter-checkbox-active', '--quick-filter-checkbox-bg-active'],
    ['--quick-filter-status-color', '--quick-filter-status-fg'],
    ['--wc-dot-size', '--widget-calendar-dot-size'],
    ['--wc-dot-color', '--widget-calendar-dot-bg'],
    ['--wc-dot-color-on-selected', '--widget-calendar-dot-bg-selected'],
    ['--wc-popover-bg', '--widget-calendar-popover-bg'],
    ['--wc-popover-border', '--widget-calendar-popover-border'],
    ['--wc-popover-shadow', '--widget-calendar-popover-shadow'],
    ['--wc-popover-radius', '--widget-calendar-popover-radius'],
    ['--wc-popover-min-width', '--widget-calendar-popover-min-width'],
    ['--wc-popover-item-padding', '--widget-calendar-popover-item-padding'],
    ['--wc-popover-item-color', '--widget-calendar-popover-item-fg'],
    ['--wc-popover-item-hover-bg', '--widget-calendar-popover-item-bg-hover'],
    ['--wc-popover-item-hover-color', '--widget-calendar-popover-item-fg-hover'],
    ['--wc-popover-font-size', '--widget-calendar-popover-font-size'],
    ['--combined-facets-color', '--combined-facets-fg'],
    ['--combined-facets-accent-contrast', '--combined-facets-fg-on-accent'],
    ['--combined-facets-muted', '--combined-facets-fg-muted'],
    ['--combined-facets-box', '--combined-facets-box-bg'],
    ['--combined-facets-badge-background', '--combined-facets-badge-bg'],
    ['--combined-facets-badge-background-hover', '--combined-facets-badge-bg-hover'],
    ['--combined-facets-badge-color', '--combined-facets-badge-fg'],
    ['--combined-facets-badge-muted', '--combined-facets-badge-fg-muted'],
    ['--combined-facets-badge-contrast', '--combined-facets-badge-fg-contrast'],
    ['--active-facets-badge-background', '--active-facets-badge-bg'],
    ['--active-facets-badge-background-hover', '--active-facets-badge-bg-hover'],
    ['--active-facets-badge-color', '--active-facets-badge-fg'],
    ['--active-facets-badge-muted', '--active-facets-badge-fg-muted'],
    ['--active-facets-badge-contrast', '--active-facets-badge-fg-contrast'],
    ['--facet-control-color', '--facet-control-fg'],
    ['--archives-title-color', '--archives-title-fg'],
    ['--archives-description-color', '--archives-description-fg'],
    ['--archives-placeholder-icon-color', '--archives-placeholder-icon'],
    ['--ner-person-color', '--ner-person-fg'],
    ['--ner-location-color', '--ner-location-fg'],
    ['--ner-institution-color', '--ner-institution-fg'],
    ['--sidebar-preview-highlight', '--sidebar-preview-outline'],
    ['--image-controls-region-active-color', '--image-controls-region-fg-active'],
    ['--pdf-selected-highlight-bg', '--pdf-highlight-bg-selected'],
    ['--image-region-focus-color', '--image-region-outline-focus'],
    ['--image-region-drawing-color', '--image-region-outline-drawing'],
    ['--chronology-chart-color', '--chronology-chart-fg'],
    ['--mei-note-active-color', '--mei-note-fg-active'],
    ['--login-nav-bookmarks-background', '--login-nav-bookmarks-bg'],
    ['--login-nav-dropdown-border-color', '--login-nav-dropdown-border'],
    ['--search-toolbar-divider-color', '--search-toolbar-divider'],
    ['--search-toolbar-icon-color', '--search-toolbar-icon'],
    ['--search-toolbar-icon-color-hover', '--search-toolbar-icon-hover'],
    ['--toc-page-hover-bg', '--toc-page-bg-hover'],
    ['--toc-page-toggle-icon-color', '--toc-page-toggle-icon'],
    ['--toc-page-toggle-icon-color-hover', '--toc-page-toggle-icon-hover'],
    ['--toc-page-link-color-hover', '--toc-page-link-fg-hover'],
    ['--toc-page-border-color', '--toc-page-border'],
    ['--image-controls-border-color', '--image-controls-border'],
    ['--image-controls-icon-color-hover', '--image-controls-icon-hover'],
    ['--image-controls-icon-color-active', '--image-controls-icon-active'],
    ['--image-controls-action-background', '--image-controls-action-bg'],
    ['--image-controls-action-background-hover', '--image-controls-action-bg-hover'],
    ['--image-controls-slider-thumb-background', '--image-controls-slider-thumb-bg'],
    ['--image-controls-slider-border-color', '--image-controls-slider-border'],
    ['--quick-filter-label-color', '--quick-filter-label-fg'],
    ['--quick-filter-control-hover-bg', '--quick-filter-control-bg-hover'],
    ['--immersive-chrome-hover-bg', '--immersive-chrome-bg-hover'],
    ['--immersive-chrome-border-color', '--immersive-chrome-border'],
    ['--immersive-key-border-color', '--immersive-key-border'],
    ['--immersive-metadata-hover-bg', '--immersive-metadata-bg-hover'],
    ['--combined-facets-background', '--combined-facets-bg'],
    ['--facet-control-active', '--facet-control-bg-active'],
    ['--facet-control-active-fg', '--facet-control-fg-active'],
    ['--archives-overlay-text', '--archives-overlay-fg'],
    ['--numeric-paginator-active-bg', '--numeric-paginator-bg-active'],
    ['--calendar-day-hover-bg', '--calendar-day-bg-hover'],
    ['--calendar-day-hover-fg', '--calendar-day-fg-hover'],
    ['--ner-navigation-hover-bg', '--ner-navigation-bg-hover'],
    ['--entity-work-color', '--entity-work-bg'],
    ['--entity-person-color', '--entity-person-bg'],
    ['--entity-agent-color', '--entity-agent-bg'],
    ['--entity-award-color', '--entity-award-bg'],
    ['--entity-event-color', '--entity-event-bg'],
    ['--border', '--border-faint (a color now — readers write border: 1px solid var(--border-faint))'],
    ['--border-controls', '--border-input (a color now — readers write border: 1px solid var(--border-input))'],
    ['--archives-card-border', '--archives-card-border-color (a color now)'],
    ['--immersive-accent-rgb', '--immersive-accent (a color, no rgb triplet)'],
    ['--immersive-ocr-block-rgb', '--immersive-ocr-block (a color)'],
    ['--immersive-ocr-line-rgb', '--immersive-ocr-line (a color)'],
    ['--immersive-ocr-word-rgb', '--immersive-ocr-word (a color)'],
    ['--immersive-chevron-scrim', null],
    ['--datepicker-border-color', null],
    ['--active-facets-badge-more-color', null],
    ['--facet-boolean-background', null],
    ['--archives-card-shadow-hover', null],
    ['--widget-toc-hover-bg', null],
    ['--widget-toc-toggle-icon-color', null],
    ['--widget-toc-toggle-icon-color-hover', null],
    ['--widget-toc-link-color-hover', null],
    ['--widget-toc-active-color', null],
]);
const renameHint = (name) => (RENAMED_TOKENS.has(name) ? (RENAMED_TOKENS.get(name) ? `renamed — now ${RENAMED_TOKENS.get(name)}` : 'removed — delete the read') : null);
const THEME_BUCKETS = ['light', 'dark', 'base', 'palette', 'root'];
const VERDICT_ORDER = ['unknown', 'renamed', 'keep', 'pair', 'redundant'];

function themeBucket(stack) {
    for (const header of stack) {
        if (/^\.mx-theme-light(\(\))?$/.test(header)) return 'light';
        if (/^\.mx-theme-dark(\(\))?$/.test(header)) return 'dark';
        if (/^\.mx-theme-base\(\)$/.test(header)) return 'base';
        if (/^\.mx-theme-palette\(\)$/.test(header)) return 'palette';
    }
    return 'root';
}

function normalizeValue(value) {
    return value
        .trim()
        .toLowerCase()
        .replace(/#([0-9a-f])([0-9a-f])([0-9a-f])(?![0-9a-f])/g, '#$1$1$2$2$3$3')
        .replace(/\s*,\s*/g, ',')
        .replace(/\s+/g, ' ');
}

function resolveTokens(value, lookup, depth = 0) {
    if (depth > 12) return value;
    const next = value.replace(/var\(\s*(--[\w-]+)\s*(?:,\s*([^()]*(?:\([^()]*\))?[^()]*))?\)/g, (match, name, fallback) => {
        const found = lookup(name);
        if (found !== undefined) return found;
        return fallback !== undefined ? fallback.trim() : match;
    });
    return next === value ? value : resolveTokens(next, lookup, depth + 1);
}

/**
 * Compares the token overrides of a theme with the core defaults of the same mode.
 *
 * @param {string[]} sources LESS sources of the theme (mixins.less, variables.less)
 * @param {{light: Map<string,string>, dark: Map<string,string>}} core emitted core tokens per mode
 * @returns {Array<{token: string, mode: string, line: number, verdict: string, theme?: string, core?: string, literal?: boolean, hint?: string}>}
 */
function classifyThemeTokens(sources, core) {
    const declared = Object.fromEntries(THEME_BUCKETS.map((bucket) => [bucket, new Map()]));
    for (const source of sources) for (const d of parseLess(source).declarations) declared[themeBucket(d.stack)].set(d.name, { value: d.value, line: d.line });
    const coreHas = (name) => core.light.has(name) || core.dark.has(name);
    const lookupFor = (mode) => {
        const theme = new Map([...declared.palette, ...declared.base, ...declared.root, ...declared[mode]].map(([name, d]) => [name, d.value]));
        return (name) => (theme.has(name) ? theme.get(name) : core[mode].get(name));
    };
    const coreLookup = (mode) => (name) => core[mode].get(name);
    const rows = [];
    for (const bucket of THEME_BUCKETS) {
        for (const [name, { value, line }] of declared[bucket]) {
            const modes = bucket === 'light' || bucket === 'dark' ? [bucket] : ['light', 'dark'];
            const row = { token: name, mode: bucket, line, theme: value, literal: COLOR_LITERAL.test(withoutStrings(value)) };
            if (!coreHas(name)) {
                const renamed = RENAMED_TOKENS.get(name);
                const target = renamed && /^--[\w-]+$/.test(renamed) && coreHas(renamed) ? renamed : null;
                if (
                    target &&
                    modes.every((mode) => normalizeValue(resolveTokens(value, lookupFor(mode))) === normalizeValue(resolveTokens(core[mode].get(target) ?? '', coreLookup(mode))))
                ) {
                    rows.push({
                        ...row,
                        verdict: 'redundant',
                        core: modes.map((mode) => core[mode].get(target)).join(' | '),
                        hint: `old name of ${target}; equals its core default`,
                    });
                } else if (renamed) rows.push({ ...row, verdict: 'renamed', hint: `now ${renamed}` });
                else
                    rows.push({
                        ...row,
                        verdict: 'unknown',
                        hint: RENAMED_TOKENS.has(name) ? 'removed — delete the line' : 'no core token of this name — theme-only feature or removed',
                    });
                continue;
            }
            row.core = modes.map((mode) => core[mode].get(name)).join(' | ');
            const same = modes.every(
                (mode) => normalizeValue(resolveTokens(value, lookupFor(mode))) === normalizeValue(resolveTokens(core[mode].get(name) ?? '', coreLookup(mode)))
            );
            row.verdict = same ? 'redundant' : 'keep';
            if (!same && row.literal && bucket !== 'palette') row.hint = 'color literal — declare it as --color-* in .mx-theme-palette() and read it here';
            rows.push(row);
        }
    }
    for (const [mode, other] of [
        ['light', 'dark'],
        ['dark', 'light'],
    ]) {
        for (const [name, { line }] of declared[mode]) {
            if (declared[other].has(name) || !coreHas(name)) continue;
            if (rows.some((r) => r.token === name && r.mode === mode && r.verdict === 'keep'))
                rows.push({ token: name, mode, line, verdict: 'pair', hint: `add a value to .mx-theme-${other}() as well` });
        }
    }
    return rows.sort((a, b) => VERDICT_ORDER.indexOf(a.verdict) - VERDICT_ORDER.indexOf(b.verdict) || a.token.localeCompare(b.token) || a.mode.localeCompare(b.mode));
}

/**
 * Compiles the core token layer and classifies the overrides of a theme against it.
 *
 * @param {string} themeLessRoot the theme's css/less directory
 * @param {{moduleRoot?: string, lessRoot?: string}} [options]
 * @returns {Promise<Array>} rows of classifyThemeTokens
 */
async function themeTokenReport(themeLessRoot, { moduleRoot = MODULE_ROOT, lessRoot = LESS_ROOT } = {}) {
    const root = path.resolve(moduleRoot, lessRoot);
    const filename = path.join(root, 'constructor.less');
    const output = await less.render(fs.readFileSync(filename, 'utf8'), { filename, paths: [root, path.join(root, 'crowdsourcing')] });
    const light = new Map();
    const dark = new Map();
    postcss.parse(output.css).walkRules((rule) => {
        for (let parent = rule.parent; parent.type !== 'root'; parent = parent.parent) if (parent.type === 'atrule') return;
        if (rule.selector === ':root') rule.walkDecls(/^--/, (d) => light.set(d.prop, d.value) && dark.set(d.prop, d.value));
        else if (rule.selector === '.color-theme-dark') rule.walkDecls(/^--/, (d) => dark.set(d.prop, d.value));
    });
    const themeRoot = path.resolve(moduleRoot, themeLessRoot);
    const sources = ['misc/mixins.less', 'misc/variables.less']
        .filter((file) => fs.existsSync(path.join(themeRoot, file)))
        .map((file) => fs.readFileSync(path.join(themeRoot, file), 'utf8'));
    return classifyThemeTokens(sources, { light, dark });
}

const VERDICT_TEXT = {
    unknown: 'unknown — no core token of this name (theme-only feature, or removed: delete)',
    renamed: 'renamed — the core token has a new name (rename the line, keep the value)',
    keep: 'keep — differs from the core default',
    pair: 'pair — declared in one mode only',
    redundant: 'redundant — equals the core default (delete)',
};

function formatThemeTokenReport(rows) {
    const lines = [VERDICT_ORDER.map((v) => `${v} ${rows.filter((r) => r.verdict === v).length}`).join(' · ')];
    for (const verdict of VERDICT_ORDER) {
        const group = rows.filter((r) => r.verdict === verdict);
        if (!group.length) continue;
        lines.push('', VERDICT_TEXT[verdict]);
        for (const r of group) {
            const value = r.theme === undefined ? '' : `  ${r.theme}`;
            const core = r.core === undefined ? '' : `  → core ${r.core}`;
            lines.push(`  ${r.token}  ${r.mode}:${r.line}${value}${core}${r.hint ? `  — ${r.hint}` : ''}`);
        }
    }
    return lines.join('\n');
}

module.exports = {
    LESS_ROOT,
    CONTRAST_PAIRS,
    splitTopLevel,
    checkContrast,
    checkModeSymmetry,
    TOKEN_LAYER,
    THEME_ROOT,
    THEME_LESS_ROOT,
    themePaths,
    BASELINE_FILE,
    parseLess,
    checkFiles,
    applyBaseline,
    formatReport,
    keyOf,
    loadBaseline,
    runTokenCheck,
    checkCompiledTree,
    classifyThemeTokens,
    themeTokenReport,
    formatThemeTokenReport,
};
