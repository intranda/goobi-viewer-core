/** Unit tests for the focus trap used by the grid overlay and the shortcuts modal. */
import { jest } from '@jest/globals';
import { createFocusTrap } from '../shared/ivA11y.mjs';

// jsdom has no layout: give every attached element a client rect so the trap sees it as visible.
const getClientRects = HTMLElement.prototype.getClientRects;
beforeAll(() => {
    HTMLElement.prototype.getClientRects = function () {
        return this.isConnected ? [{}] : [];
    };
});
afterAll(() => {
    HTMLElement.prototype.getClientRects = getClientRects;
    document.body.innerHTML = '';
});

function mount() {
    document.body.innerHTML = `<div id="dialog"><button id="first">a</button><input id="middle" /><button disabled>x</button><a id="last" href="#">z</a></div>`;
    const dialog = document.getElementById('dialog');
    const trap = createFocusTrap(dialog);
    dialog.addEventListener('keydown', trap);
    const tab = (shiftKey) => {
        const e = new KeyboardEvent('keydown', { key: 'Tab', shiftKey, bubbles: true, cancelable: true });
        document.activeElement.dispatchEvent(e);
        return e.defaultPrevented;
    };
    return { dialog, trap, tab, first: document.getElementById('first'), middle: document.getElementById('middle'), last: document.getElementById('last') };
}

test('Tab on the last focusable wraps to the first, Shift+Tab on the first wraps to the last', () => {
    const { tab, first, last } = mount();
    last.focus();
    expect(tab(false)).toBe(true);
    expect(document.activeElement).toBe(first);
    expect(tab(true)).toBe(true);
    expect(document.activeElement).toBe(last);
});

test('Tab in the middle and non-Tab keys are left to the browser', () => {
    const { dialog, tab, middle } = mount();
    middle.focus();
    expect(tab(false)).toBe(false);
    const e = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    dialog.dispatchEvent(e);
    expect(e.defaultPrevented).toBe(false);
});

test('a container without focusable elements does nothing', () => {
    document.body.innerHTML = '<div id="empty"><span>text</span></div>';
    const trap = createFocusTrap(document.getElementById('empty'));
    const e = new KeyboardEvent('keydown', { key: 'Tab', cancelable: true });
    expect(() => trap(e)).not.toThrow();
    expect(e.defaultPrevented).toBe(false);
});

test('a fixed-position element (no offsetParent) is part of the trap', () => {
    document.body.innerHTML = '<div id="dialog"><button id="first">a</button><button id="fixed" style="position:fixed">b</button></div>';
    const dialog = document.getElementById('dialog');
    dialog.addEventListener('keydown', createFocusTrap(dialog));
    document.getElementById('fixed').focus();
    const e = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    document.activeElement.dispatchEvent(e);
    expect(e.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(document.getElementById('first'));
});
