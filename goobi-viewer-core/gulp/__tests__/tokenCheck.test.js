/**
 * Unit tests for the token check (gulp/tokenCheck.js) against inline LESS
 * fixtures, plus one run against the real LESS tree.
 */
const { parseLess, checkFiles, applyBaseline, formatReport, runTokenCheck } = require('../tokenCheck');

const TOKENS = 'misc/tokens.less';
const ALIASES = 'misc/aliases.less';
const OPTS = { tokenLayer: [TOKENS], aliasFile: ALIASES, themeMixins: 'misc/mixins.less' };

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

    it('accepts declared tokens and reads with fallback', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, 'widgets/w.less': `.w { color: var(--bg); border: var(--nope, red); }` }), null, OPTS);

        expect(rules(result)).not.toContain('R1');
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

        expect(keys(result)).toEqual(['R1 misc/tokens.less --missing']);
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

describe('R3 aliases point at existing tokens', () => {
    it('flags an alias to an unknown token and a token without alias', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, [ALIASES]: `@bg: var(--bg);\n@typo: var(--bgg);` }), null, OPTS);

        expect(keys(result)).toContain('R3 misc/aliases.less --bgg');
        expect(keys(result)).toContain('R3 misc/aliases.less --fg');
        expect(keys(result)).not.toContain('R3 misc/aliases.less --bg');
    });
});

describe('R3 aliases never shadow static LESS variables', () => {
    it('flags an alias whose name is also a static variable', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, 'misc/variables.less': `@bg: #fff;\n@fontSmall: 1.2rem;`, [ALIASES]: `@bg: var(--bg);\n@fg: var(--fg);\n@n-900: var(--n-900);` }), null, OPTS);

        expect(keys(result).filter((k) => k.startsWith('R3'))).toEqual(['R3 misc/aliases.less --bg']);
        expect(result.violations.find((v) => v.rule === 'R3').message).toContain('misc/variables.less');
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
                'misc/mixins.less': `.mx-theme-light { --bg: #fff; --orphan: 1; --theme-own: 1; }`,
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
    const palette = `.gv-palette() { --ci-color1: #015999; --gray-0: #fff; --danger: #c0392b; }`;

    it('flags status colors read by features, accepts them through a role', () => {
        const tokens = `.gv-semantic-light() { --fg-danger: var(--danger); }\n.gv-semantic-dark() { --fg-danger: var(--danger); }\n.gv-features() { --a: var(--danger); --b: var(--fg-danger); }\n:root { .gv-palette(); .gv-semantic-light(); .gv-features(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens }), null, opts);

        expect(keys(result).filter((k) => k.startsWith('R8'))).toEqual(['R8 misc/tokens.less --danger']);
    });

    it('accepts palette reads in semantic mixins and brand reads anywhere', () => {
        const tokens = `.gv-semantic-light() { --bg: var(--gray-0); }\n.gv-semantic-dark() { --bg: #000; }\n.gv-features() { --f: var(--bg); --g: var(--ci-color1); }\n:root { .gv-palette(); .gv-semantic-light(); .gv-features(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens, 'views/a.less': `.a { color: var(--ci-color1); }` }), null, opts);

        expect(rules(result)).not.toContain('R8');
    });

    it('flags palette reads in features, components and aliases', () => {
        const tokens = `.gv-semantic-light() { --bg: var(--gray-0); }\n.gv-semantic-dark() { --bg: #000; }\n.gv-features() { --f: var(--gray-0); }\n:root { .gv-palette(); .gv-semantic-light(); .gv-features(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(
            files({ [PALETTE]: palette, [TOKENS]: tokens, 'views/a.less': `.a { color: var(--gray-0); }`, [ALIASES]: `@neutral-0: var(--gray-0);\n@bg: var(--bg);` }),
            null,
            opts
        );

        expect(keys(result).filter((k) => k.startsWith('R8'))).toEqual(['R8 misc/tokens.less --gray-0', 'R8 views/a.less --gray-0', 'R8 misc/aliases.less --gray-0']);
    });
});

describe('R9 colors live in the palette only', () => {
    const PALETTE = 'misc/palette.less';
    const opts = { ...OPTS, tokenLayer: [PALETTE, TOKENS], paletteFile: PALETTE };
    const palette = `.gv-palette() { --ci-color1: #015999; --gray-0: #fff; --gray-1000: rgb(0, 0, 0); }`;

    it('flags hex, numeric rgb and named colors outside the palette', () => {
        const tokens = `.gv-semantic-light() { --a: #fff; --b: rgba(255, 255, 255, 0.8); --c: color-mix(in srgb, var(--ci-color1) 60%, black); }\n.gv-semantic-dark() { --a: 1; --b: 1; --c: 1; }\n:root { .gv-palette(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens }), null, opts);

        expect(keys(result).filter((k) => k.startsWith('R9'))).toEqual(['R9 misc/tokens.less --a', 'R9 misc/tokens.less --b', 'R9 misc/tokens.less --c']);
    });

    it('accepts palette references, derived alphas, keywords and sizes', () => {
        const tokens = `.gv-semantic-light() { --a: var(--gray-0); --b: rgb(from var(--gray-0) r g b / 80%); --c: transparent; --d: 1px solid var(--gray-0); --e: 0.15s ease; --f: none; }\n.gv-semantic-dark() { --a: 1; --b: 1; --c: 1; --d: 1; --e: 1; --f: 1; }\n:root { .gv-palette(); .gv-semantic-light(); }\n.color-theme-dark { .gv-semantic-dark(); }`;
        const result = checkFiles(files({ [PALETTE]: palette, [TOKENS]: tokens }), null, opts);

        expect(rules(result)).not.toContain('R9');
    });
});

describe('R10 components read theme-visible tokens through aliases', () => {
    it('flags var() reads of theme-visible tokens, accepts internal tokens and alias use', () => {
        const result = checkFiles(
            files({
                [TOKENS]: TOKEN_LAYER_OK,
                [ALIASES]: `@bg: var(--bg);\n@fg: var(--fg);\n@n-900: var(--n-900);`,
                'views/a.less': `.a { color: var(--bg); border-color: var(--nope, red); }`,
                'views/b.less': `.b { --_x: @bg; color: var(--_x, @fg); }`,
            }),
            null,
            OPTS
        );

        expect(keys(result).filter((k) => k.startsWith('R10'))).toEqual(['R10 views/a.less --bg', 'R10 views/a.less --nope']);
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

describe('real LESS tree', () => {
    it('has no violations beyond the baseline and no stale baseline entries', () => {
        const result = runTokenCheck();

        expect(result.errors.map((v) => `${v.rule} ${v.file}:${v.line} ${v.token}`)).toEqual([]);
        expect(result.stale).toEqual([]);
    }, 30000);
});
