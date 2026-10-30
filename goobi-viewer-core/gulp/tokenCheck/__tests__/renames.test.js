/** Unit tests for the theme update report (gulp/tokenCheck/renames.js). */
const { classifyThemeTokens, formatThemeTokenReport, successor } = require('../renames');

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

    test('marks overrides equal to the core default as redundant, case and shorthand insensitive', () => {
        expect(find('--bg-general', 'light').verdict).toBe('redundant');
        expect(find('--radius-button', 'base').verdict).toBe('redundant');
    });

    test('keeps differing values and flags literals for the theme palette', () => {
        expect(find('--bg-general', 'dark')).toMatchObject({ verdict: 'keep', literal: true, core: '#0a1124' });
        expect(find('--color-brand', 'light')).toMatchObject({ verdict: 'keep', literal: true });
    });

    test('reports renamed tokens with their new name and unknown ones without', () => {
        expect(find('--border', 'light')).toMatchObject({ verdict: 'renamed', hint: expect.stringContaining('--border-subtle') });
        const renamedCore = { light: new Map([['--fg-link', '#015999']]), dark: new Map([['--fg-link', '#49a5ff']]) };
        const renamedRows = classifyThemeTokens(['.mx-theme-light() { --lk-general: #015999; }\n.mx-theme-dark() { --lk-general: #000; }'], renamedCore);
        expect(renamedRows.find((r) => r.mode === 'light')).toMatchObject({ verdict: 'redundant', hint: expect.stringContaining('--fg-link') });
        expect(renamedRows.find((r) => r.mode === 'dark')).toMatchObject({ verdict: 'renamed', hint: 'now --fg-link' });
    });

    test('follows a rename chain to the first name the core declares', () => {
        const declares =
            (...names) =>
            (name) =>
                names.includes(name);
        expect(successor('--widget-icon-color-topbar', declares('--fg-brand'))).toBe('--fg-brand');
        expect(successor('--widget-icon-color-topbar', declares('--widget-topbar-icon'))).toBe('--widget-topbar-icon');
        expect(successor('--bg-success-strong', declares('--bg-success', '--bg-success-subtle'))).toBe('--bg-success');
        expect(successor('--border', declares('--border-subtle'))).toBe('--border-subtle (a color now — readers write border: 1px solid var(--border-subtle))');
        expect(successor('--toc-page-border', declares())).toBeNull();
        expect(successor('--not-a-renamed-token', declares())).toBeNull();
    });

    test('marks a component token that dissolved into a shared role as folded, not renamed', () => {
        const roleCore = {
            light: new Map([
                ['--fg-brand', 'var(--color-brand)'],
                ['--color-brand', '#015999'],
            ]),
            dark: new Map([
                ['--fg-brand', 'var(--color-brand)'],
                ['--color-brand', '#015999'],
            ]),
        };
        const [row] = classifyThemeTokens([':root { --widget-icon-color-topbar: #1a74b0; }'], roleCore);
        expect(row).toMatchObject({ verdict: 'folded', hint: expect.stringContaining('--fg-brand') });
        expect(formatThemeTokenReport([row])).toContain('folded');
        const statusCore = {
            light: new Map([
                ['--bg-success', '#087f0d'],
                ['--bg-success-subtle', '#d4edda'],
            ]),
            dark: new Map([
                ['--bg-success', '#80d68a'],
                ['--bg-success-subtle', '#183c29'],
            ]),
        };
        expect(classifyThemeTokens(['.mx-theme-light { --bg-success: #dff0d8; }'], statusCore)[0]).toMatchObject({
            verdict: 'renamed',
            hint: expect.stringContaining('now --bg-success-subtle'),
        });
        const surfaceCore = { light: new Map([['--bg-surface', '#fff']]), dark: new Map([['--bg-surface', '#000']]) };
        expect(classifyThemeTokens([':root { --immersive-chrome-bg: #222; }'], surfaceCore)[0]).toMatchObject({ verdict: 'folded', hint: expect.stringContaining('--bg-surface') });
    });

    test('reports light overrides without a dark counterpart', () => {
        expect(rows.filter((r) => r.verdict === 'pair').map((r) => r.token)).toEqual(['--fg-general']);
    });

    test('ignores non-token declarations and theme-only feature tokens keep their verdict', () => {
        expect(rows.some((r) => r.token === 'color-scheme')).toBe(false);
        expect(find('--search-docstruct-bg', 'light').verdict).toBe('unknown');
    });

    test('formats a grouped report with counts', () => {
        const text = formatThemeTokenReport(rows);
        expect(text).toContain('redundant');
        expect(text).toContain('renamed');
        expect(text).toMatch(/--bg-general\s+light/);
    });
});
