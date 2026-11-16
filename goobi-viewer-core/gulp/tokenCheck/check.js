/**
 * Token check — the six sentences over the core tree and the theme tree: every finding with its fix and its changes.
 */

const { adviceContext, adviseColour, adviseFontSize, bindings } = require('./advice');
const { codeBlock, spanOf, tokenLayout, uniqueLines } = require('./code');
const {
    ADMIN_FILES,
    ADMIN_PALETTE_FILE,
    ADMIN_TOKENS_FILE,
    AT_USE,
    COLOR_LITERAL,
    COMPONENT_COLOR,
    CORE_DEFAULTS_FILE,
    DETACHED_DEFINITION,
    FIXED_GEOMETRY_NOTE,
    FIXED_TYPE_SIZE,
    FONT_CONSTANT,
    INTERNAL,
    LIGHT_DARK,
    MIXIN_DEFINITION,
    MODE_BRANCH,
    MODE_MIXIN,
    PALETTE_FILE,
    PALETTE_FILES,
    PALETTE_WORD,
    PALETTE_WORD_EXEMPT,
    ROLE_PREFIX,
    ROOT_SELECTORS,
    SEMANTIC_MIXIN,
    STATIC_COLOUR_TREES,
    SUBTHEME_CONSTRUCTOR,
    THEME_LAYER,
    THEME_PALETTE_MIXIN,
    TOKENS_FILE,
    TOKEN_LAYER,
    TOKEN_SELECTORS,
    TWIN_PARTNERS,
    VARIABLES_FILE,
    adminRelevant,
    isTypographyProperty,
    layerHome,
} = require('./constants');
const { colorValue, parseAll, staticColorNames, stripComments, withoutStrings } = require('./parse');
const { RENAMED_TOKENS, successorAdvice } = require('./renames');

const violation = (rule, file, token, line, message) => ({ rule, file, token, line, message });

/** Tokens read via var() or via a used @alias across a parsed tree. */

const finding = (file, line, what, fix) => ({ file, line, what, fix });

const flattened = (stack) => stack.join(' ');

const inMixin = (stack) => stack.some((h) => MIXIN_DEFINITION.test(h) || DETACHED_DEFINITION.test(h));

const underRoot = (stack) => {
    const context = stack.filter((h) => !MIXIN_DEFINITION.test(h) && !DETACHED_DEFINITION.test(h) && h !== '&');
    return stack.length > 0 && context.every((h) => TOKEN_SELECTORS.has(h)) && context.filter((h) => !h.startsWith('@')).length <= 1;
};

function staticTypeSizes(parsedMaps) {
    const definitions = parsedMaps.flatMap((m) => [...m.values()].flatMap((p) => p.statics));
    const names = new Set();
    let previous;
    do {
        previous = names.size;
        for (const d of definitions) {
            const value = withoutStrings(d.value);
            if (FIXED_TYPE_SIZE.test(value) || [...value.matchAll(AT_USE)].some((m) => names.has(m[1]))) {
                names.add(d.name);
            }
        }
    } while (names.size !== previous);
    return names;
}

function componentValues(file, p, source, staticColours, typeSizes, out) {
    const lines = source.split('\n');
    for (const d of p.properties) {
        const value = colorValue(d.value);
        const what = `${d.name}: ${d.value.replace(/\s+/g, ' ')}`;
        if (COMPONENT_COLOR.test(value)) {
            out.push(finding(file, d.line, what, 'colour written into the component — read a role, e.g. var(--fg-content-box) or var(--bg-content-box)'));
        } else if ([...value.matchAll(AT_USE)].some((m) => staticColours.has(m[1]))) {
            out.push(finding(file, d.line, what, 'static colour constant — read a role instead'));
        }
        if (!isTypographyProperty(d.name)) {
            continue;
        }
        const text = withoutStrings(d.value);
        const fixed = FIXED_TYPE_SIZE.test(text) || [...text.matchAll(AT_USE)].some((m) => typeSizes.has(m[1]) || FONT_CONSTANT.test(m[1]));
        if (fixed && !lines[d.line - 1]?.includes(FIXED_GEOMETRY_NOTE)) {
            out.push(finding(file, d.line, what, 'fixed text size — use a text role such as var(--font-size-body)'));
        }
    }
}

function reads(file, p, known, out, explain = () => null) {
    const internalHere = new Set(p.declarations.filter((d) => INTERNAL.test(d.name)).map((d) => d.name));
    for (const r of p.reads) {
        if (INTERNAL.test(r.name)) {
            if (!r.fallback && !internalHere.has(r.name)) {
                out.push(finding(file, r.line, `var(${r.name})`, 'internal token without a declaration in this file — declare it here or give the read a fallback'));
            }
        } else if (!known.has(r.name)) {
            out.push(finding(file, r.line, `var(${r.name})`, explain(r.name) ?? `no token of this name — check the spelling, or declare it in ${TOKENS_FILE}`));
        }
    }
}

function modeSymmetry(mixins, out) {
    for (const [mixin, declared] of mixins) {
        const mode = mixin.match(MODE_MIXIN);
        if (!mode) {
            continue;
        }
        const counterpart = `${mode[1]}-${mode[2] === 'light' ? 'dark' : 'light'}()`;
        const other = mixins.get(counterpart) || new Map();
        for (const [name, site] of declared) {
            if (!other.has(name)) {
                out.push(finding(site.file, site.line, name, `declared in ${mixin} but not in ${counterpart} — the other mode keeps the previous value`));
            }
        }
    }
}

function twins(parsed, out) {
    const value = (file, mixin) => new Map((parsed.get(file)?.declarations || []).filter((d) => d.stack.includes(mixin)).map((d) => [d.name, d.value.replace(/\s+/g, ' ').trim()]));
    const light = value(TOKENS_FILE, '.gv-semantic-light()'),
        dark = value(TOKENS_FILE, '.gv-semantic-dark()'),
        admin = value(ADMIN_TOKENS_FILE, '.gv-semantic-admin()');
    const names = [...light.keys()].filter((n) => dark.has(n));
    for (let i = 0; i < names.length; i++) {
        for (let j = i + 1; j < names.length; j++) {
            const a = names[i],
                b = names[j];
            if (light.get(a) !== light.get(b) || dark.get(a) !== dark.get(b)) {
                continue;
            }
            if (admin.has(a) && admin.has(b) && admin.get(a) !== admin.get(b)) {
                continue;
            }
            if (TWIN_PARTNERS.test(a) && TWIN_PARTNERS.test(b)) {
                continue;
            }
            if (TWIN_PARTNERS.test(a) || TWIN_PARTNERS.test(b)) {
                continue;
            }
            const site = (parsed.get(TOKENS_FILE).declarations.find((d) => d.name === b && d.stack.includes('.gv-semantic-light()')) || {}).line;
            out.push(finding(TOKENS_FILE, site, b, `same value as ${a} in every binding — read ${a}, or bind a different value`));
        }
    }
}

