/*
Token check for the LESS token layer (core, optionally the reference theme).

Rules
  R1  every var(--x) read in core names a token of the token layer; internal --_ tokens are
      declared in the same file or carry a fallback
  R2  tokens without the --_ prefix are declared only in the token layer, under
      :root / .color-theme-dark / .color-theme-light (specificity 0,1,0) or inside mixin definitions
  R4  --_ tokens never live in the token layer; prefers-color-scheme / .color-theme-* never outside it
  R5  (warning) token declared but read nowhere in core or theme
  R6  token in the theme mixins that core does not know and the theme never reads
  R7  every .x-light() mixin of the token layer declares the same tokens as .x-dark()
  R8  palette tokens are read only by the semantic mixins and never get a LESS alias — features and
      components read roles; only the brand trio --ci-color1, --ci-hovercolor1, --accent-color is exempt
  R9  outside misc/palette.less the token layer holds no color literal; alphas derive via rgb(from …)

Known violations are tolerated via tokenCheck.baseline.json; entries that no longer
occur are reported as stale so the baseline can only shrink.
*/

const fs = require('fs');
const path = require('path');

const MODULE_ROOT = path.resolve(__dirname, '..');
const LESS_ROOT = 'src/main/resources/META-INF/resources/resources/css/less';
const TOKEN_LAYER = ['misc/palette.less', 'misc/tokens.less'];
const THEME_LESS_ROOT = '../goobi-viewer-theme-reference/WebContent/resources/themes/reference/css/less';
const THEME_MIXINS = 'misc/mixins.less';
const BASELINE_FILE = path.join(__dirname, 'tokenCheck.baseline.json');

