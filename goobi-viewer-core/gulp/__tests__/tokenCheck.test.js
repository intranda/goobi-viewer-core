/**
 * Unit tests for the token check (gulp/tokenCheck.js) against inline LESS
 * fixtures, plus one run against the real LESS tree.
 */
const { parseLess, checkFiles, applyBaseline, formatReport, runTokenCheck, keyOf, classifyThemeTokens, formatThemeTokenReport } = require('../tokenCheck');

const TOKENS = 'misc/tokens.less';
const ALIASES = 'misc/some-aliases.less';
const OPTS = { tokenLayer: [TOKENS], themeMixins: 'misc/mixins.less' };

const files = (obj) => new Map(Object.entries(obj));
const keys = (result) => result.violations.map((v) => `${v.rule} ${v.file} ${v.token}`);
const rules = (result) => result.violations.map((v) => v.rule);
const warnings = (result) => result.warnings.map((w) => `${w.rule} ${w.token}`);

const TOKEN_LAYER_OK = `
:root {
    --bg: #fff;
    --fg: var(--n-900);
    --n-900: #111;
}
@media (prefers-color-scheme: dark) {
    :root {
        --bg: #000;
    }
}
.color-theme-dark {
    --bg: #000;
}
.color-theme-light {
    --bg: #fff;
}
`;

describe('parseLess', () => {
    it('records declarations with their selector stack and line', () => {
        const parsed = parseLess(`.a {\n  .b {\n    --x: red;\n  }\n}`);

        expect(parsed.declarations).toEqual([{ name: '--x', value: 'red', line: 3, stack: ['.a', '.b'] }]);
    });

    it('records reads with and without fallback', () => {
        const parsed = parseLess(`.a { color: var(--x); background: var(--y, red); border: var( --z ,var(--w)); }`);

        expect(parsed.reads.map((r) => [r.name, r.fallback])).toEqual([
            ['--x', false],
            ['--y', true],
            ['--z', true],
            ['--w', false],
        ]);
    });

    it('ignores comments but not protocol-relative urls', () => {
        const parsed = parseLess(`// --a: red;\n/* --b: red; */\n.x { background: url(http://h/i.png); --c: 1; }`);

        expect(parsed.declarations.map((d) => d.name)).toEqual(['--c']);
        expect(parsed.declarations[0].line).toBe(3);
    });

    it('does not split statements on semicolons inside strings or parentheses', () => {
        const parsed = parseLess(`.x { content: ";"; background: url(data:image/png;base64,AAA); --d: 1; }`);

        expect(parsed.declarations.map((d) => d.name)).toEqual(['--d']);
    });

    it('records mixin calls and alias definitions', () => {
        const parsed = parseLess(`@bg: var(--bg);\n:root { .gv-palette(); .mx-theme-light; }`);

        expect(parsed.calls.map((c) => [c.name, c.stack])).toEqual([
            ['.gv-palette()', [':root']],
            ['.mx-theme-light', [':root']],
        ]);
        expect(parsed.aliases).toEqual([{ alias: 'bg', token: '--bg', line: 1 }]);
    });

    it('treats @{…} interpolation as selector text, not as a block', () => {
        const parsed = parseLess(`@{RTL} {\n  .b { --x: 1; }\n}\n.@{mode} { --y: 1; }`);

        expect(parsed.declarations.map((d) => d.stack)).toEqual([['@{RTL}', '.b'], ['.@{mode}']]);
    });

    it('keeps nested @media in the stack', () => {
        const parsed = parseLess(`@media (prefers-color-scheme: dark) {\n :root { --x: 1; }\n}`);

        expect(parsed.declarations[0].stack).toEqual(['@media (prefers-color-scheme: dark)', ':root']);
    });
});