function mixinTokens(parsed, files) {
    const mixins = new Map();
    for (const file of files) {
        for (const d of parsed.get(file)?.declarations || []) {
            const mixin = d.stack.find((h) => MIXIN_DEFINITION.test(h))?.replace(/ when .+$/, '');
            if (!mixin) {
                continue;
            }
            if (!mixins.has(mixin)) {
                mixins.set(mixin, new Map());
            }
            mixins.get(mixin).set(d.name, { file, line: d.line });
        }
    }
    return mixins;
}

/**
 * Everything the theme rules read, derived once and frozen: the trees, the core's token layout, where a
 * line goes in the theme's token layer, what the theme declares and reads, dark mode, the token layer's
 * state, and what becomes of an old core name.
 *
 * @param {{core: Map<string,string>, theme: Map<string,string>, themeParsed: Map, templates: Map<string,string>|null, tokens: Set<string>, palette: Set<string>, roleInputs: Set<string>, parsed: Map, typeSizes: Map}} input
 * @returns {object} the frozen context
 */
function themeContext({ core, theme, themeParsed, templates, tokens, palette, roleInputs, parsed, typeSizes }) {
    const coreHas = (name) => tokens.has(name);
    const advise = adviceContext({ core: parsed, theme: themeParsed });
    const layout = new Map([...tokenLayout(core.get(PALETTE_FILE) ?? ''), ...tokenLayout(core.get(TOKENS_FILE) ?? '')]);
    const place = (name, { value = '', kind = null, dark = false } = {}) => {
        const own = kind === 'theme' || (kind !== 'core' && !tokens.has(name) && !palette.has(name));
        const known = layout.get(name);
        const under = own ? { section: 'THEME', label: null, order: null } : { section: 'CORE', label: known?.label ?? null, order: known?.order ?? null };
        if (name.startsWith('--color-')) {
            return { file: `theme/${PALETTE_FILE}`, into: '.mx-theme-palette()', ...under, section: `${under.section} PALETTE` };
        }
        const coloured = COLOR_LITERAL.test(withoutStrings(value)) || /var\(--(?:color|bg|fg|border|shadow)-/.test(value);
        const mixin = known ? known.mixin.replace(/^\.gv-/, '.mx-theme-') : !own || coloured ? '.mx-theme-semantic-light()' : '.mx-theme-base()';
        return { file: `theme/${TOKENS_FILE}`, into: dark ? mixin.replace('-light()', '-dark()') : mixin, ...under, section: own ? 'THEME TOKENS' : 'CORE OVERRIDES' };
    };
    const inDark = (d) => d.stack.some((h) => /dark/.test(h));
    const whereTo = (name, dark = false) => {
        const to = place(name, { kind: 'core', dark });
        return `${to.into} in ${to.file.replace(/^theme\//, '')}`;
    };
    const known = new Set(tokens);
    const themePalette = new Set(palette);
    const themeStatics = new Set();
    const themeRoleInputs = new Set(roleInputs);
    const declared = new Set();
    for (const [file, p] of themeParsed) {
        for (const d of p.declarations.filter((x) => !INTERNAL.test(x.name))) {
            known.add(d.name);
            declared.add(d.name);
            if (d.name.startsWith('--color-') && d.stack.some((h) => THEME_PALETTE_MIXIN.test(h))) {
                themePalette.add(d.name);
            }
        }
        if (file === TOKENS_FILE) {
            p.reads.forEach((r) => themeRoleInputs.add(r.name));
        }
        staticColorNames(p).forEach((name) => themeStatics.add(name));
    }
    const themeMixins = mixinTokens(
        themeParsed,
        [...themeParsed.keys()].filter((f) => THEME_LAYER.test(f))
    );
    const sources = [...theme.values()].map(stripComments).join('\n');
    const darkMode =
        [...themeMixins.keys()].some((m) => m.match(MODE_MIXIN)?.[2] === 'dark') ||
        (!/@colorModes\s*:\s*false\b/.test(sources) && /\.mx-dark-mode-rules|@colorModes\s*:\s*true/.test(sources));

    const missingLayer = [PALETTE_FILE, TOKENS_FILE].filter((f) => !theme.has(f));
    const brandValue = advise.brandHint
        ? `--color-brand: ${advise.brandHint.value}${advise.brandHint.source ? ` (was ${advise.brandHint.source})` : ''}`
        : '--color-brand: the theme’s brand colour';
    const constructorLines = (theme.get('constructor.less') ?? '').split('\n');
    const importLine = (target) => constructorLines.findIndex((l) => new RegExp(`@import\\s+(?:\\([^)]*\\)\\s*)?['"]${target.replace(/\./g, '\\.')}['"]`).test(l)) + 1;
    const importAfterVariables = (files) =>
        importLine(VARIABLES_FILE) ? [{ file: 'theme/constructor.less', line: importLine(VARIABLES_FILE), insert: files.map((f) => `@import '${f}';`) }] : [];

    const readAdvice = (name) => {
        if (!RENAMED_TOKENS.has(name)) {
            return null;
        }
        const advice = successorAdvice(name, coreHas);
        return advice.to
            ? `reads a token the core renamed — read ${advice.to.replace(/^(--[\w-]+)/, 'var($1)')}`
            : 'reads a token the core removed — drop the read, or declare a theme token in misc/tokens.less';
    };
    const tokenLines = (file) => THEME_LAYER.test(file) || file === CORE_DEFAULTS_FILE;
    const coreReads = new Set([...parsed.values()].flatMap((p) => p.reads.map((r) => r.name)));
    const oldName = (file, d) => tokenLines(file) && !INTERNAL.test(d.name) && !tokens.has(d.name) && RENAMED_TOKENS.has(d.name);
    let themeReads = new Set();
    const dead = (file, d) => tokenLines(file) && !INTERNAL.test(d.name) && !tokens.has(d.name) && !coreReads.has(d.name) && !themeReads.has(d.name);
    const successorName = (advice) => {
        const name = advice.to?.match(/--[\w-]+/)?.[0];
        return name && !INTERNAL.test(name) ? name : null;
    };
    const actionOf = (d) => {
        const advice = successorAdvice(d.name, coreHas);
        return advice.action === 'rename' && !successorName(advice) ? 'drop' : advice.action;
    };
    const goes = new Set();
    const live = (file, r) => !goes.has(`${file}:${r.line}:${r.owner}`);
    let gone;
    do {
        gone = goes.size;
        themeReads = new Set([...themeParsed].flatMap(([file, p]) => p.reads.filter((r) => live(file, r)).map((r) => r.name)));
        for (const [file, p] of themeParsed) {
            for (const d of p.declarations) {
                const action = oldName(file, d) ? actionOf(d) : null;
                if (action === 'drop' || (action === 'delete' && !themeReads.has(d.name)) || (action === null && dead(file, d))) {
                    goes.add(`${file}:${d.line}:${d.name}`);
                }
            }
        }
    } while (goes.size !== gone);
    const mergedInto = new Map();
    for (const [file, p] of themeParsed) {
        for (const d of p.declarations.filter((x) => oldName(file, x))) {
            const advice = successorAdvice(d.name, coreHas);
            const to = advice.action === 'rename' ? successorName(advice) : null;
            if (to) {
                mergedInto.set(to, new Set([...(mergedInto.get(to) ?? []), d.name]));
            }
        }
    }
    const sameValue = (a, b) => {
        const norm = (v) => v.replace(/\s+/g, ' ').trim().toLowerCase();
        const [x, y] = [a, b].map((v) => advise.colourOf(v));
        return norm(a) === norm(b) || (Boolean(x && y) && JSON.stringify(x) === JSON.stringify(y));
    };
    const readEdits = (name, to) =>
        uniqueLines(
            [...themeParsed].flatMap(([file, p]) =>
                p.reads.filter((r) => r.name === name && live(file, r)).map((r) => ({ file: `theme/${file}`, line: r.line, replace: [[`var(${name}`, `var(${to}`]] }))
            )
        );
    const moveTo = (d, name = d.name, value = d.value.replace(/\s+/g, ' '), kind = null) => [
        { remove: true },
        { ...place(name, { value, kind, dark: inDark(d) }), add: [`${name}: ${value};`] },
    ];
    const moving = (file) => !layerHome(file);

    return Object.freeze({
        core,
        theme,
        themeParsed,
        templates,
        tokens,
        palette,
        typeSizes,
        coreHas,
        advise,
        layout,
        place,
        inDark,
        whereTo,
        known,
        themePalette,
        themeStatics,
        themeRoleInputs,
        declared,
        themeMixins,
        darkMode,
        missingLayer,
        brandValue,
        importLine,
        importAfterVariables,
        readAdvice,
        coreReads,
        oldName,
        dead,
        successorName,
        live,
        themeReads,
        mergedInto,
        sameValue,
        readEdits,
        moveTo,
        moving,
    });
}

const step = (kind, f, group = f.fix.split(' — ')[0]) => ({ ...f, kind, group });

/**
 * The token layer: both files present and imported, and the brand colour set.
 *
 * @param {object} ctx theme context
 * @returns {Array} findings
 */
function tokenLayerSteps(ctx) {
    const { theme, advise, darkMode, layout, missingLayer, brandValue, importAfterVariables, declared, palette, place } = ctx;
    const out = [];
    if (missingLayer.length) {
        const wiring = darkMode
            ? [
                  '/* WIRING */',
                  ':root {',
                  '    .mx-theme-palette();',
                  '    .mx-theme-base();',
                  '    .mx-theme-semantic-light();',
                  '}',
                  '@media (prefers-color-scheme: dark) {',
                  '    :root {',
                  '        .mx-theme-semantic-dark();',
                  '    }',
                  '}',
                  '.color-theme-dark {',
                  '    .mx-theme-semantic-dark();',
                  '}',
                  '.color-theme-light {',
                  '    .mx-theme-semantic-light();',
                  '}',
              ]
            : ['/* WIRING: LIGHT ONLY */', ':root {', '    .mx-theme-palette();', '    .mx-theme-base();', '    .mx-theme-semantic-light();', '}'];
        const brandLabel = layout.get('--color-brand')?.label;
        const content = {
            [PALETTE_FILE]: [
                '.mx-theme-palette() {',
                '    /* ---------- CORE PALETTE ---------- */',
                '',
                ...(brandLabel ? [`    /* ${brandLabel} */`] : []),
                `    --color-brand: ${advise.brandHint?.value ?? '<brand colour>'};`,
                '}',
            ],
            [TOKENS_FILE]: ['.mx-theme-base() {', '}', '', '.mx-theme-semantic-light() {', '}', ...(darkMode ? ['', '.mx-theme-semantic-dark() {', '}'] : []), '', ...wiring],
        };
        const changes = [...missingLayer.map((f) => ({ file: `theme/${f}`, tag: 'new file', add: content[f] })), ...importAfterVariables(missingLayer)];
        const both = missingLayer.length > 1;
        out.push(
            step('todo', {
                ...finding(
                    'theme/misc',
                    0,
                    missingLayer.join(', '),
                    `token layer missing — create ${both ? 'both files' : 'the file'} and import ${both ? 'them' : 'it'} right after misc/variables.less; ${brandValue}`
                ),
                changes,
            })
        );
    } else if (theme.has('constructor.less')) {
        const constructor = stripComments(theme.get('constructor.less'));
        const unimported = [PALETTE_FILE, TOKENS_FILE].filter((f) => !new RegExp(`@import\\s+(?:\\([^)]*\\)\\s*)?['"]${f.replace(/\./g, '\\.')}['"]`).test(constructor));
        if (unimported.length) {
            out.push(
                step('todo', {
                    ...finding('theme/constructor.less', 0, unimported.join(', '), 'token layer not imported — import right after misc/variables.less'),
                    changes: importAfterVariables(unimported),
                })
            );
        }
    }
    if (!missingLayer.includes(PALETTE_FILE) && palette.has('--color-brand') && !declared.has('--color-brand')) {
        const changes = advise.brandHint ? [{ ...place('--color-brand', { kind: 'core' }), add: [`--color-brand: ${advise.brandHint.value};`] }] : [];
        out.push(
            step('todo', {
                ...finding(
                    `theme/${PALETTE_FILE}`,
                    0,
                    '--color-brand',
                    `brand colour not set — set ${brandValue} in .mx-theme-palette(), otherwise core components show the default brand colour`
                ),
                changes,
            })
        );
    }
    return out;
}

/**
 * misc/core-defaults.less goes: the core no longer reads it.
 *
 * @param {object} ctx theme context
 * @returns {Array} findings
 */
function coreDefaultsStep(ctx) {
    const { theme, themeParsed, importLine } = ctx;
    if (!theme.has(CORE_DEFAULTS_FILE)) {
        return [];
    }
    const values = (themeParsed.get(CORE_DEFAULTS_FILE)?.declarations ?? []).length;
    const fix = values
        ? 'the core no longer reads it: move its lines into the token layer (the old names as the step before shows, the rest as shown here), then delete the file and its import'
        : 'it holds no values: delete the file and its import';
    const at = importLine(CORE_DEFAULTS_FILE);
    const changes = [{ file: `theme/${CORE_DEFAULTS_FILE}`, tag: 'delete the file' }, ...(at ? [{ file: 'theme/constructor.less', line: at, remove: true }] : [])];
    return [step('todo', { ...finding(`theme/${CORE_DEFAULTS_FILE}`, 0, CORE_DEFAULTS_FILE, `old core overrides — ${fix}`), changes })];
}

/**
 * Without dark mode the core still switches its own roles to dark with a dark operating system, unless the page says light.
 *
 * @param {object} ctx theme context
 * @returns {Array} findings
 */
function lightOnlyTemplates(ctx) {
    const { darkMode, templates } = ctx;
    const out = [];
    if (darkMode || !templates) {
        return out;
    }
    for (const [name, text] of templates) {
        const html = text.match(/<html\b[^>]*>/);
        if (/^template(?:Admin|BlankUnstyled)\.html$/.test(name) || !html || html[0].includes('color-theme-light')) {
            continue;
        }
        const classAt = html[0].search(/\bclass\s*=/);
        const index = html.index + (classAt >= 0 ? classAt : html[0].length - 1);
        const line = text.slice(0, index).split('\n').length;
        const before = text.split('\n')[line - 1].trim();
        const after = classAt >= 0 ? before.replace(/class\s*=\s*"[^"]*"/, 'class="color-theme-light"') : before.replace(/\s*(\/?>)\s*$/, ' class="color-theme-light"$1');
        out.push(
            step('todo', {
                ...finding(name, line, '<html>', 'theme without dark mode — set class="color-theme-light", otherwise core components turn dark with a dark operating system'),
                changes: [{ before, after }],
            })
        );
    }
    return out;
}

/**
 * Reads: of tokens the core renamed or removed, and of palette colours the roles own.
 *
 * @param {object} ctx theme context
 * @returns {Array} findings
 */
function readSteps(ctx) {
    const { themeParsed, known, readAdvice, live, themePalette, themeRoleInputs } = ctx;
    const out = [];
    for (const [file, p] of themeParsed) {
        const inLayer = THEME_LAYER.test(file);
        const at = `theme/${file}`;
        const broken = [];
        reads(at, p, known, broken, readAdvice);
        for (const f of broken.filter((x) => p.reads.some((r) => r.line === x.line && `var(${r.name})` === x.what && live(file, r)))) {
            const now = f.fix.match(/read var\((--[\w-]+)\)/)?.[1];
            out.push(step('todo', now ? { ...f, changes: [{ replace: [[f.what.replace(/\)$/, ''), `var(${now}`]] }] } : f));
        }
        for (const r of p.reads.filter((x) => themePalette.has(x.name) && !inLayer && themeRoleInputs.has(x.name))) {
            out.push(step('full', finding(at, r.line, `var(${r.name})`, 'palette colour owned by the roles — read a role instead')));
        }
    }
    return out;
}

