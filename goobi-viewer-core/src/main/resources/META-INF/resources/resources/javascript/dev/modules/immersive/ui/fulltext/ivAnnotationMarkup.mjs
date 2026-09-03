/** Joins ALTO annotations onto the rendered page text and its image overlays by element id. */

import { altoElementId, wordTypeClasses, rectCenterInside } from '../../data/ivAnnotationModel.mjs';

/**
 * Indexes the panel's region elements by ALTO element id (blocks, lines and
 * words alike), so layout annotations can be joined onto the rendered text.
 *
 * @param {HTMLElement} box  the fulltext panel content element
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 * @returns {Map<string, HTMLElement>}
 */
export function indexRegionElements(box, pi, pageNo) {
    const map = new Map();
    box.querySelectorAll('[data-iv-region-id]').forEach((el) => {
        const elementId = altoElementId(el.dataset.ivRegionId, pi, pageNo);
        if (elementId) map.set(elementId, el);
    });
    return map;
}

/**
 * Applies the word-level annotation markup (underline/strikethrough classes) to
 * the rendered panel text. The classes only become visible while the .immersive
 * root carries the state class, so toggling never re-renders. Block-level
 * annotations are NOT styled in place — every handwriting block becomes a card
 * in the annotations column (see {@link mountBlockAnnotations}).
 *
 * @param {HTMLElement} box  the fulltext panel content element
 * @param {{words:Array}} layout
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 * @returns {Map<string, HTMLElement>} the region element index — pass it on to
 *          {@link mountBlockAnnotations} to avoid a second DOM scan
 */
export function applyTextMarkup(box, layout, pi, pageNo) {
    const els = indexRegionElements(box, pi, pageNo);
    (layout.words || []).forEach((word) => {
        const el = els.get(word.id);
        if (el) wordTypeClasses(word.types).forEach((cls) => el.classList.add(cls));
    });
    return els;
}

/**
 * Mirrors the word-level markup onto the page's image overlays, joined by ALTO element id.
 * That join is exact — no geometry — so every marked word is covered. The overlays already
 * carry the text↔image link's hover state, which lets CSS alone reveal a marking while its
 * word is pointed at, from the scan or from the panel.
 *
 * @param {Map<string, HTMLElement>} regionEls  image overlay elements, keyed by annotation URI
 * @param {{words:Array}|null} layout
 * @param {string} pi
 * @param {number|string} pageNo  1-based page number
 */
export function applyRegionMarkup(regionEls, layout, pi, pageNo) {
    if (!regionEls || !layout) return;
    const byElementId = new Map();
    regionEls.forEach((el, uri) => {
        const elementId = altoElementId(uri, pi, pageNo);
        if (elementId) byElementId.set(elementId, el);
    });
    (layout.words || []).forEach((word) => {
        const el = byElementId.get(word.id);
        if (el) wordTypeClasses(word.types).forEach((cls) => el.classList.add(cls));
    });
}

/**
 * A block's reading-order source elements in the panel: the block element when
 * the panel carries a real block region id, otherwise (the nesting drops
 * degenerate block levels on some pages) the rendered lines whose geometry lies
 * inside the block.
 */
function sourceElsFor(block, els, lineEls) {
    const srcEl = els.get(block.id);
    if (srcEl) return [srcEl];
    return lineEls.filter((l) => rectCenterInside(l.rect, block.rect)).map((l) => l.el);
}

/**
 * Marks every annotation block's sources in the reading-order text: `anno-src`
 * hides them while annotations are off (the printed-text column shows print
 * only), `anno-margin-src` while on (their card in the column replaces them).
 *
 * @returns {Map<string, HTMLElement[]>} block id → source elements
 */
export function markAnnotationSources(annotationBlocks, els, lineEls) {
    const sources = new Map(annotationBlocks.map((block) => [block.id, sourceElsFor(block, els, lineEls)]));
    sources.forEach((blockSources) => {
        blockSources.forEach((el) => el.classList.add('immersive__anno-src', 'immersive__anno-margin-src'));
    });
    return sources;
}
