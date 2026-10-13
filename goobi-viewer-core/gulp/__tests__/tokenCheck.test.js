/*
 * One fixture per sentence of gulp/tokenCheck.js, the report format, and one run against the
 * real LESS tree so `npm test` (and with it the Maven build) fails on a core finding.
 */
const { checkTree, runTokenCheck, formatReport } = require('../tokenCheck');

const files = (obj) => new Map(Object.entries(obj));
const fixes = (findings) => findings.map((f) => `${f.file}:${f.line} ${f.fix.split(' — ')[0]}`);

const CLEAN = {
    'misc/variables.less': '@white: #fff; @copy: @white; @fontSmall: 1.2rem;',
    'misc/palette.less': '.gv-palette() { --color-ink: #111; --color-paper: #fff; }',
    'misc/tokens.less':
        '.gv-semantic-light() { --fg-ink: var(--color-ink); --bg-paper: var(--color-paper); }\n' +
        '.gv-semantic-dark() { --fg-ink: var(--color-paper); --bg-paper: var(--color-ink); }\n' +
        ':root { .gv-palette(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }',
    'misc/paletteAdmin.less': '.gv-palette-admin() { --color-ink: #1a1a1a; }',
    'misc/tokensAdmin.less': '.gv-semantic-admin() { --fg-ink: var(--color-ink); }\n:root:has(#admin) { .gv-palette-admin(); .gv-semantic-admin(); }',
    'components/button.less': '.btn { color: var(--fg-ink); }',
    'views/page.less': '.page { background: var(--bg-paper); }',
};
const core = (extra) => checkTree(files({ ...CLEAN, ...extra })).core;

it('accepts a tree that keeps all six sentences', () => {
    expect(core({})).toEqual([]);
});

it('1 flags colours, static colours and fixed text sizes in components, not in the backend trees', () => {
    const found = core({
        'components/x.less': '.x { color: red; background: #fff; border-color: @copy; font-size: 14px; }\n.y { font-size: @fontSmall; }',
        'admin/y.less': '.y { color: #fff; font-size: 14px; }',
    });
    expect(fixes(found)).toEqual([
        'components/x.less:1 colour written into the component',
        'components/x.less:1 colour written into the component',
        'components/x.less:1 static colour constant',
        'components/x.less:1 fixed text size',
        'components/x.less:2 fixed text size',
    ]);
    expect(core({ 'components/ok.less': '.ok { color: var(--fg-ink); background: transparent; font: inherit; content: "red"; background-image: url(images/red.png);\nfont-size: 14px; // Fixed icon/marker geometry; independent of text scale.\n}' })).toEqual([]);
});

it('2 flags reads of tokens nobody declares', () => {
    const found = core({ 'components/x.less': '.x { color: var(--nope); border-color: var(--_local); --_mine: 1; width: var(--_mine); height: var(--_other, 1px); }' });
    expect(fixes(found)).toEqual(['components/x.less:1 no token of this name', 'components/x.less:1 internal token without a declaration in this file']);
});

it('3 keeps tokens and mode branches inside the token layer', () => {
    const found = core({
        'components/x.less': '.x { --fg-ink: 1; @media (prefers-color-scheme: dark) { color: var(--fg-ink); } }\n.color-theme-dark .y { color: var(--fg-ink); }',
        'misc/tokens.less': `${CLEAN['misc/tokens.less']}\n.foo { --loose: 1; }`,
    });
    expect(fixes(found)).toEqual([
        'components/x.less:1 public token declared outside the token layer',
        'components/x.less:1 light/dark branch outside the token layer',
        'components/x.less:2 light/dark branch outside the token layer',
        'misc/tokens.less:5 declared under ".foo"',
    ]);
});

it('4 requires light and dark to declare the same tokens', () => {
    const found = core({ 'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--bg-paper: var(--color-paper); }', '--bg-paper: var(--color-paper); --bg-only: 1; }') });
    expect(fixes(found)).toEqual(['misc/tokens.less:1 declared in .gv-semantic-light() but not in .gv-semantic-dark()']);
});

it('5 keeps colour literals in the palette; a palette line belongs to the roles or to one component', () => {
    const found = core({
        'components/x.less': '.x { color: var(--color-ink); }',
        'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--fg-ink: var(--color-ink);', '--fg-ink: #000;'),
    });
    expect(fixes(found)).toEqual(['components/x.less:1 palette colour owned by the roles', 'misc/tokens.less:1 colour literal in the token layer']);
    expect(core({ 'misc/palette.less': '.gv-palette() { --color-ink: #111; --color-paper: #fff; --color-badge: light-dark(#f00, #f88); }', 'views/x.less': '.x { color: var(--color-badge); }' })).toEqual([]);
});

it('5 keeps light-dark() in the palette', () => {
    const found = core({
        'views/x.less': '.x { color: light-dark(var(--fg-ink), var(--bg-paper)); }',
        'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--fg-ink: var(--color-ink);', '--fg-ink: light-dark(var(--color-ink), var(--color-paper));'),
    });
    expect(fixes(found)).toEqual(['misc/tokens.less:1 light-dark() outside the palette', 'views/x.less:1 light-dark() in a component']);
});

it('6 binds for the admin exactly what admin pages read', () => {
    const found = core({
        'components/y.less': '.y { background: var(--bg-paper); }',
        'misc/tokensAdmin.less': CLEAN['misc/tokensAdmin.less'].replace('--fg-ink: var(--color-ink); }', '--fg-ink: var(--color-ink); --bg-extra: 1; }'),
    });
    expect(fixes(found)).toEqual(['components/y.less:1 read on admin pages but not bound for the admin', 'misc/tokensAdmin.less:1 bound for the admin but no admin page reads it']);
});

