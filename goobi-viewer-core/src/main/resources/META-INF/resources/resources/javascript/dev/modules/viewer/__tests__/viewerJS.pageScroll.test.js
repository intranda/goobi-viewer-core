/**
 * Unit tests for viewerJS.pageScroll.
 *
 * init() binds a window scroll handler and a click handler on the
 * "back to top" element. We freeze jQuery animations and drive
 * window.pageYOffset directly to assert the show/hide behavior.
 *
 * scrollToFragment() is independent of init().
 */
$.fx.off = true;

const viewerJS = require('../viewerJS.pageScroll.js');

beforeEach(() => {
    document.body.innerHTML = '<a id="topBtn" href="#top" style="display:none">Top</a><div id="top">anchor</div>';
});

describe('viewerJS.pageScroll.init scroll handler', () => {
    test('shows the "back to top" element once pageYOffset > 200', () => {
        viewerJS.pageScroll.init('#topBtn', '#top');
        // Fake the scroll position and dispatch a scroll event on window.
        Object.defineProperty(window, 'pageYOffset', { value: 250, configurable: true });
        $(window).trigger('scroll');
        expect(document.getElementById('topBtn').style.display).not.toBe('none');
    });

    test('hides the element again when pageYOffset drops to 200 or below', () => {
        viewerJS.pageScroll.init('#topBtn', '#top');
        Object.defineProperty(window, 'pageYOffset', { value: 250, configurable: true });
        $(window).trigger('scroll');
        Object.defineProperty(window, 'pageYOffset', { value: 100, configurable: true });
        $(window).trigger('scroll');
        expect(document.getElementById('topBtn').style.display).toBe('none');
    });
});

describe('viewerJS.pageScroll.init click handler', () => {
    test('clicking the back-to-top element triggers a scroll animation', () => {
        viewerJS.pageScroll.init('#topBtn', '#top');
        const animateSpy = jest.spyOn($.fn, 'animate').mockImplementation(function () {
            return this;
        });
        try {
            $('#topBtn').trigger('click');
            expect(animateSpy).toHaveBeenCalled();
            // First arg of the latest call should request scrollTop.
            const opts = animateSpy.mock.calls[0][0];
            expect(opts).toHaveProperty('scrollTop');
        } finally {
            animateSpy.mockRestore();
        }
    });
});

describe('viewerJS.pageScroll.init hash jump', () => {
    // Regression: the jump used to set scrollTop to the raw anchor offset, which put the
    // target under the sticky header. scrollIntoView respects html{scroll-padding-top}.
    test('scrolls the hash target into view instead of setting a raw offset', async () => {
        document.body.innerHTML = '<a id="topBtn" href="#top"></a><div id="top"></div><div id="topDocAnchor"></div>';
        const anchorEl = document.getElementById('topDocAnchor');
        anchorEl.scrollIntoView = jest.fn();
        const scrollTopSpy = jest.spyOn($.fn, 'scrollTop');
        const origHref = window.location.href;
        history.replaceState({}, '', '/viewer/image/PI/3/#topDocAnchor');
        try {
            viewerJS.pageScroll.init('#topBtn', '#top');
            // one tick for jQuery's ready callback, one for the module's setTimeout(0)
            await new Promise((resolve) => setTimeout(resolve, 0));
            await new Promise((resolve) => setTimeout(resolve, 0));
            expect(anchorEl.scrollIntoView).toHaveBeenCalled();
            expect(scrollTopSpy).not.toHaveBeenCalledWith(expect.anything());
        } finally {
            scrollTopSpy.mockRestore();
            history.replaceState({}, '', origHref);
        }
    });
});

describe('viewerJS.pageScroll.scrollToFragment', () => {
    test('focuses the element matched by the explicit fragment selector', () => {
        document.body.innerHTML = '<input id="target" />';
        const focusSpy = jest.spyOn(document.getElementById('target'), 'focus');
        viewerJS.pageScroll.scrollToFragment('#target');
        expect(focusSpy).toHaveBeenCalled();
        focusSpy.mockRestore();
    });

    test('falls back to extracting the fragment from window.location.href when called with no argument', () => {
        document.body.innerHTML = '<input id="auto" />';
        const focusSpy = jest.spyOn(document.getElementById('auto'), 'focus');
        const origHref = window.location.href;
        // jsdom allows replaceState within the same origin/path.
        history.replaceState({}, '', '/?x=1#auto');
        try {
            viewerJS.pageScroll.scrollToFragment();
            expect(focusSpy).toHaveBeenCalled();
        } finally {
            focusSpy.mockRestore();
            history.replaceState({}, '', origHref);
        }
    });
});
