/** Shared accessibility utilities for the immersive view. */

const FOCUSABLE_QUERY = 'a[href],button,input,select,textarea,[tabindex]:not([tabindex="-1"])';

/**
 * Returns a keydown handler that traps Tab focus within `container`: reaching
 * the last focusable element wraps to the first, and Shift+Tab at the first
 * wraps to the last. Attach with addEventListener and detach when the dialog
 * closes to avoid stacking handlers.
 *
 * @param {HTMLElement} container  the modal or overlay element
 * @returns {function(KeyboardEvent):void}
 */
export function createFocusTrap(container) {
    return function trapFocus(e) {
        if (e.key !== 'Tab') return;
        const f = Array.from(container.querySelectorAll(FOCUSABLE_QUERY)).filter(
            (n) => !n.hidden && !n.disabled && n.offsetParent !== null
        );
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
}
