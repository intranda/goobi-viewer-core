/**
 * Unit tests for the ReplayWeb.page patch generator.
 *
 * These run against small synthetic fixtures rather than the real 720 KB
 * bundle: what matters here is that the transformation is correct and that
 * the fail-fast guards actually fire. The shipped artifact is covered
 * separately by replaywebpageShipped.test.js.
 */
const { ANCHOR, PATCHED, BANNER_MARK, buildBanner, applyPatch } = require('../replaywebpagePatch');

const PREFIX = 'var x=1;';
const SUFFIX = 'J`<sl-menu-item>Download Archive</sl-menu-item>`:te}';
const VERSION = '9.9.9';

const count = (haystack, needle) => haystack.split(needle).length - 1;

describe('applyPatch', () => {
    it('replaces the anchor with the guarded expression', () => {
        const result = applyPatch(PREFIX + ANCHOR + SUFFIX, VERSION);

        expect(count(result, PATCHED)).toBe(1);
        expect(result).not.toContain(ANCHOR);
        expect(result).toContain(SUFFIX);
    });

    it('prepends a banner naming the upstream version', () => {
        const result = applyPatch(PREFIX + ANCHOR + SUFFIX, VERSION);

        expect(result.startsWith('/*! ' + BANNER_MARK)).toBe(true);
        expect(result).toContain('replaywebpage@' + VERSION);
    });

    it('throws when the anchor is missing', () => {
        expect(() => applyPatch(PREFIX + SUFFIX, VERSION)).toThrow(/found 0/);
    });

    it('throws when the anchor occurs more than once', () => {
        const twice = PREFIX + ANCHOR + SUFFIX + ANCHOR + SUFFIX;

        expect(() => applyPatch(twice, VERSION)).toThrow(/found 2/);
    });

    it('throws when the input already carries the guard', () => {
        expect(() => applyPatch(PREFIX + PATCHED + SUFFIX, VERSION)).toThrow(/already present/);
    });
});

describe('PATCHED', () => {
    it('is derived from ANCHOR and stays parenthesis-balanced', () => {
        expect(PATCHED).toContain(ANCHOR.slice(2, -1));
        expect(count(PATCHED, '(')).toBe(count(PATCHED, ')'));
        expect(count(PATCHED, '(') - count(ANCHOR, '(')).toBe(1);
        expect(count(PATCHED, ')') - count(ANCHOR, ')')).toBe(1);
    });
});

describe('buildBanner', () => {
    it('is a closed block comment carrying a modification date', () => {
        const banner = buildBanner(VERSION);

        expect(banner.startsWith('/*!')).toBe(true);
        expect(banner.trimEnd().endsWith('*/')).toBe(true);
        expect(count(banner, '*/')).toBe(1);
        // AGPL v3 §5(a) requires modified files to state the change and a date.
        expect(banner).toMatch(/\d{4}-\d{2}-\d{2}/);
    });
});
