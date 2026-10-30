/**
 * One fixture per sentence of gulp/tokenCheck/, the report format, and one run against the
 * real LESS tree so `npm test` (and with it the Maven build) fails on a core finding.
 */
const path = require('path');
const { checkTree, runTokenCheck, formatReport, themeSteps, stepFiles, parseLess, readLessTree, MODULE_ROOT, LESS_ROOT } = require('..');
const { RENAMED_TOKENS, successorAdvice } = require('../renames');

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

test('accepts a tree that keeps all six sentences', () => {
    expect(core({})).toEqual([]);
});

test('1 flags colours, static colours and fixed text sizes in components, not in the backend trees', () => {
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
    expect(
        core({
            'components/ok.less':
                '.ok { color: var(--fg-ink); background: transparent; font: inherit; content: "red"; background-image: url(images/red.png);\nfont-size: 14px; // Fixed icon/marker geometry; independent of text scale.\n}',
        })
    ).toEqual([]);
});

test('2 flags reads of tokens nobody declares', () => {
    const found = core({ 'components/x.less': '.x { color: var(--nope); border-color: var(--_local); --_mine: 1; width: var(--_mine); height: var(--_other, 1px); }' });
    expect(fixes(found)).toEqual(['components/x.less:1 no token of this name', 'components/x.less:1 internal token without a declaration in this file']);
});

test('3 keeps tokens and mode branches inside the token layer', () => {
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

test('4 requires light and dark to declare the same tokens', () => {
    const found = core({ 'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--bg-paper: var(--color-paper); }', '--bg-paper: var(--color-paper); --bg-only: 1; }') });
    expect(fixes(found)).toEqual(['misc/tokens.less:1 declared in .gv-semantic-light() but not in .gv-semantic-dark()']);
});

test('5 keeps colour literals in the palette; a palette line belongs to the roles or to one component', () => {
    const found = core({
        'components/x.less': '.x { color: var(--color-ink); }',
        'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--fg-ink: var(--color-ink);', '--fg-ink: #000;'),
    });
    expect(fixes(found)).toEqual(['components/x.less:1 palette colour owned by the roles', 'misc/tokens.less:1 colour literal in the token layer']);
    expect(
        core({
            'misc/palette.less': '.gv-palette() { --color-ink: #111; --color-paper: #fff; --color-badge: light-dark(#f00, #f88); }',
            'views/x.less': '.x { color: var(--color-badge); }',
        })
    ).toEqual([]);
});

test('5 keeps light-dark() in the palette', () => {
    const found = core({
        'views/x.less': '.x { color: light-dark(var(--fg-ink), var(--bg-paper)); }',
        'misc/tokens.less': CLEAN['misc/tokens.less'].replace('--fg-ink: var(--color-ink);', '--fg-ink: light-dark(var(--color-ink), var(--color-paper));'),
    });
    expect(fixes(found)).toEqual(['misc/tokens.less:1 light-dark() outside the palette', 'views/x.less:1 light-dark() in a component']);
});

test('6 binds for the admin exactly what admin pages read', () => {
    const found = core({
        'components/y.less': '.y { background: var(--bg-paper); }',
        'misc/tokensAdmin.less': CLEAN['misc/tokensAdmin.less'].replace('--fg-ink: var(--color-ink); }', '--fg-ink: var(--color-ink); --bg-extra: 1; }'),
    });
    expect(fixes(found)).toEqual(['components/y.less:1 read on admin pages but not bound for the admin', 'misc/tokensAdmin.less:1 bound for the admin but no admin page reads it']);
});

test('7 names roles by property and palette lines by tone and step', () => {
    const found = core({
        'misc/palette.less': '.gv-palette() { --color-ink: #111; --color-paper: #fff; --color-ink-hover: #222; --color-on-badge: #000; --color-danger-text: #900; }',
        'misc/tokens.less': CLEAN['misc/tokens.less']
            .replace('.gv-semantic-light() {', '.gv-semantic-light() { --brand: 1;')
            .replace('.gv-semantic-dark() {', '.gv-semantic-dark() { --brand: 1;'),
        'views/x.less': '.x { color: var(--brand); }',
    });
    expect(fixes(found)).toEqual([
        'misc/palette.less:1 property or state word in a palette name',
        'misc/tokens.less:1 role name without a property prefix',
        'misc/tokens.less:2 role name without a property prefix',
    ]);
});

