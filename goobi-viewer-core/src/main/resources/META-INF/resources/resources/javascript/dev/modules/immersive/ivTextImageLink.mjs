/** OCR text ↔ image region hover linking for the immersive fulltext panel (block > line > word). */

const ACTIVE_CLASS = 'is-linked-active';

/**
 * Indexes elements by their `data-iv-region-id` (elements without one are skipped).
 *
 * @param {Iterable<HTMLElement>} elements
 * @returns {Map<string, HTMLElement>}
 */
export function indexById(elements) {
    const map = new Map();
    for (const el of elements) {
        const id = el.dataset && el.dataset.ivRegionId;
        if (id) map.set(id, el);
    }
    return map;
}

/**
 * Computes the scrollTop needed to center a line within its scroll container,
 * or null if the line is already fully visible. Pure (numbers only) so it is
 * testable without a layout engine.
 *
 * @param {{offsetTop:number, height:number, scrollTop:number, clientHeight:number, scrollHeight:number}} metrics
 * @returns {number|null}
 */
export function scrollTopToReveal(metrics) {
    const top = metrics.offsetTop;
    const bottom = metrics.offsetTop + metrics.height;
    if (top >= metrics.scrollTop && bottom <= metrics.scrollTop + metrics.clientHeight) return null;
    const target = metrics.offsetTop - metrics.clientHeight / 2 + metrics.height / 2;
    const max = Math.max(0, metrics.scrollHeight - metrics.clientHeight);
    return Math.min(Math.max(0, target), max);
}

/**
 * Cumulative offsetTop of `el` relative to `container` (walks the offsetParent
 * chain), for scrolling panel content into view without moving the page.
 *
 * @param {HTMLElement} el
 * @param {HTMLElement} container
 * @returns {number}
 */
export function offsetTopWithin(el, container) {
    let top = 0;
    for (let n = el; n && n !== container; n = n.offsetParent) top += n.offsetTop;
    return top;
}

/**
 * Wires bidirectional hover highlighting between the panel text elements
 * (inside `box`) and the image region overlays (`regionEls`) that share the
 * same `data-iv-region-id`. Hover on either side toggles `is-linked-active` on
 * both. On the text side the DOM nesting (block > line > word) already puts
 * every hovered ancestor into its own mouseenter state, so each element only
 * toggles its own id; the flat image overlays cascade explicitly through the
 * `parents` chain instead. Deliberately pointer-only: the highlight is a
 * supplementary cue, the text itself stays fully readable without it.
 *
 * @param {object} opts
 * @param {HTMLElement} opts.box fulltext panel element containing the text elements
 * @param {Map<string, HTMLElement>} opts.regionEls image overlay elements, indexed by id
 * @param {Map<string, string>} [opts.parents] region id → parent region id; hovering
 *   an overlay also activates all its ancestors on both sides
 * @param {HTMLElement} [opts.scrollContainer] scrollable panel; when set, hovering an
 *   image overlay scrolls its text into view if not fully visible
 * @param {Set<string>} [opts.revealIds] overlay ids allowed to trigger that scroll;
 *   large regions (paragraphs) stay out so grazing them never jumps the panel
 * @returns {{destroy: function():void}}
 */
export function mountTextImageLink({ box, regionEls, parents, scrollContainer, revealIds }) {
    const spanEls = indexById(box.querySelectorAll('[data-iv-region-id]'));
    const regions = regionEls || new Map();
    const parentIds = parents || new Map();
    const listeners = [];

    const setActive = (id, on) => {
        const span = spanEls.get(id);
        const overlay = regions.get(id);
        if (span) span.classList.toggle(ACTIVE_CLASS, on);
        if (overlay) overlay.classList.toggle(ACTIVE_CLASS, on);
    };

    const chain = (id) => {
        const ids = [];
        for (let current = id; current != null && !ids.includes(current); current = parentIds.get(current)) {
            ids.push(current);
        }
        return ids;
    };

    const revealSpan = (id) => {
        if (!scrollContainer) return;
        const span = spanEls.get(id);
        if (!span) return;
        const target = scrollTopToReveal({
            offsetTop: offsetTopWithin(span, scrollContainer),
            height: span.offsetHeight,
            scrollTop: scrollContainer.scrollTop,
            clientHeight: scrollContainer.clientHeight,
            scrollHeight: scrollContainer.scrollHeight,
        });
        if (target !== null) scrollContainer.scrollTop = target;
    };

    const bind = (el, id, isOverlay) => {
        const ids = isOverlay ? chain(id) : [id];
        const enter = () => {
            ids.forEach((i) => setActive(i, true));
            if (isOverlay && (!revealIds || revealIds.has(id))) revealSpan(id);
        };
        const leave = () => ids.forEach((i) => setActive(i, false));
        el.addEventListener('mouseenter', enter);
        el.addEventListener('mouseleave', leave);
        listeners.push([el, enter, leave]);
    };

    spanEls.forEach((el, id) => bind(el, id, false));
    regions.forEach((el, id) => bind(el, id, true));

    return {
        destroy() {
            listeners.forEach(([el, enter, leave]) => {
                el.removeEventListener('mouseenter', enter);
                el.removeEventListener('mouseleave', leave);
            });
            listeners.length = 0;
        },
    };
}

/**
 * Builds the combined multi-level panel markup from nested OCR levels: one
 * `<div>` per text block containing one `<span>` per line, which in turn holds
 * one `<span>` per word (single spaces between words). Lines without word
 * geometry render their plain line text instead. Every real region carries
 * `data-iv-region-id`; synthetic blocks (id null) render as plain wrappers.
 * All text is set via `textContent`, so OCR content can never inject markup.
 *
 * @param {Array} blocks  nested levels, see ivManifestSource.nestTextLevels
 * @returns {DocumentFragment}
 */
export function buildTextLevels(blocks) {
    const frag = document.createDocumentFragment();
    (blocks || []).forEach((block) => {
        const blockEl = document.createElement('div');
        blockEl.className = 'immersive__fulltext-block';
        if (block.id && block.rect) blockEl.dataset.ivRegionId = block.id;
        block.lines.forEach((line) => {
            const lineEl = document.createElement('span');
            lineEl.className = 'immersive__fulltext-line';
            if (line.id && line.rect) lineEl.dataset.ivRegionId = line.id;
            if (line.words.length) {
                line.words.forEach((word, i) => {
                    if (i > 0) lineEl.appendChild(document.createTextNode(' '));
                    const wordEl = document.createElement('span');
                    wordEl.className = 'immersive__fulltext-word';
                    wordEl.dataset.ivRegionId = word.id;
                    wordEl.textContent = word.chars || '';
                    lineEl.appendChild(wordEl);
                });
            } else {
                lineEl.textContent = line.chars || '';
            }
            blockEl.appendChild(lineEl);
        });
        frag.appendChild(blockEl);
    });
    return frag;
}
