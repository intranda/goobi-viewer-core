/** Keyboard-shortcuts help modal with platform-specific modifier keycaps. */

import { isTypingTarget, isMacPlatform, macKeyLabel } from './viewerImmersive.mjs';

/**
 * Keyboard-shortcuts help modal: opened via the rail button or the `?` key,
 * closed via its X button, Escape or a click on the scrim. The rail button
 * follows panel semantics (an open left panel is closed first); `?` is a
 * no-op while another immersive overlay is open — no overlay stacking — or
 * while typing in a form field. Focus is trapped inside the dialog (grid
 * pattern) and restored on close. Registers `?`/Escape on the key dispatcher
 * with the highest precedence (priority 10).
 *
 * @param {Function} closePanels  closes the open left panel(s)
 * @param {{register: Function}} keys  the immersive key dispatcher
 */
export function setupShortcutsModal(closePanels, keys) {
    const overlay = document.getElementById('immersiveShortcuts');
    const trigger = document.querySelector('[data-immersive-shortcuts-trigger]');
    if (!overlay || !trigger) return;
    // On Apple platforms modifiers are conventionally shown as symbols (⌥, ⇧);
    // the accessible name keeps the spoken key name. The symbol gets its own
    // span so the flex keycap centers the glyph instead of baseline-aligning it.
    if (isMacPlatform(navigator.userAgentData?.platform ?? navigator.platform)) {
        overlay.querySelectorAll('kbd[data-key]').forEach((kbd) => {
            const mac = macKeyLabel(kbd.dataset.key);
            if (!mac) return;
            const symbol = document.createElement('span');
            symbol.className = 'immersive__shortcuts-key-symbol';
            symbol.textContent = mac.symbol;
            kbd.replaceChildren(symbol, document.createTextNode(mac.name));
            kbd.setAttribute('aria-label', mac.label);
        });
    }
    const dialog = overlay.querySelector('.immersive__shortcuts-dialog');
    const closeBtn = overlay.querySelector('.immersive__shortcuts-close');
    let opener = null;
    let trapHandler = null;
    const focusables = () =>
        Array.from(overlay.querySelectorAll('a[href],button,input,[tabindex]:not([tabindex="-1"])')).filter((n) => !n.hidden && !n.disabled && n.offsetParent !== null);
    const otherOverlayOpen = () =>
        !!document.querySelector('#immersiveGridOverlay:not([hidden]), #immersivePageDropdown:not([hidden]), .immersive__panel--left.is-open, .popover.show');
    const openShortcuts = () => {
        if (!overlay.hidden || otherOverlayOpen()) return;
        opener = document.activeElement;
        overlay.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        trapHandler = (e) => {
            if (e.key !== 'Tab') return;
            const f = focusables();
            if (!f.length) return;
            const first = f[0];
            const last = f[f.length - 1];
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        };
        overlay.addEventListener('keydown', trapHandler);
        if (closeBtn) closeBtn.focus();
    };
    const closeShortcuts = () => {
        if (overlay.hidden) return;
        overlay.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        if (trapHandler) {
            overlay.removeEventListener('keydown', trapHandler);
            trapHandler = null;
        }
        // A '?'-opened dialog has no focused opener (activeElement is <body>);
        // falling back to the trigger keeps keyboard users at a sensible spot.
        const restore = opener && opener !== document.body ? opener : trigger;
        opener = null;
        if (restore && typeof restore.focus === 'function') restore.focus();
    };
    trigger.addEventListener('click', () => {
        if (!overlay.hidden) {
            closeShortcuts();
            return;
        }
        if (typeof closePanels === 'function') closePanels();
        openShortcuts();
    });
    if (closeBtn) closeBtn.addEventListener('click', closeShortcuts);
    overlay.addEventListener('click', (e) => {
        if (dialog && !dialog.contains(e.target)) closeShortcuts();
    });
    keys.register(10, (e) => {
        if (e.key === 'Escape' && !overlay.hidden) {
            closeShortcuts();
            return true;
        }
        if (e.key === '?' && overlay.hidden && !e.ctrlKey && !e.metaKey && !e.altKey && !isTypingTarget(e.target) && !otherOverlayOpen()) {
            e.preventDefault();
            openShortcuts();
            return true;
        }
        return false;
    });
}
