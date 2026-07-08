/** Fulltext panel wiring: combined block/line/word text with image hover linking. */

import { loadPageTextLevels, flattenTextLevels } from './ivManifestSource.mjs';
import { buildTextLevels, mountTextImageLink } from './ivTextImageLink.mjs';
import { currentOrder } from './viewerImmersive.mjs';

/**
 * Wires the fulltext panel: loads the nested OCR levels for the visible page,
 * renders them, and mounts the bidirectional text ↔ image hover linking. The
 * teardown runs through the panels' close hook so leaving the panel always
 * clears the image overlays.
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 * @param {{closePanels: Function, registerCloseHook: Function}} panels
 * @returns {{updateFulltextAvail: Function}}
 */
export function setupFulltextPanel(viewer, pi, apiBase, panels) {
    const fulltextPanel = document.getElementById('immersivePanelFulltext');
    const fulltextBox = document.getElementById('immersiveFulltext');
    const fulltextLoader = document.getElementById('immersiveFulltextLoader');
    const fulltextBtn = document.querySelector('[data-immersive-panel="immersivePanelFulltext"]');
    const fulltextTitleDefault = fulltextBtn ? fulltextBtn.getAttribute('title') : '';

    if (fulltextPanel && fulltextBox) {
        let fulltextReq = 0;
        let currentLink = null;

        const clearFulltextLink = () => {
            if (currentLink) {
                currentLink.destroy();
                currentLink = null;
            }
            viewer.clearTextRegions();
            if (fulltextLoader) fulltextLoader.hidden = true;
        };
        panels.registerCloseHook(clearFulltextLink);

        const loadFulltext = async () => {
            const order = currentOrder(viewer);
            const req = ++fulltextReq;
            clearFulltextLink();
            if (fulltextLoader) fulltextLoader.hidden = false;
            fulltextBox.textContent = '';
            fulltextBox.classList.remove('-empty');
            let blocks = null;
            try {
                blocks = await loadPageTextLevels(pi, apiBase, order);
            } catch {
                blocks = null;
            }
            if (req !== fulltextReq) return;
            if (fulltextLoader) fulltextLoader.hidden = true;
            if (blocks && blocks.length) {
                fulltextBox.replaceChildren(buildTextLevels(blocks));
                const flat = flattenTextLevels(blocks);
                const regionEls = viewer.setTextRegions(flat);
                const parents = new Map(flat.filter((r) => r.parentId).map((r) => [r.id, r.parentId]));
                const revealIds = new Set(flat.filter((r) => r.level !== 'block').map((r) => r.id));
                currentLink = mountTextImageLink({ box: fulltextBox, regionEls, parents, scrollContainer: fulltextPanel, revealIds });
            } else {
                fulltextBox.textContent = fulltextBox.dataset.labelEmpty || '';
                fulltextBox.classList.add('-empty');
            }
        };

        fulltextPanel.addEventListener('immersive:panel-open', loadFulltext);
        viewer.onPageChange.subscribe(() => {
            if (fulltextPanel.classList.contains('is-open')) loadFulltext();
        });
    }

    const updateFulltextAvail = () => {
        if (!fulltextBtn) return;
        const doublePage = viewer.isDoublePage();
        fulltextBtn.classList.toggle('-disabled', doublePage);
        fulltextBtn.setAttribute('aria-disabled', String(doublePage));
        fulltextBtn.setAttribute('tabindex', doublePage ? '-1' : '0');
        const title = (doublePage && fulltextBtn.dataset.labelDisabled) || fulltextTitleDefault;
        fulltextBtn.setAttribute('title', title);
        fulltextBtn.setAttribute('aria-label', title);
        if (doublePage && fulltextPanel && fulltextPanel.classList.contains('is-open')) panels.closePanels();
    };
    updateFulltextAvail();

    return { updateFulltextAvail };
}
