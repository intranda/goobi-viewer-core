/**
 * Unit tests for the shortcuts help modal: '?'/Escape through the dispatcher,
 * open guards (typing targets, other overlays), and the focus-restore
 * regression (a '?'-opened dialog must return focus to the rail trigger, not
 * leave it on <body>).
 */
import { jest } from '@jest/globals';
import { setupShortcutsModal } from '../ivShortcutsModal.mjs';
import { createKeyDispatcher } from '../ivKeys.mjs';

function mountMarkup() {
    document.body.innerHTML = `
        <button data-immersive-shortcuts-trigger aria-expanded="false"></button>
        <div id="immersiveShortcuts" hidden>
            <div class="immersive__shortcuts-dialog">
                <button class="immersive__shortcuts-close"></button>
            </div>
        </div>
        <input id="someInput" type="text" />`;
    const keys = createKeyDispatcher();
    const closePanels = jest.fn();
    setupShortcutsModal(closePanels, keys);
    return {
        keys,
        closePanels,
        overlay: document.getElementById('immersiveShortcuts'),
        trigger: document.querySelector('[data-immersive-shortcuts-trigger]'),
        closeBtn: document.querySelector('.immersive__shortcuts-close'),
        input: document.getElementById('someInput'),
    };
}

const question = (target) => ({ key: '?', ctrlKey: false, metaKey: false, altKey: false, target: target || document.body, preventDefault: jest.fn() });

describe('setupShortcutsModal', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    test("'?' opens the modal and focuses the close button", () => {
        const { keys, overlay, trigger, closeBtn } = mountMarkup();
        expect(keys.handleEvent(question())).toBe(true);
        expect(overlay.hidden).toBe(false);
        expect(trigger.getAttribute('aria-expanded')).toBe('true');
        expect(document.activeElement).toBe(closeBtn);
    });

    test("'?' inside a typing target does not open the modal", () => {
        const { keys, overlay, input } = mountMarkup();
        expect(keys.handleEvent(question(input))).toBe(false);
        expect(overlay.hidden).toBe(true);
    });

    test("'?' while another immersive overlay is open does not open the modal", () => {
        const { keys, overlay } = mountMarkup();
        const panel = document.createElement('aside');
        panel.className = 'immersive__panel-left is-open';
        document.body.appendChild(panel);
        expect(keys.handleEvent(question())).toBe(false);
        expect(overlay.hidden).toBe(true);
    });

    test("Escape closes a '?'-opened modal and restores focus to the trigger (body-opener regression)", () => {
        const { keys, overlay, trigger } = mountMarkup();
        keys.handleEvent(question()); // opener is <body>
        expect(keys.handleEvent({ key: 'Escape' })).toBe(true);
        expect(overlay.hidden).toBe(true);
        expect(trigger.getAttribute('aria-expanded')).toBe('false');
        expect(document.activeElement).toBe(trigger);
    });

    test('the rail trigger toggles the modal and closes open panels first', () => {
        const { overlay, trigger, closePanels } = mountMarkup();
        trigger.click();
        expect(closePanels).toHaveBeenCalledTimes(1);
        expect(overlay.hidden).toBe(false);
        trigger.click();
        expect(overlay.hidden).toBe(true);
    });

    test('a click on the scrim closes; a click inside the dialog does not', () => {
        const { overlay, trigger } = mountMarkup();
        trigger.click();
        overlay.querySelector('.immersive__shortcuts-dialog').dispatchEvent(new MouseEvent('click', { bubbles: true }));
        expect(overlay.hidden).toBe(false);
        overlay.dispatchEvent(new MouseEvent('click', { bubbles: true }));
        expect(overlay.hidden).toBe(true);
    });
});
