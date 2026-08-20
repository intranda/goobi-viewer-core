const fs = require('fs');
const path = require('path');
const compiler = require('riot-compiler');

const TAG_PATH = path.resolve(__dirname, '../richtextQuestion.tag');

global.Crowdsourcing = { language: 'en' };
// getConfig is a passthrough that returns exactly what's passed in — it
// must NOT fabricate fields like license_key itself, or a test asserting
// on those fields would pass regardless of whether the tag's own code is
// actually correct. The real regression check is that the raw global
// tinymce.init is never called directly (see the first test below);
// license_key correctness is already covered by viewerJS.tinyMce.test.js
// in core, which tests the real getConfig().
global.viewerJS = {
    tinyMce: {
        getConfig: jest.fn((overrides) => ({ ...overrides })),
        init: jest.fn(),
    },
};
// Stub the raw global too, so we can assert it is never touched directly.
global.tinymce = { init: jest.fn() };

let scriptFn;

beforeAll(() => {
    const source = fs.readFileSync(TAG_PATH, 'utf-8');
    const compiled = compiler.compile(source);
    const riotStub = {
        tag2: (name, tmpl, css, attrs, fn) => {
            scriptFn = fn;
        },
    };
    new Function('riot', compiled)(riotStub);
});

function createTag(opts = {}) {
    const ctx = { opts, refs: {}, on: () => {}, update: () => {} };
    scriptFn.call(ctx, opts);
    return ctx;
}

describe('richtextQuestion.tag initTinyMce', () => {
    beforeEach(() => {
        document.body.innerHTML = '<textarea class="tinyMCE"></textarea>';
        global.viewerJS.tinyMce.init.mockClear();
        global.viewerJS.tinyMce.getConfig.mockClear();
        global.tinymce.init.mockClear();
    });

    test('calls the shared wrapper, not the raw tinymce global directly', () => {
        const tag = createTag({ item: { isReviewMode: () => false } });
        tag.initTinyMce();

        expect(global.viewerJS.tinyMce.init).toHaveBeenCalledTimes(1);
        expect(global.tinymce.init).not.toHaveBeenCalled();
    });

    test('passes a real boolean true for readonly in review mode, not the number 1', () => {
        const tag = createTag({ item: { isReviewMode: () => true } });
        tag.initTinyMce();

        const configArg = global.viewerJS.tinyMce.init.mock.calls[0][0];
        expect(configArg.readonly).toBe(true);
    });

    test('does not set readonly outside review mode', () => {
        const tag = createTag({ item: { isReviewMode: () => false } });
        tag.initTinyMce();

        const configArg = global.viewerJS.tinyMce.init.mock.calls[0][0];
        expect(configArg.readonly).toBeUndefined();
    });

    test('does nothing when there is no .tinyMCE element in the DOM', () => {
        document.body.innerHTML = '';
        const tag = createTag({ item: { isReviewMode: () => false } });
        tag.initTinyMce();

        expect(global.viewerJS.tinyMce.init).not.toHaveBeenCalled();
    });
});