test('8 flags two roles that share a value in every binding, except static partners', () => {
    const found = core({
        'misc/tokens.less': CLEAN['misc/tokens.less']
            .replace(
                '--fg-ink: var(--color-ink); --bg-paper: var(--color-paper); }',
                '--fg-ink: var(--color-ink); --bg-paper: var(--color-paper); --fg-twin: var(--color-ink); --fg-on-paper: var(--color-ink); }'
            )
            .replace(
                '--fg-ink: var(--color-paper); --bg-paper: var(--color-ink); }',
                '--fg-ink: var(--color-paper); --bg-paper: var(--color-ink); --fg-twin: var(--color-paper); --fg-on-paper: var(--color-paper); }'
            ),
        'views/x.less': '.x { color: var(--fg-twin); border-color: var(--fg-on-paper); }',
    });
    expect(fixes(found)).toEqual(['misc/tokens.less:1 same value as --fg-ink in every binding']);
});

const OLD_THEME = {
    'constructor.less': "@import 'misc/variables.less';\n@import 'misc/core-defaults.less';",
    'misc/variables.less': ':root {\n    --bg-general: #fff;\n    --fg-general: #333;\n    --fg-hover: #06c;\n    --datepicker-header-bg: #06c;\n}',
    'components/x.less': '.x { color: var(--bg-general); background: var(--ci-color1); }',
    'misc/core-defaults.less': ':root {\n    --lk-general: #06c;\n}',
};
const OLD_TEMPLATES = new Map([
    ['template.html', '<!DOCTYPE html>\n<html\n    class="#{sessionBean.get(\'colorTheme\')}">'],
    ['templateAdmin.html', '<html>'],
]);
const rows = (f) => f.code.map((b) => [b.file, ...b.rows.map((r) => `${r.sign} ${r.text}`)]);

