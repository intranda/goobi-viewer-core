/**
 * Tests for the ReplayWeb.page bundle that actually ships in the WAR.
 *
 * The viewer hides the archive download entry (see webarchive.xhtml). Nothing
 * else in the build would notice if that guard silently disappeared during a
 * dependency update, so these tests assert it on the checked-in artifact —
 * and they pin down the loading chain that makes the patch effective at all.
 */
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const { ANCHOR, PATCHED, BANNER_MARK, OUTPUT_FILE } = require('../replaywebpagePatch');

// __dirname is <module>/gulp/__tests__; the module root is two levels up.
const moduleRoot = path.join(__dirname, '..', '..');
const libDir = path.dirname(path.join(moduleRoot, OUTPUT_FILE));

const bundle = fs.readFileSync(path.join(moduleRoot, OUTPUT_FILE), 'utf8');
const serviceWorker = fs.readFileSync(path.join(libDir, 'sw.js'), 'utf8');
const webarchiveXhtml = fs.readFileSync(
    path.join(moduleRoot, 'src/main/resources/META-INF/resources/resources/components/webarchive.xhtml'),
    'utf8'
);

// The banner is generator prose, not upstream code. Slicing it off keeps
// occurrence counts and offsets tied to the bundle body, independent of how
// the banner happens to be worded.
const body = bundle.slice(bundle.indexOf('*/') + 2);

const count = (haystack, needle) => haystack.split(needle).length - 1;

describe('shipped ui.js', () => {
    it('is marked as generated', () => {
        expect(bundle.startsWith('/*! ' + BANNER_MARK)).toBe(true);
    });

    it('preserves the upstream AGPL notice', () => {
        // Deliberately against `bundle`, not `body`: the notice must be in the
        // file regardless of the banner. Checking `body` would couple this test
        // to the banner's presence, because `body` anchors on the first */.
        expect(bundle).toContain('Affero General Public License');
    });

    it('is syntactically valid JavaScript', () => {
        // Catches a botched replacement that leaves the expression unbalanced —
        // something an occurrence count cannot see. Parses only, runs nothing.
        expect(() => new vm.Script(bundle)).not.toThrow();
    });

    it('carries the noMediaDownloadUI guard exactly once', () => {
        expect(count(body, PATCHED)).toBe(1);
    });

    it('no longer contains the unguarded expression', () => {
        expect(body).not.toContain(ANCHOR);
    });

    it('keeps the guard directly in front of the Download Archive entry', () => {
        const guardEnd = body.indexOf(PATCHED) + PATCHED.length;
        const label = body.indexOf('Download Archive', guardEnd);

        expect(label).toBeGreaterThan(-1);
        // Measured from the END of the replacement string: 313 characters in
        // 2.5.0. The bound tolerates markup churn but still fails if the guard
        // drifts onto a different block.
        expect(label - guardEnd).toBeLessThan(1000);
    });

    it('binds the guard to an option upstream still sets', () => {
        // ANCHOR does not contain the option name, so an upstream rename of
        // noMediaDownloadUI would pass every fail-fast check in the generator
        // and silently turn our guard into a permanent `!undefined` — i.e. into
        // no guard at all. These two assertions pin the option to the embed
        // attribute that feeds it.
        expect(body).toContain('{noMediaDownloadUI:!0}');
        expect(count(body, 'noMediaDownloadUI')).toBeGreaterThanOrEqual(3);
    });
});

describe('replay iframe loading chain', () => {
    // The patch is only effective if the file the iframe loads is the patched
    // one. The service worker generates that iframe's index page itself.
    it('has the service worker point its generated index page at ./ui.js', () => {
        expect(serviceWorker).toContain('"indexScript")||"./ui.js"');
    });

    it('never overrides indexScript from the UI', () => {
        // If upstream ever set this param, the iframe would load a different
        // file and the patch would silently stop taking effect.
        expect(body).not.toContain('indexScript');
    });

    it('loads the generated bundle from the overlay', () => {
        expect(webarchiveXhtml).toContain('replaywebpage/' + path.basename(OUTPUT_FILE));
    });
});

describe('download surface', () => {
    it('exposes exactly the two known Download Archive sites', () => {
        // 1) the "Get A Copy!" button inside rwp-embed-receipt (lower offset),
        //    which only renders for embed="replay-with-info" — unused here
        // 2) the dropdown menu entry (higher offset) — guarded by the patch
        // A third occurrence means upstream added a download path worth review.
        expect(count(body, 'Download Archive')).toBe(2);
    });

    it('renders rwp-embed-receipt only for replay-with-info', () => {
        expect(body).toContain('"replay-with-info"!==this.embed?""');
    });

    it('keeps the viewer embed on "full"', () => {
        // Switching to replay-with-info would surface the unguarded button above.
        expect(webarchiveXhtml).toMatch(/embed="full"/);
    });
});