describe('R1 reads need a core default or an inline fallback', () => {
    it('flags a read of an undeclared token without fallback', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, 'widgets/w.less': `.w { color: var(--nope); }` }), null, OPTS);

        expect(keys(result)).toContain('R1 widgets/w.less --nope');
    });

    it('accepts declared tokens, flags undeclared ones even with a fallback', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, 'widgets/w.less': `.w { color: var(--bg); border: var(--nope, red); }` }), null, OPTS);

        expect(keys(result).filter((k) => k.startsWith('R1'))).toEqual(['R1 widgets/w.less --nope']);
    });

    it('accepts internal tokens declared in the same file, flags undeclared ones', () => {
        const result = checkFiles(
            files({
                [TOKENS]: TOKEN_LAYER_OK,
                'views/a.less': `.a { --_gap: 4px; .b { gap: var(--_gap); } }`,
                'views/b.less': `.b { gap: var(--_other); }`,
            }),
            null,
            OPTS
        );

        expect(keys(result)).toEqual(['R1 views/b.less --_other']);
    });

    it('applies to the token layer itself', () => {
        const result = checkFiles(files({ [TOKENS]: `:root { --a: var(--missing); --b: var(--missing, red); }` }), null, OPTS);

        expect(keys(result)).toEqual(['R1 misc/tokens.less --missing', 'R1 misc/tokens.less --missing']);
    });
});

describe('R2 theme-visible tokens live only in the token layer, under (0,1,0) selectors', () => {
    it('flags a component declaring a theme-visible token', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, 'widgets/w.less': `.w { .badge { --bg: red; } }` }), null, OPTS);

        expect(keys(result)).toContain('R2 widgets/w.less --bg');
        expect(result.violations.find((v) => v.rule === 'R2').message).toContain('.w .badge');
    });

    it('accepts internal tokens on components', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, 'widgets/w.less': `.w { --_bg: var(--bg); }` }), null, OPTS);

        expect(rules(result)).not.toContain('R2');
    });

    it('accepts :root, .color-theme-*, @media dark wrappers and mixin definitions in the token layer', () => {
        const layer = `${TOKEN_LAYER_OK}\n.gv-palette() { --n-0: #fff; }\n:root { .gv-palette(); }`;
        const result = checkFiles(files({ [TOKENS]: layer }), null, OPTS);

        expect(result.violations).toEqual([]);
    });

    it('accepts the admin scope wiring beside the mode selectors', () => {
        const layer = `${TOKEN_LAYER_OK}\n.gv-palette() { --n-0: #fff; }\n:root { .gv-palette(); }\n:root:has(#admin) { --bg: #fff; .gv-palette(); }`;
        const result = checkFiles(files({ [TOKENS]: layer }), null, OPTS);

        expect(result.violations).toEqual([]);
    });

    it('flags stronger or foreign selectors in the token layer', () => {
        const layer = `:root:not(.color-theme-light) { --a: 1; }\n.foo { --b: 1; }\nhtml { --c: 1; }\n.foo { .gv-x(); }\n.gv-x() { --d: 1; }`;
        const result = checkFiles(files({ [TOKENS]: layer }), null, OPTS);

        expect(keys(result)).toEqual(['R2 misc/tokens.less --a', 'R2 misc/tokens.less --b', 'R2 misc/tokens.less --c', 'R2 misc/tokens.less .gv-x()']);
    });

    it('flags a top-level declaration in the token layer', () => {
        const result = checkFiles(files({ [TOKENS]: `--a: 1;` }), null, OPTS);

        expect(keys(result)).toEqual(['R2 misc/tokens.less --a']);
    });
});

describe('R4 mode branching stays inside the token layer', () => {
    it('flags internal tokens in the token layer', () => {
        const result = checkFiles(files({ [TOKENS]: `:root { --_x: 1; }` }), null, OPTS);

        expect(keys(result)).toContain('R4 misc/tokens.less --_x');
    });

    it('flags prefers-color-scheme and .color-theme-* in component LESS', () => {
        const result = checkFiles(
            files({
                [TOKENS]: TOKEN_LAYER_OK,
                'views/a.less': `.a { @media (prefers-color-scheme: dark) { color: red; } }`,
                'views/b.less': `.color-theme-dark .b { color: red; }`,
            }),
            null,
            OPTS
        );

        expect(keys(result)).toEqual(['R4 views/a.less .a @media (prefers-color-scheme: dark)', 'R4 views/b.less .color-theme-dark .b']);
    });
});