describe('theme', () => {
    const THEME = {
        'misc/palette.less': '.mx-theme-palette() { --color-theme-sea: #06c; }',
        'misc/tokens.less':
            '.mx-theme-semantic-light() { --fg-ink: var(--color-theme-sea); }\n.mx-theme-semantic-dark() { --fg-ink: var(--color-theme-sea); }\n' +
            ':root { .mx-theme-palette(); .mx-theme-semantic-light(); }\n.color-theme-dark { .mx-theme-semantic-dark(); }',
        'components/x.less': '.x { color: var(--fg-ink); }',
    };

    test('applies the same sentences to theme files and reports them apart from the core', () => {
        const clean = checkTree(files(CLEAN), files(THEME));
        expect(clean).toEqual({ core: [], theme: [] });
        const broken = checkTree(
            files(CLEAN),
            files({
                ...THEME,
                'components/x.less': '.x { color: #fff; border-color: var(--nope); background: var(--color-theme-sea); }',
                'misc/tokens.less': THEME['misc/tokens.less'].replace(
                    '.mx-theme-semantic-dark() { --fg-ink: var(--color-theme-sea); }',
                    '.mx-theme-semantic-dark() { --fg-ink: #000; }'
                ),
            })
        );
        expect(broken.core).toEqual([]);
        expect(fixes(broken.theme)).toEqual([
            'theme/components/x.less:1 no token of this name',
            'theme/components/x.less:1 palette colour owned by the roles',
            'theme/components/x.less:1 colour written into the component',
            "theme/misc/palette.less:0 carry the theme's colours into the token layer",
            "theme/misc/tokens.less:0 carry the theme's colours into the token layer",
            'theme/misc/tokens.less:2 colour literal outside the palette',
        ]);
    });

    test('names theme tokens after their component and own colours --color-theme-*', () => {
        const found = checkTree(
            files(CLEAN),
            files({
                ...THEME,
                'misc/palette.less': '.mx-theme-palette() { --color-theme-sea: #06c; --color-sand: #eda; }',
                'misc/tokens.less': THEME['misc/tokens.less'].replace(
                    /--fg-ink: var\(--color-theme-sea\);/g,
                    '--fg-ink: var(--color-theme-sea); --bg-sand: var(--color-sand); --panel-bg: var(--color-sand);'
                ),
                'components/x.less': '.x { color: var(--fg-ink); background: var(--bg-sand); border-color: var(--panel-bg); }',
            })
        ).theme;
        expect(fixes(found)).toEqual([
            'theme/misc/palette.less:1 palette colour the core does not declare',
            'theme/misc/tokens.less:1 role name the core does not declare',
            'theme/misc/tokens.less:2 role name the core does not declare',
        ]);
    });

    test('flags theme tokens nothing reads, core tokens and read theme tokens stay quiet', () => {
        const found = checkTree(
            files(CLEAN),
            files({
                ...THEME,
                'misc/tokens.less': THEME['misc/tokens.less'].replace(
                    /--fg-ink: var\(--color-theme-sea\);/g,
                    '--fg-ink: var(--color-theme-sea); --panel-unused: var(--color-theme-sea);'
                ),
            })
        ).theme;
        expect(fixes(found)).toEqual(['theme/misc/tokens.less:1 nothing reads this token', 'theme/misc/tokens.less:2 nothing reads this token']);
        expect(found.every((f) => f.kind === 'todo')).toBe(true);
    });

    test('turns an old theme into a to-do list with the successor of every old name', () => {
        const todo = checkTree(files(CLEAN), files(OLD_THEME), OLD_TEMPLATES).theme.filter((f) => f.kind === 'todo');
        const say = (group, what) => todo.find((f) => f.group === group && f.what === what)?.fix ?? '';
        expect(say('token layer missing', 'misc/palette.less, misc/tokens.less')).toContain('create both files and import them right after misc/variables.less');
        expect(say('reads a token the core renamed', 'var(--ci-color1)')).toContain('read var(--color-brand)');
        expect(say('old core token name', '--bg-general')).toContain(
            'move it to .mx-theme-semantic-light() in misc/tokens.less as --bg-page, keep the value, and read var(--bg-page)'
        );
        expect(say('old core token name', '--fg-general')).toContain(
            '--fg-general, --fg-hover all become --fg-default: keep the one value that fits, in .mx-theme-semantic-light() in misc/tokens.less'
        );
        expect(say('old core token name', '--fg-hover')).toContain(
            'delete the line, --fg-default gets #333 from --fg-general (misc/variables.less:3) in this step, keep the value that fits'
        );
        expect(say('old core token name', '--fg-hover')).not.toContain(
            '--fg-general, --fg-hover all become --fg-default: keep the one value that fits, in .mx-theme-semantic-light()in misc/tokens.less'
        );
        expect(say('old core token name', '--datepicker-header-bg')).toContain('delete the line, the component reads --bg-brand now');
        expect(say('old core token name', '--lk-general')).toContain('move it to .mx-theme-semantic-light() in misc/tokens.less as --fg-link, keep the value');
        expect(say('old core overrides', 'misc/core-defaults.less')).toContain('move its lines into the token layer');
        expect(checkTree(files(CLEAN), files({ 'misc/core-defaults.less': ':root {\n}' })).theme.find((f) => f.group === 'old core overrides').fix).toContain('holds no values');
        expect(todo.filter((f) => f.group === 'theme without dark mode').map((f) => `${f.file}:${f.line}`)).toEqual(['template.html:3']);
    });

    test('a successor the layer has, or that an earlier line moves in, gets no second line; a subtheme constant never sets the brand', () => {
        const subtheme = { 'subthemes/s1/subThemeConstructor.less': "@import 'constructor.less';\n@ci-color1: #abcdef;" };
        const old = files({
            ...OLD_THEME,
            ...subtheme,
            'misc/variables.less': '@ci-color1: #123456;\n:root {\n    --fg-general: #333;\n    --fg-content-box: #333;\n}',
            'components/x.less': '.x { color: @ci-color1; }',
        });
        const found = checkTree(files(CLEAN), old, OLD_TEMPLATES).theme;
        const say = (name) => found.find((f) => f.group === 'old core token name' && f.what === name)?.fix;
        expect(found.find((f) => f.group === 'token layer missing').fix).toContain('--color-brand: #123456 (was @ci-color1)');
        expect(fixes(found.filter((f) => f.group === 'delete the colour and size constants'))).toEqual([
            'theme/misc/variables.less:1 delete the colour and size constants',
            'theme/subthemes/s1/subThemeConstructor.less:2 delete the colour and size constants',
        ]);
        expect(say('--fg-general')).toContain('--fg-general, --fg-content-box all become --fg-default');
        expect(say('--fg-content-box')).toContain('delete the line, --fg-default already carries this value: --fg-general (misc/variables.less:3) moves there in this step');

        const layered = files({
            ...old,
            'misc/palette.less': '.mx-theme-palette() {\n    --color-brand: #123456;\n}',
            'misc/tokens.less': '.mx-theme-semantic-light() {\n    --fg-default: #333;\n    --fg-link: #06c;\n}\n:root { .mx-theme-palette(); .mx-theme-semantic-light(); }',
            'misc/variables.less': '@ci-color1: #123456;\n:root {\n    --fg-general: #333;\n    --lk-general: #0066cc;\n    --ci-color1: #000;\n}',
        });
        const again = checkTree(files(CLEAN), layered, OLD_TEMPLATES).theme;
        const says = (name) => again.find((f) => f.group === 'old core token name' && f.what === name)?.fix;
        expect(says('--fg-general')).toContain('delete the line, --fg-default already carries this value in misc/tokens.less');
        expect(says('--lk-general')).toContain('delete the line, --fg-link already carries this value in misc/tokens.less');
        expect(says('--ci-color1')).toContain('delete the line, --color-brand is already declared in misc/palette.less with #123456, keep the value that fits');
        expect(
            again
                .filter((f) => f.group === 'old core token name')
                .flatMap((f) => f.changes)
                .some((c) => c.add)
        ).toBe(false);
    });

    test('shows every change as code: the lines as they stand and as they read afterwards', () => {
        const todo = checkTree(files(CLEAN), files(OLD_THEME), OLD_TEMPLATES).theme;
        const code = (group, what) => rows(todo.find((f) => f.group === group && f.what === what));
        expect(code('token layer missing', 'misc/palette.less, misc/tokens.less')).toEqual([
            ['theme/misc/palette.less', '+ .mx-theme-palette() {', '+     /* ---------- CORE PALETTE ---------- */', '+ ', '+     --color-brand: <brand colour>;', '+ }'],
            [
                'theme/misc/tokens.less',
                '+ .mx-theme-base() {',
                '+ }',
                '+ ',
                '+ .mx-theme-semantic-light() {',
                '+ }',
                '+ ',
                '+ /* WIRING: LIGHT ONLY */',
                '+ :root {',
                '+     .mx-theme-palette();',
                '+     .mx-theme-base();',
                '+     .mx-theme-semantic-light();',
                '+ }',
            ],
            ['theme/constructor.less', "  @import 'misc/variables.less';", "+ @import 'misc/palette.less';", "+ @import 'misc/tokens.less';"],
        ]);
        expect(code('old core overrides', 'misc/core-defaults.less')).toEqual([
            ['theme/misc/core-defaults.less'],
            ['theme/constructor.less', "- @import 'misc/core-defaults.less';"],
        ]);
        expect(code('old core token name', '--bg-general')).toEqual([
            ['theme/misc/variables.less', '- --bg-general: #fff;'],
            ['theme/misc/tokens.less', '+ --bg-page: #fff;'],
            ['theme/components/x.less', '- .x { color: var(--bg-general); background: var(--ci-color1); }', '+ .x { color: var(--bg-page); background: var(--ci-color1); }'],
        ]);
        const moved = todo.find((f) => f.what === '--bg-general').code[1];
        expect(moved).toMatchObject({ into: '.mx-theme-semantic-light()', section: 'CORE OVERRIDES', label: null });
        expect(code('old core token name', '--datepicker-header-bg')).toEqual([['theme/misc/variables.less', '- --datepicker-header-bg: #06c;']]);
        expect(code('reads a token the core renamed', 'var(--ci-color1)')).toEqual([
            ['theme/components/x.less', '- .x { color: var(--bg-general); background: var(--ci-color1); }', '+ .x { color: var(--bg-general); background: var(--color-brand); }'],
        ]);
        expect(code('theme without dark mode', '<html>')).toEqual([['template.html', '- class="#{sessionBean.get(\'colorTheme\')}">', '+ class="color-theme-light">']]);
    });

    test('writes the complete migration as code: roles for colours and sizes, local values, no dark branches', () => {
        const core = files({ ...CLEAN, 'misc/tokens.less': `${CLEAN['misc/tokens.less']}\n.gv-base() { --font-size-body: 1.4rem; }` });
        const theme = files({
            ...THEME,
            'misc/tokens.less': '.mx-theme-semantic-light() { --fg-ink: var(--color-theme-sea); }\n:root { .mx-theme-palette(); .mx-theme-semantic-light(); }',
            'misc/variables.less': '@red: #c7254e;\n@unused: #123456;',
            'components/x.less':
                '.x {\n    color: darken(@red, 10%);\n    background: url(white.png) white;\n    font-size: 14px;\n    --local: 1px;\n    width: var(--local);\n}\n.color-theme-dark .x {\n    display: none;\n}',
        });
        const full = checkTree(core, theme).theme;
        const code = (group) => full.filter((f) => f.group === group).flatMap(rows);
        expect(code('colours in components')).toEqual([
            ['theme/components/x.less', '- color: darken(@red, 10%);', '+ color: var(--x-fg);'],
            ['theme/components/x.less', '- background: url(white.png) white;', '+ background: url(white.png) var(--bg-paper);'],
        ]);
        expect(code('fixed text sizes')).toEqual([['theme/components/x.less', '- font-size: 14px;', '+ font-size: var(--font-size-body);']]);
        expect(code('token declared in a component')).toEqual([
            ['theme/components/x.less', '- --local: 1px;', '+ --_local: 1px;'],
            ['theme/components/x.less', '- width: var(--local);', '+ width: var(--_local);'],
        ]);
        const shared = checkTree(core, new Map([...theme, ['components/y.less', '.y { width: var(--local); }']])).theme.find((f) => f.group === 'token declared in a component');
        expect(shared.fix).toContain('move the line into .mx-theme-base() in misc/tokens.less');
        expect(shared.code[1]).toMatchObject({ file: 'theme/misc/tokens.less', into: '.mx-theme-base()', section: 'THEME TOKENS', label: null });
        expect(code('light/dark branch in a component')).toEqual([['theme/components/x.less', '- .color-theme-dark .x {', '-     display: none;', '- }']]);
        expect(code("carry the theme's colours into the token layer")).toEqual([
            ['theme/misc/palette.less', '+ --color-theme-red-darken-10: #9c1d3d;'],
            ['theme/misc/tokens.less', '+ --x-fg: var(--color-theme-red-darken-10);'],
        ]);
        const bound = full.filter((f) => f.group === "carry the theme's colours into the token layer").map((f) => f.code[0]);
        expect(bound.map((b) => [b.into, b.section, b.label])).toEqual([
            ['.mx-theme-palette()', 'THEME PALETTE', null],
            ['.mx-theme-semantic-light()', 'THEME TOKENS', null],
        ]);
        expect(code('delete the colour and size constants')).toEqual([['theme/misc/variables.less', '- @red: #c7254e;']]);
    });

    test('reads the old mode rules as a branch: the outermost block once, nothing replaced inside it, a constant read only there goes with it', () => {
        expect(parseLess('.x { .mx-dark-mode-rules({ color: red; }); }')).toMatchObject({
            properties: [{ name: 'color', value: 'red', line: 1, stack: ['.x', '.mx-dark-mode-rules'] }],
            blocks: [{ header: '.x' }, { header: '.mx-dark-mode-rules' }],
        });
        const dark = files({
            ...THEME,
            'misc/variables.less': '@colorModes: true;\n@black: #000;\n@white: #fff;\n@shade: #333;\n@fluid: clamp(10px, 2vw, 20px);',
            'components/x.less':
                '.x {\n    color: @black;\n    box-shadow: 0 1px @shade;\n    font-size: @fluid;\n    & when (@colorModes = true) {\n        .mx-dark-mode-rules({ color: @white; });\n    }\n}',
        });
        const found = checkTree(files(CLEAN), dark).theme;
        const of = (group) => found.filter((f) => f.group === group);
        expect(of('light/dark branch in a component').map((f) => [f.kind, `${f.file}:${f.line}`])).toEqual([['todo', 'theme/components/x.less:5']]);
        expect(fixes(of('colours in components'))).toEqual(['theme/components/x.less:2 static colour constant', 'theme/components/x.less:3 static colour constant']);
        expect(fixes(of('delete the colour and size constants'))).toEqual([
            'theme/misc/variables.less:2 delete the colour and size constants',
            'theme/misc/variables.less:3 delete the colour and size constants',
        ]);

        const off = checkTree(
            files(CLEAN),
            new Map([
                ...dark,
                ['misc/tokens.less', '.mx-theme-semantic-light() { --fg-ink: var(--color-theme-sea); }\n:root { .mx-theme-palette(); .mx-theme-semantic-light(); }'],
                ['misc/variables.less', dark.get('misc/variables.less').replace('@colorModes: true', '@colorModes: false')],
                ['components/y.less', '.y {\n    .mx-light-mode-rules({\n        color: @black;\n    });\n}'],
            ])
        ).theme;
        const branches = off.filter((f) => f.group === 'light/dark branch in a component');
        expect(branches.map((f) => [f.kind, `${f.file}:${f.line}`, f.fix.split(': ')[1]])).toEqual([
            ['todo', 'theme/components/x.less:5', 'delete the branch, it never applies'],
            ['todo', 'theme/components/y.less:2', 'keep the rules, drop the wrapper'],
        ]);
        expect(branches[0].code[0].rows.map((r) => `${r.sign}${r.text.trim()}`)).toEqual(['-& when (@colorModes = true) {', '-.mx-dark-mode-rules({ color: @white; });', '-}']);
        expect(fixes(off.filter((f) => f.group === 'colours in components'))).toEqual([
            'theme/components/x.less:2 static colour constant',
            'theme/components/x.less:3 static colour constant',
            'theme/components/y.less:3 static colour constant',
        ]);
        expect(fixes(off.filter((f) => f.group === 'delete the colour and size constants'))).toEqual([
            'theme/misc/variables.less:2 delete the colour and size constants',
            'theme/misc/variables.less:3 delete the colour and size constants',
        ]);
    });

    test('a renamed line keeps reading its colour, and a read edit is listed once however many bindings rename', () => {
        const bindings =
            '.mx-theme-semantic-light() { --fg-general: var(--color-theme-sea); }\n.mx-theme-semantic-dark() { --fg-general: var(--color-theme-sea); }\n:root { .mx-theme-palette(); .mx-theme-semantic-light(); }\n.color-theme-dark { .mx-theme-semantic-dark(); }';
        const unread = checkTree(files(CLEAN), files({ ...THEME, 'misc/tokens.less': bindings })).theme;
        expect(fixes(unread)).toEqual(['theme/misc/tokens.less:1 old core token name', 'theme/misc/tokens.less:2 old core token name']);
        const read = checkTree(files(CLEAN), files({ ...THEME, 'misc/tokens.less': bindings, 'components/y.less': '.y { color: var(--fg-general); }' })).theme;
        const step = themeSteps(read).sections[0].steps.find((s) => s.group === 'old core token name');
        const y = stepFiles(step).find((f) => f.file === 'components/y.less');
        expect(y.rows.map((r) => `${r.sign} ${r.text}`)).toEqual(['- .y { color: var(--fg-general); }', '+ .y { color: var(--fg-default); }']);
    });

    test('new lines stand under the labels of the core, in the order of the core file', () => {
        const core = files({
            ...CLEAN,
            'misc/palette.less': '.gv-palette() {\n    /* BRAND */\n    --color-brand: #06c;\n    --color-ink: #111;\n    --color-paper: #fff;\n}',
            'misc/tokens.less':
                '.gv-base() {\n    /* SHAPE */\n    --radius-button: 0;\n}\n.gv-semantic-light() {\n    /* SURFACES */\n    --bg-page: var(--color-paper);\n    /* BRAND */\n    --bg-brand: var(--color-brand);\n}\n.gv-semantic-dark() {\n    --bg-page: var(--color-ink);\n    --bg-brand: var(--color-brand);\n}\n:root { .gv-palette(); .gv-base(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }',
            'components/button.less': '.btn { color: var(--bg-brand); border-radius: var(--radius-button); }',
        });
        const theme = files({
            ...THEME,
            'misc/tokens.less': '.mx-theme-semantic-light() { --bg-page: var(--color-theme-sea); }\n:root { .mx-theme-palette(); .mx-theme-semantic-light(); }',
            'misc/core-defaults.less':
                ':root {\n    --bg-brand: #000;\n    --radius-button: 4px;\n    --bg-page: #fff;\n    --color-brand: #06c;\n}\n.color-theme-dark {\n    --bg-brand: #fff;\n}',
        });
        const moved = themeSteps(checkTree(core, theme).theme).sections[0].steps.find((s) => s.group === 'old core overrides');
        const added = (file) =>
            stepFiles(moved)
                .find((f) => f.file === file)
                .rows.map((r) => `${r.sign} ${r.text}`);
        expect(added('misc/tokens.less')).toEqual([
            '  .mx-theme-semantic-light() {',
            '+     /* ---------- CORE OVERRIDES ---------- */',
            '+ ',
            '+     /* SURFACES */',
            '+     --bg-page: #fff;',
            '+ ',
            '+     /* BRAND */',
            '+     --bg-brand: #000;',
            '  }',
            '  .mx-theme-base() {',
            '+     /* ---------- CORE OVERRIDES ---------- */',
            '+ ',
            '+     /* SHAPE */',
            '+     --radius-button: 4px;',
            '  }',
            '  .mx-theme-semantic-dark() {',
            '+     /* ---------- CORE OVERRIDES ---------- */',
            '+ ',
            '+     /* BRAND */',
            '+     --bg-brand: #fff;',
            '  }',
        ]);
        expect(added('misc/palette.less')).toEqual([
            '  .mx-theme-palette() {',
            '+     /* ---------- CORE PALETTE ---------- */',
            '+ ',
            '+     /* BRAND */',
            '+     --color-brand: #06c;',
            '  }',
        ]);
        expect(added('misc/core-defaults.less').filter((r) => r.includes('--bg-brand: #fff'))).toEqual(['- --bg-brand: #fff;']);
        expect(checkTree(core, theme).theme.find((f) => f.what === '--bg-brand' && f.line === 8).fix).toContain('move the line into .mx-theme-semantic-dark() in misc/tokens.less');
    });

    test('an old role whose successor is a palette colour moves to the palette; one whose successor is a local value goes', () => {
        const theme = files({ 'misc/variables.less': ':root {\n    --bg-image-frame: #abc;\n    --bg-viewer-canvas-stripe: #eee;\n}' });
        const found = checkTree(files(CLEAN), theme).theme.filter((f) => f.group === 'old core token name');
        expect(found.map((f) => f.fix)).toEqual([
            'old core token name — move it to .mx-theme-palette() in misc/palette.less as var(--color-image-frame), keep the value',
            'old core token name — delete the line, the component reads var(--_stripe) now',
        ]);
        expect(found.flatMap(rows)).toEqual([
            ['theme/misc/variables.less', '- --bg-image-frame: #abc;'],
            ['theme/misc/palette.less', '+ --color-image-frame: #abc;'],
            ['theme/misc/variables.less', '- --bg-viewer-canvas-stripe: #eee;'],
        ]);
    });

    test('replaces a colour inside an escaped string, which is CSS, but not inside a plain string or url()', () => {
        const theme = files({ ...THEME, 'components/x.less': ".x {\n    background: ~'linear-gradient(white, white)' url(white.png);\n    content: 'white';\n}" });
        const found = checkTree(files(CLEAN), theme).theme.filter((f) => f.group === 'colours in components');
        expect(found.flatMap(rows)).toEqual([
            [
                'theme/components/x.less',
                "- background: ~'linear-gradient(white, white)' url(white.png);",
                "+ background: ~'linear-gradient(var(--bg-paper), var(--bg-paper))' url(white.png);",
            ],
        ]);
    });

    test('accepts a migrated light-only theme whose templates force light', () => {
        const migrated = {
            ...THEME,
            'constructor.less': "@import 'misc/variables.less';\n@import 'misc/palette.less';\n@import 'misc/tokens.less';",
            'misc/tokens.less': '.mx-theme-semantic-light() { --fg-ink: var(--color-theme-sea); }\n:root { .mx-theme-palette(); .mx-theme-semantic-light(); }',
        };
        const templates = new Map([['template.html', '<html class="color-theme-light">']]);
        expect(checkTree(files(CLEAN), files(migrated), templates).theme).toEqual([]);
    });

    test('accepts a theme without dark mode: light-only wiring has no counterpart to miss', () => {
        const lightOnly = {
            ...THEME,
            'misc/tokens.less': '.mx-theme-semantic-light() { --fg-ink: var(--color-theme-sea); }\n:root { .mx-theme-palette(); .mx-theme-semantic-light(); }',
        };
        expect(checkTree(files(CLEAN), files(lightOnly)).theme).toEqual([]);
    });
});

