/**
 * Unit tests for viewerJS.sidebarMenu.
 *
 * The module is a single $(document).ready() handler that binds a
 * click listener directly to the buttons that exist at ready-time
 * (no event delegation). We therefore have to:
 *   1. Set up the DOM fixture BEFORE requiring the source.
 *   2. Avoid replacing innerHTML in beforeEach — replacement would
 *      drop the bound handlers.
 *   3. Reset only mutable state (classes / inline display) between tests.
 */
// jQuery animations are async (queued on rAF) and never complete
// under jsdom. Disabling fx makes slideToggle/fadeToggle/animate
// resolve synchronously to their end state, which is what we need
// to assert against.
$.fx.off = true;

document.body.innerHTML = `
    <div class="sidebar-menu">
        <button class="sidebar-menu__submenu-button" id="btn1">Toggle</button>
        <ul class="sidebar-menu__submenu" id="sub1" style="display:none">
            <li>Item</li>
        </ul>
    </div>`;

require('../viewerJS.sidebarMenu.js');

beforeAll(async () => {
    // jQuery's $(document).ready() defers its callback to the next
    // tick even when the document is already complete. Yield once
    // so the click handler is bound before any test runs.
    await new Promise((resolve) => setTimeout(resolve, 0));
});

beforeEach(() => {
    // Reset state without recreating the elements (handlers are bound
    // to the original references).
    const $btn = $('#btn1');
    $btn.removeClass('-active');
    document.getElementById('sub1').style.display = 'none';
});

describe('viewerJS.sidebarMenu', () => {
    test('clicking a submenu button toggles the -active class on it', () => {
        const $btn = $('#btn1');
        expect($btn.hasClass('-active')).toBe(false);

        $btn.trigger('click');
        expect($btn.hasClass('-active')).toBe(true);

        $btn.trigger('click');
        expect($btn.hasClass('-active')).toBe(false);
    });

    test('clicking a submenu button reveals the next .sidebar-menu__submenu sibling', () => {
        const $btn = $('#btn1');
        const submenu = document.getElementById('sub1');

        expect(submenu.style.display).toBe('none');
        $btn.trigger('click');
        // jQuery's slideToggle replaces the inline display value; under
        // jsdom (no animation frames) it ends up as 'block' or '' once
        // the toggle completes. Either way it is no longer 'none'.
        expect(submenu.style.display).not.toBe('none');
    });
});