/**
 * What becomes of a line with an old core name: moved under its new name, deleted, or left where the theme reads it.
 * A successor the token layer already declares, or that an earlier line of this step moves in, takes no second line.
 *
 * @param {object} ctx theme context
 * @returns {(file: string, d: object) => {fix: string, changes: Array, note?: string|null}|null} the advice per line
 */
function oldLineAdvice(ctx) {
    const { themeParsed, advise, missingLayer, coreHas, successorName, themeReads, readEdits, mergedInto, moving, sameValue, whereTo, inDark, moveTo, place } = ctx;
    const layerDeclares = new Map();
    for (const file of [PALETTE_FILE, TOKENS_FILE]) {
        for (const d of themeParsed.get(file)?.declarations ?? []) {
            layerDeclares.set(d.name, {
                value: d.value,
                same: `already carries this value in ${file}`,
                differs: `is already declared in ${file} with ${d.value}, keep the value that fits`,
            });
        }
    }
    if (missingLayer.includes(PALETTE_FILE) && advise.brandHint) {
        const { value } = advise.brandHint;
        layerDeclares.set('--color-brand', {
            value,
            same: `already carries this value, from step 1 in ${PALETTE_FILE}`,
            differs: `gets ${value} in step 1, keep the value that fits`,
        });
    }
    return (file, d) => {
        const advice = successorAdvice(d.name, coreHas);
        const to = successorName(advice);
        const read = Boolean(to) && themeReads.has(d.name);
        const reads = read ? `, and read var(${to}) where the theme reads var(${d.name})` : '';
        const edits = read ? readEdits(d.name, to) : [];
        const sharing = [...(mergedInto.get(to) ?? [])];
        const move = moving(file);
        const already = move && to && advice.action === 'rename' ? layerDeclares.get(to) : null;
        if (already) {
            const kept = `${to} ${sameValue(already.value, d.value) ? already.same : already.differs}`;
            return { fix: `old core token name — delete the line, ${kept}${reads}`, changes: [{ remove: true }, ...edits], note: kept };
        }
        if (move && to && advice.action === 'rename') {
            const from = `${d.name} (${file}:${d.line})`;
            layerDeclares.set(to, {
                value: d.value,
                same: `already carries this value: ${from} moves there in this step`,
                differs: `gets ${d.value} from ${from} in this step, keep the value that fits`,
            });
        }
        if (advice.action === 'rename' && to && sharing.length > 1) {
            return {
                fix: `old core token name — ${sharing.join(', ')} all become ${advice.to}: keep the one value that fits${move ? `, in ${whereTo(to, inDark(d))}` : ''}, delete the other lines${reads}`,
                changes: [...(move ? moveTo(d, to, undefined, 'core') : [{ replace: [[d.name, to]] }]), ...edits],
                note: `${sharing.join(', ')} all become ${to}: keep one of the values`,
            };
        }
        if (advice.action === 'rename' && to) {
            const triplet = d.name.endsWith('-rgb');
            const note = triplet && read ? `a colour now: rgba(var(${d.name}), .5) becomes color-mix(in srgb, var(${to}) 50%, transparent)` : null;
            return {
                fix: `old core token name — ${move ? `move it to ${whereTo(to, inDark(d))} as ${advice.to}` : `rename it to ${advice.to}`}${triplet ? ', keep the value but write the triplet as rgb(…)' : ', keep the value'}${reads}`,
                changes: [
                    ...(move ? moveTo(d, to, triplet ? `rgb(${d.value})` : undefined, 'core') : [triplet ? { after: `${to}: rgb(${d.value});` } : { replace: [[d.name, to]] }]),
                    ...edits,
                ],
                note,
            };
        }
        if (advice.action === 'drop' || advice.action === 'rename') {
            return {
                fix: `old core token name — delete the line, the component reads ${advice.to} now${reads}`,
                changes: [{ remove: true }, ...edits],
                note: `the component reads ${advice.to} now`,
            };
        }
        if (!themeReads.has(d.name)) {
            return { fix: 'old core token name — delete the line, removed in the core', changes: [{ remove: true }], note: 'removed in the core' };
        }
        if (file !== CORE_DEFAULTS_FILE) {
            return null;
        }
        return {
            fix: `old core token name — removed in the core, but the theme reads it itself: move the line into ${place(d.name, { value: d.value, dark: inDark(d) }).into} in ${TOKENS_FILE}`,
            changes: moveTo(d),
        };
    };
}

