/** DOM tests for the annotations toggle. */
import { jest } from '@jest/globals';
import { setupAnnotationsToggle } from '../ui/fulltext/ivAnnotations.mjs';
import { createKeyDispatcher } from '../shared/ivKeys.mjs';

describe('setupAnnotationsToggle (jsdom)', () => {
    const storage = { getItem: () => null, setItem: () => {} };

    const buildToggle = () => {
        const root = document.createElement('div');
        root.className = 'immersive';
        const toggleRow = document.createElement('div');
        toggleRow.className = 'immersive__anno-toggle';
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        toggleRow.appendChild(checkbox);
        root.appendChild(toggleRow);
        return { root, toggleRow, checkbox };
    };

    test('hides the whole toggle row on pages without annotations', () => {
        const { root, toggleRow, checkbox } = buildToggle();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage });
        toggle.setPageData(null);
        expect(toggleRow.hidden).toBe(true);
        toggle.setPageData({ blocks: [], words: [], lines: [] });
        expect(toggleRow.hidden).toBe(true);
    });

    test('shows the toggle row again once a page has annotations', () => {
        const { root, toggleRow, checkbox } = buildToggle();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage });
        toggle.setPageData(null);
        toggle.setPageData({ blocks: [{ id: 'b-1', author: 'gr_jacob_grimm' }], words: [], lines: [] });
        expect(toggleRow.hidden).toBe(false);
        expect(checkbox.disabled).toBe(false);
    });

    test('applies the root state class only while the page actually has annotations', () => {
        const persistedOn = { getItem: () => 'true', setItem: () => {} };
        const { root, checkbox } = buildToggle();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage: persistedOn });
        // Persisted "on", but no page data yet: the class (and its min-width /
        // two-column styling) must not leak onto annotation-less pages.
        expect(root.classList.contains('-annotations-on')).toBe(false);
        toggle.setPageData({ blocks: [{ id: 'b-1', author: 'gr_jacob_grimm' }], words: [], lines: [] });
        expect(root.classList.contains('-annotations-on')).toBe(true);
        toggle.setPageData(null);
        expect(root.classList.contains('-annotations-on')).toBe(false);
    });

    test("the 'a' shortcut leaves the key to OpenSeadragon while the stage has focus (WASD panning)", () => {
        const { root, checkbox } = buildToggle();
        document.body.appendChild(root);
        document.body.insertAdjacentHTML('beforeend', '<div data-immersive-image><div id="osdCanvas" tabindex="0"></div></div>');
        const keys = createKeyDispatcher();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage, keys });
        toggle.setPageData({ blocks: [{ id: 'b-1', author: 'gr_jacob_grimm' }], words: [], lines: [] });
        const canvas = document.getElementById('osdCanvas');
        canvas.focus();
        expect(keys.handleEvent({ key: 'a', target: canvas })).toBe(false);
        expect(checkbox.checked).toBe(false);
        canvas.blur();
        expect(keys.handleEvent({ key: 'a', target: document.body })).toBe(true);
        expect(checkbox.checked).toBe(true);
        document.body.innerHTML = '';
    });

    test('onChange returns an unsubscribe; destroy detaches checkbox and shortcut', () => {
        const { root, checkbox } = buildToggle();
        const keys = createKeyDispatcher();
        const toggle = setupAnnotationsToggle({ root, checkbox, storage, keys });
        toggle.setPageData({ blocks: [{ id: 'b-1', author: 'gr_jacob_grimm' }], words: [], lines: [] });
        const seen = jest.fn();
        toggle.onChange(seen)();
        checkbox.checked = true;
        checkbox.dispatchEvent(new Event('change'));
        expect(seen).not.toHaveBeenCalled();
        expect(root.classList.contains('-annotations-on')).toBe(true);
        toggle.destroy();
        checkbox.checked = false;
        checkbox.dispatchEvent(new Event('change'));
        expect(root.classList.contains('-annotations-on')).toBe(true);
        expect(keys.handleEvent({ key: 'a', target: document.body })).toBe(false);
    });
});
