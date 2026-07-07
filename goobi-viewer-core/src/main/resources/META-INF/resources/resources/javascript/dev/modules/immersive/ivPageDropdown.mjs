/** Title page picker: "go to page" input plus a labelled listbox per page. */

/**
 * Builds the title page picker: a "go to page" input plus a listbox with one
 * option per page, labelled with the manifest page labels. Registers its
 * Escape handling on the key dispatcher (priority 30: under the modal and the
 * grid, above the panels).
 *
 * @param {IvViewer} viewer
 * @param {string[]} labels page labels, index-aligned with the page orders
 * @param {{register: Function}} keys  the immersive key dispatcher
 */
export function setupPageDropdown(viewer, labels, keys) {
    const total = viewer.getPageCount();
    const trigger = document.querySelector('[data-immersive-title-trigger]');
    const dropdown = document.getElementById('immersivePageDropdown');
    const list = document.getElementById('immersivePageList');
    const input = document.getElementById('immersivePageInput');
    if (!trigger || !dropdown || !list) return;
    if (total < 2) {
        trigger.classList.add('immersive__title-trigger--static');
        // no dropdown for single-page records: drop the popup semantics announced by the markup
        trigger.removeAttribute('aria-haspopup');
        trigger.removeAttribute('aria-expanded');
        trigger.removeAttribute('aria-controls');
        return;
    }
    if (input) input.max = String(total);
    const listLabel = input && input.labels && input.labels[0] ? input.labels[0].textContent.trim() : '';
    if (listLabel) list.setAttribute('aria-label', listLabel);

    const items = [];
    for (let order = 0; order < total; order++) {
        const li = document.createElement('li');
        li.setAttribute('role', 'presentation');
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'immersive__page-dropdown-item';
        btn.dataset.order = String(order);
        btn.setAttribute('role', 'option');
        btn.setAttribute('aria-selected', 'false');
        btn.tabIndex = -1;
        const num = document.createElement('span');
        num.className = 'immersive__page-dropdown-number';
        num.textContent = `${order + 1}:`;
        const lbl = document.createElement('span');
        lbl.className = 'immersive__page-dropdown-item-label';
        lbl.textContent = (labels[order] || '').trim();
        btn.append(num, lbl);
        li.append(btn);
        list.append(li);
        items.push(btn);
    }

    const markActive = () => {
        const current = viewer.getCurrentPages();
        let rovingSet = false;
        items.forEach((btn) => {
            const on = current.includes(Number(btn.dataset.order));
            btn.classList.toggle('immersive__page-dropdown-item--active', on);
            btn.setAttribute('aria-selected', on ? 'true' : 'false');
            btn.tabIndex = on && !rovingSet ? 0 : -1;
            if (on) rovingSet = true;
        });
        if (!rovingSet && items.length) items[0].tabIndex = 0;
    };

    const focusOption = (idx) => {
        if (idx < 0) idx = 0;
        if (idx > items.length - 1) idx = items.length - 1;
        const target = items[idx];
        if (!target) return;
        items.forEach((btn) => (btn.tabIndex = btn === target ? 0 : -1));
        target.focus();
    };

    const isOpen = () => !dropdown.hidden;
    const onOutside = (e) => {
        if (!dropdown.contains(e.target) && !trigger.contains(e.target)) close();
    };
    const close = () => {
        if (!isOpen()) return;
        dropdown.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        document.removeEventListener('pointerdown', onOutside, true);
    };
    const open = () => {
        if (isOpen()) return;
        markActive();
        dropdown.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        document.addEventListener('pointerdown', onOutside, true);
        const active = list.querySelector('.immersive__page-dropdown-item--active');
        if (active) {
            list.scrollTop = Math.max(0, active.offsetTop - list.offsetTop - (list.clientHeight - active.clientHeight) / 2);
        }
        if (input) {
            input.value = '';
            input.focus({ preventScroll: true });
        }
    };

    trigger.addEventListener('click', () => (isOpen() ? close() : open()));
    list.addEventListener('click', (e) => {
        const btn = e.target.closest('.immersive__page-dropdown-item');
        if (!btn) return;
        viewer.goToPage(Number(btn.dataset.order));
        close();
    });
    list.addEventListener('keydown', (e) => {
        const cur = items.indexOf(document.activeElement);
        if (cur === -1 && !['Escape'].includes(e.key)) return;
        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                focusOption(cur + 1);
                break;
            case 'ArrowUp':
                e.preventDefault();
                focusOption(cur - 1);
                break;
            case 'Home':
                e.preventDefault();
                focusOption(0);
                break;
            case 'End':
                e.preventDefault();
                focusOption(items.length - 1);
                break;
            case 'Enter':
            case ' ':
            case 'Spacebar':
                e.preventDefault();
                viewer.goToPage(Number(items[cur].dataset.order));
                close();
                trigger.focus();
                break;
            case 'Escape':
                e.preventDefault();
                close();
                trigger.focus();
                break;
            default:
                break;
        }
    });
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (e.key !== 'Enter') return;
            e.preventDefault();
            const n = parseInt(input.value, 10);
            if (Number.isFinite(n) && n >= 1 && n <= total) {
                viewer.goToPage(n - 1);
                close();
            }
        });
    }
    keys.register(30, (e) => {
        if (e.key !== 'Escape' || !isOpen()) return false;
        close();
        trigger.focus();
        return true;
    });
    viewer.onPageChange.subscribe(markActive);
    markActive();
}
