/**
 * Centralized Prettier configuration with filetype-specific overrides.
 */
module.exports = {
    // === Global defaults ===
    useTabs: false,
    printWidth: 120,
    tabWidth: 4,
    singleQuote: true,
    semi: true,
    trailingComma: 'es5',
    bracketSpacing: true,
    arrowParens: 'always',
    endOfLine: 'lf',

    // === File-specific overrides ===
    overrides: [
        // JavaScript / TypeScript
        {
            files: ['**/*.{js,cjs,mjs,jsx,ts,tsx}'],
            options: {
                jsxSingleQuote: false,
            },
        },
        // HTML / XHTML
        {
            files: ['**/*.{html,xhtml}'],
            options: {
                singleAttributePerLine: true,
                bracketSameLine: true,
                htmlWhitespaceSensitivity: 'ignore',
            },
        },
        // JSON / YAML
        {
            files: ['**/*.{json,yml,yaml}'],
            options: {
                trailingComma: 'none',
                tabWidth: 2,
            },
        },
        // Markdown
        {
            files: ['**/*.md'],
            options: {
                proseWrap: 'always',
            },
        },
    ],
};
