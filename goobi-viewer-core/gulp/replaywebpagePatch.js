/*
Patch generator for the ReplayWeb.page UI bundle.

WHY THIS EXISTS
---------------
ReplayWeb.page renders a "Download Archive" entry in its replay dropdown menu.
The viewer embeds the player with noMediaDownload="true" (see
resources/components/webarchive.xhtml). Upstream honours that flag for
"Select Media to Download" only — "Download Archive" stays visible. This module
adds the missing guard.

Note what this does NOT do: the URLs tab still offers "Download Resource" per
resource, the Archive Info dialog still links the archive's files, and the
archive URL sits in the DOM as the source= attribute anyway. The patch is UI
hygiene, not an access control. Real download protection belongs on the server
side of the web archive endpoint.

WHY THE OUTPUT IS NAMED ui.js
-----------------------------
The service worker builds the replay iframe's index page itself and hardcodes
a script tag for "./ui.js":

    getIndexHtml(e){const t=e.get("indexScript")||"./ui.js", …}

The UI never sets the indexScript param, so an unpatched ui.js sitting next to
a differently-named patched copy would be the one the iframe loads. This used
to "work" only because ui.js was absent from the repo and a fallback in
onLoad() injected the outer script URL instead. That fallback is now
unreachable by design: with ui.js present, the iframe finds replay-app-main
already defined and never injects anything. So the patched bundle IS ui.js —
outer <script> tag and service worker index page then load the same file.

The generated file must not be edited by hand; it is overwritten on every
copyDeps run. ReplayWeb.page is licensed under AGPL v3, whose §5(a) requires
modified files to carry a notice and a date — that is what the banner provides.
PATCH_DATE is a maintained constant, not a runtime value, so repeated runs do
not produce diff noise.

TWO INVARIANTS THIS DEPENDS ON
------------------------------
1. copy-deps runs `gulp.series(copyDependencies, patchReplayWebPage)` — in that
   order. Before this module existed, the differing file name (ui-modded.js)
   was what kept copyDeps from overwriting the patch; now it is the fact that
   the patch step runs last. Reversing or parallelising the steps breaks it.
2. The generated ui.js is a TRACKED artifact. CI builds the WAR with
   -DskipTests, i.e. without Node, so nothing regenerates it there. Forgetting
   `git add` ships a WAR without the file.

Paths here are relative, so generate() must run with the module directory
(goobi-viewer-core/) as CWD. That is how `npm run copyDeps` invokes it.

IF A VERSIONED FILE NAME IS EVER NEEDED
---------------------------------------
The name ui.js is not a dead end: the service worker reads its parameters from
its own script URL, and swName is an attribute of <replay-web-page>. Setting
swName="sw.js?indexScript=./ui-x.js&amp;serveIndex=1" yields
"sw.js?indexScript=./ui-x.js&serveIndex=1?serveIndex=1", which URLSearchParams
resolves correctly, and the iframe inherits swName. Awkward because of the
double "?", but it works — worth knowing if cache busting via file name ever
becomes necessary.

HOW TO UPDATE replaywebpage
---------------------------
  1. bump the version in package.json
  2. npm install
  3. npm run copyDeps   -> copies sw.js, regenerates ui.js
  4. npm test           -> verifies the patch survived
  5. open a web archive in the viewer and check the dropdown menu

IF THE BUILD FAILS HERE
-----------------------
The upstream markup changed. Recover the anchor like this:
  1. open node_modules/replaywebpage/ui.js
  2. find the "Download Archive" occurrence inside the dropdown menu — it is
     the one at the HIGHER offset; the earlier one lives in rwp-embed-receipt,
     which only renders for embed="replay-with-info" and is unused here
  3. the enclosing ${ ... }? expression is the new ANCHOR — copy it verbatim,
     from the opening ${ up to and including the trailing ?
  4. PATCHED derives from ANCHOR automatically; there is no second constant
  5. bump PATCH_DATE, re-run npm run copyDeps and review the diff
*/

const fs = require('fs');
const path = require('path');

