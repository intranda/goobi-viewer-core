/* Unit tests for the theme update report (gulp/themeTokens.js). */
const { classifyThemeTokens, formatThemeTokenReport } = require('../themeTokens');

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