describe('R5 warns about tokens nobody reads', () => {
    const layer = `:root { --used-var: 1; --used-alias: 1; --used-theme: 1; --dead: 1; }`;

    it('counts var() reads, alias usage and theme reads', () => {
        const result = checkFiles(
            files({
                [TOKENS]: layer,
                [ALIASES]: `@used-alias: var(--used-alias);\n@dead: var(--dead);`,
                'views/a.less': `.a { color: var(--used-var); border: @used-alias; }`,
            }),
            files({ 'misc/mixins.less': ``, 'views/t.less': `.t { color: var(--used-theme); }` }),
            OPTS
        );

        expect(warnings(result).filter((w) => w.startsWith('R5'))).toEqual(['R5 --dead']);
    });
});

describe('R6 theme mixin tokens must exist in core or be read by the theme', () => {
    it('flags an orphan, accepts known and theme-read tokens', () => {
        const result = checkFiles(
            files({ [TOKENS]: TOKEN_LAYER_OK }),
            files({
                'misc/mixins.less': `.mx-theme-light { --bg: var(--n-900); --orphan: 1; --theme-own: 1; }`,
                'layout/x.less': `.x { color: var(--theme-own); }`,
            }),
            OPTS
        );

        expect(keys(result)).toEqual(['R6 misc/mixins.less --orphan']);
    });

    it('counts theme reads through a used @alias', () => {
        const result = checkFiles(
            files({ [TOKENS]: TOKEN_LAYER_OK }),
            files({
                'misc/mixins.less': `.mx-theme-light { --via-alias: 1; --alias-unused: 1; }`,
                'misc/variables.less': `@via-alias: var(--via-alias);\n@alias-unused: var(--alias-unused);`,
                'layout/x.less': `.x { color: @via-alias; }`,
            }),
            OPTS
        );

        expect(keys(result)).toEqual(['R6 misc/mixins.less --alias-unused']);
    });

    it('says when core reads the theme token via fallback', () => {
        const result = checkFiles(
            files({ [TOKENS]: TOKEN_LAYER_OK, 'widgets/w.less': `.w { color: var(--theme-global, red); }` }),
            files({ 'misc/mixins.less': `.mx-theme-light { --theme-global: #000; }` }),
            OPTS
        );

        const r6 = result.violations.filter((v) => v.rule === 'R6');
        expect(r6.map((v) => v.token)).toEqual(['--theme-global']);
        expect(r6[0].message).toContain('token layer');
    });

    it('skips theme rules when no theme is given', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK }), null, OPTS);

        expect(result.violations).toEqual([]);
    });
});