describe('report', () => {
    const report = (findings, options) => formatReport({ core: [], theme: { status: 'checked', name: 'old', path: 'x', findings } }, options);
    const withSteps = (wanted, fn) => {
        process.env.GV_TOKENCHECK_STEP = wanted;
        try {
            return fn();
        } finally {
            delete process.env.GV_TOKENCHECK_STEP;
        }
    };

    test('prints one line per core finding, sums theme findings up and links the report', () => {
        const finding = { file: 'components/x.less', line: 3, what: 'color: red', fix: 'colour written into the component — read a role' };
        const failed = formatReport({ core: [finding], theme: { status: 'checked', name: 'reference', path: 'x', findings: [] } });
        expect(failed).toContain('1 finding in the core');
        expect(failed).toContain('components/x.less:3  color: red  →  colour written into the component');
        const themed = [{ ...finding, file: 'theme/components/x.less', kind: 'full' }];
        const passed = formatReport({ core: [], theme: { status: 'checked', name: 'reference', path: 'x', findings: themed } }, { report: '/tmp/token-check/reference.html' });
        expect(passed).toContain('Token check passed. Theme: reference.');
        expect(passed).toContain('information only');
        expect(passed).toContain('  report: file:///tmp/token-check/reference.html');
        expect(passed).toContain('steps in the terminal: GV_TOKENCHECK_STEP=3');
        expect(passed).not.toContain('3 │   color: red');
        expect(withSteps('all', () => report(themed))).toContain('     components/x.less\n       3 │   color: red');
        expect(formatReport({ core: [], theme: { status: 'missing', name: 'x', path: '../gone/css/less', findings: [] } })).toContain('was not checked');
    });

    test('opens with an overview of the numbered steps; on request each step follows as code', () => {
        const findings = checkTree(files(CLEAN), files(OLD_THEME), OLD_TEMPLATES).theme;
        const overview = report(findings);
        expect(overview).toContain('Theme old — 9 required · 0 for the complete token migration');
        expect(overview).toMatch(/ {2}REQUIRED FOR THE NEW CORE\n {4}1 {2}token layer missing +1\n {4}2 {2}reads a token the core renamed +1\n/);
        expect(overview).not.toContain('━━ REQUIRED FOR THE NEW CORE');
        const text = withSteps('all', () => report(findings));
        expect(text).toContain('  1  token layer missing\n     create both files and import them right after misc/variables.less');
        expect(text).toContain("     constructor.less\n       1 │   @import 'misc/variables.less';\n         │ + @import 'misc/palette.less';");
        expect(text).toContain(
            '     misc/palette.less  new file\n         │ + .mx-theme-palette() {\n         │ +     /* ---------- CORE PALETTE ---------- */\n         │ +\n         │ +     --color-brand: <brand colour>;\n         │ + }'
        );
        expect(text).toMatch(/ {5}misc\/variables\.less\n {7}2 │ - --bg-general: #fff; +moves to misc\/tokens\.less\n {7}3 │ - --fg-general: #333; +moves to misc\/tokens\.less\n/);
        expect(text).toMatch(
            / {5}misc\/tokens\.less\n {9}│ {3}\.mx-theme-semantic-light\(\) \{\n {9}│ \+ {5}\/\* ---------- CORE OVERRIDES ---------- \*\/\n {9}│ \+\n {9}│ \+ {5}--fg-link: #06c;\n {9}│ \+ {5}--bg-page: #fff;\n {9}│ \+ {5}--fg-default: #333; +--fg-general, --fg-hover all become --fg-default: keep one of the values\n/
        );
        expect(text).toContain("     misc/core-defaults.less  delete the file\n\n     constructor.less\n       2 │ - @import 'misc/core-defaults.less';");
        expect(text).toContain('     template.html\n       3 │ - class="#{sessionBean.get(\'colorTheme\')}">\n         │ + class="color-theme-light">');
        expect(text.indexOf('━━ REQUIRED FOR THE NEW CORE')).toBeGreaterThan(text.indexOf('token layer missing'));
    });

    test('shows single steps on request', () => {
        const findings = [
            { file: 'template.html', line: 5, what: '<html>', fix: 'theme without dark mode — set class="color-theme-light"', kind: 'todo', group: 'theme without dark mode' },
            { file: 'theme/components/a.less', line: 3, what: 'color: red', fix: 'colour written into the component — read a role', kind: 'full', group: 'colours in components' },
            { file: 'theme/components/a.less', line: 4, what: 'font-size: 12px', fix: 'fixed text size — use a text role', kind: 'full', group: 'fixed text sizes' },
        ];
        expect(report(findings)).toMatch(/ {2}COMPLETE TOKEN MIGRATION\n {4}2 {2}colours in components +1\n {4}3 {2}fixed text sizes +1\n/);
        const one = withSteps('3', () => report(findings));
        expect(one).toContain('  3  fixed text sizes');
        expect(one).toContain('4 │   font-size: 12px');
        expect(one).not.toContain('3 │   color: red');
        expect(one).not.toContain('━━ REQUIRED');
        const two = withSteps('1,2', () => report(findings));
        expect(two).toContain('━━ REQUIRED FOR THE NEW CORE');
        expect(two).toContain('3 │   color: red');
        expect(two).not.toContain('font-size: 12px');
    });
});

describe('real LESS tree', () => {
    test('keeps the eight sentences in the core', async () => {
        const result = await runTokenCheck();
        expect(result.core.map((f) => `${f.file}:${f.line} ${f.what}`)).toEqual([]);
    }, 30000);

    test('renames every old name to a token the core declares, or drops it', () => {
        const declared = new Set([...readLessTree(path.resolve(MODULE_ROOT, LESS_ROOT)).values()].flatMap((source) => parseLess(source).declarations.map((d) => d.name)));
        const dangling = [...RENAMED_TOKENS.keys()].filter((name) => {
            const to = successorAdvice(name, (n) => declared.has(n)).to?.match(/--[\w-]+/)?.[0];
            return to && !declared.has(to);
        });
        expect(dangling).toEqual([]);
    });
});