const TOKEN_SELECTORS = new Set([':root', '.color-theme-dark', '.color-theme-light', '@media (prefers-color-scheme: dark)', '@media (prefers-color-scheme: light)']);
const PALETTE_FILE = 'misc/palette.less';
const BRAND_TRIO = /^--(ci-color1|ci-hovercolor1|accent-color)$/;
const SEMANTIC_MIXIN = /^\.[\w-]*semantic[\w-]*\(\)$/;
const COLOR_LITERAL = /#[0-9a-f]{3,8}\b|\b(rgba?|hsla?)\(\s*\d|(^|[\s,(])(black|white)(?=[\s,)]|$)/i;
const MIXIN_DEFINITION = /^\.[\w-]+\([^)]*\)$/;
const MIXIN_CALL = /^\.[\w-]+(\([^)]*\))?$/;
const DECLARATION = /^(--[\w-]+)\s*:\s*([\s\S]*)$/;
const ALIAS = /^@([\w-]+)\s*:\s*var\(\s*(--[\w-]+)/;
const STATIC = /^@([\w-]+)\s*:/;
const READ = /var\(\s*(--[\w-]+)\s*(,)?/g;
const AT_USE = /@([\w-]+)/g;
const INTERNAL = /^--_/;
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

function collectReads(text, line, stack, into) {
    for (const m of text.matchAll(READ)) {
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
    const result = { declarations: [], reads: [], calls: [], aliases: [], statics: [], imports: [], blocks: [], atUses: new Set() };
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
            return;
        }
        const stat = stmt.match(STATIC);
        if (stat && context.length === 0) {
            result.statics.push({ name: stat[1], line: at });
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
        for (const r of p.reads) used.add(r.name);
        for (const a of p.aliases) aliasByToken.set(a.token, a.alias);
        for (const u of p.atUses) atUsed.add(u);
    }
    for (const [token, alias] of aliasByToken) if (atUsed.has(alias)) used.add(token);
    return used;
}
const flattened = (stack) => stack.join(' ');
const allowedInTokenLayer = (stack) => stack.length > 0 && stack.every((h) => TOKEN_SELECTORS.has(h) || MIXIN_DEFINITION.test(h));

/**
 * Runs all rules over parsed sources.
 *
 * @param {Map<string,string>} core relative path → LESS source (core tree)
 * @param {Map<string,string>|null} theme relative path → LESS source (theme tree), or null to skip R5-theme/R6
 * @param {{tokenLayer: string[], aliasFile: string, themeMixins: string}} options
 * @returns {{violations: Array, warnings: Array, tokens: Set<string>}}
 */
function checkFiles(core, theme, { tokenLayer = TOKEN_LAYER, paletteFile = PALETTE_FILE, themeMixins = THEME_MIXINS } = {}) {
    const parsed = new Map();
    for (const [file, source] of core) parsed.set(file, parseLess(source));

    const violations = [];
    const warnings = [];
    const tokens = new Set();
    const isTokenLayer = (file) => tokenLayer.includes(file);

    for (const file of tokenLayer) {
        const p = parsed.get(file);
        if (!p) continue;
        for (const d of p.declarations) if (!INTERNAL.test(d.name)) tokens.add(d.name);
    }

    for (const [file, p] of parsed) {
        if (isTokenLayer(file)) {
            for (const d of p.declarations) {
                if (INTERNAL.test(d.name)) {
                    violations.push(violation('R4', file, d.name, d.line, 'internal token declared in the token layer'));
                } else if (!allowedInTokenLayer(d.stack)) {
                    violations.push(
                        violation('R2', file, d.name, d.line, `declared under "${flattened(d.stack) || '(top level)'}" — only :root, .color-theme-dark, .color-theme-light`)
                    );
                }
            }
            for (const c of p.calls) {
                if (!allowedInTokenLayer(c.stack)) {
                    violations.push(
                        violation('R2', file, c.name, c.line, `mixin called under "${flattened(c.stack) || '(top level)'}" — only :root, .color-theme-dark, .color-theme-light`)
                    );
                }
            }
            const byMixin = new Map();
            for (const d of p.declarations) {
                const mixin = d.stack.find((h) => MIXIN_DEFINITION.test(h));
                if (!mixin) continue;
                if (!byMixin.has(mixin)) byMixin.set(mixin, new Map());
                byMixin.get(mixin).set(d.name, d.line);
            }
            for (const [mixin, lightTokens] of byMixin) {
                const m = mixin.match(/^(\.[\w-]+)-light\(\)$/);
                if (!m) continue;
                const darkName = `${m[1]}-dark()`;
                const darkTokens = byMixin.get(darkName) || new Map();
                for (const [name, line] of lightTokens)
                    if (!darkTokens.has(name)) violations.push(violation('R7', file, name, line, `declared in ${mixin} but not in ${darkName}`));
                for (const [name, line] of darkTokens)
                    if (!lightTokens.has(name)) violations.push(violation('R7', file, name, line, `declared in ${darkName} but not in ${mixin}`));
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
                violations.push(violation('R1', file, r.name, r.line, 'read of a token the token layer does not declare'));
            }
        }
    }

    const paletteTokens = new Set();
    const paletteParsed = parsed.get(paletteFile);
    if (paletteParsed) for (const d of paletteParsed.declarations) paletteTokens.add(d.name);
    const restricted = new Set([...paletteTokens].filter((n) => !BRAND_TRIO.test(n)));
    for (const [file, p] of parsed) {
        for (const r of p.reads) {
            if (!restricted.has(r.name)) continue;
            if (isTokenLayer(file) && r.stack.some((h) => SEMANTIC_MIXIN.test(h))) continue;
            violations.push(violation('R8', file, r.name, r.line, 'palette token read outside the semantic roles — read a semantic role instead'));
        }
        for (const a of p.aliases) {
            if (restricted.has(a.token)) violations.push(violation('R8', file, a.token, a.line, `@${a.alias} exposes a palette token as a LESS variable`));
        }
        if (paletteParsed && isTokenLayer(file) && file !== paletteFile) {
            for (const d of p.declarations) {
                if (COLOR_LITERAL.test(d.value)) violations.push(violation('R9', file, d.name, d.line, `color literal "${d.value}" — every color comes from ${paletteFile}`));
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

/* ---------- baseline ---------- */

const keyOf = (v) => `${v.rule} ${v.file} ${v.token}`;

/**
 * @param {Array} violations
 * @param {string[]} baseline tolerated keys "RULE file token"
 */
function applyBaseline(violations, baseline) {
    const tolerated = new Set(baseline);
    const hit = new Set();
    const errors = [];
    const kept = [];
    for (const v of violations) {
        const key = keyOf(v);
        if (tolerated.has(key)) {
            hit.add(key);
            kept.push(v);
        } else {
            errors.push(v);
        }
    }
    return { errors, tolerated: kept, stale: baseline.filter((k) => !hit.has(k)) };
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

/**
 * Reads the real LESS trees and applies the baseline.
 *
 * @param {{moduleRoot?: string, lessRoot?: string, themeLessRoot?: string, baseline?: string[]}} [options]
 * @returns {{errors: Array, stale: string[], tolerated: Array, warnings: Array, violations: Array, themeChecked: boolean}}
 */
function runTokenCheck({ moduleRoot = MODULE_ROOT, lessRoot = LESS_ROOT, themeLessRoot = THEME_LESS_ROOT, baseline = loadBaseline() } = {}) {
    const core = readLessTree(path.resolve(moduleRoot, lessRoot));
    const themeDir = path.resolve(moduleRoot, themeLessRoot);
    const theme = fs.existsSync(themeDir) ? readLessTree(themeDir) : null;
    const { violations, warnings } = checkFiles(core, theme);
    return { ...applyBaseline(violations, baseline), warnings, violations, themeChecked: theme !== null };
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
};