describe('R7 light and dark mixins declare the same tokens', () => {
    it('flags tokens present in only one mode and a missing counterpart', () => {
        const layer = `.gv-features-light() { --a: 1; --b: 1; }\n.gv-features-dark() { --a: 2; --c: 2; }\n.gv-only-light() { --d: 1; }\n:root { .gv-features-light(); .gv-only-light(); }\n.color-theme-dark { .gv-features-dark(); }`;
        const result = checkFiles(files({ [TOKENS]: layer }), null, OPTS);

        expect(keys(result).filter((k) => k.startsWith('R7'))).toEqual(['R7 misc/tokens.less --b', 'R7 misc/tokens.less --c', 'R7 misc/tokens.less --d']);
    });

    it('accepts matching pairs and mode-free mixins', () => {
        const layer = `.gv-palette() { --p: 1; }\n.gv-semantic-light() { --a: 1; }\n.gv-semantic-dark() { --a: 2; }\n:root { .gv-palette(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [TOKENS]: layer }), null, OPTS);

        expect(rules(result)).not.toContain('R7');
    });
});

describe('R8 palette tokens are read only by semantic roles', () => {
    const PALETTE = 'misc/palette.less';
    const opts = { ...OPTS, tokenLayer: [PALETTE, TOKENS], paletteFile: PALETTE };
    const palette = `.gv-palette() { --color-brand: #015999; --color-gray-0: #fff; --color-danger: #c0392b; }`;

    it('flags status colors read by features, accepts them through a role', () => {
        const tokens = `.gv-semantic-light() { --fg-danger: var(--color-danger); }\n.gv-semantic-dark() { --fg-danger: var(--color-danger); }\n.gv-features() { --a: var(--color-danger); --b: var(--fg-danger); }\n:root { .gv-palette(); .gv-semantic-light(); .gv-features(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens }), null, opts);

        expect(keys(result).filter((k) => k.startsWith('R8'))).toEqual(['R8 misc/tokens.less --color-danger']);
    });

    it('accepts palette reads in semantic mixins, flags the brand primitive in features and components', () => {
        const tokens = `.gv-semantic-light() { --bg: var(--color-gray-0); --brand: var(--color-brand); }\n.gv-semantic-dark() { --bg: #000; --brand: var(--color-brand); }\n.gv-features() { --f: var(--bg); --g: var(--color-brand); --h: var(--brand); }\n:root { .gv-palette(); .gv-semantic-light(); .gv-features(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens, 'views/a.less': `.a { color: var(--color-brand); background: var(--brand); }` }), null, opts);

        expect(keys(result).filter((k) => k.startsWith('R8'))).toEqual(['R8 misc/tokens.less --color-brand', 'R8 views/a.less --color-brand']);
    });

    it('flags palette reads in features, components and aliases', () => {
        const tokens = `.gv-semantic-light() { --bg: var(--color-gray-0); }\n.gv-semantic-dark() { --bg: #000; }\n.gv-features() { --f: var(--color-gray-0); }\n:root { .gv-palette(); .gv-semantic-light(); .gv-features(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(
            files({ [PALETTE]: palette, [TOKENS]: tokens, 'views/a.less': `.a { color: var(--color-gray-0); }`, [ALIASES]: `@neutral-0: var(--color-gray-0);\n@bg: var(--bg);` }),
            null,
            opts
        );

        expect(keys(result).filter((k) => k.startsWith('R8'))).toEqual([
            'R8 misc/tokens.less --color-gray-0',
            'R8 views/a.less --color-gray-0',
            'R8 misc/some-aliases.less --color-gray-0',
        ]);
    });
});

describe('R9 colors live in the palette only', () => {
    const PALETTE = 'misc/palette.less';
    const opts = { ...OPTS, tokenLayer: [PALETTE, TOKENS], paletteFile: PALETTE };
    const palette = `.gv-palette() { --color-brand: #015999; --color-gray-0: #fff; --color-gray-1000: rgb(0, 0, 0); }`;

    it('flags hex, numeric rgb and named colors outside the palette', () => {
        const tokens = `.gv-semantic-light() { --a: #fff; --b: rgba(255, 255, 255, 0.8); --c: color-mix(in srgb, var(--color-brand) 60%, black); }\n.gv-semantic-dark() { --a: 1; --b: 1; --c: 1; }\n:root { .gv-palette(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens }), null, opts);

        expect(keys(result).filter((k) => k.startsWith('R9'))).toEqual(['R9 misc/tokens.less --a', 'R9 misc/tokens.less --b', 'R9 misc/tokens.less --c']);
    });

    it('accepts palette references, derived alphas, keywords and sizes', () => {
        const tokens = `.gv-semantic-light() { --a: var(--color-gray-0); --b: rgb(from var(--color-gray-0) r g b / 80%); --c: transparent; --d: 1px solid var(--color-gray-0); --e: 0.15s ease; --f: none; }\n.gv-semantic-dark() { --a: 1; --b: 1; --c: 1; --d: 1; --e: 1; --f: 1; }\n:root { .gv-palette(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens }), null, opts);

        expect(rules(result)).not.toContain('R9');
    });
});

describe('applyBaseline', () => {
    const violations = [
        { rule: 'R1', file: 'a.less', token: '--x', line: 1, message: 'm' },
        { rule: 'R2', file: 'b.less', token: '--y', line: 2, message: 'm' },
    ];

    it('separates new errors, tolerated hits and stale entries', () => {
        const result = applyBaseline(violations, ['R1 a.less --x', 'R9 gone.less --z']);

        expect(result.errors).toEqual([violations[1]]);
        expect(result.tolerated).toEqual([violations[0]]);
        expect(result.stale).toEqual(['R9 gone.less --z']);
    });

    it('deduplicates identical keys with different lines', () => {
        const twice = [violations[0], { ...violations[0], line: 5 }];
        const result = applyBaseline(twice, ['R1 a.less --x']);

        expect(result.errors).toEqual([]);
        expect(result.stale).toEqual([]);
    });
});

describe('formatReport', () => {
    it('lists errors with file and line, and stale baseline entries', () => {
        const text = formatReport({
            errors: [{ rule: 'R1', file: 'a.less', token: '--x', line: 7, message: 'read without default' }],
            stale: ['R2 b.less --y'],
            tolerated: [],
            warnings: [{ rule: 'R5', token: '--dead', message: 'never read' }],
        });

        expect(text).toContain('R1 a.less:7 --x');
        expect(text).toContain('R2 b.less --y');
        expect(text).toContain('--dead');
    });
});

describe('R10 component colors', () => {
    const scan = (source, file = 'components/example.less', options = {}) =>
        checkFiles(
            files({
                'misc/variables.less': '@white: #fff; @black: #1a1a1a; @brand: rgb(1, 89, 153); @danger: red; @copy: @white; @fontSmall: 1.2rem; @RTL: ~"html[dir=rtl]";',
                [file]: source,
            }),
            null,
            options
        ).violations.filter((v) => v.rule === 'R10');

    it.each([
        '#fff',
        '#FFFF',
        '#123456',
        '#12345678',
        'black',
        'WHITE',
        'rgb(0, 0, 0)',
        'rgba(0 0 0 / 20%)',
        'hsl(120 50% 50%)',
        'hsla(.5turn, 50%, 50%, .5)',
        '@white',
        '@black',
        '@brand',
        '@danger',
        '@copy',
        'fade(@black, 18%)',
        'darken(@brand, 10%)',
        'mix(@white, @black, 30%)',
        '1px solid #ccc',
        'linear-gradient(#fff, var(--surface))',
        'var(--surface, white)',
        '~"rgba(0, 0, 0, .5)"',
    ])('rejects %s in property values', (value) => {
        expect(scan(`.a { .b { background: ${value}; } }`)).toEqual([expect.objectContaining({ rule: 'R10', file: 'components/example.less', line: 1 })]);
    });

    it.each([
        'color: var(--fg)',
        'color: var(--white)',
        'color: var(--black-text)',
        'color: var(--color-brand)',
        'background: transparent',
        'border-color: currentColor',
        'color: inherit',
        'font-size: @fontSmall',
        'color: rgb(from var(--fg) r g b / 18%)',
        'color: color-mix(in srgb, var(--brand) 60%, var(--fg))',
        'content: "black #fff @white rgba(0,0,0,1)"',
        'background: url(#fff)',
        'background: url("data:image/svg+xml,<svg fill=\'#fff\'/>")',
        'background: url(data:image/svg+xml,%23fff)',
        'width: 100px',
    ])('allows non-color statics, token expressions and data: %s', (declaration) => {
        expect(scan(`.a { ${declaration}; }`)).toEqual([]);
    });

    it('ignores comments, selectors, LESS definitions and mixin defaults', () => {
        expect(scan('/* color: #fff; */\n// background: @white;\n#fff { color: var(--fg); } @local: #fff; .m(@color: #fff) {}')).toEqual([]);
    });

    it('records custom properties, merged values and declarations without a final semicolon', () => {
        const result = scan('.@{RTL} {\n --_local: #fff;\n box-shadow+: 0 0 1px @black;\n color: white\n}');
        expect(result.map((v) => v.line)).toEqual([2, 3, 4]);
    });

    it('checks literal colors behind interpolated property names', () => {
        expect(scan('@property: color; .a { @{property}: #fff; }')).toHaveLength(1);
    });

    it.each([
        'misc/palette.less',
        'misc/tokens.less',
        'misc/variables.less',
        'admin/layout/basics.less',
        'cms/views/cmsCreatePage.less',
        'cms/includes/mediaSelect.less',
        'crowdsourcing/admin/adminCrowdCampaigns.less',
    ])('exempts the token layer, static definitions or approved backend file %s', (file) => {
        expect(scan('.a { color: #fff; }', file)).toEqual([]);
    });

    it.each([
        'misc/helper.less',
        'cms/modules/options.less',
        'cms/contentComponents/text.less',
        'cms/templates/cmsPage.less',
        'crowdsourcing/crowdsourcing.less',
        'crowdsourcing/annotations.less',
        'archives/archivesTree.less',
    ])('keeps public and shared styles in scope: %s', (file) => {
        expect(scan('.a { color: #fff; }', file)).toHaveLength(1);
    });

    it('requires a reason and an exact file or directory boundary for exceptions', () => {
        expect(scan('.a { color: #fff; }', 'components/example.less', { colorExemptions: [{ path: 'components/', reason: '' }] })).toHaveLength(1);
        expect(scan('.a { color: #fff; }', 'admin-copy/example.less')).toHaveLength(1);
        expect(scan('.a { color: #fff; }', 'cms/includes/mediaSelect.less.copy.less')).toHaveLength(1);
    });

    it('does not exempt backend files from other token rules', () => {
        const result = checkFiles(files({ 'admin/a.less': '.a { --public: #fff; color: var(--missing); }' }));
        expect(rules(result)).toEqual(expect.arrayContaining(['R1', 'R2']));
        expect(rules(result)).not.toContain('R10');
    });

    it('tolerates line movement but rejects changed selectors, properties or values', () => {
        const baseline = scan('.a { color: #fff; }').map(keyOf);
        expect(applyBaseline(scan('\n\n.a {\n color: #fff;\n}'), baseline).errors).toEqual([]);
        for (const changed of ['.b { color: #fff; }', '.a { background: #fff; }', '.a { color: #000; }']) {
            const result = applyBaseline(scan(changed), baseline);
            expect(result.errors).toHaveLength(1);
            expect(result.stale).toEqual(baseline);
        }
    });

    it('counts duplicates so a known declaration cannot be added again', () => {
        const once = scan('.a { color: #fff; }');
        const twice = scan('.a { color: #fff; color: #fff; }');
        expect(applyBaseline(twice, once.map(keyOf)).errors).toHaveLength(1);
        expect(applyBaseline(twice, twice.map(keyOf)).errors).toEqual([]);
        expect(applyBaseline(once, twice.map(keyOf)).stale).toEqual(once.map(keyOf));
        expect(applyBaseline(scan('.a { color: var(--fg); }'), once.map(keyOf)).stale).toEqual(once.map(keyOf));
    });
});

describe('real LESS tree', () => {
    it('has no violations beyond the baseline and no stale baseline entries', async () => {
        const result = await runTokenCheck();

        expect(result.errors.map((v) => `${v.rule} ${v.file}:${v.line} ${v.token}`)).toEqual([]);
        expect(result.stale).toEqual([]);
    }, 30000);
});

describe('reference theme token contract', () => {
    const core = files({
        'misc/palette.less': '.gv-palette() { --color-ink: #111; --color-brand: #015999; }',
        [TOKENS]: '.gv-semantic-light() { --fg: var(--color-ink); --brand: var(--color-brand); } .gv-semantic-dark() { --fg: var(--color-ink); --brand: var(--color-brand); }',
    });
    it.each([
        ['.item { color: var(--missing); }', 'R1'],
        ['.item { --public: 1; }', 'R2'],
        ['.item { & when (@colorModes = true) { color: var(--fg); } }', 'R4'],
        ['.item { color: var(--color-ink); }', 'R8'],
        ['.item { background: var(--color-brand); }', 'R8'],
        ['@old: #fff; .item { color: @old; }', 'R10'],
        ['.item { color: rebeccapurple; }', 'R10'],
    ])('rejects %s with %s', (source, rule) => {
        expect(rules(checkFiles(core, files({ 'item.less': source })))).toContain(rule);
    });
    it('requires paired theme roles and palette-only literals', () => {
        const theme = files({ 'misc/mixins.less': '.mx-theme-light() { --example: #fff; }', 'item.less': '.item { color: var(--example); }' });
        expect(rules(checkFiles(core, theme))).toEqual(expect.arrayContaining(['R7', 'R9']));
    });
    it('accepts theme palette, paired roles, brand reads in the theme roles and component reads', () => {
        const theme = files({
            'misc/mixins.less':
                '.mx-theme-palette() { --color-example: #abc; } .mx-theme-light() { --example: var(--color-example); --tint: var(--color-brand); } .mx-theme-dark() { --example: var(--color-example); --tint: var(--color-brand); }',
            'item.less': '.item { color: var(--example); background: var(--tint); }',
        });
        expect(checkFiles(core, theme).violations).toEqual([]);
    });
});

describe('theme token report', () => {
    const core = {
        light: new Map([
            ['--color-brand', '#015999'],
            ['--brand', 'var(--color-brand)'],
            ['--bg-general', '#f2f3f4'],
            ['--fg-general', '#212529'],
            ['--radius-button', '0'],
        ]),
        dark: new Map([
            ['--color-brand', '#015999'],
            ['--brand', 'var(--color-brand-dark)'],
            ['--color-brand-dark', '#49a5ff'],
            ['--bg-general', '#0a1124'],
            ['--fg-general', 'rgb(from #fff r g b / 80%)'],
            ['--radius-button', '0'],
        ]),
    };
    const theme = `
.mx-theme-light {
    color-scheme: light;
    --color-brand: #1565c0;
    --bg-general: #F2F3F4;
    --fg-general: #000;
    --border: 1px solid #ccc;
    --search-docstruct-bg: var(--color-brand);
}
.mx-theme-dark {
    --color-brand: #1565c0;
    --bg-general: #000;
}
.mx-theme-base() {
    --radius-button: 0;
}`;
    const rows = classifyThemeTokens([theme], core);
    const find = (token, mode) => rows.find((r) => r.token === token && r.mode === mode);

    it('marks overrides equal to the core default as redundant, case and shorthand insensitive', () => {
        expect(find('--bg-general', 'light').verdict).toBe('redundant');
        expect(find('--radius-button', 'base').verdict).toBe('redundant');
    });

    it('keeps differing values and flags literals for the theme palette', () => {
        expect(find('--bg-general', 'dark')).toMatchObject({ verdict: 'keep', literal: true, core: '#0a1124' });
        expect(find('--color-brand', 'light')).toMatchObject({ verdict: 'keep', literal: true });
    });

    it('reports renamed tokens with their new name and unknown ones without', () => {
        expect(find('--border', 'light')).toMatchObject({ verdict: 'renamed', hint: expect.stringContaining('--border-faint') });
        const renamedCore = { light: new Map([['--fg-link', '#015999']]), dark: new Map([['--fg-link', '#49a5ff']]) };
        const renamedRows = classifyThemeTokens(['.mx-theme-light() { --lk-general: #015999; }\n.mx-theme-dark() { --lk-general: #000; }'], renamedCore);
        expect(renamedRows.find((r) => r.mode === 'light')).toMatchObject({ verdict: 'redundant', hint: expect.stringContaining('--fg-link') });
        expect(renamedRows.find((r) => r.mode === 'dark')).toMatchObject({ verdict: 'renamed', hint: 'now --fg-link' });
    });

    it('reports light overrides without a dark counterpart', () => {
        expect(rows.filter((r) => r.verdict === 'pair').map((r) => r.token)).toEqual(['--fg-general']);
    });

    it('ignores non-token declarations and theme-only feature tokens keep their verdict', () => {
        expect(rows.some((r) => r.token === 'color-scheme')).toBe(false);
        expect(find('--search-docstruct-bg', 'light').verdict).toBe('unknown');
    });

    it('formats a grouped report with counts', () => {
        const text = formatThemeTokenReport(rows);
        expect(text).toContain('redundant');
        expect(text).toContain('renamed');
        expect(text).toMatch(/--bg-general\s+light/);
    });
});
