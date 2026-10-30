/**
 * Unit tests for the complete-migration advice (gulp/tokenAdvice.js).
 */
const { parseLess } = require('..');
const { parseColour, adviceContext, adviseColour, adviseFontSize, bindings } = require('../advice');

const parsed = (files) => new Map(Object.entries(files).map(([file, source]) => [file, parseLess(source)]));

const context = adviceContext({
    core: parsed({
        'misc/variables.less': '@fontNormal: 1.6rem; @fontXLarge: 2.1rem;',
        'misc/palette.less': '.gv-palette() { --color-neutral-0: #fff; --color-neutral-200: #e2e6e9; --color-neutral-900: #15212a; }',
        'misc/tokens.less':
            '.gv-base() { --font-scale: 1; --font-size-body: calc(@fontNormal * var(--font-scale)); --font-size-heading-small: calc(@fontXLarge * var(--font-scale)); }\n' +
            '.gv-semantic-light() { --fg-default: var(--color-neutral-900); --bg-surface: var(--color-neutral-0); --border-default: var(--color-neutral-200); --fg-danger: #b33226; }',
    }),
    theme: parsed({ 'misc/variables.less': '@black: #1a1a1a; @ci-color1: #003064; @red: #d4004b; @shade: rgba(0, 0, 0, 0.3);' }),
});

describe('token advice', () => {
    test('reads the colour notations a theme writes', () => {
        expect(parseColour('#fff')).toEqual({ r: 255, g: 255, b: 255, a: 1 });
        expect(parseColour('#00000080').a).toBeCloseTo(0.5, 2);
        expect(parseColour('rgba(0, 0, 0, .3)')).toEqual({ r: 0, g: 0, b: 0, a: 0.3 });
        expect(parseColour('white')).toEqual({ r: 255, g: 255, b: 255, a: 1 });
        expect(parseColour('transparent').a).toBe(0);
        expect(parseColour('solid')).toBeNull();
        expect(parseColour('darken(#c7254e, 10%)')).toEqual({ r: 156, g: 29, b: 61, a: 1 });
        expect(parseColour('fade(#c7254e, 15%)').a).toBeCloseTo(0.15, 2);
    });

    test('suggests the brand role for the brand colour and the nearest grey role for a grey', () => {
        expect(adviseColour('background: @ci-color1', context, 'layout/header.less').text).toBe('@ci-color1 → var(--bg-brand)');
        expect(adviseColour('color: @black', context, 'layout/header.less').text).toBe('@black → var(--fg-default)');
        expect(adviseColour('border: 1px solid #ddd', context, 'layout/header.less').text).toBe('#ddd → var(--border-default)');
    });

    test('gives a colour no general role fits a theme token named after its component', () => {
        expect(adviseColour('background: @red', context, 'cms/templates/08_templateCollectionTiled.less').text).toBe('@red → var(--template-collection-tiled-bg), a theme token');
        expect(adviseColour('color: @shade', context, 'layout/header.less').text).toBe('@shade → var(--header-fg), a theme token');
    });

    test('names a theme token after a file with dots cleanly, and a colour function of a literal after the colour', () => {
        const own = adviceContext({ core: parsed({}), theme: parsed({}) });
        expect(adviseColour('background: #fff', own, 'kuni2/mapbox-gl_v0.46.0.less').text).toBe('#fff → var(--mapbox-gl_v0-46-0-bg), a theme token');
        const faded = adviseColour('background-color: fade(white, 20%)', own, 'views/searchList.less');
        expect(faded.replacements).toEqual([
            { from: 'fade(white, 20%)', role: '--search-list-bg', value: 'rgba(255, 255, 255, 0.2)', source: 'fade(white, 20%)', label: null, constants: [], theme: true },
        ]);
        expect(bindings(faded.replacements, own).palette.map((p) => p.name)).toEqual(['--color-theme-ffffff-a20']);
    });

    test('replaces a LESS colour function as a whole and gives every colour of a component its own theme token', () => {
        const own = adviceContext({ core: parsed({}), theme: parsed({ 'misc/variables.less': '@red: #d4004b; @navy: #002b5c;' }) });
        const first = adviseColour('background: darken(@red, 10%)', own, 'layout/footer.less');
        expect(first.replacements).toEqual([
            { from: 'darken(@red, 10%)', role: '--footer-bg', value: '#a10039', source: 'darken(@red, 10%)', label: 'red-darken-10', constants: ['red'], theme: true },
        ]);
        expect(adviseColour('background: @navy', own, 'layout/footer.less').text).toBe('@navy → var(--footer-bg-navy), a theme token');
        expect(adviseColour('border: 1px solid darken(@red, 10%)', own, 'layout/footer.less').text).toBe('darken(@red, 10%) → var(--footer-border), a theme token');
    });

    test('maps fixed text sizes to the nearest text role, a fluid size only as a whole', () => {
        expect(adviseFontSize('font-size: 16px', context)).toEqual({ text: 'var(--font-size-body)', from: '16px', to: 'var(--font-size-body)', note: null });
        expect(adviseFontSize('font-size: 2rem', context).text).toBe('var(--font-size-heading-small)  (2.1rem instead of 2rem)');
        expect(adviseFontSize('font-size: @fontNormal', context)).toMatchObject({ from: '@fontNormal', to: 'var(--font-size-body)' });
        expect(adviseFontSize('font-size: clamp(30px, 3vw, 60px)', context)).toEqual({ text: expect.stringContaining('fluid size') });
        expect(adviseFontSize('font-size: 1.2em', context).text).toContain('relative size');
    });

    test('counts the brand colour as set, as the required steps set it', () => {
        expect(context.brandHint).toEqual({ value: '#003064', source: '@ci-color1' });
        expect(context.colourOf('var(--color-brand)')).toEqual({ r: 0, g: 48, b: 100, a: 1 });
    });

    test('binds every colour once and names a role that would need two values', () => {
        const replacements = [
            ...adviseColour('color: @black', context, 'a.less').replacements,
            ...adviseColour('color: #1a1a1a', context, 'b.less').replacements,
            ...adviseColour('color: #222', context, 'c.less').replacements,
        ];
        const bound = bindings(replacements, context);
        expect(bound.palette.map((p) => `${p.name}: ${p.value}`)).toEqual(['--color-theme-222222: #222222', '--color-theme-black: #1a1a1a']);
        expect(bound.roles).toEqual([
            {
                role: '--fg-default',
                palette: '--color-theme-black',
                uses: 2,
                theme: false,
                others: [{ value: '#222222', source: null, palette: '--color-theme-222222', uses: 1, theme: false }],
            },
        ]);
    });
});
