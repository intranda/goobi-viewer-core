/** Platform and input-context predicates shared by the keyboard handling. */

/**
 * Whether a key-event target sits in a typing context — a form field or a
 * contenteditable region (also via ancestors) — where single-character
 * shortcuts like `?` must not fire. Pure + tested.
 *
 * @param {EventTarget|null} element  e.g. a keydown event's target
 * @returns {boolean}
 */
export function isTypingTarget(element) {
    if (!element || typeof element.closest !== 'function') return false;
    return !!element.closest('input, textarea, select, [contenteditable]:not([contenteditable="false"])');
}

/**
 * Whether a platform string names an Apple platform, where modifier keys are
 * conventionally shown as symbols (⌥, ⇧) instead of their PC names.
 * Pure + tested.
 *
 * @param {string} [platform]  navigator.userAgentData?.platform or navigator.platform
 * @returns {boolean}
 */
export function isMacPlatform(platform) {
    return /mac|iphone|ipad|ipod/i.test(platform || '');
}

/**
 * Mac display variant of a modifier key: the Apple symbol plus the key name
 * for the keycap (a bare ⇧ reads like an arrow key next to real ↑/↓ caps),
 * and the spoken name for assistive technology. Symbol and name stay separate
 * so the keycap can flex-center the symbol glyph independently of the text
 * baseline. Null for keys that read the same on every platform. Pure + tested.
 *
 * @param {string} key  lowercase modifier name from a `data-key` attribute
 * @returns {{symbol:string, name:string, label:string}|null}
 */
export function macKeyLabel(key) {
    const labels = {
        alt: { symbol: '⌥', name: 'Option', label: 'Option' },
        shift: { symbol: '⇧', name: 'Shift', label: 'Shift' },
    };
    return labels[key] || null;
}
