/**
 * Jest setup for the "browser-modules" project (jsdom env).
 *
 * Loaded via setupFiles in jest.config.js. Runs ONCE per test file, BEFORE
 * the test framework. Its job is to make `global.jQuery` / `global.$` real
 * jQuery instances bound to jsdom's window — every viewer JS module's IIFE
 * call site reads `jQuery` as a free variable and would otherwise crash on
 * require().
 *
 * --- How to add a new test for an existing module ---
 *
 *   1. Drop a CommonJS export footer at the end of the source file (only if
 *      it doesn't already have one — see e.g. viewerJS.helper.js):
 *
 *          if (typeof module !== 'undefined' && module.exports) {
 *              module.exports = viewerJS;
 *          }
 *
 *   2. Create `<module-dir>/__tests__/<module>.test.js` and require the
 *      source. jQuery and the jsdom DOM are already wired up by this file:
 *
 *          const viewerJS = require('../viewerJS.foo.js');
 *
 *          describe('viewerJS.foo.bar', () => {
 *              test('should ...', () => { ... });
 *          });
 *
 *   3. If the module pulls in something we don't ship in node_modules
 *      (e.g. jqplot, jquery-ui's autocomplete, flatpickr), stub the
 *      specific symbol(s) at the top of the test file before require().
 *      Examples are in the existing tests (statistics.test.js for jqplot,
 *      cmsJS.tagList.test.js for autocomplete).
 *
 *   4. If the module's IIFE shape forces a cross-file `var Foo = (...)(Foo)`
 *      pattern (Crowdsourcing.* is the only example), use indirect eval to
 *      load each file in the global scope — see
 *      Crowdsourcing.Annotation.Plaintext.test.js for the pattern.
 */

// jquery's CJS export is a factory `function(window)` until first called;
// once bound it has `.fn`. Detect either form so the setup is idempotent.
const jqueryFactory = require('jquery');
const $ = typeof jqueryFactory === 'function' && !jqueryFactory.fn ? jqueryFactory(window) : jqueryFactory;

global.jQuery = $;
global.$ = $;
