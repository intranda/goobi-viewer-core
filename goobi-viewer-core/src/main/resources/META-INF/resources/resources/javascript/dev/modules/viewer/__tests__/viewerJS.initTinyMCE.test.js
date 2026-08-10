const fs = require('fs');
const path = require('path');

// viewerJS.js and viewerJS.tinyMce.js are not requirable together via plain
// require() — each has its own top-level `var viewerJS = ...`, and Node's
// CommonJS wrapper would give each file its own isolated local variable
// instead of the single accumulating global object a real page produces
// via sequential <script> tags. Indirect eval ((0, eval)(src)) executes in
// the true global scope, exactly like a browser loading concatenated
// script files, so `var viewerJS = ...` in each file really does attach to
// (and, where the file reads `viewerJS || {}` first, extend) the same
// global.viewerJS. This mirrors the established pattern for the
// Crowdsourcing.* namespace — see crowdsourcing-loader.js.
function loadViewerJSFiles(files) {
    files.forEach((file) => {
        const src = fs.readFileSync(path.resolve(__dirname, '..', file), 'utf8');
        (0, eval)(src);
    });
    return global.viewerJS;
}

describe('viewerJS.initTinyMCE / _defaults.setup integration', () => {
    beforeEach(() => {
        // viewerJS.js references rxjs.Subject at module-eval time
        // (viewer.initialized = new rxjs.Subject();) and currentLang /
        // currentPage as page-global free variables set by inline <script>
        // blocks on specific pages — stub all three so the file loads.
        global.rxjs = {
            Subject: function () {
                this.subscribe = () => {};
                this.next = () => {};
            },
        };
        global.currentLang = 'de';
        global.currentPage = 'somePage';
        global.viewerJS = undefined;
        global.tinymce = { init: jest.fn(), triggerSave: jest.fn() };
    });

    test('after initTinyMCE runs, the merged config still has a working setup with a blur handler', () => {
        const viewerJS = loadViewerJSFiles(['viewerJS.js', 'viewerJS.tinyMce.js']);
        document.body.innerHTML = '<textarea class="tinyMCE"></textarea>';
        viewerJS.tinyConfig = {};

        viewerJS.initTinyMCE();

        // This is the actual regression guard: before Task 6's fix,
        // initTinyMCE sets its own viewer.tinyConfig.setup here, which
        // getConfig()/init() then use INSTEAD of _defaults.setup — losing
        // the blur handler. After the fix, initTinyMCE must not set .setup
        // at all, so _defaults.setup (which does register blur) survives
        // the $.extend(true, {}, _defaults, config) merge.
        expect(viewerJS.tinyConfig.setup).toBeUndefined();
        // Guards against the assertion above passing vacuously if a future
        // change breaks the .tinyMCE guard so initTinyMCE() never calls
        // viewerJS.tinyMce.init() at all.
        expect(global.tinymce.init).toHaveBeenCalled();

        const mergedConfig = viewerJS.tinyMce.getConfig(viewerJS.tinyConfig);
        const registered = [];
        const fakeEd = {
            targetElm: document.createElement('textarea'),
            on: (ev) => registered.push(ev),
            save: jest.fn(),
            getElement: () => null,
        };
        mergedConfig.setup(fakeEd);

        expect(registered).toContain('init');
        expect(registered).toContain('change input paste');
        expect(registered).toContain('blur');
    });
});
