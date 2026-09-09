/**
 * Unit tests for the token check (gulp/tokenCheck.js) against inline LESS
 * fixtures, plus one run against the real LESS tree.
 */
const { parseLess, checkFiles, applyBaseline, formatReport, runTokenCheck } = require('../tokenCheck');

const TOKENS = 'misc/tokens.less';
const ALIASES = 'misc/variables.less';
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
    it('flags an alias to an unknown token and warns about tokens without alias', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK, [ALIASES]: `@bg: var(--bg);\n@typo: var(--bgg);` }), null, OPTS);

        expect(keys(result)).toContain('R3 misc/variables.less --bgg');
        expect(warnings(result)).toContain('W3 --fg');
        expect(warnings(result)).not.toContain('W3 --bg');
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

        expect(result.violations.map((v) => [v.rule, v.token])).toEqual([['R6', '--theme-global']]);
        expect(result.violations[0].message).toContain('token layer');
    });

    it('skips theme rules when no theme is given', () => {
        const result = checkFiles(files({ [TOKENS]: TOKEN_LAYER_OK }), null, OPTS);

        expect(result.violations).toEqual([]);
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
