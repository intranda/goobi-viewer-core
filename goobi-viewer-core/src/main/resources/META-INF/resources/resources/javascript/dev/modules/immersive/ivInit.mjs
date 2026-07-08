/** Immersive view bootstrap: engine construction plus the UI wiring modules. */

import IvViewer from './ivViewer.mjs';
import { loadPageServices, loadPageLabels } from './ivManifestSource.mjs';
import { attachUrlSync } from './viewerImmersive.mjs';
import { createKeyDispatcher } from './ivKeys.mjs';
import { setupPanels, setupPanelResize, setupToc, setupMetadataToggle } from './ivPanelsWiring.mjs';
import { setupShortcutsModal } from './ivShortcutsModal.mjs';
import { setupFulltextPanel } from './ivFulltextPanel.mjs';
import { setupFulltextSearch } from './ivSearchPanel.mjs';
import { setupPageDropdown } from './ivPageDropdown.mjs';
import { setupOverviewGrid } from './ivGridOverlay.mjs';
import { setupBottomBar } from './ivBottomBar.mjs';
import { bindImageFiltersMount, setupImmersivePopoverA11y } from './ivImageFilters.mjs';

/** Hides the stage loading indicator at the latest after this, even without a painted tile. */
const STAGE_LOADER_TIMEOUT_MS = 8000;

/**
 * Bootstraps the immersive image viewer: fetches the IIIF manifest, instantiates
 * the engine, and attaches the wiring modules. Keyboard shortcuts run through
 * one dispatcher whose priorities define the overlay precedence: shortcuts
 * modal (10) → grid overlay (20) → page dropdown (30) → panels (40).
 *
 * @param {HTMLElement} el  the [data-immersive-image] mount element
 */
export function initImmersiveViewer(el) {
    const pi = el.dataset.pi;
    const apiBase = el.dataset.apiBase;
    const startOrder = Number(el.dataset.startOrder) || 0;
    const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom, 10) : undefined;
    const immersiveRoot = el.closest('.immersive');

    const keys = createKeyDispatcher();
    keys.attach(document);
    const panels = setupPanels(immersiveRoot, keys);
    setupPanelResize(immersiveRoot);
    setupMetadataToggle();
    setupImmersivePopoverA11y();
    setupShortcutsModal(panels.closePanels, keys);

    // Stage loading indicator (same pattern as the thumbnail grid): hidden once
    // the first tile has been painted, with a timeout and init-failure fallback.
    const stageLoader = document.getElementById('immersiveStageLoader');
    let stageLoaderDone = false;
    const hideStageLoader = () => {
        if (stageLoaderDone) return;
        stageLoaderDone = true;
        if (stageLoader) stageLoader.hidden = true;
    };
    setTimeout(hideStageLoader, STAGE_LOADER_TIMEOUT_MS);

    loadPageServices(pi, apiBase)
        .then((services) => {
            const viewer = new IvViewer({ element: el, services, startOrder, maxZoom });
            viewer.viewer.openseadragon.addOnceHandler('tile-loaded', hideStageLoader);
            attachUrlSync(viewer, pi);
            bindImageFiltersMount(viewer);

            loadPageLabels(pi, apiBase, fetch, document.documentElement.lang)
                .then((labels) => setupPageDropdown(viewer, labels, keys))
                .catch((e) => console.warn('immersive page labels failed', e));

            setupToc(viewer);
            setupFulltextSearch(viewer, pi, apiBase);
            const fulltext = setupFulltextPanel(viewer, pi, apiBase, panels);
            const toggleGrid = setupOverviewGrid(viewer, pi, apiBase, keys);
            setupBottomBar(el, viewer, { toggleGrid, updateFulltextAvail: fulltext.updateFulltextAvail });
        })
        .catch((e) => {
            hideStageLoader();
            console.error('immersive viewer init failed', e);
        });
}