/**
 * Declarations: old core names, lines nothing reads, old overrides, tokens whose meaning changed, tokens declared
 * in a component, and names and values that do not belong in the token layer.
 *
 * @param {object} ctx theme context
 * @returns {Array} findings
 */
function declarationSteps(ctx) {
    const { themeParsed, tokens, palette, coreHas, coreReads, oldName, dead, inDark, place, moveTo, moving, whereTo, readEdits } = ctx;
    const oldLine = oldLineAdvice(ctx);
    const out = [];
    for (const [file, p] of themeParsed) {
        const inLayer = THEME_LAYER.test(file);
        const at = `theme/${file}`;
        for (const d of p.declarations.filter((x) => !INTERNAL.test(x.name))) {
            if (oldName(file, d)) {
                const old = oldLine(file, d);
                if (old) {
                    out.push(step('todo', { ...finding(at, d.line, d.name, old.fix), changes: old.changes, note: old.note ?? null }));
                }
                continue;
            }
            if (dead(file, d)) {
                out.push(
                    step('todo', {
                        ...finding(at, d.line, d.name, 'nothing reads this token — delete the line, neither the core nor the theme reads it'),
                        changes: [{ remove: true }],
                    })
                );
                continue;
            }
            if (file === CORE_DEFAULTS_FILE) {
                const to = place(d.name, { value: d.value, dark: inDark(d) });
                out.push(
                    step('todo', { ...finding(at, d.line, d.name, `old core overrides — move the line into ${to.into} in ${to.file.replace(/^theme\//, '')}`), changes: moveTo(d) })
                );
                continue;
            }
            const advice = successorAdvice(d.name, coreHas);
            if (advice.reused && file !== TOKENS_FILE) {
                const move = moving(file);
                out.push(
                    step('todo', {
                        ...finding(
                            at,
                            d.line,
                            d.name,
                            `token whose meaning changed — ${move ? `move it to ${whereTo(advice.to, inDark(d))} as ${advice.to}` : `rename it to ${advice.to}`}, keep the value`
                        ),
                        changes: [...(move ? moveTo(d, advice.to, undefined, 'core') : [{ replace: [[d.name, advice.to]] }]), ...readEdits(d.name, advice.to)],
                    })
                );
                continue;
            }
            if (!inLayer) {
                const elsewhere = [...themeParsed].some(([other, q]) => other !== file && q.reads.some((r) => r.name === d.name)) || coreReads.has(d.name);
                const here = p.reads.filter((r) => r.name === d.name);
                const local = `--_${d.name.slice(2)}`;
                if (elsewhere) {
                    const value = d.value.replace(/\s+/g, ' ');
                    const to = place(d.name, { value, kind: 'theme' });
                    const changes = [{ remove: true }, { ...to, add: [`${d.name}: ${value};`] }];
                    out.push(
                        step('full', {
                            ...finding(at, d.line, d.name, `token declared in a component — move the line into ${to.into} in ${to.file.replace(/^theme\//, '')}`),
                            changes,
                        })
                    );
                } else if (here.length) {
                    const changes = [
                        { replace: [[d.name, local]] },
                        ...uniqueLines(here.filter((r) => r.line !== d.line).map((r) => ({ file: at, line: r.line, replace: [[`var(${d.name}`, `var(${local}`]] }))),
                    ];
                    out.push(step('full', { ...finding(at, d.line, d.name, `token declared in a component — rename it to ${local} here and where this file reads it`), changes }));
                } else {
                    out.push(step('full', { ...finding(at, d.line, d.name, 'token declared in a component — nothing reads it: delete the line'), changes: [{ remove: true }] }));
                }
            }
            if (inLayer && ROLE_PREFIX.test(d.name) && !tokens.has(d.name)) {
                out.push(step('full', finding(at, d.line, d.name, 'role name the core does not declare — a theme token is named after its component, the property last')));
            }
            if (inLayer && d.name.startsWith('--color-') && !palette.has(d.name) && !d.name.startsWith('--color-theme-')) {
                out.push(step('full', finding(at, d.line, d.name, 'palette colour the core does not declare — own colours are --color-theme-*')));
            }
            if (!d.stack.some((h) => THEME_PALETTE_MIXIN.test(h)) && LIGHT_DARK.test(d.value)) {
                out.push(step('full', finding(at, d.line, `${d.name}: ${d.value}`, 'light-dark() outside the palette — read a token that differs per mode')));
            }
            const paletteSpot = d.stack.some((h) => THEME_PALETTE_MIXIN.test(h)) || (SUBTHEME_CONSTRUCTOR.test(file) && /^--color-brand(-400)?$/.test(d.name));
            if (!paletteSpot && COLOR_LITERAL.test(withoutStrings(d.value))) {
                out.push(
                    step(
                        'full',
                        finding(at, d.line, `${d.name}: ${d.value}`, 'colour literal outside the palette — declare it as --color-* in .mx-theme-palette() and read it here')
                    )
                );
            }
        }
    }
    return out;
}

