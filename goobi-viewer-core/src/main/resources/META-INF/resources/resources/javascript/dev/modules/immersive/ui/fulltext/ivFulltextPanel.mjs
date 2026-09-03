/** Fulltext panel wiring: combined block/line/word text with image hover linking and layout annotations. */

import { loadPageTextLevels, flattenTextLevels, loadPageLayout } from '../../data/ivManifestSource.mjs';
import { buildTextLevels, mountTextImageLink } from './ivTextImageLink.mjs';
import { setupAnnotationsToggle } from './ivAnnotations.mjs';
import { applyTextMarkup, applyRegionMarkup } from './ivAnnotationMarkup.mjs';
import { mountBlockAnnotations } from './ivAnnotationNotes.mjs';
import { overlayRegions } from '../../data/ivAnnotationModel.mjs';
import { createDisposer } from '../../shared/ivLifecycle.mjs';
import { localStore } from '../../shared/ivPrefs.mjs';

/**
 * Wires the fulltext panel: loads the nested OCR levels for the visible page,
 * renders them, and mounts the bidirectional text ↔ image hover linking. The
 * teardown runs through the panels' close hook so leaving the panel always
 * clears the image overlays.
 *
 * The ALTO layout annotations ride alongside. Annotations are only ever visible
 * while the fulltext panel is OPEN and the checkbox is on, so the layout JSON is
 * fetched, applied and overlaid strictly per open panel; closing the panel clears
 * everything, the persisted checkbox state kicks back in on the next open.
 *
 * @param {IvViewer} viewer
 * @param {string} pi
 * @param {string} apiBase
 * @param {{closePanels: Function, registerCloseHook: Function}} panels
 * @param {{register: Function}} [keys]  the immersive key dispatcher (annotations shortcut)
 * @returns {{updateFulltextAvail: Function, destroy: Function}}
 */
