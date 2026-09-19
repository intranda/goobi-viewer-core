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
        '.gv-semantic-light() { --fg: var(--color-ink); --bg: var(--color-paper); }\n' +
        '.gv-semantic-dark() { --fg: var(--color-paper); --bg: var(--color-ink); }\n' +
        ':root { .gv-palette(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }',
    'misc/paletteAdmin.less': '.gv-palette-admin() { --color-ink: #1a1a1a; }',
    'misc/tokensAdmin.less': '.gv-semantic-admin() { --fg: var(--color-ink); }\n:root:has(#admin) { .gv-palette-admin(); .gv-semantic-admin(); }',
    'components/button.less': '.btn { color: var(--fg); }',
    'views/page.less': '.page { background: var(--bg); }',
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
    expect(core({ 'components/ok.less': '.ok { color: var(--fg); background: transparent; font: inherit; content: "red"; background-image: url(images/red.png);\nfont-size: 14px; // Fixed icon/marker geometry; independent of text scale.\n}' })).toEqual([]);
});

it('2 flags reads of tokens nobody declares', () => {
    const found = core({ 'components/x.less': '.x { color: var(--nope); border-color: var(--_local); --_mine: 1; width: var(--_mine); height: var(--_other, 1px); }' });
    expect(fixes(found)).toEqual(['components/x.less:1 no token of this name', 'components/x.less:1 internal token without a declaration in this file']);
});

it('2 accepts optional theme layout tokens only with a fallback', () => {
    expect(core({ 'views/page.less': '.page { max-width: var(--page-max-width, 192rem); padding-inline: var(--page-gutter, 2.4rem); }' })).toEqual([]);

    const found = core({
        'views/page.less': '.page { max-width: var(--page-max-width); padding-inline: var(--page-gutter); margin: var(--page-guter, 2.4rem); }',
    });
    expect(found.map((finding) => finding.what)).toEqual(['var(--page-max-width)', 'var(--page-gutter)', 'var(--page-guter)']);
    expect(found.every((finding) => finding.fix.startsWith('no token of this name'))).toBe(true);
});

it('3 keeps tokens and mode branches inside the token layer', () => {
    const found = core({
        'components/x.less': '.x { --fg: 1; @media (prefers-color-scheme: dark) { color: var(--fg); } }\n.color-theme-dark .y { color: var(--fg); }',
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
    const found = core({ 'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--bg: var(--color-paper); }', '--bg: var(--color-paper); --only: 1; }') });
    expect(fixes(found)).toEqual(['misc/tokens.less:1 declared in .gv-semantic-light() but not in .gv-semantic-dark()']);
});

it('5 keeps colour literals in the palette and palette reads in the roles', () => {
    const found = core({
        'components/x.less': '.x { color: var(--color-ink); }',
        'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--fg: var(--color-ink);', '--fg: #000;'),
    });
    expect(fixes(found)).toEqual(['components/x.less:1 palette colour read outside the semantic roles', 'misc/tokens.less:1 colour literal in the token layer']);
});

it('6 binds for the admin exactly what admin pages read', () => {
    const found = core({
        'components/y.less': '.y { background: var(--bg); }',
        'misc/tokensAdmin.less': CLEAN['misc/tokensAdmin.less'].replace('--fg: var(--color-ink); }', '--fg: var(--color-ink); --extra: 1; }'),
    });
    expect(fixes(found)).toEqual(['components/y.less:1 read on admin pages but not bound for the admin', 'misc/tokensAdmin.less:1 bound for the admin but no admin page reads it']);
});

describe('theme', () => {
    const THEME = {
        'misc/palette.less': '.mx-theme-palette() { --color-sea: #06c; }',
        'misc/tokens.less':
            '.mx-theme-semantic-light() { --fg: var(--color-sea); }\n.mx-theme-semantic-dark() { --fg: var(--color-sea); }\n' +
            ':root { .mx-theme-palette(); .mx-theme-semantic-light(); }\n.color-theme-dark { .mx-theme-semantic-dark(); }',
        'components/x.less': '.x { color: var(--fg); }',
    };

    it('applies the same sentences to theme files and reports them apart from the core', () => {
        const clean = checkTree(files(CLEAN), files(THEME));
        expect(clean).toEqual({ core: [], theme: [] });
        const broken = checkTree(
            files(CLEAN),
            files({
                ...THEME,
                'components/x.less': '.x { color: #fff; border-color: var(--nope); background: var(--color-sea); }',
                'misc/tokens.less': THEME['misc/tokens.less'].replace('.mx-theme-semantic-dark() { --fg: var(--color-sea); }', '.mx-theme-semantic-dark() { --fg: #000; }'),
            })
        );
        expect(broken.core).toEqual([]);
        expect(fixes(broken.theme)).toEqual([
            'theme/components/x.less:1 no token of this name',
            'theme/components/x.less:1 palette colour read in a component',
            'theme/components/x.less:1 colour written into the component',
            'theme/misc/tokens.less:2 colour literal outside the palette',
        ]);
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
    it('keeps the six sentences in the core', async () => {
        const result = await runTokenCheck();
        expect(result.core.map((f) => `${f.file}:${f.line} ${f.what}`)).toEqual([]);
    }, 30000);
});
