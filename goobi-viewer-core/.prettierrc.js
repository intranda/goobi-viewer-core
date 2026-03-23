/**
 * Centralized Prettier configuration with filetype-specific overrides.
 * Preserves existing indentation, spaces, and line width.
 */
module.exports = {
    // === Global defaults ===
    useTabs: false,
    tabWidth: 4,
    printWidth: 120,
    singleQuote: true,
    semi: true,
    trailingComma: 'es5',
    bracketSpacing: true,
    arrowParens: 'always',
    endOfLine: 'lf',
    embeddedLanguageFormatting: 'off',

    // Plugins
    // Wrapper around @prettier/plugin-xml that prevents embedded JS formatter
    // from reformatting HTML strings inside <script> tags in XHTML files.
    plugins: [require.resolve('./prettier-plugin-xhtml-script-fix.mjs')],

    // === File-specific overrides ===
    overrides: [
        // JavaScript / TypeScript
        {
            files: ['**/*.{js,cjs,mjs,jsx,ts,tsx}'],
            options: {
                jsxSingleQuote: false,
                useTabs: false,
                tabWidth: 4,
                printWidth: 120,
                semi: true,
                singleQuote: true,
            },
        },

        // HTML (non-JSF)
        {
            files: ['**/*.html'],
            options: {
                singleAttributePerLine: true,
                bracketSameLine: true,
                htmlWhitespaceSensitivity: 'ignore',
                useTabs: false,
                tabWidth: 4,
                printWidth: 120,
            },
        },

        // XHTML / XML (JSF / Facelets)
        {
            files: ['**/*.{xhtml,xml}'],
            options: {
                parser: 'xml',
                xmlSelfClosingSpace: true,
                singleAttributePerLine: true,
                bracketSameLine: true,
                xmlWhitespaceSensitivity: 'ignore',
                useTabs: false,
                tabWidth: 4,
                printWidth: 120,
                semi: true,
                singleQuote: true,
            },
        },

        // JSON / YAML
        {
            files: ['**/*.{json,yml,yaml}'],
            options: {
                trailingComma: 'none',
                tabWidth: 2,
                printWidth: 120,
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