it('7 names roles by property and palette lines by tone and step', () => {
    const found = core({
        'misc/palette.less': '.gv-palette() { --color-ink: #111; --color-paper: #fff; --color-ink-hover: #222; --color-on-badge: #000; --color-danger-text: #900; }',
        'misc/tokens.less': CLEAN['misc/tokens.less'].replace('.gv-semantic-light() {', '.gv-semantic-light() { --brand: 1;').replace('.gv-semantic-dark() {', '.gv-semantic-dark() { --brand: 1;'),
        'views/x.less': '.x { color: var(--brand); }',
    });
    expect(fixes(found)).toEqual(['misc/palette.less:1 property or state word in a palette name', 'misc/tokens.less:1 role name without a property prefix', 'misc/tokens.less:2 role name without a property prefix']);
});

it('8 flags two roles that share a value in every binding, except static partners', () => {
    const found = core({
        'misc/tokens.less': CLEAN['misc/tokens.less']
            .replace('--fg-ink: var(--color-ink); --bg-paper: var(--color-paper); }', '--fg-ink: var(--color-ink); --bg-paper: var(--color-paper); --fg-twin: var(--color-ink); --fg-on-paper: var(--color-ink); }')
            .replace('--fg-ink: var(--color-paper); --bg-paper: var(--color-ink); }', '--fg-ink: var(--color-paper); --bg-paper: var(--color-ink); --fg-twin: var(--color-paper); --fg-on-paper: var(--color-paper); }'),
        'views/x.less': '.x { color: var(--fg-twin); border-color: var(--fg-on-paper); }',
    });
    expect(fixes(found)).toEqual(['misc/tokens.less:1 same value as --fg-ink in every binding']);
});

describe('theme', () => {
    const THEME = {
        'misc/palette.less': '.mx-theme-palette() { --color-theme-sea: #06c; }',
        'misc/tokens.less':
            '.mx-theme-semantic-light() { --fg-ink: var(--color-theme-sea); }\n.mx-theme-semantic-dark() { --fg-ink: var(--color-theme-sea); }\n' +
            ':root { .mx-theme-palette(); .mx-theme-semantic-light(); }\n.color-theme-dark { .mx-theme-semantic-dark(); }',
        'components/x.less': '.x { color: var(--fg-ink); }',
    };

    it('applies the same sentences to theme files and reports them apart from the core', () => {
        const clean = checkTree(files(CLEAN), files(THEME));
        expect(clean).toEqual({ core: [], theme: [] });
        const broken = checkTree(
            files(CLEAN),
            files({
                ...THEME,
                'components/x.less': '.x { color: #fff; border-color: var(--nope); background: var(--color-theme-sea); }',
                'misc/tokens.less': THEME['misc/tokens.less'].replace('.mx-theme-semantic-dark() { --fg-ink: var(--color-theme-sea); }', '.mx-theme-semantic-dark() { --fg-ink: #000; }'),
            })
        );
        expect(broken.core).toEqual([]);
        expect(fixes(broken.theme)).toEqual([
            'theme/components/x.less:1 no token of this name',
            'theme/components/x.less:1 palette colour owned by the roles',
            'theme/components/x.less:1 colour written into the component',
            'theme/misc/tokens.less:2 colour literal outside the palette',
        ]);
    });

    it('names theme tokens after their component and own colours --color-theme-*', () => {
        const found = checkTree(
            files(CLEAN),
            files({
                ...THEME,
                'misc/palette.less': '.mx-theme-palette() { --color-theme-sea: #06c; --color-sand: #eda; }',
                'misc/tokens.less': THEME['misc/tokens.less'].replace(/--fg-ink: var\(--color-theme-sea\);/g, '--fg-ink: var(--color-theme-sea); --bg-sand: var(--color-sand); --panel-bg: var(--color-sand);'),
            })
        ).theme;
        expect(fixes(found)).toEqual(['theme/misc/palette.less:1 palette colour the core does not declare', 'theme/misc/tokens.less:1 role name the core does not declare', 'theme/misc/tokens.less:2 role name the core does not declare']);
    });
});

describe('report', () => {
    it('prints one line per finding and keeps theme findings as information', () => {
        const finding = { file: 'components/x.less', line: 3, what: 'color: red', fix: 'colour written into the component — read a role' };
        const failed = formatReport({ core: [finding], theme: { status: 'checked', name: 'reference', path: 'x', findings: [] } });
        expect(failed).toContain('1 finding in the core');
        expect(failed).toContain('components/x.less:3  color: red  →  colour written into the component');
        const passed = formatReport({ core: [], theme: { status: 'checked', name: 'reference', path: 'x', findings: [{ ...finding, file: 'theme/components/x.less' }] } });
        expect(passed).toContain('Token check passed. Theme: reference.');
        expect(passed).toContain('information only');
        expect(passed).toContain('theme/components/x.less:3');
        expect(formatReport({ core: [], theme: { status: 'missing', name: 'x', path: '../gone/css/less', findings: [] } })).toContain('was not checked');
    });
});

describe('real LESS tree', () => {
    it('keeps the eight sentences in the core', async () => {
        const result = await runTokenCheck();
        expect(result.core.map((f) => `${f.file}:${f.line} ${f.what}`)).toEqual([]);
    }, 30000);
});
