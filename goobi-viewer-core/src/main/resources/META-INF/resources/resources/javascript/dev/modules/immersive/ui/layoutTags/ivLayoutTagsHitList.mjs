/** Layout-tag hit list: rows grouped by page, per-page counts and error markers, the active row and the progress line. */

import { formatMessage, entryKey, cursorIndex } from '../../data/ivLayoutTags.mjs';
import { el } from '../../shared/ivDom.mjs';

const REVEAL_MARGIN = 8;

/**
 * @param {object} opts
 * @param {object} opts.state  the panel state (createLayoutTagsState)
 * @param {DOMStringMap} opts.labels  data-label-* of the panel
 * @param {HTMLElement} opts.mount
 * @param {HTMLElement} opts.panel  the scroll container
 * @param {HTMLElement|null} opts.sticky
 * @param {HTMLElement|null} opts.progress
 * @param {(index: number) => void} opts.onActivate
 * @returns {{render: Function, markActive: Function, pendingRows: Function}}
 */
export function createHitList({ state, labels, mount, panel, sticky, progress, onActivate }) {
    let revealedKey = null;
    let builtKey = null;

    const syncCounts = () => {
        mount.querySelectorAll('.immersive__layout-tags-hit').forEach((btn) => {
            const page = Number(btn.dataset.pageNo);
            const label = btn.dataset.tagLabel;
            const occ = state.occurrencesOf(page, label);
            const count = occ ? occ.length : 0;
            const countEl = btn.querySelector('.immersive__layout-tags-hit-count');
            btn.classList.toggle('-pending', occ === undefined);
            btn.classList.toggle('-error', occ === null);
            countEl.textContent = occ === undefined ? '…' : occ === null ? '!' : count > 1 ? `(${count})` : '';
            let name = `${state.displayName(label)}, ${labels.labelPage || ''} ${page}`;
            if (occ === null) name += `, ${labels.labelError || ''}`;
            else if (occ !== undefined && count > 1) name += `, ${formatMessage(labels.labelOccurrences, count)}`;
            btn.setAttribute('aria-label', name);
            btn.title = occ === null ? labels.labelError || '' : '';
        });
    };

    const emptyText = () => {
        if (state.failed) return labels.labelError || '';
        if (!state.selected.size) return labels.labelNoneSelected || '';
        return (state.rangeIsFull() ? labels.labelEmpty : labels.labelEmptyRange) || '';
    };

    const build = () => {
        revealedKey = null;
        mount.replaceChildren();
        const list = state.failed || !state.selected.size ? [] : state.entries();
        if (!list.length) {
            mount.append(el('p', 'immersive__layout-tags-empty', emptyText()));
            return;
        }
        const ul = el('ul', 'immersive__layout-tags-hits');
        let currentPage = null;
        let typeList = null;
        list.forEach((entry, i) => {
            if (entry.page !== currentPage) {
                currentPage = entry.page;
                const pageLi = el('li', 'immersive__layout-tags-page');
                const pageNo = el('span', 'immersive__layout-tags-pageno');
                pageNo.append(el('span', 'sr-only', `${labels.labelPage || ''} `), document.createTextNode(String(entry.page)));
                typeList = el('ul', 'immersive__layout-tags-types');
                pageLi.append(pageNo, typeList);
                ul.append(pageLi);
            }
            const item = el('li');
            const btn = el('button', 'immersive__layout-tags-hit');
            btn.type = 'button';
            const tokens = state.styleTokens(entry.label);
            btn.classList.add(...tokens);
            btn.dataset.spotIndex = String(i);
            btn.dataset.spotKey = entryKey(entry);
            btn.dataset.pageNo = String(entry.page);
            btn.dataset.tagLabel = entry.label;
            // a colour dot only for tags with a configured style class
            if (tokens.length) {
                const dot = el('span', 'immersive__layout-tags-dot');
                dot.setAttribute('aria-hidden', 'true');
                btn.append(dot);
            }
            btn.append(el('span', 'immersive__layout-tags-hit-name', state.displayName(entry.label)), el('span', 'immersive__layout-tags-hit-count'));
            btn.addEventListener('click', () => onActivate(i));
            item.append(btn);
            typeList.append(item);
        });
        mount.append(ul);
        syncCounts();
    };

    const revealActive = () => {
        if (state.cursorKey === revealedKey) return;
        const row = mount.querySelector('.immersive__layout-tags-hit.-active');
        if (!row) return;
        revealedKey = state.cursorKey;
        const stickyHeight = sticky ? sticky.offsetHeight : 0;
        const top = row.offsetTop - panel.offsetTop;
        const bottom = top + row.offsetHeight;
        const viewTop = panel.scrollTop + stickyHeight;
        const viewBottom = panel.scrollTop + panel.clientHeight;
        if (top < viewTop) panel.scrollTop = Math.max(0, top - stickyHeight - REVEAL_MARGIN);
        else if (bottom > viewBottom) panel.scrollTop = bottom - panel.clientHeight + REVEAL_MARGIN;
    };

    const syncProgress = () => {
        if (!progress) return;
        const { total, loaded, failed } = state.progress();
        let text = '';
        if (failed) text = labels.labelError || '';
        else if (total && loaded < total) text = formatMessage(labels.labelProgressLoading, loaded, total);
        else if (total === 1) text = labels.labelProgressDoneOne || '';
        else if (total) text = formatMessage(labels.labelProgressDone, total);
        progress.textContent = text;
    };

    const markActive = () => {
        const list = state.entries();
        const cursor = cursorIndex(list, state.cursorKey);
        mount.querySelectorAll('.immersive__layout-tags-hit').forEach((btn) => {
            const on = list.length > 0 && Number(btn.dataset.spotIndex) === cursor;
            btn.classList.toggle('-active', on);
            if (on) btn.setAttribute('aria-current', 'true');
            else btn.removeAttribute('aria-current');
        });
        revealActive();
        syncProgress();
    };

    const render = (force = false) => {
        const key = state.compositionKey();
        if (force || key !== builtKey) {
            builtKey = key;
            const focused = document.activeElement;
            const focusKey = focused && focused.dataset ? focused.dataset.spotKey : null;
            build();
            if (focusKey) {
                const again = mount.querySelector(`[data-spot-key="${focusKey}"]`);
                if (again) again.focus({ preventScroll: true });
            }
        } else {
            syncCounts();
        }
        markActive();
    };

    const pendingRows = () =>
        Array.from(mount.querySelectorAll('.immersive__layout-tags-hit.-pending'))
            .filter((btn) => !state.inFlight.has(Number(btn.dataset.pageNo)))
            .map((btn) => {
                const r = btn.getBoundingClientRect();
                return { page: Number(btn.dataset.pageNo), top: r.top, bottom: r.bottom };
            });

    return { render, markActive, pendingRows };
}