// The complete condition expression gating the "Download Archive" menu entry.
// Verified to occur exactly once in replaywebpage 2.5.0.
const ANCHOR =
    '${!this.editable&&(null===(e=this.downloadUrl)||void 0===e?void 0:e.startsWith("http://"))||(null===(t=this.downloadUrl)||void 0===t?void 0:t.startsWith("https://"))?';

// `noMediaDownloadUI` is an existing upstream embed option, set when the embed
// carries the noMediaDownload attribute.
const GUARD = '!this.embedOpts.noMediaDownloadUI&&(';

// Derived from ANCHOR so the two can never drift apart: drop the leading "${"
// and the trailing "?", wrap the remainder in the guard's parenthesis.
const PATCHED = '${' + GUARD + ANCHOR.slice(2, -1) + ')?';

// Date of the last change to the patch itself. Bump when ANCHOR/GUARD change.
const PATCH_DATE = '2026-08-12';

const UPSTREAM_DIR = 'node_modules/replaywebpage';
const UPSTREAM_UI = UPSTREAM_DIR + '/ui.js';
const UPSTREAM_PKG = UPSTREAM_DIR + '/package.json';
const OUTPUT_FILE = 'src/main/resources/META-INF/resources/resources/javascript/libs/replaywebpage/ui.js';

const BANNER_MARK = 'GENERATED FILE - DO NOT EDIT';

/**
 * Builds the notice prepended to the generated bundle.
 *
 * @param {string} version Upstream package version.
 * @returns {string} Block comment, newline-terminated.
 */
function buildBanner(version) {
    return (
        `/*! ${BANNER_MARK}\n` +
        `    Source: replaywebpage@${version} (${UPSTREAM_UI})\n` +
        `    Generator: gulp/replaywebpagePatch.js  (npm run copyDeps)\n` +
        `    Modified ${PATCH_DATE}: the archive download menu entry is hidden\n` +
        `    when the noMediaDownload embed option is set. */\n`
    );
}

/**
 * Applies the download-menu patch to an upstream bundle.
 *
 * @param {string} source Contents of the upstream ui.js.
 * @param {string} version Upstream package version, for the banner.
 * @returns {string} Patched bundle including the banner.
 * @throws {Error} If the anchor is absent, ambiguous, or already patched.
 */
function applyPatch(source, version) {
    if (source.includes(PATCHED)) {
        throw new Error(
            'replaywebpage patch: the guard is already present in the upstream bundle. ' +
                'Upstream appears to have adopted the fix — drop this patch and ship ui.js unmodified. ' +
                'See the header of gulp/replaywebpagePatch.js.'
        );
    }

    const hits = source.split(ANCHOR).length - 1;
    if (hits !== 1) {
        throw new Error(
            `replaywebpage patch: expected the anchor expression exactly once, found ${hits}. ` +
                'Upstream changed the download menu markup — the anchor must be re-derived. ' +
                'See "IF THE BUILD FAILS HERE" in the header of gulp/replaywebpagePatch.js.'
        );
    }

    return buildBanner(version) + source.split(ANCHOR).join(PATCHED);
}

/**
 * Reads the upstream bundle, patches it and writes the shipped ui.js.
 *
 * @returns {{version: string, file: string, bytes: number}} Result summary for logging.
 * @throws {Error} If node_modules is missing or the patch cannot be applied.
 */
function generate() {
    if (!fs.existsSync(UPSTREAM_UI)) {
        throw new Error(`replaywebpage patch: ${UPSTREAM_UI} not found — run npm install first.`);
    }

    const version = JSON.parse(fs.readFileSync(UPSTREAM_PKG, 'utf8')).version;
    const patched = applyPatch(fs.readFileSync(UPSTREAM_UI, 'utf8'), version);

    fs.mkdirSync(path.dirname(OUTPUT_FILE), { recursive: true });
    fs.writeFileSync(OUTPUT_FILE, patched);

    return { version, file: OUTPUT_FILE, bytes: Buffer.byteLength(patched) };
}

module.exports = {
    ANCHOR,
    PATCHED,
    BANNER_MARK,
    PATCH_DATE,
    UPSTREAM_UI,
    OUTPUT_FILE,
    buildBanner,
    applyPatch,
    generate,
};
