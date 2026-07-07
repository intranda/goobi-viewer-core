/**
 * Unit tests for the title page picker: option building, roving tabindex,
 * keyboard navigation, the go-to-page input, and the single-page static case.
 */
import { jest } from '@jest/globals';
import { setupPageDropdown } from '../ivPageDropdown.mjs';
import { createKeyDispatcher } from '../ivKeys.mjs';
import { Emitter } from '../ivViewer.mjs';

function fakeViewer(total, current = [0]) {
    return {
        getPageCount: () => total,
        getCurrentPages: () => current,
        goToPage: jest.fn(),
        onPageChange: new Emitter(),
    };
}

function mountMarkup() {
    document.body.innerHTML = `
        <button data-immersive-title-trigger aria-haspopup="dialog" aria-expanded="false" aria-controls="immersivePageDropdown"></button>
        <div id="immersivePageDropdown" hidden>
            <label for="immersivePageInput">Gehe zu Seite</label>
            <input id="immersivePageInput" type="number" />
            <ul id="immersivePageList"></ul>
        </div>`;
    return {
        trigger: document.querySelector('[data-immersive-title-trigger]'),
        dropdown: document.getElementById('immersivePageDropdown'),
        list: document.getElementById('immersivePageList'),
        input: document.getElementById('immersivePageInput'),
    };
}

const key = (k) => new KeyboardEvent('keydown', { key: k, bubbles: true });

describe('setupPageDropdown', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('single-page records get a static trigger without popup semantics', () => {
        const { trigger, list } = mountMarkup();
        setupPageDropdown(fakeViewer(1), [''], createKeyDispatcher());
        expect(trigger.classList.contains('immersive__title-trigger-static')).toBe(true);
        expect(trigger.hasAttribute('aria-haspopup')).toBe(false);
        expect(trigger.hasAttribute('aria-controls')).toBe(false);
        expect(list.children).toHaveLength(0);
    });

    test('builds one labelled option per page and marks the current page (roving tabindex)', () => {
        const { list } = mountMarkup();
        setupPageDropdown(fakeViewer(3, [1]), ['Titel', 'Seite 2', ''], createKeyDispatcher());
        const options = list.querySelectorAll('.immersive__page-dropdown-item');
        expect(options).toHaveLength(3);
        expect(options[1].textContent).toBe('2:Seite 2');
        expect(options[1].getAttribute('aria-selected')).toBe('true');
        expect(options[1].tabIndex).toBe(0);
        expect(options[0].tabIndex).toBe(-1);
    });

    test('the trigger toggles the dropdown and focuses the jump input on open', () => {
        const { trigger, dropdown, input } = mountMarkup();
        setupPageDropdown(fakeViewer(3), ['', '', ''], createKeyDispatcher());
        trigger.click();
        expect(dropdown.hidden).toBe(false);
        expect(trigger.getAttribute('aria-expanded')).toBe('true');
        expect(document.activeElement).toBe(input);
        trigger.click();
        expect(dropdown.hidden).toBe(true);
    });

    test('arrow keys move the option focus; Enter navigates and closes', () => {
        const { trigger, dropdown, list } = mountMarkup();
        const viewer = fakeViewer(3);
        setupPageDropdown(viewer, ['', '', ''], createKeyDispatcher());
        trigger.click();
        const options = list.querySelectorAll('.immersive__page-dropdown-item');
        options[0].focus();
        list.dispatchEvent(key('ArrowDown'));
        expect(document.activeElement).toBe(options[1]);
        list.dispatchEvent(key('End'));
        expect(document.activeElement).toBe(options[2]);
        list.dispatchEvent(key('Enter'));
        expect(viewer.goToPage).toHaveBeenCalledWith(2);
        expect(dropdown.hidden).toBe(true);
        expect(document.activeElement).toBe(trigger);
    });

    test('the jump input navigates on Enter within range and ignores out-of-range values', () => {
        const { trigger, dropdown, input } = mountMarkup();
        const viewer = fakeViewer(5);
        setupPageDropdown(viewer, ['', '', '', '', ''], createKeyDispatcher());
        trigger.click();
        input.value = '4';
        input.dispatchEvent(key('Enter'));
        expect(viewer.goToPage).toHaveBeenCalledWith(3);
        expect(dropdown.hidden).toBe(true);
        trigger.click();
        input.value = '9';
        input.dispatchEvent(key('Enter'));
        expect(viewer.goToPage).toHaveBeenCalledTimes(1);
        expect(dropdown.hidden).toBe(false);
    });

    test('Escape via the dispatcher closes the dropdown and focuses the trigger', () => {
        const { trigger, dropdown } = mountMarkup();
        const keys = createKeyDispatcher();
        setupPageDropdown(fakeViewer(3), ['', '', ''], keys);
        trigger.click();
        expect(keys.handleEvent({ key: 'Escape' })).toBe(true);
        expect(dropdown.hidden).toBe(true);
        expect(document.activeElement).toBe(trigger);
        expect(keys.handleEvent({ key: 'Escape' })).toBe(false);
    });

    test('page changes re-mark the active option', () => {
        const { trigger, list } = mountMarkup();
        const viewer = fakeViewer(3, [0]);
        setupPageDropdown(viewer, ['', '', ''], createKeyDispatcher());
        viewer.getCurrentPages = () => [2];
        viewer.onPageChange.emit(2);
        trigger.click();
        const options = list.querySelectorAll('.immersive__page-dropdown-item');
        expect(options[2].getAttribute('aria-selected')).toBe('true');
        expect(options[0].getAttribute('aria-selected')).toBe('false');
    });
});