/**
 * Light/dark branches in components. With dark mode an old branch must become a token (the mode mixins it used
 * are gone); without, a dark branch never applies and a light one always does. The outermost branch is listed,
 * what it wraps goes with it.
 *
 * @param {object} ctx theme context
 * @returns {{findings: Array, gone: Map<string, Set<number>>, covered: Set<string>}} findings, the lines each file loses, and those lines as file:line
 */
function branchSteps(ctx) {
    const { theme, themeParsed, darkMode } = ctx;
    const findings = [];
    const gone = new Map();
    const covered = new Set();
    for (const [file, p] of themeParsed) {
        if (THEME_LAYER.test(file) || file === CORE_DEFAULTS_FILE) {
            continue;
        }
        const at = `theme/${file}`;
        const code = stripComments(theme.get(file)).split('\n');
        const lost = new Set();
        const leaves = (b) => {
            const [first, last] = spanOf(code, b.line, true);
            for (let i = first; i <= last; i++) {
                lost.add(i + 1);
            }
        };
        for (const b of p.blocks.filter((x) => MODE_BRANCH.test(x.header) && !x.stack.some((h) => MODE_BRANCH.test(h)))) {
            const where = flattened([...b.stack, b.header]);
            if (darkMode) {
                leaves(b);
                findings.push(
                    step('todo', {
                        ...finding(
                            at,
                            b.line,
                            where,
                            `light/dark branch in a component — the core switches modes through tokens now: bind what differs as a theme token in .mx-theme-semantic-light() and .mx-theme-semantic-dark() in ${TOKENS_FILE}, read the token here, then delete the branch`
                        ),
                        changes: [{ block: 'show' }],
                    })
                );
                continue;
            }
            const mode = /@colorModes/.test(b.header) ? 'dark' : b.header.match(/dark|light/)?.[0];
            const fix = { dark: 'delete the branch, it never applies', light: 'keep the rules, drop the wrapper' }[mode] ?? 'keep what applies in light mode';
            const block = { dark: 'remove', light: 'unwrap' }[mode] ?? 'show';
            if (block === 'remove') {
                leaves(b);
            }
            const kind = /@colorModes|^\.mx-/.test(b.header) ? 'todo' : 'full';
            findings.push(step(kind, { ...finding(at, b.line, where, `light/dark branch in a component — the theme has no dark mode: ${fix}`), changes: [{ block }] }));
        }
        gone.set(file, lost);
        lost.forEach((line) => covered.add(`${file}:${line}`));
    }
    return { findings, gone, covered };
}

