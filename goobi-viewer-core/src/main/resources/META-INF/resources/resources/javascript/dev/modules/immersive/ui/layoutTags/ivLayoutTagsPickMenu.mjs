/** Layout-tag pick menu: multi-select rows (one per tag or configured group) with optional filter; the trigger shows the selection as text and colour dots. */

import { formatMessage } from '../../data/ivLayoutTags.mjs';
import { el } from '../../shared/ivDom.mjs';
import { createDisposer } from '../../shared/ivLifecycle.mjs';
import { KEY_PRIORITY } from '../../shared/ivKeys.mjs';

const TRIGGER_DOTS = 4;

/**
 * @param {object} opts
 * @param {object} opts.state  the panel state (createLayoutTagsState)
 * @param {DOMStringMap} opts.labels  data-label-* of the panel
 * @param {HTMLElement} opts.trigger
 * @param {HTMLElement} opts.menu
 * @param {HTMLElement} opts.list
 * @param {HTMLElement|null} opts.dots
 * @param {HTMLElement|null} opts.text
 * @param {HTMLElement|null} opts.filterWrap
 * @param {HTMLInputElement|null} opts.filterInput
 * @param {{register: Function}} opts.keys
 * @param {(key: string) => void} opts.onToggle  called with the pick item's key (a tag label or a group key)
 * @returns {{build: Function, sync: Function, close: Function, setFilterVisible: Function, destroy: Function}}
 */
export function setupPickMenu({ state, labels, trigger, menu, list, dots, text, filterWrap, filterInput, keys, onToggle }) {
    const bag = createDisposer();
    const isOpen = () => !menu.hidden;
    const onOutside = (e) => {
        if (!menu.contains(e.target) && !trigger.contains(e.target)) close();
    };
    /** Opens the menu; `focus` is false when it opens on its own, so it never steals the focus. */
    const open = ({ focus = true } = {}) => {
        if (isOpen()) return;
        menu.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        document.addEventListener('pointerdown', onOutside, true);
        if (!focus) return;
        const first = filterWrap && !filterWrap.hidden && filterInput ? filterInput : menu.querySelector('input[type="checkbox"]:not([disabled])');
        if (first) first.focus();
    };
    const close = (restoreFocus = false) => {
        if (!isOpen()) return;
        menu.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        document.removeEventListener('pointerdown', onOutside, true);
        if (restoreFocus) trigger.focus();
    };

    const syncRows = () => {
        list.querySelectorAll('.immersive__layout-tags-pick-row').forEach((row) => {
            const item = state.itemByKey(row.dataset.itemKey);
            if (!item) return;
            const inRange = state.inRangeCount(item);
            const selection = state.itemSelection(item.key);
            const cb = row.querySelector('input');
            const count = row.querySelector('.immersive__layout-tags-pick-count');
            row.classList.toggle('-empty', !inRange);
            cb.checked = selection === 'all';
            cb.indeterminate = selection === 'some';
            cb.disabled = !inRange && selection === 'none';
            if (count) count.textContent = String(inRange);
        });
    };

    const build = () => {
        list.replaceChildren();
        const items = state.visibleItems();
        if (!items.length) {
            list.append(el('p', 'immersive__layout-tags-pick-none', labels.labelFilterNone || ''));
            return;
        }
        items.forEach((item, i) => {
            const row = el('div', 'immersive__layout-tags-pick-row');
            const tokens = state.styleTokens(item.key);
            row.classList.add(...tokens);
            row.dataset.itemKey = item.key;
            const id = `immersiveLayoutTagsPick-${i}`;
            const cb = el('input', 'immersive__layout-tags-pick-checkbox');
            cb.type = 'checkbox';
            cb.id = id;
            cb.addEventListener('change', () => onToggle(item.key));
            const label = el('label', 'immersive__layout-tags-pick-label');
            label.htmlFor = id;
            label.title = item.labels.join(', ');
            // a colour dot only for entries with a configured style class
            if (tokens.length) {
                const dot = el('span', 'immersive__layout-tags-dot');
                dot.setAttribute('aria-hidden', 'true');
                label.append(dot);
            }
            label.append(el('span', 'immersive__layout-tags-pick-name', item.name));
            row.append(cb, label, el('span', 'immersive__layout-tags-pick-count', String(state.inRangeCount(item))));
            list.append(row);
        });
        syncRows();
    };

    const sync = () => {
        const chosen = state.selectedItems();
        if (dots) {
            dots.replaceChildren();
            chosen
                .filter((item) => state.styleTokens(item.key).length)
                .slice(0, TRIGGER_DOTS)
                .forEach((item) => {
                    const dot = el('span', 'immersive__layout-tags-dot');
                    dot.classList.add(...state.styleTokens(item.key));
                    dots.append(dot);
                });
        }
        let caption = labels.labelPick || '';
        if (chosen.length === 1 || chosen.length === 2) caption = chosen.map((t) => t.name).join(', ');
        else if (chosen.length > 2) caption = formatMessage(labels.labelPickMore, chosen[0].name, chosen.length - 1);
        if (text) text.textContent = caption;
        trigger.setAttribute('aria-label', chosen.length ? formatMessage(labels.labelPickSelected, chosen.length) : labels.labelPick || '');
        syncRows();
    };

    bag.listen(trigger, 'click', () => (isOpen() ? close() : open()));
    if (filterInput) {
        bag.listen(filterInput, 'input', () => {
            state.setFilter(filterInput.value);
            build();
        });
    }
    bag.add(
        keys.register(KEY_PRIORITY.layoutTagsPick, (e) => {
            if (e.key !== 'Escape' || !isOpen()) return false;
            close(true);
            return true;
        })
    );

    return {
        build,
        sync,
        open,
        close,
        setFilterVisible(visible) {
            if (filterWrap) filterWrap.hidden = !visible;
        },
        destroy() {
            close();
            bag.dispose();
        },
    };
}