export function setupFulltextPanel(viewer, pi, apiBase, panels, keys) {
    const bag = createDisposer();
    const fulltextPanel = document.getElementById('immersivePanelFulltext');
    const fulltextBox = document.getElementById('immersiveFulltext');
    const fulltextLoader = document.getElementById('immersiveFulltextLoader');
    const fulltextBtn = document.querySelector('[data-immersive-panel="immersivePanelFulltext"]');
    const fulltextTitleDefault = fulltextBtn ? fulltextBtn.getAttribute('title') : '';

    const annoCheckbox = document.getElementById('immersiveAnnotationsToggle');
    const annoColumnsHead = document.getElementById('immersiveAnnotationsColumns');
    const immersiveRoot = fulltextPanel ? fulltextPanel.closest('.immersive') : null;
    const storage = localStore();
    const annotations = annoCheckbox && immersiveRoot ? setupAnnotationsToggle({ root: immersiveRoot, checkbox: annoCheckbox, storage, keys }) : null;
    if (annotations) bag.add(() => annotations.destroy());

    const panelOpen = () => !!fulltextPanel && fulltextPanel.classList.contains('is-open');

    let layoutReq = 0;
    let currentLayout = null;
    let currentFlat = null;
    let currentAnnotations = null;

    const clearBlockAnnotations = () => {
        if (currentAnnotations) {
            currentAnnotations.destroy();
            currentAnnotations = null;
        }
    };

    /** Applies inline markup + block annotations (margin column etc.) to the rendered panel text. */
    const decorate = () => {
        if (!annotations || !currentLayout || !panelOpen() || !fulltextBox || !fulltextBox.childElementCount) return;
        const pageNo = viewer.getCurrentOrder() + 1;
        const els = applyTextMarkup(fulltextBox, currentLayout, pi, pageNo);
        applyRegionMarkup(textRegionEls, currentLayout, pi, pageNo);
        clearBlockAnnotations();
        currentAnnotations = mountBlockAnnotations({
            box: fulltextBox,
            layout: currentLayout,
            pi,
            pageNo,
            flat: currentFlat,
            els,
            columnsHead: annoColumnsHead,
            scrollContainer: fulltextPanel,
            getRegionEls: () => overlayEls,
        });
        if (currentAnnotations) currentAnnotations.attachRegions(overlayEls, textRegionEls);
    };

    /**
     * Overlay boxes are viewport-positioned against the CURRENT OSD item, which only exists
     * once the (per-spread rebuilt) OpenSeadragon has settled — so they are (re)mounted on
     * every onOpen, not on page change; nothing is mounted while the panel is closed.
     */
    let overlayEls = new Map();
    let textRegionEls = new Map();
    const mountOverlays = () => {
        overlayEls = viewer.setAnnotationRegions(panelOpen() ? overlayRegions(currentLayout) : []);
        if (currentAnnotations) currentAnnotations.attachRegions(overlayEls, textRegionEls);
    };

    /**
     * Per-page layout annotations while the panel is open: checkbox availability,
     * image overlays and panel decoration. With the panel closed (or in double-page
     * mode, where the panel is unavailable) nothing is fetched and overlays clear.
     */
    const refreshLayout = async () => {
        if (!annotations) return;
        const req = ++layoutReq;
        currentLayout = null;
        if (!panelOpen() || viewer.isDoublePage()) {
            mountOverlays();
            return;
        }
        let layout = null;
        try {
            layout = await loadPageLayout(pi, apiBase, viewer.getCurrentOrder());
        } catch {
            layout = null;
        }
        if (req !== layoutReq) return;
        currentLayout = layout;
        annotations.setPageData(layout);
        mountOverlays();
        decorate();
    };

    if (annotations) {
        bag.add(viewer.onPageChange.subscribe(refreshLayout));
        bag.add(viewer.onOpen.subscribe(mountOverlays));
        bag.add(
            annotations.onChange((on) => {
                if (on && currentAnnotations) requestAnimationFrame(() => currentAnnotations && currentAnnotations.relayout());
            })
        );
    }

    if (fulltextPanel && fulltextBox) {
        let fulltextReq = 0;
        let currentLink = null;

        const clearFulltextLink = () => {
            fulltextReq++;
            if (currentLink) {
                currentLink.destroy();
                currentLink = null;
            }
            viewer.clearTextRegions();
            viewer.clearAnnotationRegions();
            textRegionEls = new Map();
            clearBlockAnnotations();
            if (fulltextLoader) fulltextLoader.hidden = true;
        };
        bag.add(panels.registerCloseHook(clearFulltextLink));
        bag.add(clearFulltextLink);

        const loadFulltext = async () => {
            const order = viewer.getCurrentOrder();
            clearFulltextLink();
            const req = ++fulltextReq;
            if (fulltextLoader) fulltextLoader.hidden = false;
            fulltextBox.textContent = '';
            fulltextBox.classList.remove('-empty', '-error');
            let blocks = null;
            let failed = false;
            try {
                blocks = await loadPageTextLevels(pi, apiBase, order);
            } catch {
                failed = true;
            }
            if (req !== fulltextReq) return;
            if (fulltextLoader) fulltextLoader.hidden = true;
            if (blocks && blocks.length) {
                fulltextBox.replaceChildren(buildTextLevels(blocks));
                const flat = flattenTextLevels(blocks);
                currentFlat = flat;
                const regionEls = viewer.setTextRegions(flat);
                textRegionEls = regionEls;
                const parents = new Map(flat.filter((r) => r.parentId).map((r) => [r.id, r.parentId]));
                const revealIds = new Set(flat.filter((r) => r.level !== 'block').map((r) => r.id));
                currentLink = mountTextImageLink({ box: fulltextBox, regionEls, parents, scrollContainer: fulltextPanel, revealIds });
                decorate();
            } else {
                currentFlat = null;
                fulltextBox.textContent = (failed ? fulltextBox.dataset.labelError : fulltextBox.dataset.labelEmpty) || '';
                fulltextBox.classList.add('-empty');
                fulltextBox.classList.toggle('-error', failed);
            }
        };

        bag.listen(fulltextPanel, 'immersive:panel-open', loadFulltext);
        bag.listen(fulltextPanel, 'immersive:panel-open', () => {
            if (annotations) refreshLayout();
        });
        bag.add(
            viewer.onPageChange.subscribe(() => {
                if (panelOpen()) loadFulltext();
            })
        );
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
        if (doublePage && panelOpen()) panels.closePanels();
        if (annotations) refreshLayout();
    };
    updateFulltextAvail();

    return { updateFulltextAvail, destroy: bag.dispose };
}
