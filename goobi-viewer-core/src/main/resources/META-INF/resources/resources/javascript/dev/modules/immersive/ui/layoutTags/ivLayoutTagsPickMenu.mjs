/** Layout-tag pick menu: multi-select rows with optional filter; the trigger shows the selection as text and colour dots. */

import { formatMessage } from '../../data/ivLayoutTags.mjs';
import { styleClassTokens, el } from '../../shared/ivDom.mjs';
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
 * @param {(label: string) => void} opts.onToggle
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
            const tag = state.tagByLabel(row.dataset.tagLabel);
            if (!tag) return;
            const inRange = state.inRangeCount(tag);
            const selected = state.selected.has(tag.label);
            const cb = row.querySelector('input');
            const count = row.querySelector('.immersive__layout-tags-pick-count');
            row.classList.toggle('-empty', !inRange);
            cb.checked = selected;
            cb.disabled = !inRange && !selected;
            if (count) count.textContent = String(inRange);
        });
    };

    const build = () => {
        list.replaceChildren();
        const tags = state.visibleTags();
        if (!tags.length) {
            list.append(el('p', 'immersive__layout-tags-pick-none', labels.labelFilterNone || ''));
            return;
        }
        tags.forEach((tag, i) => {
            const row = el('div', 'immersive__layout-tags-pick-row');
            row.classList.add(...state.styleTokens(tag.label));
            row.dataset.tagLabel = tag.label;
            const id = `immersiveLayoutTagsPick-${i}`;
            const cb = el('input', 'immersive__layout-tags-pick-checkbox');
            cb.type = 'checkbox';
            cb.id = id;
            cb.addEventListener('change', () => onToggle(tag.label));
            const label = el('label', 'immersive__layout-tags-pick-label');
            label.htmlFor = id;
            label.title = tag.label;
            const dot = el('span', 'immersive__layout-tags-dot');
            dot.setAttribute('aria-hidden', 'true');
            label.append(dot, el('span', 'immersive__layout-tags-pick-name', tag.name));
            row.append(cb, label, el('span', 'immersive__layout-tags-pick-count', String(state.inRangeCount(tag))));
            list.append(row);
        });
        syncRows();
    };

    const sync = () => {
        const chosen = state.selectedTags();
        if (dots) {
            dots.replaceChildren();
            chosen
                .filter((tag) => styleClassTokens(tag.styleClass).length)
                .slice(0, TRIGGER_DOTS)
                .forEach((tag) => {
                    const dot = el('span', 'immersive__layout-tags-dot');
                    dot.classList.add(...styleClassTokens(tag.styleClass));
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
