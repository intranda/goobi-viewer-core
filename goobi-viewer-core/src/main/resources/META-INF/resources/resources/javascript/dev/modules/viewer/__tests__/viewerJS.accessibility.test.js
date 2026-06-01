/**
 * Unit tests for viewerJS.accessibility.
 *
 * Pure DOM helpers — no third-party deps. Each test sets up its own
 * fixture and exercises the corresponding method. We do not call
 * init() because most of its work delegates to these methods, and
 * exercising init() would re-bind global keydown handlers across tests.
 */
const viewerJS = require('../viewerJS.accessibility.js');
const a11y = viewerJS.accessibility;

describe('triggerClick', () => {
    test('calls click() on the element when Enter is pressed', () => {
        const btn = document.createElement('span');
        btn.click = jest.fn();
        const e = { key: 'Enter', preventDefault: jest.fn() };
        a11y.triggerClick.call(btn, e);
        expect(e.preventDefault).toHaveBeenCalled();
        expect(btn.click).toHaveBeenCalled();
    });

    test('calls click() on the element when Space is pressed', () => {
        const btn = document.createElement('span');
        btn.click = jest.fn();
        const e = { key: ' ', preventDefault: jest.fn() };
        a11y.triggerClick.call(btn, e);
        expect(btn.click).toHaveBeenCalled();
    });

    test('does nothing for any other key', () => {
        const btn = document.createElement('span');
        btn.click = jest.fn();
        const e = { key: 'a', preventDefault: jest.fn() };
        a11y.triggerClick.call(btn, e);
        expect(btn.click).not.toHaveBeenCalled();
        expect(e.preventDefault).not.toHaveBeenCalled();
    });
});

describe('checkSidebar', () => {
    test('does nothing when the sidebar element is missing', () => {
        document.body.innerHTML = '<a id="skip-to-sidebar" style="display:none">Skip</a>';
        expect(() => a11y.checkSidebar()).not.toThrow();
        expect(document.getElementById('skip-to-sidebar').style.display).toBe('none');
    });

    test('does nothing when the skip link is missing', () => {
        document.body.innerHTML = '<div id="sidebarGroup"><span>x</span></div>';
        expect(() => a11y.checkSidebar()).not.toThrow();
    });

    test('shows the skip link when the sidebar has children', () => {
        document.body.innerHTML = `
            <div id="sidebarGroup"><div>content</div></div>
            <a id="skip-to-sidebar" style="display:none">Skip</a>`;
        a11y.checkSidebar();
        expect(document.getElementById('skip-to-sidebar').style.display).toBe('');
    });

    test('shows the skip link when the sidebar has only text content', () => {
        document.body.innerHTML = `
            <div id="sidebarGroup">just text</div>
            <a id="skip-to-sidebar" style="display:none">Skip</a>`;
        a11y.checkSidebar();
        expect(document.getElementById('skip-to-sidebar').style.display).toBe('');
    });

    test('hides the skip link when the sidebar is empty', () => {
        document.body.innerHTML = `
            <div id="sidebarGroup"></div>
            <a id="skip-to-sidebar">Skip</a>`;
        a11y.checkSidebar();
        expect(document.getElementById('skip-to-sidebar').style.display).toBe('none');
    });
});

describe('detectKeyboardUsage', () => {
    beforeEach(() => {
        // Reset the body class between tests since the listeners are
        // installed once and read/write `document.body.classList`.
        document.body.className = '';
    });

    test('Tab keydown adds the using-keyboard class', () => {
        a11y.detectKeyboardUsage();
        document.body.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', keyCode: 9, bubbles: true }));
        expect(document.body.classList.contains('using-keyboard')).toBe(true);
    });

    test('mousedown removes the using-keyboard class', () => {
        // Note: detectKeyboardUsage was already called in the previous
        // test (handler stays bound). Adding the class first proves the
        // mousedown branch works.
        document.body.classList.add('using-keyboard');
        document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
        expect(document.body.classList.contains('using-keyboard')).toBe(false);
    });

    test('non-Tab keydown does not add the class', () => {
        document.body.dispatchEvent(new KeyboardEvent('keydown', { key: 'a', keyCode: 65, bubbles: true }));
        expect(document.body.classList.contains('using-keyboard')).toBe(false);
    });
});

describe('findPseudoBtns', () => {
    test('binds keydown handlers on every [role="button"] in the DOM', () => {
        document.body.innerHTML = `
            <span role="button" id="b1">A</span>
            <span role="button" id="b2">B</span>
            <span id="not-a-button">C</span>`;
        const b1 = document.getElementById('b1');
        b1.click = jest.fn();

        a11y.findPseudoBtns();

        // Pressing Enter on a pseudo-button now triggers click().
        b1.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
        expect(b1.click).toHaveBeenCalled();
    });

    test('leaves elements without role="button" alone', () => {
        document.body.innerHTML = '<span id="plain">x</span>';
        const plain = document.getElementById('plain');
        plain.click = jest.fn();

        a11y.findPseudoBtns();

        plain.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
        expect(plain.click).not.toHaveBeenCalled();
    });
});
