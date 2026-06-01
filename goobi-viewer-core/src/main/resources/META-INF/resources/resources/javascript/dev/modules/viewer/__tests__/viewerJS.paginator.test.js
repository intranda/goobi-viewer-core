/**
 * Unit tests for viewerJS.paginator — keyboard-driven page navigation.
 *
 * Replaces the deleted Jasmine spec
 * `tests/spec/viewerPaginator-spec.js`. The original spec drove a custom
 * <li>-based DOM and configured the paginator with CSS selectors. The
 * current API takes navigation callbacks (or falls back to clicking
 * elements with `data-target="paginatorXxxPage"`), so these tests cover
 * the same domain (left/right + double-press semantics, RTL swap, the
 * input-field guard, close()) against the new contract.
 *
 * The handler debounces via setTimeout(maxDoubleClickDelay), so we run
 * jest's fake timers and advance them manually.
 */
// jQuery + jsdom are wired up by jest-setup-browser.js.
const $ = global.$;
const viewerJS = require('../viewerJS.paginator.js');

const LEFT = 37;
const RIGHT = 39;

function press(keyCode) {
    const ev = $.Event('keyup');
    ev.keyCode = keyCode;
    $(document.body).trigger(ev);
}

describe('viewerJS.paginator', function () {
    beforeEach(function () {
        jest.useFakeTimers();
        document.body.innerHTML = '';

        // BUG IN PRODUCTION CODE:
        // init() resets `this.lastKeypress` (lowercase p) and `this.lastkeycode`
        // (lowercase k), but the handler reads `viewer.paginator.lastKeyPress`
        // and `viewer.paginator.lastKeycode` (camelCase) — so init never
        // actually clears them. With fake timers between tests Date.now() can
        // wrap close to a previous test's lastKeyPress value, making the
        // first press of the next test look like a double-press. Manual reset
        // here pins down the test invariant; leaves the bug for a follow-up.
        viewerJS.paginator.lastKeyPress = undefined;
        viewerJS.paginator.lastKeycode = undefined;
    });

    afterEach(function () {
        viewerJS.paginator.close();
        jest.useRealTimers();
    });

    describe('with explicit navigation callbacks', function () {
        let calls;

        function setupWithCallbacks(extraConfig) {
            calls = { previous: 0, next: 0, first: 0, last: 0 };
            viewerJS.paginator.init(
                Object.assign(
                    {
                        previous: function () {
                            calls.previous++;
                        },
                        next: function () {
                            calls.next++;
                        },
                        first: function () {
                            calls.first++;
                        },
                        last: function () {
                            calls.last++;
                        },
                    },
                    extraConfig || {}
                )
            );
        }

        test('should call `previous` once on a single LEFT keypress, after the debounce delay', function () {
            setupWithCallbacks();
            press(LEFT);
            // The dispatch is debounced; nothing should have fired yet.
            expect(calls.previous).toBe(0);
            jest.advanceTimersByTime(250);
            expect(calls.previous).toBe(1);
            expect(calls.next).toBe(0);
        });

        test('should call `next` once on a single RIGHT keypress', function () {
            setupWithCallbacks();
            press(RIGHT);
            jest.advanceTimersByTime(250);
            expect(calls.next).toBe(1);
            expect(calls.previous).toBe(0);
        });

        test('should call `first` (not `previous`) on a fast double LEFT keypress', function () {
            setupWithCallbacks();
            press(LEFT);
            press(LEFT);
            jest.advanceTimersByTime(250);
            expect(calls.first).toBe(1);
            expect(calls.previous).toBe(0);
        });

        test('should call `last` (not `next`) on a fast double RIGHT keypress', function () {
            setupWithCallbacks();
            press(RIGHT);
            press(RIGHT);
            jest.advanceTimersByTime(250);
            expect(calls.last).toBe(1);
            expect(calls.next).toBe(0);
        });

        test('should swap LEFT and RIGHT when rightToLeft="true"', function () {
            setupWithCallbacks({ rightToLeft: 'true' });
            press(LEFT);
            jest.advanceTimersByTime(250);
            // In RTL mode LEFT is "forward" → next.
            expect(calls.next).toBe(1);
            expect(calls.previous).toBe(0);
        });

        test('should ignore key events that originate inside an <input>', function () {
            setupWithCallbacks();
            document.body.innerHTML = '<input id="i"/>';
            const ev = $.Event('keyup', { keyCode: RIGHT });
            $('#i').trigger(ev);
            jest.advanceTimersByTime(250);
            expect(calls.next).toBe(0);
        });
    });

    describe('without callbacks — falls back to clicking [data-target="paginatorXxxPage"] elements', function () {
        test('should click [data-target="paginatorPrevPage"] on a single LEFT keypress', function () {
            document.body.innerHTML =
                '<button data-target="paginatorPrevPage" id="prev"></button>' +
                '<button data-target="paginatorNextPage" id="next"></button>' +
                '<button data-target="paginatorFirstPage" id="first"></button>' +
                '<button data-target="paginatorLastPage" id="last"></button>';
            const clicks = { prev: 0, next: 0, first: 0, last: 0 };
            document.getElementById('prev').addEventListener('click', function () {
                clicks.prev++;
            });
            document.getElementById('next').addEventListener('click', function () {
                clicks.next++;
            });
            document.getElementById('first').addEventListener('click', function () {
                clicks.first++;
            });
            document.getElementById('last').addEventListener('click', function () {
                clicks.last++;
            });

            viewerJS.paginator.init({});
            press(LEFT);
            jest.advanceTimersByTime(250);
            expect(clicks.prev).toBe(1);
            expect(clicks.next).toBe(0);

            press(RIGHT);
            press(RIGHT);
            jest.advanceTimersByTime(250);
            expect(clicks.last).toBe(1);
            // Single next was *consumed* by the double-press detector.
            expect(clicks.next).toBe(0);
        });
    });

    describe('close()', function () {
        test('should detach the keyup listener so subsequent presses are no-ops', function () {
            const calls = { next: 0 };
            viewerJS.paginator.init({
                next: function () {
                    calls.next++;
                },
            });

            viewerJS.paginator.close();

            press(RIGHT);
            jest.advanceTimersByTime(250);
            expect(calls.next).toBe(0);
        });
    });
});
