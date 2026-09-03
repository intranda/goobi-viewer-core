/** Unit tests for the shared overlay-open check: one list for every keyboard guard. */
import { isOverlayOpen, isStageFocused } from '../shared/ivSelectors.mjs';

afterEach(() => {
    document.body.innerHTML = '';
});

test('nothing open on an empty page', () => {
    expect(isOverlayOpen()).toBe(false);
    expect(isOverlayOpen({ panels: true })).toBe(false);
});

test.each([
    ['grid overlay', '<div id="immersiveGridOverlay"></div>'],
    ['shortcuts modal', '<div id="immersiveShortcuts"></div>'],
    ['page dropdown', '<div id="immersivePageDropdown"></div>'],
    ['bootstrap popover', '<div class="popover show"></div>'],
])('%s counts as open', (_, html) => {
    document.body.innerHTML = html;
    expect(isOverlayOpen()).toBe(true);
});

test('a hidden overlay does not count', () => {
    document.body.innerHTML = '<div id="immersiveGridOverlay" hidden></div><div class="popover"></div>';
    expect(isOverlayOpen()).toBe(false);
});

test('an open left panel counts only when asked for', () => {
    document.body.innerHTML = '<aside class="immersive__panel-left is-open"></aside>';
    expect(isOverlayOpen()).toBe(false);
    expect(isOverlayOpen({ panels: true })).toBe(true);
});

test('isStageFocused is true only while focus sits inside the image mount', () => {
    document.body.innerHTML = '<div data-immersive-image><button id="inside"></button></div><button id="outside"></button>';
    expect(isStageFocused()).toBe(false);
    document.getElementById('inside').focus();
    expect(isStageFocused()).toBe(true);
    document.getElementById('outside').focus();
    expect(isStageFocused()).toBe(false);
});
