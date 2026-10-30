/**
 * Token check — where things live in the LESS trees and the syntax the check reads: file names, selectors, regexes.
 */

const path = require('path');
const less = require('less');

const MODULE_ROOT = path.resolve(__dirname, '../..');

const LESS_ROOT = 'src/main/resources/META-INF/resources/resources/css/less';

const THEME_ROOT = '../goobi-viewer-theme-reference/WebContent/resources/themes/reference';

const THEME_LESS_ROOT = `${THEME_ROOT}/css/less`;

/**
 * Which theme is checked: GV_THEME_LESS_ROOT (a theme's css/less directory) or the reference
 * submodule. GV_THEMECHECK=0 skips the theme.
 *
 * @returns {{lessRoot: string, root: string, enabled: boolean}}
 */
function themePaths() {
    const lessRoot = process.env.GV_THEME_LESS_ROOT || THEME_LESS_ROOT;
    return { lessRoot, root: lessRoot.replace(/[\\/]css[\\/]less[\\/]?$/, ''), enabled: process.env.GV_THEMECHECK !== '0' };
}

const PALETTE_FILE = 'misc/palette.less';

const TOKENS_FILE = 'misc/tokens.less';

const ADMIN_PALETTE_FILE = 'misc/paletteAdmin.less';

const ADMIN_TOKENS_FILE = 'misc/tokensAdmin.less';

const VARIABLES_FILE = 'misc/variables.less';

const CORE_DEFAULTS_FILE = 'misc/core-defaults.less';

const TOKEN_LAYER = [PALETTE_FILE, TOKENS_FILE, ADMIN_PALETTE_FILE, ADMIN_TOKENS_FILE];

const PALETTE_FILES = [PALETTE_FILE, ADMIN_PALETTE_FILE];

const ADMIN_FILES = [ADMIN_PALETTE_FILE, ADMIN_TOKENS_FILE];

const ROOT_SELECTORS = [':root', '.color-theme-dark', '.color-theme-light', ':root:has(#admin)'];

const TOKEN_SELECTORS = new Set([...ROOT_SELECTORS, '@media (prefers-color-scheme: dark)', '@media (prefers-color-scheme: light)']);

const STATIC_COLOUR_TREES = /^(?:admin\/|cms\/views\/|cms\/includes\/mediaSelect\.less$|crowdsourcing\/admin\/)/;

const FRONTEND_ONLY = /^(?:views\/|archives\/|cms\/(?:templates|components|tags)\/|crowdsourcing\/(?!admin\/)|widgets\/(?!widgets\.less$))/;

const adminRelevant = (file) => !FRONTEND_ONLY.test(file) && file !== PALETTE_FILE && file !== TOKENS_FILE;

const THEME_LAYER = /^(?:misc\/(?:mixins|palette|tokens|variables)\.less$|subthemes\/[^/]+\/subThemeConstructor\.less$)/;

const THEME_PALETTE_MIXIN = /^\.mx-theme-[\w-]*palette\(\)$/;

const ROLE_PREFIX = /^--(?:bg|fg|border|outline|shadow|display|filter)-/;

const PALETTE_WORD = /-(?:bg|fg|border|text|surface|hover|pressed|selected|disabled|active|fill)(?:-|$)/;

const PALETTE_WORD_EXEMPT = /^--color-(?:on-|(?:success|danger|warning|info)-)/;

const LIGHT_DARK = /\blight-dark\(/;

const TWIN_PARTNERS = /^--(?:fg-on-|fg-white$|fg-black$|bg-document$|fg-document$|bg-input$|outline-focus-inner$|bg-scrim$|fg-placeholder$)/;

const SUBTHEME_CONSTRUCTOR = /^subthemes\/[^/]+\/subThemeConstructor\.less$/;

const layerHome = (file) => file === TOKENS_FILE || file === PALETTE_FILE || SUBTHEME_CONSTRUCTOR.test(file);

const NAMED_COLOURS = Object.keys(less.data.colors).join('|');

const SEMANTIC_MIXIN = /^\.[\w-]*semantic[\w-]*\(\)$/;

const MODE_MIXIN = /^(\.[\w-]+)-(light|dark)\(\)$/;

const MIXIN_DEFINITION = /^\.[\w-]+\([^)]*\)(?: when .+)?$/;

const DETACHED_DEFINITION = /^@[\w-]+:$/;

const DECLARATION = /^(--[\w-]+)\s*:\s*([\s\S]*)$/;

const STATIC = /^@([\w-]+)\s*:/;

const READ = /var\(\s*(--[\w-]+)\s*(,)?/g;

const AT_USE = /@([\w-]+)/g;

const INTERNAL = /^--_/;

const MODE_BRANCH = /prefers-color-scheme|\.color-theme-(dark|light)|@colorModes|^\.mx-(?:dark|light)-mode-rules$/;

const COLOR_LITERAL = new RegExp(`#[0-9a-f]{3,8}\\b|\\b(?:rgba?|hsla?|hwb|lab|lch|oklab|oklch|color)\\((?!\\s*from\\b)|(?:^|[\\s,(])(?:${NAMED_COLOURS})(?=[\\s,)]|$)`, 'i');

const COMPONENT_COLOR = new RegExp(
    `#[\\da-f]{3}(?:[\\da-f](?:[\\da-f]{2}(?:[\\da-f]{2})?)?)?\\b|\\b(?:rgba?|hsla?)\\(\\s*[+-]?(?:\\d|\\.\\d)|(?:^|[\\s,(])(?:${NAMED_COLOURS})(?=[\\s,)!]|$)`,
    'i'
);

const FIXED_TYPE_SIZE = /(?:\d*\.)?\d+(?:px|rem|pt|pc|in|cm|mm|q|vw|vh|vmin|vmax)\b/i;

const FIXED_GEOMETRY_NOTE = '// Fixed icon/marker geometry; independent of text scale.';

const FONT_CONSTANT = /^font(?:Small|Medium|Normal|Large|XLarge)$/;

const isTypographyProperty = (name) => name === 'font-size' || name === 'font';

module.exports = {
    MODULE_ROOT,
    LESS_ROOT,
    THEME_ROOT,
    THEME_LESS_ROOT,
    themePaths,
    PALETTE_FILE,
    TOKENS_FILE,
    ADMIN_PALETTE_FILE,
    ADMIN_TOKENS_FILE,
    VARIABLES_FILE,
    CORE_DEFAULTS_FILE,
    TOKEN_LAYER,
    PALETTE_FILES,
    ADMIN_FILES,
    ROOT_SELECTORS,
    TOKEN_SELECTORS,
    STATIC_COLOUR_TREES,
    FRONTEND_ONLY,
    adminRelevant,
    THEME_LAYER,
    THEME_PALETTE_MIXIN,
    ROLE_PREFIX,
    PALETTE_WORD,
    PALETTE_WORD_EXEMPT,
    LIGHT_DARK,
    TWIN_PARTNERS,
    SUBTHEME_CONSTRUCTOR,
    layerHome,
    NAMED_COLOURS,
    SEMANTIC_MIXIN,
    MODE_MIXIN,
    MIXIN_DEFINITION,
    DETACHED_DEFINITION,
    DECLARATION,
    STATIC,
    READ,
    AT_USE,
    INTERNAL,
    MODE_BRANCH,
    COLOR_LITERAL,
    COMPONENT_COLOR,
    FIXED_TYPE_SIZE,
    FIXED_GEOMETRY_NOTE,
    FONT_CONSTANT,
    isTypographyProperty,
};
