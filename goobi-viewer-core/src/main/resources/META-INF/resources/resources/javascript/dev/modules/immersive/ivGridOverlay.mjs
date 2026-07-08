/** Thumbnail overview overlay: lazy riot grid with modal focus handling. */

import { normalizeThumbSizeStep, readThumbSizeStep, writeThumbSizeStep, THUMB_SIZE_MAX, currentOrder } from './viewerImmersive.mjs';
import { createFocusTrap } from './ivA11y.mjs';

/**
 * Mounts the thumbnail grid overlay lazily (riot `thumbnails` tag) with modal
 * focus handling and keeps its selection on the current page. Also wires the
 * thumbnail-size slider (persisted via localStorage) and a back-to-top button
 * for the grid scroll container. Escape closes via the key dispatcher
 * (priority 20: under the shortcuts modal, above dropdown and panels).
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 * @param {{register: Function}} keys  the immersive key dispatcher
 * @returns {function():void} toggles the overlay
 */
export function setupOverviewGrid(viewer, pi, apiBase, keys) {
    const GRID_LOADER_TIMEOUT_MS = 8000;
    const GRID_TOP_VISIBLE_AFTER_PX = 200;
    // IIIF size; must cover the largest grid step (230x330 CSS px) on HiDPI
    const GRID_THUMB_IMAGE_SIZE = '!400,560';
    const gridOverlay = document.getElementById('immersiveGridOverlay');
    const gridLoader = document.getElementById('immersiveGridLoader');
    const gridClose = gridOverlay && gridOverlay.querySelector('.immersive__grid-close');
    const gridTrigger = document.querySelector('[data-immersive-action="overview"]:not(.immersive__grid-close)');
    const gridScroll = gridOverlay && gridOverlay.querySelector('.immersive__grid-scroll');
    const gridTopBtn = gridOverlay && gridOverlay.querySelector('.immersive__grid-top');
    const gridSizeSlider = document.getElementById('immersiveGridSize');
    const gridSizeLabels = gridSizeSlider ? [gridSizeSlider.dataset.labelSmall, gridSizeSlider.dataset.labelMedium, gridSizeSlider.dataset.labelLarge] : [];
    const applyGridThumbSize = (step) => {
        if (gridOverlay) gridOverlay.setAttribute('data-thumb-size', String(step));
        if (gridSizeSlider) {
            gridSizeSlider.value = String(step);
            gridSizeSlider.style.setProperty('--immersive-slider-fill', `${(step / THUMB_SIZE_MAX) * 100}%`);
            if (gridSizeLabels[step]) {
                gridSizeSlider.setAttribute('aria-valuetext', gridSizeLabels[step]);
            } else {
                gridSizeSlider.removeAttribute('aria-valuetext');
            }
        }
    };
    applyGridThumbSize(readThumbSizeStep(window.localStorage));
    if (gridSizeSlider) {
        gridSizeSlider.addEventListener('input', () => {
            const step = normalizeThumbSizeStep(gridSizeSlider.value);
            applyGridThumbSize(step);
            writeThumbSizeStep(window.localStorage, step);
        });
    }
    if (gridScroll && gridTopBtn) {
        gridTopBtn.addEventListener('click', () => {
            const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
            gridScroll.scrollTo({ top: 0, behavior });
        });
        gridScroll.addEventListener(
            'scroll',
            () => {
                const hide = gridScroll.scrollTop < GRID_TOP_VISIBLE_AFTER_PX;
                if (hide && document.activeElement === gridTopBtn && gridClose) gridClose.focus();
                if (gridTopBtn.hidden !== hide) gridTopBtn.hidden = hide;
            },
            { passive: true }
        );
    }
    let gridMounted = false;
    let gridTag = null;
    let gridOpener = null;
    let gridTrapHandler = null;
    const gridActions = new rxjs.Subject();
    gridActions.subscribe((e) => {
        if (e && e.action === 'clickImage' && typeof e.value === 'number') {
            viewer.goToPage(e.value);
            closeGrid();
        }
    });
    const syncGridSelection = () => {
        if (gridTag) {
            gridTag.opts.index = currentOrder(viewer);
            gridTag.update();
        }
    };
    viewer.onPageChange.subscribe(() => {
        if (gridOverlay && !gridOverlay.hidden) syncGridSelection();
    });
    const openGrid = () => {
        gridOpener = gridTrigger || document.activeElement;
        gridOverlay.hidden = false;
        gridOverlay.setAttribute('role', 'dialog');
        gridOverlay.setAttribute('aria-modal', 'true');
        const gridLabel = gridTrigger && gridTrigger.getAttribute('aria-label');
        if (gridLabel) gridOverlay.setAttribute('aria-label', gridLabel);
        gridTrapHandler = createFocusTrap(gridOverlay);
        gridOverlay.addEventListener('keydown', gridTrapHandler);
        if (!gridMounted) {
            gridTag = riot.mount('#immersiveThumbnails', 'thumbnails', {
                source: `${apiBase}/records/${pi}/manifest`,
                type: 'sequence',
                actionlistener: gridActions,
                imagesize: GRID_THUMB_IMAGE_SIZE,
                index: currentOrder(viewer),
            })[0];
            gridMounted = true;
            let gridLoaderDone = false;
            let gridLoaderTimer;
            const hideGridLoader = () => {
                if (gridLoaderDone) return;
                gridLoaderDone = true;
                clearTimeout(gridLoaderTimer);
                if (gridLoader) gridLoader.hidden = true;
            };
            const thumbsMount = document.getElementById('immersiveThumbnails');
            if (thumbsMount) thumbsMount.addEventListener('load', hideGridLoader, { capture: true, once: true });
            gridLoaderTimer = setTimeout(hideGridLoader, GRID_LOADER_TIMEOUT_MS);
        } else {
            syncGridSelection();
        }
        if (gridTrigger) gridTrigger.setAttribute('aria-expanded', 'true');
        if (gridClose) gridClose.focus();
    };
    const closeGrid = () => {
        if (!gridOverlay || gridOverlay.hidden) return;
        gridOverlay.hidden = true;
        if (gridTrigger) gridTrigger.setAttribute('aria-expanded', 'false');
        if (gridTrapHandler) {
            gridOverlay.removeEventListener('keydown', gridTrapHandler);
            gridTrapHandler = null;
        }
        const restore = gridOpener || gridTrigger;
        gridOpener = null;
        if (restore && typeof restore.focus === 'function') restore.focus();
    };
    const toggleGrid = () => {
        if (!gridOverlay) return;
        if (gridOverlay.hidden) openGrid();
        else closeGrid();
    };

    keys.register(20, (e) => {
        if (e.key !== 'Escape' || !gridOverlay || gridOverlay.hidden) return false;
        closeGrid();
        return true;
    });
    return toggleGrid;
}
