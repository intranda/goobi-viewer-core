/**
 * Centralized Prettier configuration with filetype-specific overrides.
 * Comments are allowed because this is a JS config file.
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

        // CSS / SCSS / LESS
        {
            files: ['**/*.{css,scss,less}'],
            options: {},
        },

        // HTML / XHTML
        {
            files: ['**/*.{html,xhtml}'],
            options: {
                singleAttributePerLine: true,
                bracketSameLine: true,
            },
        },

        // JSON / YAML
        {
            files: ['**/*.{json,yml,yaml}'],
            options: {
                trailingComma: 'none',
                tabWidth: 2,
                bracketSpacing: false,
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
