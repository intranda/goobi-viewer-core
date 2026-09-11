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
const THEME_LESS_ROOT = '../goobi-viewer-theme-reference/WebContent/resources/themes/reference/css/less';
const THEME_MIXINS = 'misc/mixins.less';
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
            for (const d of p.declarations) if (d.name.startsWith('--color-') && d.stack.includes('.mx-theme-palette()')) themePalette.add(d.name);
            for (const name of staticColorNames(p)) themeStaticColors.add(name);
        }
        for (const [file, p] of themeParsed) {
            const inLayer = file === themeMixins || file === VARIABLES_FILE || /^subthemes\/[^/]+\/subThemeConstructor\.less$/.test(file);
            const report = (rule, token, line, message) => violations.push(violation(rule, `theme/${file}`, token, line, message));
            for (const r of p.reads) {
                if (!INTERNAL.test(r.name) && !themeTokens.has(r.name)) report('R1', r.name, r.line, renameHint(r.name) || 'unknown core or theme token');
                if ((themePalette.has(r.name) || paletteTokens.has(r.name)) && !(file === themeMixins && r.stack.some((h) => /^\.mx-theme-(light|dark)\(\)$/.test(h))))
                    report('R8', r.name, r.line, 'theme components and features read roles, not palette primitives');
            }
            for (const d of p.declarations) {
                if (!inLayer && !INTERNAL.test(d.name)) report('R2', d.name, d.line, 'public theme tokens belong in the root token wiring');
                const paletteDeclaration = file === themeMixins && d.stack.includes('.mx-theme-palette()');
                const subthemeBrand = /^subthemes\/[^/]+\/subThemeConstructor\.less$/.test(file) && /^--color-brand(-hover|-dark)?$/.test(d.name);
                if (!paletteDeclaration && !subthemeBrand && COLOR_LITERAL.test(withoutStrings(d.value)))
                    report('R9', d.name, d.line, 'theme color literals belong in .mx-theme-palette()');
                const mode = d.stack.find((h) => /^\.mx-theme-(light|dark)\(\)$/.test(h));
                if (mode) {
                    const other = mode.includes('-light') ? '.mx-theme-dark()' : '.mx-theme-light()';
                    if (!p.declarations.some((x) => x.name === d.name && x.stack.includes(other))) report('R7', d.name, d.line, `missing counterpart in ${other}`);
                }
            }
            if (!inLayer) {
                checkTypography(`theme/${file}`, p, theme.get(file));
                for (const b of p.blocks)
                    if (MODE_BRANCH.test(b.header) || /@colorModes/.test(b.header)) report('R4', b.header, b.line, 'theme mode branching belongs in the token wiring');
                for (const d of p.properties) {
                    const value = colorValue(d.value);
                    if (COLOR_LITERAL.test(value) || [...value.matchAll(AT_USE)].some((m) => themeStaticColors.has(m[1])))
                        report('R10', d.name, d.line, 'theme component colors must read tokens');
                }
            }
        }

        const mixins = themeParsed.get(themeMixins);
        if (mixins) {
            const seen = new Set();
            for (const d of mixins.declarations) {
                if (INTERNAL.test(d.name) || tokens.has(d.name) || readByTheme.has(d.name) || seen.has(d.name)) continue;
                seen.add(d.name);
                const message = readByCore.has(d.name)
                    ? 'theme token that core reads via fallback only — belongs in the token layer'
                    : 'theme token unknown to core and never read by the theme';
                violations.push(violation('R6', themeMixins, d.name, d.line, message));
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
async function checkCompiledTree(root, { entry = 'constructor.less', tokens = new Set(), theme = false } = {}) {
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
        // Repeated mode expansions should not flood the report.
        if (!violations.some((v) => v.rule === rule && v.file === file && v.line === line && v.token === token && v.message === message)) {
            violations.push(violation(rule, file, token, line, message));
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
            !(theme && file === THEME_MIXINS) &&
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
        const inLayer = theme ? file === VARIABLES_FILE || file === THEME_MIXINS : TOKEN_LAYER.includes(file);
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

function formatReport({ errors, stale, tolerated, warnings }, { showWarnings = true } = {}) {
    const lines = [];
    if (errors.length) {
        lines.push(`${errors.length} token violation(s):`);
        for (const v of errors) lines.push(`  ${v.rule} ${v.file}:${v.line} ${v.token} — ${v.message}`);
    }
    if (stale.length) {
        lines.push(`${stale.length} stale baseline entr${stale.length === 1 ? 'y' : 'ies'} (remove from tokenCheck.baseline.json):`);
        for (const k of stale) lines.push(`  ${k}`);
    }
    lines.push(`${tolerated.length} tolerated by baseline, ${warnings.length} warning(s)${showWarnings || !warnings.length ? '' : ' (GV_TOKENWARN=1 lists them)'}`);
    if (showWarnings) for (const w of warnings) lines.push(`  ${w.rule} ${w.token} — ${w.message}`);
    return lines.join('\n');
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
async function runTokenCheck({ moduleRoot = MODULE_ROOT, lessRoot = LESS_ROOT, themeLessRoot = THEME_LESS_ROOT, baseline = loadBaseline() } = {}) {
    const core = readLessTree(path.resolve(moduleRoot, lessRoot));
    const themeDir = path.resolve(moduleRoot, themeLessRoot);
    const theme = fs.existsSync(themeDir) ? readLessTree(themeDir) : null;
    const { violations, warnings, tokens } = checkFiles(core, theme);
    violations.push(...(await checkCompiledTree(path.resolve(moduleRoot, lessRoot), { tokens })));
    if (theme) {
        const entries = [...theme.keys()].filter((file) => /(?:^constructor\.less$|^crowdsourcing\/csConstructor\.less$|^subthemes\/[^/]+\/subThemeConstructor\.less$)/.test(file));
        for (const entry of entries) violations.push(...(await checkCompiledTree(themeDir, { entry, theme: true, tokens })));
    }
    return { ...applyBaseline(violations, baseline), warnings, violations, themeChecked: theme !== null };
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
    TOKEN_LAYER,
    THEME_LESS_ROOT,
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
