/** Immersive view bootstrap: engine construction plus the UI wiring modules. */

import IvViewer from './engine/ivViewer.mjs';
import { buildDeniedPlaceholder } from './engine/ivAccessOverlay.mjs';
import { loadPageServices, loadPageLabels, loadPageAccess } from './data/ivManifestSource.mjs';
import { attachUrlSync } from './ui/ivUrlSync.mjs';
import { createKeyDispatcher, registerViewerNavKeys } from './shared/ivKeys.mjs';
import { setupPanels, setupPanelResize, setupToc, setupMetadataToggle } from './ui/ivPanelsWiring.mjs';
import { setupShortcutsModal } from './ui/ivShortcutsModal.mjs';
import { setupFulltextPanel } from './ui/fulltext/ivFulltextPanel.mjs';
import { setupLayoutTagsPanel } from './ui/layoutTags/ivLayoutTagsPanel.mjs';
import { setupSearchPanel } from './ui/ivSearchPanel.mjs';
import { setupPageDropdown } from './ui/ivPageDropdown.mjs';
import { setupOverviewGrid } from './ui/ivGridOverlay.mjs';
import { setupBottomBar } from './ui/ivBottomBar.mjs';
import { setupImageFilters, setupImmersivePopoverA11y } from './ui/ivImageFilters.mjs';

/** Hides the stage loading indicator at the latest after this, even without a painted tile. */
const STAGE_LOADER_TIMEOUT_MS = 8000;

/** Fills the stage with the access-denied styled placeholder, worded for a failed load. */
function showStageError(el) {
    const text = el.dataset.msgError;
    if (!text) return;
    if (getComputedStyle(el).position === 'static') el.style.position = 'relative';
    el.appendChild(buildDeniedPlaceholder({ text, role: 'alert' }));
}

/**
 * Bootstraps the immersive view. The chrome (panels, keyboard dispatcher,
 * shortcuts modal, metadata fold) is wired whenever the view is present, even
 * without VIEW_IMAGES. The image engine and its dependent wiring (bottom bar,
 * grid, fulltext, search, layout tags, page dropdown, TOC sync) are only built when the
 * server rendered the [data-immersive-image] mount, i.e. the user may view the
 * images. Keyboard shortcuts run through one dispatcher; KEY_PRIORITY in
 * shared/ivKeys defines who wins a key. Every setup module returns a destroyable; the
 * returned handle tears all of them down along with the engine.
 *
 * @param {HTMLElement} immersiveRoot  the .immersive root element
 * @returns {{destroy: function():void}}
 */
export function initImmersiveViewer(immersiveRoot) {
    const parts = [];
    const use = (part) => {
        parts.push(part);
        return part;
    };
    let viewer = null;
    let destroyed = false;
    let stageLoaderTimer = null;
    const handle = {
        destroy() {
            destroyed = true;
            clearTimeout(stageLoaderTimer);
            while (parts.length) parts.pop().destroy();
            if (viewer) viewer.destroy();
            viewer = null;
            keys.detach();
        },
    };

    const keys = createKeyDispatcher();
    keys.attach(document);
    const panels = use(setupPanels(immersiveRoot, keys));
    use(setupPanelResize(immersiveRoot));
    use(setupMetadataToggle());
    use(setupImmersivePopoverA11y());
    use(setupShortcutsModal(panels.closePanels, keys));

    const el = immersiveRoot.querySelector('[data-immersive-image]');
    if (!el) return handle;

    const pi = el.dataset.pi;
    const apiBase = el.dataset.apiBase;
    const startOrder = Number(el.dataset.startOrder) || 0;
    const maxZoom = el.dataset.maxZoom ? parseInt(el.dataset.maxZoom, 10) : undefined;
    const allowZoom = el.dataset.allowZoom !== 'false';

    const stageLoader = document.getElementById('immersiveStageLoader');
    let stageLoaderDone = false;
    const hideStageLoader = () => {
        if (stageLoaderDone) return;
        stageLoaderDone = true;
        if (stageLoader) stageLoader.hidden = true;
    };
    stageLoaderTimer = setTimeout(hideStageLoader, STAGE_LOADER_TIMEOUT_MS);

    const deniedText = el.dataset.msgDenied || '';

    Promise.all([loadPageServices(pi, apiBase), loadPageAccess(pi, apiBase)])
        .then(([services, restricted]) => {
            if (destroyed) return;
            viewer = new IvViewer({
                element: el,
                services,
                startOrder,
                maxZoom,
                allowZoom,
                restricted,
                deniedText,
                deniedContent: document.getElementById('immersiveDeniedContent'),
            });
            viewer.onceTileLoaded(hideStageLoader);
            viewer.onOpen.subscribe(hideStageLoader);
            use(attachUrlSync(viewer, pi));
            use(setupImageFilters(viewer));
            use(registerViewerNavKeys(keys, viewer, el));

            loadPageLabels(pi, apiBase, fetch, document.documentElement.lang)
                .then((labels) => {
                    if (!destroyed) use(setupPageDropdown(viewer, labels, keys));
                })
                .catch((e) => console.warn('immersive page labels failed', e));

            use(setupToc(viewer));
            use(setupSearchPanel(viewer, pi, apiBase));
            const fulltext = use(setupFulltextPanel(viewer, pi, apiBase, panels, keys));
            use(setupLayoutTagsPanel(viewer, pi, apiBase, panels, keys));
            const grid = use(setupOverviewGrid(viewer, pi, apiBase, keys));
            use(setupBottomBar(el, viewer, { toggleGrid: grid.toggleGrid, updateFulltextAvail: fulltext.updateFulltextAvail }));
            panels.replayOpen();
        })
        .catch((e) => {
            hideStageLoader();
            showStageError(el);
            console.error('immersive viewer init failed', e);
        });
    return handle;
}
