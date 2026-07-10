/** Immersive view bootstrap: engine construction plus the UI wiring modules. */

import IvViewer from './ivViewer.mjs';
import { loadPageServices, loadPageLabels, loadPageAccess } from './ivManifestSource.mjs';
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
 * Bootstraps the immersive view. The chrome (panels, keyboard dispatcher,
 * shortcuts modal, metadata fold) is wired whenever the view is present, even
 * without VIEW_IMAGES. The image engine and its dependent wiring (bottom bar,
 * grid, fulltext, search, page dropdown, TOC sync) are only built when the
 * server rendered the [data-immersive-image] mount, i.e. the user may view the
 * images. Keyboard shortcuts run through one dispatcher whose priorities define
 * the overlay precedence: shortcuts modal (10) → grid overlay (20) → page
 * dropdown (30) → panels (40).
 *
 * @param {HTMLElement} immersiveRoot  the .immersive root element
 */
export function initImmersiveViewer(immersiveRoot) {
    const keys = createKeyDispatcher();
    keys.attach(document);
    const panels = setupPanels(immersiveRoot, keys);
    setupPanelResize(immersiveRoot);
    setupMetadataToggle();
    setupImmersivePopoverA11y();
    setupShortcutsModal(panels.closePanels, keys);

    // No VIEW_IMAGES: the stage shows a server-rendered access-denied placeholder
    // and the image-dependent chrome is not rendered, so there is no viewer to build.
    const el = immersiveRoot.querySelector('[data-immersive-image]');
    if (!el) return;

    const pi = el.dataset.pi;
    const apiBase = el.dataset.apiBase;
    const startOrder = Number(el.dataset.startOrder) || 0;
    const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom, 10) : undefined;
    // Server-rendered VIEW/ZOOM_IMAGES privilege; absent attribute defaults to allowed.
    const allowZoom = el.dataset.allowZoom !== 'false';

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

    const deniedText = el.dataset.msgDenied || '';

    // Services and per-page access share the memoized manifest fetch (one request).
    Promise.all([loadPageServices(pi, apiBase), loadPageAccess(pi, apiBase)])
        .then(([services, restricted]) => {
            const viewer = new IvViewer({ element: el, services, startOrder, maxZoom, allowZoom, restricted, deniedText });
            viewer.viewer.openseadragon.addOnceHandler('tile-loaded', hideStageLoader);
            // A restricted entry page never fires tile-loaded (tiles 403); onOpen fires
            // once the (failed) load settles, so the loader is not left spinning.
            viewer.onOpen.subscribe(hideStageLoader);
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
