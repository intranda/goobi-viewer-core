/** Bottom bar and page-status wiring: indicator, chevrons, actions, fullscreen. */

/**
 * Wires the paging UI and the bottom-bar actions: page indicator (bottom bar +
 * title pill + canvas aria-label), chevron visibility, the action buttons
 * (zoom, rotate, reset, double page, overview, fullscreen) and the
 * fullscreen-change bookkeeping incl. re-homing Bootstrap popovers into the
 * fullscreen element.
 *
 * @param {HTMLElement} el  the [data-immersive-image] mount element
 * @param {IvViewer} viewer
 * @param {object} hooks
 * @param {Function} hooks.toggleGrid  toggles the thumbnail overview overlay
 * @param {Function} hooks.updateFulltextAvail  fulltext availability follows double-page mode
 */
export function setupBottomBar(el, viewer, { toggleGrid, updateFulltextAvail }) {
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
    viewer.onPageChange.subscribe(() => updateIndicator());

    const prevChevron = document.querySelector('.immersive__chevron[data-immersive-page="prev"]');
    const nextChevron = document.querySelector('.immersive__chevron[data-immersive-page="next"]');
    const updateChevrons = () => {
        const pages = viewer.getCurrentPages();
        if (!pages.length) return;
        if (prevChevron) prevChevron.hidden = Math.min(...pages) <= 0;
        if (nextChevron) nextChevron.hidden = Math.max(...pages) >= total - 1;
    };
    updateChevrons();
    viewer.onPageChange.subscribe(updateChevrons);

    document.querySelectorAll('[data-immersive-page]').forEach((btn) => {
        btn.addEventListener('click', () => (btn.dataset.immersivePage === 'next' ? viewer.next() : viewer.prev()));
    });
    document.querySelectorAll('[data-immersive-action]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const action = btn.dataset.immersiveAction;
            if (action === 'zoom-in') viewer.zoomIn();
            else if (action === 'zoom-out') viewer.zoomOut();
            else if (action === 'rotate-left') viewer.rotateLeft();
            else if (action === 'rotate-right') viewer.rotateRight();
            else if (action === 'reset') viewer.resetView();
            else if (action === 'fullscreen') toggleImmersiveFullscreen();
            else if (action === 'overview') toggleGrid();
            else if (action === 'double-page') {
                const on = viewer.toggleDoublePage();
                btn.setAttribute('aria-pressed', String(on));
                btn.classList.toggle('immersive__tool-btn--active', on);
                document.querySelector('.immersive__viewer')?.classList.toggle('is-double-page', on);
                updateFulltextAvail();
            }
        });
    });

    const fullscreenBtn = document.querySelector('[data-immersive-action="fullscreen"]');
    document.addEventListener('fullscreenchange', () => {
        if (fullscreenBtn) {
            fullscreenBtn.setAttribute('aria-pressed', String(!!document.fullscreenElement));
            const exitLabel = fullscreenBtn.dataset.labelExit;
            const enterLabel = fullscreenBtn.dataset.labelEnter || fullscreenBtn.getAttribute('aria-label');
            const label = document.fullscreenElement && exitLabel ? exitLabel : enterLabel;
            if (label) {
                fullscreenBtn.setAttribute('aria-label', label);
                fullscreenBtn.setAttribute('title', label);
            }
        }
        document.querySelectorAll('[data-popover-element]').forEach((trigger) => {
            const $trigger = window.$ && window.$(trigger);
            const inst = $trigger && $trigger.data('bs.popover');
            if (!inst) return;
            $trigger.popover('hide');
            if (document.fullscreenElement) {
                if (inst.config._savedContainer === undefined) {
                    inst.config._savedContainer = inst.config.container;
                }
                inst.config.container = document.fullscreenElement;
            } else if (inst.config._savedContainer !== undefined) {
                inst.config.container = inst.config._savedContainer;
                delete inst.config._savedContainer;
            }
        });
    });
}

/** Toggles native browser fullscreen on the immersive viewer hero (Esc exits). */
function toggleImmersiveFullscreen() {
    const el = document.querySelector('.immersive__viewer');
    if (!el) return;
    if (document.fullscreenElement) {
        document.exitFullscreen();
    } else {
        el.requestFullscreen?.();
    }
}