/**
 * Fixed values in components: a role for every colour and text size, with what the replaced lines read.
 *
 * @param {object} ctx theme context
 * @param {Map<string, Set<number>>} gone the lines each file loses to a removed branch
 * @returns {{findings: Array, covered: Set<string>, replacements: Array, constantsRead: Set<string>}} findings, replaced lines as file:line, the colour replacements, and the constants those lines read
 */
function valueSteps(ctx, gone) {
    const { theme, themeParsed, themeStatics, typeSizes, advise } = ctx;
    const findings = [];
    const covered = new Set();
    const replacements = [];
    const constantsRead = new Set();
    for (const [file, p] of themeParsed) {
        if (THEME_LAYER.test(file) || file === CORE_DEFAULTS_FILE) {
            continue;
        }
        const at = `theme/${file}`;
        const lost = gone.get(file) ?? new Set();
        const values = [];
        componentValues(at, p, theme.get(file), themeStatics, typeSizes, values);
        for (const f of values.filter((v) => !lost.has(v.line))) {
            if (/^fixed text size/.test(f.fix)) {
                const size = adviseFontSize(f.what, advise);
                if (size.from) {
                    covered.add(`${file}:${f.line}`);
                }
                findings.push(
                    step('full', { ...f, advice: size.text, changes: size.from ? [{ replace: [[size.from, size.to]] }] : [{}], note: size.note ?? null }, 'fixed text sizes')
                );
                continue;
            }
            const colour = adviseColour(f.what, advise, file);
            if (colour.replacements.length) {
                covered.add(`${file}:${f.line}`);
            }
            replacements.push(...colour.replacements);
            colour.replacements.forEach((r) => r.constants.forEach((name) => constantsRead.add(name)));
            findings.push(step('full', { ...f, advice: colour.text, changes: [{ replace: colour.replacements.map((r) => [r.from, `var(${r.role})`]) }] }, 'colours in components'));
        }
        for (const name of values.filter((f) => /^fixed text size/.test(f.fix)).flatMap((f) => [...f.what.matchAll(AT_USE)].map((m) => m[1]))) {
            constantsRead.add(name);
        }
    }
    return { findings, covered, replacements, constantsRead };
}

/**
 * So the look stays, the roles the components read now carry the theme's own colours.
 *
 * @param {object} ctx theme context
 * @param {Array} replacements the colour replacements of the components
 * @returns {Array} findings
 */
function bindingSteps(ctx, replacements) {
    const { advise, place } = ctx;
    const out = [];
    const bound = bindings(replacements, advise);
    for (const entry of bound.palette.filter((e) => e.name !== '--color-brand' || e.value !== advise.brandHint?.value)) {
        const note = entry.source ? `was ${entry.source}` : null;
        const to = place(entry.name);
        out.push(
            step('full', {
                ...finding(to.file, 0, `${entry.name}: ${entry.value};`, `carry the theme's colours into the token layer — in ${to.into}${note ? `, ${note}` : ''}`),
                changes: [{ ...to, add: [`${entry.name}: ${entry.value};`] }],
                note,
            })
        );
    }
    for (const entry of bound.roles) {
        const others = entry.others.map((o) => `${o.value}${o.source ? ` (${o.source})` : ''} ${o.uses}×`).join(', ');
        const uses = `${entry.theme ? 'theme token, ' : ''}read ${entry.uses}×`;
        const note = others ? `${uses} · also ${others}` : uses;
        const to = place(entry.role, { value: `var(${entry.palette})`, kind: entry.theme ? 'theme' : 'core' });
        out.push(
            step('full', {
                ...finding(to.file, 0, `${entry.role}: var(${entry.palette});`, `carry the theme's colours into the token layer — in ${to.into}, ${note}`),
                changes: [{ ...to, add: [`${entry.role}: var(${entry.palette});`] }],
                note,
            })
        );
    }
    return out;
}

