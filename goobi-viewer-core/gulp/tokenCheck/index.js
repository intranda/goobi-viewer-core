/**
 * Token check — six sentences the LESS tree keeps, one line per finding.
 *
 *   1  Values stay out of components: no colour, no static @-colour, no fixed text size. Read a role.
 *   2  What a component reads exists in the token layer.
 *   3  Tokens and mode branches live only in the token layer.
 *   4  Light and dark declare the same tokens.
 *   5  Colour literals only in the palette; the palette is read by the roles only.
 *   6  The admin binds exactly what admin pages read.
 *
 * Core findings fail the build, the watcher and `npm test`. Theme findings are printed as
 * information only: a theme may lag behind the core contract. The check reads the LESS sources
 * as written; gulp/publicTokens.json freezes the public token names (see publicTokens.test.js).
 *
 * Modules: constants (where things live, syntax), parse (reading LESS), code (the lines of a finding),
 * check (the rules), steps (numbered steps, terminal report), advice, renames (old names), report (HTML).
 */

const fs = require('fs');
const path = require('path');
const { checkTree } = require('./check');
const { COLOR_LITERAL, LESS_ROOT, MODULE_ROOT, THEME_LESS_ROOT, THEME_ROOT, themePaths } = require('./constants');
const { parseLess, readLessTree, withoutStrings } = require('./parse');
const { formatReport, stepFiles, themeSteps } = require('./steps');

/**
 * Reads the real trees and runs the check.
 *
 * @param {{moduleRoot?: string, lessRoot?: string, themeLessRoot?: string}} [options]
 * @returns {Promise<{core: Array, theme: {status: string, name: string, path: string, findings: Array}}>}
 */
async function runTokenCheck({ moduleRoot = MODULE_ROOT, lessRoot = LESS_ROOT, themeLessRoot = themePaths().lessRoot } = {}) {
    const core = readLessTree(path.resolve(moduleRoot, lessRoot));
    const themeDir = path.resolve(moduleRoot, themeLessRoot);
    const enabled = themePaths().enabled;
    const found = fs.existsSync(themeDir);
    const theme = enabled && found ? readLessTree(themeDir) : null;
    const themeRoot = path.resolve(themeDir, '..', '..');
    const templates =
        theme && fs.existsSync(themeRoot)
            ? new Map(
                  fs
                      .readdirSync(themeRoot)
                      .filter((f) => /^template\w*\.html$/.test(f))
                      .map((f) => [f, fs.readFileSync(path.join(themeRoot, f), 'utf8')])
              )
            : null;
    const result = checkTree(core, theme, templates);
    return {
        core: result.core,
        theme: { status: !enabled ? 'off' : found ? 'checked' : 'missing', name: path.basename(path.resolve(themeDir, '..', '..')), path: themeLessRoot, findings: result.theme },
    };
}

module.exports = {
    LESS_ROOT,
    THEME_ROOT,
    THEME_LESS_ROOT,
    MODULE_ROOT,
    themePaths,
    parseLess,
    withoutStrings,
    COLOR_LITERAL,
    readLessTree,
    checkTree,
    runTokenCheck,
    themeSteps,
    stepFiles,
    formatReport,
};
