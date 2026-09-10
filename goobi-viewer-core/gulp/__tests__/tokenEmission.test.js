const fs = require('fs');
const os = require('os');
const path = require('path');
const { checkFiles, checkCompiledTree } = require('../tokenCheck');

let root;
beforeEach(() => {
    root = fs.mkdtempSync(path.join(os.tmpdir(), 'viewer-token-emission-'));
});
afterEach(() => {
    fs.rmSync(root, { recursive: true, force: true });
});

async function compile(layer, component = '', options = {}) {
    fs.mkdirSync(path.join(root, 'misc'));
    fs.writeFileSync(path.join(root, 'misc/palette.less'), ':root { --palette: #fff; }');
    fs.writeFileSync(path.join(root, 'misc/tokens.less'), layer);
    fs.writeFileSync(path.join(root, 'component.less'), component);
    fs.writeFileSync(
        path.join(root, 'constructor.less'),
        `@import 'misc/palette.less'; @import ${options.reference ? '(reference)' : ''} 'misc/tokens.less'; @import 'component.less';`
    );
    return checkCompiledTree(root, options);
}

it.each([
    [':root { --known: 1; } .tokens() { --public: 2; }', '.component { .tokens(); }'],
    [':root { --known: 1; }', '@name: --known; .component { @{name}: blue; }'],
    [':root { .color-theme-dark { --known: 1; } }', ''],
    ['.tokens() { :root { --known: 1; } }', '.component { .tokens(); }'],
    [':root { --known: 1; }', ':root { --known: 2; }'],
    [':root { --known: 1 !important; }', ''],
])('rejects expanded public declarations outside the root contract: %s / %s', async (layer, component) => {
    expect((await compile(layer, component)).map((v) => v.rule)).toContain('R2');
});

it('accepts public tokens emitted on the admin scope of the token layer', async () => {
    expect((await compile(':root { --known: 1; }\n:root:has(#admin) { --known: 2; }')).map((v) => v.rule)).not.toContain('R2');
});

it('finds an unknown read behind a LESS alias and reports the component source', async () => {
    const result = await compile(':root { --known: 1; }', '@alias: var(--missing); .component { color: @alias; }');
    expect(result).toEqual(expect.arrayContaining([expect.objectContaining({ rule: 'R1', file: 'component.less', token: '--missing', line: 1 })]));
});

it('does not count declarations suppressed by reference imports', async () => {
    expect(await compile(':root { --known: 1; }', '.component { color: var(--known); }', { reference: true })).toEqual(
        expect.arrayContaining([expect.objectContaining({ rule: 'R1', token: '--known' })])
    );
});

it('detects interpolated mode selectors in components', async () => {
    expect((await compile(':root { --known: 1; }', '@mode: color-theme-dark; .@{mode} .component { color: red; }')).map((v) => v.rule)).toContain('R4');
});

it.each(['red', 'oklch(60% 0.2 30)', '@literal'])('rejects emitted color %s in the token layer', async (value) => {
    expect((await compile(`@literal: #f00; :root { --known: ${value}; }`)).map((v) => v.rule)).toContain('R9');
});

it('accepts guarded and detached mixins that emit only root tokens', async () => {
    const layer = '.tokens() when (1 = 1) { --known: 1; } @rules: { --other: 2; }; :root { .tokens(); @rules(); }';
    expect(checkFiles(new Map([['misc/tokens.less', layer]])).violations).toEqual([]);
    expect(await compile(layer, '.component { color: var(--known); }')).toEqual([]);
});

it('distinguishes quoted content from LESS escaped CSS syntax', async () => {
    expect(await compile(':root { --known: 1; }', '.component { content: "var(--missing)"; }')).toEqual([]);
});

it('rejects local public token overrides in themes but accepts private properties', async () => {
    expect(await compile(':root { --known: 1; }', '.component { --known: 2; --_theme-local: 3; }', { theme: true, tokens: new Set(['--known']) })).toEqual([
        expect.objectContaining({ rule: 'R2', token: '--known', file: 'component.less' }),
    ]);
});

it('checks a dark-only mixin for its missing light counterpart', () => {
    expect(checkFiles(new Map([['misc/tokens.less', '.only-dark() { --known: 1; }']])).violations).toEqual([expect.objectContaining({ rule: 'R7', token: '--known' })]);
});

it('checks reads in alias definitions without treating ordinary strings as reads', () => {
    const result = checkFiles(new Map([['component.less', '@alias: var(--missing); .component { content: "var(--text)"; color: ~"var(--escaped)"; }']]));
    expect(result.violations.filter((v) => v.rule === 'R1').map((v) => v.token)).toEqual(['--missing', '--escaped']);
});

it('accepts whitespace before the origin of relative colors', () => {
    const result = checkFiles(
        new Map([
            ['misc/palette.less', ':root { --ci-color1: #fff; }'],
            ['misc/tokens.less', ':root { --known: rgb( from var(--ci-color1) r g b / 20%); }'],
        ])
    );
    expect(result.violations).toEqual([]);
});

it('compares guarded light/dark mixins by their names', () => {
    const result = checkFiles(
        new Map([
            [
                'misc/tokens.less',
                `
        .paired-light() when (1 = 1) { --known: 1; }
        .paired-dark() when (1 = 1) { --known: 2; }
    `,
            ],
        ])
    );
    expect(result.violations).toEqual([]);
});

it('reports LESS compilation failures as check errors', async () => {
    expect(await compile(':root { --known: @undefined; }')).toEqual([expect.objectContaining({ rule: 'LESS', file: 'misc/tokens.less' })]);
});

it.each(['red', '#fff', '@literal', '~"rgb(0 0 0)"'])('rejects emitted theme colors hidden behind aliases or escapes: %s', async (value) => {
    const result = await compile(':root { --known: 1; }', `@literal: #fff; .component { color: ${value}; }`, { theme: true });
    expect(result).toEqual(expect.arrayContaining([expect.objectContaining({ rule: 'R10', file: 'component.less' })]));
});

it('detects a mode branch hidden in a detached theme mixin', async () => {
    const result = await compile(':root { --known: 1; }', '.mode(@rules) { @media (prefers-color-scheme: dark) { @rules(); } } .component { .mode({ color: var(--known); }); }', {
        theme: true,
    });
    expect(result.map((v) => v.rule)).toContain('R4');
});
