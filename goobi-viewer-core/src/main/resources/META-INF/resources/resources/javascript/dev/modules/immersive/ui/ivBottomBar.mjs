/** Bottom bar and page-status wiring: indicator and actions. */

import { createDisposer } from '../shared/ivLifecycle.mjs';

/**
 * Wires the paging UI and the bottom-bar actions: page indicator (bottom bar +
 * title pill + canvas aria-label) and the action buttons (zoom, rotate, reset,
 * double page, overview).
 *
 * Fullscreen is not among them: it is a page of its own (the fullscreen route
 * renders this view edge to edge), reached by the plain link in the right
 * toolbar, so nothing here calls or watches the Fullscreen API.
 *
 * @param {HTMLElement} el  the [data-immersive-image] mount element
 * @param {IvViewer} viewer
 * @param {object} hooks
 * @param {Function} hooks.toggleGrid  toggles the thumbnail overview overlay
 * @param {Function} hooks.updateFulltextAvail  fulltext availability follows double-page mode
 * @returns {{destroy: function():void}}
 */
export function setupBottomBar(el, viewer, { toggleGrid, updateFulltextAvail }) {
    const bag = createDisposer();
    const indicator = document.getElementById('immersivePageIndicator');
    const titlePage = document.getElementById('immersiveTitlePage');
    const total = viewer.getPageCount();
    const workTitle = (document.querySelector('.immersive__title-text')?.textContent || '').trim();
    const updateIndicator = () => {
        const pages = viewer.getCurrentPages().map((p) => p + 1);
        const label = pages.length > 1 ? `${pages[0]}–${pages[pages.length - 1]}` : `${pages[0]}`;
        if (indicator) indicator.textContent = `${label} / ${total}`;
        if (titlePage) titlePage.textContent = `(${label} / ${total})`;
        const imageSurface = el.querySelector('.openseadragon-canvas') || el;
        imageSurface.setAttribute('role', 'img');
        imageSurface.setAttribute('aria-label', workTitle ? `${workTitle}, ${label} / ${total}` : `${label} / ${total}`);
    };
    updateIndicator();
    bag.add(viewer.onPageChange.subscribe(() => updateIndicator()));

    document.querySelectorAll('[data-immersive-page]').forEach((btn) => {
        bag.listen(btn, 'click', () => (btn.dataset.immersivePage === 'next' ? viewer.next() : viewer.prev()));
    });
    document.querySelectorAll('[data-immersive-action]').forEach((btn) => {
        bag.listen(btn, 'click', () => {
            const action = btn.dataset.immersiveAction;
            if (action === 'zoom-in') viewer.zoomIn();
            else if (action === 'zoom-out') viewer.zoomOut();
            else if (action === 'rotate-left') viewer.rotateLeft();
            else if (action === 'rotate-right') viewer.rotateRight();
            else if (action === 'reset') viewer.resetView();
            else if (action === 'overview') toggleGrid();
            else if (action === 'double-page') {
                const on = viewer.toggleDoublePage();
                btn.setAttribute('aria-pressed', String(on));
                btn.classList.toggle('-active', on);
                document.querySelector('.immersive__viewer')?.classList.toggle('is-double-page', on);
                updateFulltextAvail();
            }
        });
    });

    return { destroy: bag.dispose };
}