/**
 * A constant goes once the steps above leave it no reader: every line reading it is replaced or goes; a reader that stays keeps it.
 *
 * @param {object} ctx theme context
 * @param {Set<string>} covered the lines the steps above replace or delete, as file:line
 * @param {Set<string>} constantsRead the constants the replaced lines read
 * @returns {Array} findings
 */
function constantSteps(ctx, covered, constantsRead) {
    const { theme, themeParsed, themeStatics } = ctx;
    const declaredAt = new Map([...themeParsed].filter(([file]) => !SUBTHEME_CONSTRUCTOR.test(file)).flatMap(([file, p]) => p.statics.map((s) => [s.name, `${file}:${s.line}`])));
    const readsOf = new Map();
    for (const file of [...themeParsed.keys()].filter((f) => !SUBTHEME_CONSTRUCTOR.test(f))) {
        stripComments(theme.get(file))
            .split('\n')
            .forEach((text, i) => {
                for (const m of text.matchAll(/@\{?([\w-]+)/g)) {
                    const at = `${file}:${i + 1}`;
                    if (at !== declaredAt.get(m[1])) {
                        readsOf.set(m[1], [...(readsOf.get(m[1]) ?? []), at]);
                    }
                }
            });
    }
    let deletable = new Set([...constantsRead, ...[...themeStatics].filter((name) => readsOf.has(name))].filter((name) => declaredAt.has(name)));
    let before;
    do {
        before = deletable.size;
        const stays = (at) => !covered.has(at) && ![...deletable].some((other) => declaredAt.get(other) === at);
        deletable = new Set([...deletable].filter((name) => !(readsOf.get(name) ?? []).some(stays)));
    } while (deletable.size !== before);
    const out = [];
    for (const [file, p] of themeParsed) {
        for (const s of p.statics.filter((x) => deletable.has(x.name))) {
            out.push(
                step('full', {
                    ...finding(`theme/${file}`, s.line, `@${s.name}`, 'delete the colour and size constants — nothing reads them once the steps above are done'),
                    changes: [{ remove: true }],
                })
            );
        }
    }
    return out;
}

/**
 * Dark mode closes a migration that is under way; a theme done with it stays clean.
 *
 * @param {object} ctx theme context
 * @param {Array} found the findings so far
 * @returns {Array} findings
 */
function darkModeStep(ctx, found) {
    if (ctx.darkMode || !found.some((f) => f.kind === 'full')) {
        return [];
    }
    return [
        step(
            'full',
            finding(
                `theme/${TOKENS_FILE}`,
                0,
                'dark mode',
                `dark mode, only if wanted — add .mx-theme-semantic-dark() with the same tokens as light, wire it like the reference theme (prefers-color-scheme and .color-theme-dark), then put class="#{sessionBean.get('colorTheme')}" back on <html> in the templates`
            )
        ),
    ];
}

/**
 * Light and dark declare the same tokens. A theme without dark mode wires light only and has no dark mixin at all: a choice, not a gap.
 *
 * @param {object} ctx theme context
 * @returns {Array} findings
 */
function symmetrySteps(ctx) {
    const { themeMixins } = ctx;
    const paired = [...themeMixins].filter(([m]) => {
        const mode = m.match(MODE_MIXIN);
        return !mode || themeMixins.has(`${mode[1]}-${mode[2] === 'light' ? 'dark' : 'light'}()`);
    });
    const asymmetric = [];
    modeSymmetry(new Map(paired.map(([m, d]) => [m, new Map([...d].map(([n, s]) => [n, { ...s, file: `theme/${s.file}` }]))])), asymmetric);
    return asymmetric.map((f) => step('todo', f, 'light and dark declare different tokens'));
}

/**
 * Every finding carries its code: the lines as they stand, and as they read after the change.
 *
 * @param {object} ctx theme context
 * @param {Array} found the findings
 * @returns {Array} the same findings, with code and changed
 */
function withCode(ctx, found) {
    const { theme, templates } = ctx;
    const sourceCache = new Map();
    const sourceOf = (file) => {
        if (!sourceCache.has(file)) {
            const text = file.startsWith('theme/') ? theme.get(file.slice('theme/'.length)) : templates?.get(file);
            sourceCache.set(file, text === undefined ? null : { raw: text.split('\n'), code: (file.endsWith('.less') ? stripComments(text) : text).split('\n') });
        }
        return sourceCache.get(file);
    };
    for (const f of found) {
        f.code = (f.changes ?? [{}]).map((c) => {
            const change = { file: f.file, line: c.add || c.tag ? 0 : f.line, ...c };
            return codeBlock(change, sourceOf(change.file));
        });
        f.changed = f.code.some((b) => b.rows.some((r) => r.sign === '-' || r.sign === '+'));
    }
    return found;
}

/**
 * The theme findings, rule by rule over one frozen context: kind "todo" is what the new core requires (token layer,
 * renamed and removed tokens, light-only wiring), kind "full" is what a complete token migration asks for
 * (dark mode, roles instead of fixed values). `group` is the heading a finding is listed under, `changes`
 * what to do in the code and `code` the lines that changes touches, as they stand and as they read afterwards.
 *
 * @param {{core: Map<string,string>, theme: Map<string,string>, themeParsed: Map, templates: Map<string,string>|null, tokens: Set<string>, palette: Set<string>, roleInputs: Set<string>, parsed: Map, typeSizes: Map}} input
 * @param {Array} out theme findings
 */
function checkTheme(input, out) {
    const ctx = themeContext(input);
    const found = [...tokenLayerSteps(ctx), ...coreDefaultsStep(ctx), ...lightOnlyTemplates(ctx), ...readSteps(ctx), ...declarationSteps(ctx)];
    const branches = branchSteps(ctx);
    const values = valueSteps(ctx, branches.gone);
    found.push(...branches.findings, ...values.findings, ...bindingSteps(ctx, values.replacements));
    found.push(...constantSteps(ctx, new Set([...branches.covered, ...values.covered]), values.constantsRead));
    found.push(...darkModeStep(ctx, found), ...symmetrySteps(ctx));
    out.push(...withCode(ctx, found));
}

/**
 * Runs the six sentences over the core tree and, when given, the theme tree.
 *
 * @param {Map<string,string>} core relative path → LESS source
 * @param {Map<string,string>|null} theme relative path → LESS source, or null
 * @param {Map<string,string>|null} templates the theme's template*.html by file name, for the light-only check
 * @returns {{core: Array, theme: Array}} findings; theme files are reported as theme/<path>
 */
function checkTree(core, theme = null, templates = null) {
    const parsed = parseAll(core);
    const out = { core: [], theme: [] };

    const tokens = new Set();
    const palette = new Set();
    for (const file of TOKEN_LAYER) {
        for (const d of parsed.get(file)?.declarations || []) {
            if (!INTERNAL.test(d.name)) {
                tokens.add(d.name);
            }
        }
    }
    for (const file of PALETTE_FILES) {
        for (const d of parsed.get(file)?.declarations || []) {
            palette.add(d.name);
        }
    }
    const staticColours = staticColorNames(parsed.get(VARIABLES_FILE));
    const roleInputs = new Set();
    for (const file of [TOKENS_FILE, ADMIN_TOKENS_FILE]) {
        for (const r of parsed.get(file)?.reads || []) {
            roleInputs.add(r.name);
        }
    }
    const themeParsed = theme ? parseAll(theme) : new Map();
    const typeSizes = staticTypeSizes([parsed, themeParsed]);

    for (const [file, p] of parsed) {
        const layer = TOKEN_LAYER.includes(file);
        if (layer) {
            for (const d of p.declarations) {
                if (INTERNAL.test(d.name)) {
                    out.core.push(finding(file, d.line, d.name, 'internal --_ token in the token layer — public tokens carry no underscore'));
                } else if (!underRoot(d.stack) && !inMixin(d.stack)) {
                    out.core.push(finding(file, d.line, d.name, `declared under "${flattened(d.stack) || '(top level)'}" — only ${ROOT_SELECTORS.join(', ')} or a mixin`));
                }
                if (!PALETTE_FILES.includes(file) && COLOR_LITERAL.test(withoutStrings(d.value))) {
                    out.core.push(
                        finding(
                            file,
                            d.line,
                            `${d.name}: ${d.value}`,
                            `colour literal in the token layer — every colour comes from ${file === ADMIN_TOKENS_FILE ? ADMIN_PALETTE_FILE : PALETTE_FILE}`
                        )
                    );
                }
                if (!PALETTE_FILES.includes(file) && LIGHT_DARK.test(d.value)) {
                    out.core.push(
                        finding(
                            file,
                            d.line,
                            `${d.name}: ${d.value}`,
                            'light-dark() outside the palette — a component colour carries its mode in the palette, a role binds per mode'
                        )
                    );
                }
                if (d.stack.some((h) => SEMANTIC_MIXIN.test(h)) && !ROLE_PREFIX.test(d.name)) {
                    out.core.push(
                        finding(file, d.line, d.name, 'role name without a property prefix — roles start with bg-, fg-, border-, outline-, shadow-, display- or filter-')
                    );
                }
                if (PALETTE_FILES.includes(file) && PALETTE_WORD.test(d.name) && !PALETTE_WORD_EXEMPT.test(d.name)) {
                    out.core.push(finding(file, d.line, d.name, 'property or state word in a palette name — the palette carries tone and step only'));
                }
            }
        } else if (file !== VARIABLES_FILE) {
            for (const d of p.declarations) {
                if (!INTERNAL.test(d.name)) {
                    out.core.push(
                        finding(
                            file,
                            d.line,
                            d.name,
                            `public token declared outside the token layer — declare it in ${TOKENS_FILE}, or use --_${d.name.slice(2)} for a local value`
                        )
                    );
                }
            }
            for (const b of p.blocks) {
                if (MODE_BRANCH.test(b.header)) {
                    out.core.push(finding(file, b.line, flattened([...b.stack, b.header]), 'light/dark branch outside the token layer — read a token that differs per mode'));
                }
            }
            for (const d of [...p.declarations, ...p.properties]) {
                if (LIGHT_DARK.test(d.value)) {
                    out.core.push(finding(file, d.line, `${d.name}: ${d.value}`, 'light-dark() in a component — read a token that differs per mode'));
                }
            }
            if (!STATIC_COLOUR_TREES.test(file)) {
                componentValues(file, p, core.get(file), staticColours, typeSizes, out.core);
            }
        }
        reads(file, p, tokens, out.core);
        for (const r of p.reads) {
            if (!palette.has(r.name) || layer || !roleInputs.has(r.name)) {
                continue;
            }
            out.core.push(finding(file, r.line, `var(${r.name})`, 'palette colour owned by the roles — read a role instead'));
        }
    }
    twins(parsed, out.core);
    modeSymmetry(mixinTokens(parsed, TOKEN_LAYER), out.core);

    const bound = new Map();
    for (const file of ADMIN_FILES) {
        for (const d of parsed.get(file)?.declarations || []) {
            if (!INTERNAL.test(d.name)) {
                bound.set(d.name, { file, line: d.line });
            }
        }
    }
    if (bound.size) {
        const adminReads = new Map();
        for (const [file, p] of parsed) {
            if (adminRelevant(file)) {
                for (const r of p.reads) {
                    if (tokens.has(r.name) && !adminReads.has(r.name)) {
                        adminReads.set(r.name, { file, line: r.line });
                    }
                }
            }
        }
        for (const [name, site] of adminReads) {
            if (!bound.has(name)) {
                out.core.push(
                    finding(
                        site.file,
                        site.line,
                        `var(${name})`,
                        `read on admin pages but not bound for the admin — add it to ${name.startsWith('--color-') ? ADMIN_PALETTE_FILE : ADMIN_TOKENS_FILE}`
                    )
                );
            }
        }
        for (const [name, site] of bound) {
            if (!adminReads.has(name)) {
                out.core.push(finding(site.file, site.line, name, 'bound for the admin but no admin page reads it — delete the line'));
            }
        }
    }

    if (theme) {
        checkTheme({ core, theme, themeParsed, templates, tokens, palette, roleInputs, parsed, typeSizes }, out.theme);
    }

    const byPlace = (a, b) => a.file.localeCompare(b.file) || a.line - b.line;
    out.core.sort(byPlace);
    out.theme.sort(byPlace);
    return out;
}

module.exports = {
    violation,
    finding,
    flattened,
    inMixin,
    underRoot,
    staticTypeSizes,
    componentValues,
    reads,
    modeSymmetry,
    twins,
    mixinTokens,
    checkTheme,
    checkTree,
};
