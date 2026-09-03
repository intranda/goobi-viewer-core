/** Annotations toggle: persisted checkbox, root state class and the `a` shortcut. */

import { isTypingTarget } from '../../shared/ivPlatform.mjs';
import { readAnnotationsEnabled, writeAnnotationsEnabled } from '../../shared/ivPrefs.mjs';
import { hasAnnotations } from '../../data/ivAnnotationModel.mjs';
import { isOverlayOpen, isStageFocused } from '../../shared/ivSelectors.mjs';
import { KEY_PRIORITY } from '../../shared/ivKeys.mjs';

/** State class on the .immersive root while annotations are switched on (panel markup AND image overlays key off it). */
const STATE_CLASS = '-annotations-on';

/**
 * Wires the annotations checkbox: persisted state, the root state class and
 * the `a` keyboard shortcut. The whole toggle row is hidden (not just
 * disabled — it would otherwise dictate the panel's minimum width) and the
 * root state class is dropped whenever the current page has no annotation
 * data; the persisted preference survives such pages.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.root      the .immersive root (carries the state class)
 * @param {HTMLInputElement} opts.checkbox
 * @param {Storage} opts.storage      e.g. window.localStorage
 * @param {{register: Function}} [opts.keys]  the immersive key dispatcher
 * @returns {{setPageData: Function, onChange: Function, destroy: Function}}
 */
export function setupAnnotationsToggle({ root, checkbox, storage, keys }) {
    const listeners = [];
    const toggleRow = checkbox.closest('.immersive__anno-toggle');
    let available = false;

    const sync = () => {
        root.classList.toggle(STATE_CLASS, checkbox.checked && available);
    };

    const setEnabled = (on) => {
        checkbox.checked = on;
        writeAnnotationsEnabled(storage, on);
        sync();
        listeners.forEach((cb) => cb(on));
    };

    const onCheckboxChange = () => setEnabled(checkbox.checked);
    checkbox.checked = readAnnotationsEnabled(storage);
    checkbox.addEventListener('change', onCheckboxChange);

    let unregisterKeys = null;
    if (keys) {
        unregisterKeys = keys.register(KEY_PRIORITY.annotationsToggle, (e) => {
            if (e.key !== 'a' || e.ctrlKey || e.metaKey || e.altKey || e.shiftKey) return false;
            if (isTypingTarget(e.target) || isStageFocused()) return false;
            const panel = checkbox.closest('.immersive__panel-left');
            if (checkbox.disabled || (panel && !panel.classList.contains('is-open'))) return false;
            if (isOverlayOpen()) return false;
            setEnabled(!checkbox.checked);
            return true;
        });
    }

    sync();

    return {
        /**
         * Updates the toggle availability for the current page's layout data.
         *
         * @param {object|null} layout  the page's layout annotations
         */
        setPageData(layout) {
            available = hasAnnotations(layout);
            checkbox.disabled = !available;
            if (toggleRow) toggleRow.hidden = !available;
            sync();
        },
        onChange(cb) {
            listeners.push(cb);
            return () => {
                const i = listeners.indexOf(cb);
                if (i !== -1) listeners.splice(i, 1);
            };
        },
        destroy() {
            checkbox.removeEventListener('change', onCheckboxChange);
            if (unregisterKeys) unregisterKeys();
            listeners.length = 0;
        },
    };
}
